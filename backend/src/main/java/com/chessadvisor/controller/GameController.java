package com.chessadvisor.controller;

import com.chessadvisor.dto.request.ImportGameRequest;
import com.chessadvisor.dto.request.MoveRequest;
import com.chessadvisor.dto.request.NewGameRequest;
import com.chessadvisor.dto.response.AnalysisResponse;
import com.chessadvisor.dto.response.GameListResponse;
import com.chessadvisor.dto.response.GameResponse;
import com.chessadvisor.exception.ResourceNotFoundException;
import com.chessadvisor.repository.UserRepository;
import com.chessadvisor.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
@Slf4j
public class GameController {

    private final GameService gameService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<GameResponse> createGame(
            @Valid @RequestBody NewGameRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        log.info("Creating new game for user {}", userId);
        GameResponse response = gameService.createGame(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<GameListResponse>> getUserGames(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        log.debug("Fetching games for user {}", userId);
        List<GameListResponse> games = gameService.getUserGames(userId);
        return ResponseEntity.ok(games);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameResponse> getGame(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        log.debug("Fetching game {} for user {}", id, userId);
        GameResponse response = gameService.getGame(id, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/move")
    public ResponseEntity<AnalysisResponse> makeMove(
            @PathVariable Long id,
            @Valid @RequestBody MoveRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        log.info("Making move in game {} for user {}: {} -> {}", id, userId,
                request.getFrom(), request.getTo());
        AnalysisResponse response = gameService.makeMove(id, userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/undo")
    public ResponseEntity<GameResponse> undoMove(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        log.info("Undoing move in game {} for user {}", id, userId);
        GameResponse response = gameService.undoMove(id, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<String> exportPgn(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        log.debug("Exporting PGN for game {} for user {}", id, userId);
        String pgn = gameService.exportPgn(id, userId);
        return ResponseEntity.ok(pgn);
    }

    @PostMapping("/import")
    public ResponseEntity<GameResponse> importGame(
            @Valid @RequestBody ImportGameRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        log.info("Importing game from PGN for user {}", userId);
        GameResponse response = gameService.importGame(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        log.info("Deleting game {} for user {}", id, userId);
        gameService.deleteGame(id, userId);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }
}
