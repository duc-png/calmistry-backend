package com.example.demo.dto.request;

import lombok.Data;

@Data
public class SubmitFuiedsRequest {

    // Raw answers (0-4 scale) for each component
    private Integer feelingsAnswer; // F - Cảm xúc
    private Integer understandingAnswer; // U - Tự nhận thức
    private Integer interactionAnswer; // I - Xã hội
    private Integer energyAnswer; // E - Năng lượng
    private Integer driveAnswer; // D - Động lực
    private Integer stabilityAnswer; // S - Ổn định cảm xúc
}
