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
public class AnalysisResponse {
    private String currentFen;
    private double evaluation;
    private String openingName;
    private List<RecommendedMove> recommendedMoves;
    private boolean isCheck;
    private boolean isCheckmate;
    private boolean isStalemate;
}
