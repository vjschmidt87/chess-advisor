package com.chessadvisor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameListResponse {
    private Long id;
    private String playerColor;
    private String status;
    private String openingName;
    private int totalMoves;
    private String createdAt;
}
