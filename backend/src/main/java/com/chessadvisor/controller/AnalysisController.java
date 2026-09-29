package com.chessadvisor.controller;

import com.chessadvisor.dto.request.AnalysisRequest;
import com.chessadvisor.dto.request.LegalMovesRequest;
import com.chessadvisor.dto.response.AnalysisResponse;
import com.chessadvisor.service.ChessEngineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Slf4j
public class AnalysisController {

    private final ChessEngineService chessEngineService;

    @PostMapping("/position")
    public ResponseEntity<AnalysisResponse> analyzePosition(
            @Valid @RequestBody AnalysisRequest request) {
        log.info("Analyzing position: {}", request.getFen());
        AnalysisResponse response = chessEngineService.analyzePosition(request.getFen());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/legal-moves")
    public ResponseEntity<List<String>> getLegalMoves(
            @Valid @RequestBody LegalMovesRequest request) {
        log.debug("Getting legal moves for square {} in position: {}",
                request.getSquare(), request.getFen());
        List<String> moves = chessEngineService.getLegalMoves(
                request.getFen(), request.getSquare());
        return ResponseEntity.ok(moves);
    }
}
