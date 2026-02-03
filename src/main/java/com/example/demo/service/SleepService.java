package com.example.demo.service;

import com.example.demo.dto.request.AnswerDTO;
import com.example.demo.dto.request.SubmitSleepQuizRequest;
import com.example.demo.dto.response.SleepHistoryResponse;
import com.example.demo.dto.response.SleepSessionResponse;
import com.example.demo.entity.*;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.SleepAnswerRepository;
import com.example.demo.repository.SleepScoreRepository;
import com.example.demo.repository.SleepSessionRepository;
import com.example.demo.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SleepService {

    SleepSessionRepository sleepSessionRepository;
    SleepAnswerRepository sleepAnswerRepository;
    SleepScoreRepository sleepScoreRepository;
    UserRepository userRepository;

    /**
     * Submit sleep quiz and calculate score
     */
    @Transactional
    public SleepSessionResponse submitSleepQuiz(SubmitSleepQuizRequest request) {
        // Get authenticated user
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Check if session already exists for this date
        if (sleepSessionRepository.existsByUserIdAndRecordDate(user.getId(), request.getRecordDate())) {
            throw new AppException(ErrorCode.SLEEP_SESSION_ALREADY_EXISTS);
        }

        // Create session
        SleepSession session = new SleepSession();
        session.setUser(user);
        session.setRecordDate(request.getRecordDate());
        session = sleepSessionRepository.save(session);

        // Save answers
        for (AnswerDTO answerDTO : request.getAnswers()) {
            SleepAnswer answer = new SleepAnswer();
            answer.setSession(session);
            answer.setQuestionCode(answerDTO.getQuestionCode());
            answer.setAnswerValue(answerDTO.getAnswerValue());
            sleepAnswerRepository.save(answer);
        }

        // Calculate and save score
        SleepScore score = calculateScore(session, request.getAnswers());
        score.setSession(session);
        score = sleepScoreRepository.save(score);

        // Build response
        return buildSessionResponse(session, score, request.getAnswers());
    }

    /**
     * Get user's sleep history
     */
    public SleepHistoryResponse getSleepHistory(int page, int size) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Pageable pageable = PageRequest.of(page, size);
        Page<SleepSession> sessionsPage = sleepSessionRepository.findByUserIdOrderByRecordDateDesc(user.getId(),
                pageable);

        List<SleepHistoryResponse.SleepHistoryItem> items = sessionsPage.getContent().stream()
                .map(session -> {
                    SleepScore score = sleepScoreRepository.findBySessionId(session.getId()).orElse(null);
                    return SleepHistoryResponse.SleepHistoryItem.builder()
                            .sessionId(session.getId())
                            .recordDate(session.getRecordDate())
                            .finalScore100(score != null ? score.getFinalScore100() : 0)
                            .status(score != null && score.getStatus() != null ? score.getStatus().name() : "UNKNOWN")
                            .categoryTitle(score != null ? score.getCategoryTitle() : "Chưa phân loại")
                            .build();
                })
                .collect(Collectors.toList());

        // Calculate average score
        double averageScore = items.stream()
                .mapToInt(SleepHistoryResponse.SleepHistoryItem::getFinalScore100)
                .average()
                .orElse(0.0);

        return SleepHistoryResponse.builder()
                .sessions(items)
                .totalSessions((int) sessionsPage.getTotalElements())
                .averageScore(averageScore)
                .build();
    }

    /**
     * Get latest sleep session
     */
    public SleepSessionResponse getLatestSleepSession() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        SleepSession session = sleepSessionRepository.findTopByUserIdOrderByRecordDateDesc(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION)); // No sessions found

        SleepScore score = sleepScoreRepository.findBySessionId(session.getId()).orElse(null);
        List<SleepAnswer> answers = sleepAnswerRepository.findBySessionId(session.getId());

        List<AnswerDTO> answerDTOs = answers.stream()
                .map(a -> new AnswerDTO(a.getQuestionCode(), a.getAnswerValue()))
                .collect(Collectors.toList());

        return buildSessionResponse(session, score, answerDTOs);
    }

    private SleepScore calculateScore(SleepSession session, List<AnswerDTO> answers) {
        SleepScore score = new SleepScore();

        // Calculate total points
        int totalPoints = 0;
        for (AnswerDTO answer : answers) {
            try {
                totalPoints += Integer.parseInt(answer.getAnswerValue());
            } catch (NumberFormatException e) {
                log.warn("Could not parse answer value: {}", answer.getAnswerValue());
            }
        }

        int finalScore = Math.min(100, totalPoints);
        score.setFinalScore100(finalScore);

        // Determine status based on total points
        if (finalScore >= 85)
            score.setStatus(SleepScore.SleepStatus.EXCELLENT);
        else if (finalScore >= 70)
            score.setStatus(SleepScore.SleepStatus.GOOD);
        else if (finalScore >= 50)
            score.setStatus(SleepScore.SleepStatus.FAIR);
        else
            score.setStatus(SleepScore.SleepStatus.POOR);

        // PSQI and Efficiency (Legacy/Compatibility)
        score.setPsqiScore(Math.max(0, Math.min(21, (100 - finalScore) / 5)));
        score.setSleepEfficiencyPercent(BigDecimal.valueOf(finalScore * 0.88).setScale(2, RoundingMode.HALF_UP));

        // Determine specialized result category
        determineResultCategory(score, answers);

        return score;
    }

    private String getValue(String questionCode, List<AnswerDTO> answers) {
        for (AnswerDTO answer : answers) {
            if (answer.getQuestionCode().equals(questionCode)) {
                try {
                    int points = Integer.parseInt(answer.getAnswerValue());
                    if (points >= 10)
                        return "A";
                    if (points >= 7)
                        return "B";
                    if (points >= 4)
                        return "C";
                    return "D";
                } catch (Exception e) {
                    return "A";
                }
            }
        }
        return "A";
    }

    private void determineResultCategory(SleepScore score, List<AnswerDTO> answers) {
        String q1 = getValue("Q1_DURATION", answers);
        String q2 = getValue("Q2_LATENCY", answers);
        String q3 = getValue("Q3_WAKE_FREQ", answers);
        String q4 = getValue("Q4_QUALITY", answers);
        String q5 = getValue("Q5_DAYTIME", answers);
        String q7 = getValue("Q7_ENV", answers);
        String q8 = getValue("Q8_THOUGHTS", answers);
        String q9 = getValue("Q9_MORNING", answers);

        long countC = entriesByPoint(answers, 4);
        long countD = entriesByPoint(answers, 1);
        long countAB = entriesByPoint(answers, 7) + entriesByPoint(answers, 10);

        java.util.Random rand = new java.util.Random();

        // KQ7 - Risk (High Priority)
        if ((countC + countD) >= 5 || (q4.equals("D") && q5.equals("D"))) {
            String[] advices = {
                    "Tình trạng này kéo dài có thể bào mòn sức lực của bạn. Hãy ưu tiên bản thân hơn, thử dành 1 ngày nghỉ ngơi hoàn toàn và cân nhắc trò chuyện với chuyên gia tâm lý để gỡ rối những áp lực đang đè nặng. Bạn không cần phải đối mặt với chuyện này một mình đâu.",
                    "Giấc ngủ đang gửi cho bạn một lời cầu cứu khẩn thiết. Đừng ép bản thân quá mức nữa. Hãy thử thiết lập một 'giờ giới nghiêm' cho mọi công việc và thiết bị điện tử, dành ít nhất 1 tiếng trước ngủ để hoàn toàn tĩnh lặng. Nếu mệt mỏi quá, việc tìm kiếm sự trợ giúp y khoa là quyết định dũng cảm và sáng suốt nhất."
            };
            setCategory(score, "KQ7", "Nguy cơ rối loạn giấc ngủ",
                    "Giấc ngủ đang ảnh hưởng sâu sắc đến sinh hoạt của bạn, khiến cơ thể luôn trong trạng thái cạn kiệt năng lượng.",
                    advices[rand.nextInt(advices.length)]);
            return;
        }

        // KQ6 - Stress
        if (q8.equals("D") && (q5.equals("C") || q5.equals("D")) && (q9.equals("C") || q9.equals("D"))) {
            String[] advices = {
                    "Những áp lực ban ngày đang len lỏi vào cả giấc mơ của bạn. Hãy thử bài tập 'Trút bỏ lo âu': Viết hết những gì đang nghĩ ra giấy trước khi lên giường để 'gửi' chúng lại đó. Khi nằm xuống, hãy chỉ tập trung vào nhịp thở nhẹ nhàng và nhắc nhở mình rằng tối nay là dành riêng cho việc nghỉ ngơi.",
                    "Đừng để những lo lắng của ngày mai đánh cắp giấc ngủ của đêm nay. Hãy thử phương pháp thư giãn cơ bắp (PMR) - gồng rồi thả lỏng từng phần cơ thể. Điều này giúp não bộ nhận tín hiệu rằng cơ thể đã an toàn để nghỉ ngơi. Bạn đã vất vả cả ngày rồi, xứng đáng được ngủ ngon."
            };
            setCategory(score, "KQ6", "Căng thẳng xâm chiếm giấc ngủ",
                    "Tâm trí bạn dường như chưa được 'tắt nguồn' hẳn khi lên giường, dẫn đến cảm giác nặng nề mỗi khi thức dậy.",
                    advices[rand.nextInt(advices.length)]);
            return;
        }

        // KQ2 - Sleep Quantity
        if ((q1.equals("C") || q1.equals("D")) && countAB >= 6 && (q2.equals("A") || q2.equals("B"))
                && (q3.equals("A") || q3.equals("B"))) {
            String[] advices = {
                    "Bạn có khả năng ngủ rất tốt, chỉ là thời gian dành cho nó đang bị cắt xén. Thử đặt báo thức để 'đi ngủ' thay vì chỉ báo thức để 'thức dậy'. Hãy coi giấc ngủ là một cuộc hẹn quan trọng với chính sức khỏe của mình mà bạn không nên lỡ hẹn.",
                    "Cơ thể bạn vẫn đang hợp tác tốt, nhưng nó cần thêm thời gian để phục hồi. Hãy thử bớt đi 15-30 phút lướt điện thoại mỗi tối để bù vào thời gian ngủ. Bạn sẽ thấy sáng mai mọi thứ tươi sáng hơn hẳn khi được sạc đầy năng lượng."
            };
            setCategory(score, "KQ2", "Thời lượng ngủ chưa đủ",
                    "Bạn có một nền tảng giấc ngủ tốt, nhưng cuộc sống bận rộn đang khiến thời gian nghỉ ngơi bị rút ngắn.",
                    advices[rand.nextInt(advices.length)]);
            return;
        }

        // KQ3 - Latency
        if ((q2.equals("C") || q2.equals("D")) && (q8.equals("C") || q8.equals("D"))) {
            String[] advices = {
                    "Não bộ của bạn dường như đang chạy quá tốc độ vào ban đêm. Thử tạo một 'khoảng lặng tâm hồn' 30 phút trước khi ngủ: không màn hình, không công việc, chỉ có ánh sáng dịu và những bản nhạc không lời. Hãy để tâm trí từ từ hạ cánh an toàn trước khi vào giấc.",
                    "Overthinking là kẻ thù của cơn buồn ngủ. Thay vì cố ép mình phải ngủ, hãy thử bài tập thở 4-7-8 (Hít vào 4 giây, giữ 7 giây, thở ra 8 giây). Cách này giúp làm dịu hệ thần kinh và đưa bạn vào trạng thái thả lỏng tự nhiên nhất."
            };
            setCategory(score, "KQ3", "Khó đi vào giấc ngủ",
                    "Cơ thể đã sẵn sàng nhưng tâm trí vẫn còn đang trăn trở nhiều điều, khiến cơn buồn ngủ đến chậm hơn mong muốn.",
                    advices[rand.nextInt(advices.length)]);
            return;
        }

        // KQ4 - Shallow sleep
        if ((q3.equals("C") || q3.equals("D")) || (q7.equals("C") || q7.equals("D"))) {
            String[] advices = {
                    "Giấc ngủ của bạn giống như một mặt hồ dễ bị lay động. Hãy kiểm tra lại không gian ngủ: rèm có đủ tối không, nhiệt độ có mát mẻ không? Một chút tinh dầu hoa oải hương hoặc tiếng ồn trắng (tiếng mưa, tiếng sóng) có thể giúp bạn duy trì trạng thái ngủ sâu lâu hơn.",
                    "Sự gián đoạn giữa đêm làm giấc ngủ mất đi giá trị hồi phục. Hãy hạn chế uống nước sau 8h tối và đảm bảo phòng ngủ hoàn toàn yên tĩnh. Nếu tỉnh giấc, đừng nhìn vào đồng hồ hay điện thoại, hãy cứ nhắm mắt và thở sâu để quay lại giấc ngủ nhanh nhất."
            };
            setCategory(score, "KQ4", "Giấc ngủ chập chờn, không sâu",
                    "Bạn ngủ chưa đủ sâu để cơ thể thực sự tái tạo năng lượng, dễ bị đánh thức bởi những yếu tố xung quanh.",
                    advices[rand.nextInt(advices.length)]);
            return;
        }

        // KQ5 - Unrefreshing
        if ((q1.equals("A") || q1.equals("B")) && (q4.equals("C") || q4.equals("D"))
                && (q5.equals("C") || q5.equals("D") || q9.equals("C") || q9.equals("D"))) {
            String[] advices = {
                    "Ngủ đủ tiếng thôi là chưa đủ, chất lượng giấc ngủ mới là chìa khóa. Có thể bạn đang thiếu các giai đoạn ngủ sâu hoặc ngủ mơ (REM). Hãy thử tập thể dục nhẹ nhàng vào buổi chiều và tránh ăn quá no sát giờ ngủ để cơ thể không phải vất vả tiêu hóa khi đang nghỉ.",
                    "Dù nằm đủ lâu nhưng giấc ngủ chưa mang lại cảm giác 'sạc pin'. Đã đến lúc xem xét lại thói quen sinh hoạt: hãy thử đi ngủ và thức dậy vào một giờ cố định kể cả cuối tuần. Sự ổn định sẽ giúp nhịp sinh học của bạn hoạt động hiệu quả hơn."
            };
            setCategory(score, "KQ5", "Ngủ đủ nhưng vẫn mệt mỏi",
                    "Dù ngủ đạt số giờ tiêu chuẩn, nhưng bạn vẫn cảm thấy thiếu sức sống và khó tập trung khi thức dậy.",
                    advices[rand.nextInt(advices.length)]);
            return;
        }

        // KQ8 - Habits
        if ((q8.equals("B") || q8.equals("C")) && (q9.equals("B") || q9.equals("C"))) {
            String[] advices = {
                    "Một vài thay đổi nhỏ trong nghi thức trước khi ngủ sẽ mang lại hiệu quả bất ngờ. Thử đọc một cuốn sách giấy hoặc ngâm chân nước ấm thay vì cầm điện thoại. Những hành động lặp đi lặp lại này sẽ báo hiệu cho não bộ rằng đã đến lúc 'ngắt kết nối' với thế giới bên ngoài.",
                    "Giấc ngủ của bạn đang ở mức khá, nhưng có thể tuyệt vời hơn nếu bạn chăm sóc nó kỹ hơn. Hãy biến phòng ngủ thành một thánh đường của sự nghỉ ngơi: gọn gàng, thơm mát và không có công việc. Bạn sẽ thấy mình yêu việc đi ngủ hơn bao giờ hết."
            };
            setCategory(score, "KQ8", "Thói quen ngủ chưa tối ưu",
                    "Giấc ngủ của bạn ổn định nhưng vẫn có thể được nâng cấp để mang lại cảm giác sảng khoái hơn mỗi sáng.",
                    advices[rand.nextInt(advices.length)]);
            return;
        }

        // KQ1 - Stable (Default)
        String[] advices = {
                "Bạn đang làm rất tốt việc gìn giữ 'kho báu' giấc ngủ của mình. Hãy duy trì sự cân bằng này nhé. Khi cơ thể được nghỉ ngơi đầy đủ, bạn sẽ có đủ sức mạnh để đối mặt với mọi thử thách của cuộc sống một cách bình thản nhất.",
                "Giấc ngủ của bạn thực sự là một tấm gương cho lối sống lành mạnh. Hãy tiếp tục lắng nghe cơ thể mình. Đừng quên dành cho mình những lời khen ngợi vì đã yêu thương bản thân đúng cách thông qua từng giấc ngủ ngon mỗi tối."
        };
        setCategory(score, "KQ1", "Giấc ngủ ổn định & lành mạnh",
                "Bạn đang sở hữu một giấc ngủ rất tuyệt vời, cơ thể và tâm trí dường như đang hòa quyện nhịp nhàng.",
                advices[rand.nextInt(advices.length)]);
    }

    private long entriesByPoint(List<AnswerDTO> answers, int point) {
        return answers.stream().filter(a -> {
            try {
                return Integer.parseInt(a.getAnswerValue()) == point;
            } catch (Exception e) {
                return false;
            }
        }).count();
    }

    private void setCategory(SleepScore score, String code, String title, String desc, String advice) {
        score.setCategoryCode(code);
        score.setCategoryTitle(title);
        score.setDescription(desc);
        score.setAdvice(advice);
    }

    private SleepSessionResponse buildSessionResponse(SleepSession session, SleepScore score, List<AnswerDTO> answers) {
        List<SleepSessionResponse.AnswerResponse> answerResponses = answers.stream()
                .map(a -> SleepSessionResponse.AnswerResponse.builder()
                        .questionCode(a.getQuestionCode())
                        .answerValue(a.getAnswerValue())
                        .build())
                .collect(Collectors.toList());

        return SleepSessionResponse.builder()
                .sessionId(session.getId())
                .recordDate(session.getRecordDate())
                .createdAt(session.getCreatedAt())
                .psqiScore(score != null ? score.getPsqiScore() : null)
                .sleepEfficiencyPercent(score != null ? score.getSleepEfficiencyPercent() : null)
                .finalScore100(score != null ? score.getFinalScore100() : 0)
                .status(score != null && score.getStatus() != null ? score.getStatus().name() : "UNKNOWN")
                .categoryTitle(score != null ? score.getCategoryTitle() : null)
                .description(score != null ? score.getDescription() : null)
                .advice(score != null ? score.getAdvice() : null)
                .answers(answerResponses)
                .build();
    }
}
