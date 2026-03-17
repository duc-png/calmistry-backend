package com.example.demo.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailService {

    Optional<JavaMailSender> mailSender;

    public void sendOtpEmail(String toEmail, String otp, int minutesValid) {
        String subject = "Calmistry - Mã OTP đặt lại mật khẩu";
        String body = "Bạn vừa yêu cầu đặt lại mật khẩu Calmistry.\n\n"
                + "Mã OTP của bạn là: " + otp + "\n"
                + "Mã có hiệu lực trong " + minutesValid + " phút.\n\n"
                + "Nếu bạn không yêu cầu, hãy bỏ qua email này.";

        if (mailSender.isEmpty()) {
            log.warn("JavaMailSender not configured. OTP for {} is {}", toEmail, otp);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        mailSender.get().send(message);
    }
}

