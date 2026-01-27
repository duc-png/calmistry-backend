package com.example.demo.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmitSleepQuizRequest {

    @NotNull(message = "Record date is required")
    LocalDate recordDate;

    @NotEmpty(message = "Answers cannot be empty")
    @Valid
    List<AnswerDTO> answers;
}
