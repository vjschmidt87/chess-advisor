package com.chessadvisor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameResponse {
    private Long id;
    private String playerColor;
    private String status;
    private String pgn;
    private String openingName;
    private String currentFen;
    private List<MoveResponse> moves;
    private String createdAt;
}
