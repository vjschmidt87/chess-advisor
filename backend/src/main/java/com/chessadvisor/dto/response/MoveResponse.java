package com.chessadvisor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveResponse {
    private int moveNumber;
    private String whiteMove;
    private String blackMove;
    private String fen;
    private String evaluation;
}
