package com.chessadvisor.service;

import com.chessadvisor.dto.request.ImportGameRequest;
import com.chessadvisor.dto.request.MoveRequest;
import com.chessadvisor.dto.request.NewGameRequest;
import com.chessadvisor.dto.response.AnalysisResponse;
import com.chessadvisor.dto.response.GameListResponse;
import com.chessadvisor.dto.response.GameResponse;
import com.chessadvisor.dto.response.MoveResponse;
import com.chessadvisor.engine.Board;
import com.chessadvisor.entity.Game;
import com.chessadvisor.entity.GameMove;
import com.chessadvisor.entity.GameStatus;
import com.chessadvisor.entity.Opening;
import com.chessadvisor.entity.User;
import com.chessadvisor.exception.BadRequestException;
import com.chessadvisor.exception.ResourceNotFoundException;
import com.chessadvisor.repository.GameMoveRepository;
import com.chessadvisor.repository.GameRepository;
import com.chessadvisor.repository.OpeningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private static final String STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final GameRepository gameRepository;
    private final GameMoveRepository gameMoveRepository;
    private final OpeningRepository openingRepository;
    private final ChessEngineService chessEngineService;

    @Transactional
    public GameResponse createGame(Long userId, NewGameRequest request) {
        log.info("Creating new game for user {} with color {}", userId, request.getPlayerColor());

        Game game = Game.builder()
                .user(User.builder().id(userId).build())
                .playerColor(request.getPlayerColor())
                .status(GameStatus.IN_PROGRESS)
                .pgn("")
                .moves(new ArrayList<>())
                .build();

        game = gameRepository.save(game);
        log.info("Game created with ID: {}", game.getId());

        return toGameResponse(game, STARTING_FEN);
    }

    @Transactional(readOnly = true)
    public GameResponse getGame(Long gameId, Long userId) {
        log.debug("Getting game {} for user {}", gameId, userId);

        Game game = findGameByIdAndUserId(gameId, userId);
        String currentFen = getCurrentFen(game);

        return toGameResponse(game, currentFen);
    }

    @Transactional(readOnly = true)
    public List<GameListResponse> getUserGames(Long userId) {
        log.debug("Getting games for user {}", userId);

        List<Game> games = gameRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return games.stream()
                .map(this::toGameListResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AnalysisResponse makeMove(Long gameId, Long userId, MoveRequest request) {
        log.info("Making move {} -> {} in game {} for user {}",
                request.getFrom(), request.getTo(), gameId, userId);

        Game game = findGameByIdAndUserId(gameId, userId);

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new BadRequestException("Game is not in progress");
        }

        String currentFen = getCurrentFen(game);

        // Validate and make the move
        Board newBoard = chessEngineService.makeMove(
                currentFen, request.getFrom(), request.getTo(), request.getPromotion());
        String newFen = chessEngineService.getFen(newBoard);

        // Get the SAN notation for the move
        String san = chessEngineService.getMoveNotation(
                currentFen, request.getFrom(), request.getTo(), request.getPromotion());

        // Determine move number and which side moved
        List<GameMove> existingMoves = game.getMoves();
        int lastMoveNumber = existingMoves.isEmpty() ? 0 :
                existingMoves.get(existingMoves.size() - 1).getMoveNumber();

        boolean isWhiteMove = currentFen.contains(" w ");

        GameMove gameMove;
        if (isWhiteMove) {
            // White's move: create a new move entry
            gameMove = GameMove.builder()
                    .game(game)
                    .moveNumber(lastMoveNumber + 1)
                    .whiteMove(san)
                    .fen(newFen)
                    .build();
            game.getMoves().add(gameMove);
        } else {
            // Black's move: update the last move entry
            if (!existingMoves.isEmpty()) {
                gameMove = existingMoves.get(existingMoves.size() - 1);
                gameMove.setBlackMove(san);
                gameMove.setFen(newFen);
            } else {
                // Edge case: black moves first (unusual, but handle gracefully)
                gameMove = GameMove.builder()
                        .game(game)
                        .moveNumber(1)
                        .blackMove(san)
                        .fen(newFen)
                        .build();
                game.getMoves().add(gameMove);
            }
        }

        // Update PGN
        game.setPgn(buildPgn(game.getMoves()));

        // Try to identify opening from move history
        identifyOpening(game);

        // Analyze the new position
        AnalysisResponse analysis = chessEngineService.analyzePosition(newFen);

        // Update game status if the game is over
        if (analysis.isCheckmate()) {
            game.setStatus(GameStatus.COMPLETED);
            game.setFinalEvaluation(isWhiteMove ? "1-0" : "0-1");
            log.info("Game {} ended by checkmate", gameId);
        } else if (analysis.isStalemate()) {
            game.setStatus(GameStatus.COMPLETED);
            game.setFinalEvaluation("1/2-1/2");
            log.info("Game {} ended by stalemate", gameId);
        }

        // Save evaluation on the move
        gameMove.setEvaluation(String.format("%.2f", analysis.getEvaluation()));

        gameRepository.save(game);

        return analysis;
    }

    @Transactional
    public GameResponse undoMove(Long gameId, Long userId) {
        log.info("Undoing last move in game {} for user {}", gameId, userId);

        Game game = findGameByIdAndUserId(gameId, userId);

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new BadRequestException("Game is not in progress");
        }

        List<GameMove> moves = game.getMoves();
        if (moves.isEmpty()) {
            throw new BadRequestException("No moves to undo");
        }

        GameMove lastMove = moves.get(moves.size() - 1);

        if (lastMove.getBlackMove() != null) {
            // Undo black's move: clear black move and restore FEN to after white's move
            lastMove.setBlackMove(null);
            // Restore FEN to the position after white's move
            if (moves.size() > 1) {
                GameMove previousMove = moves.get(moves.size() - 2);
                // We need to recalculate FEN after white's move
                // Use the previous complete move's FEN as the base, then replay white's move
                String baseFen = previousMove.getFen();
                Board board = Board.fromFen(baseFen);
                // The current lastMove's white move was made from the previous FEN
                // We need to store intermediate FEN or recalculate
                // For simplicity, replay from start
                lastMove.setFen(replayToGetFen(moves, true));
            } else {
                // Only one move entry, undo black's part
                lastMove.setFen(replayToGetFen(moves, true));
            }
            lastMove.setEvaluation(null);
        } else {
            // Undo white's move: remove the entire move entry
            moves.remove(moves.size() - 1);
            gameMoveRepository.delete(lastMove);
        }

        // Update PGN
        game.setPgn(buildPgn(game.getMoves()));

        gameRepository.save(game);

        String currentFen = getCurrentFen(game);
        return toGameResponse(game, currentFen);
    }

    @Transactional(readOnly = true)
    public String exportPgn(Long gameId, Long userId) {
        log.debug("Exporting PGN for game {} for user {}", gameId, userId);

        Game game = findGameByIdAndUserId(gameId, userId);
        return buildFullPgn(game);
    }

    @Transactional
    public GameResponse importGame(Long userId, ImportGameRequest request) {
        log.info("Importing game from PGN for user {}", userId);

        Game game = Game.builder()
                .user(User.builder().id(userId).build())
                .playerColor(com.chessadvisor.entity.PlayerColor.WHITE)
                .status(GameStatus.COMPLETED)
                .pgn(request.getPgn())
                .moves(new ArrayList<>())
                .build();

        // Parse PGN and replay moves
        String[] moveTokens = parsePgnMoves(request.getPgn());
        String currentFen = STARTING_FEN;
        int moveNumber = 0;
        GameMove currentGameMove = null;

        for (String token : moveTokens) {
            if (token.isEmpty() || token.matches("\\d+\\.+") ||
                    token.equals("1-0") || token.equals("0-1") || token.equals("1/2-1/2") || token.equals("*")) {
                if (token.equals("1-0") || token.equals("0-1") || token.equals("1/2-1/2")) {
                    game.setFinalEvaluation(token);
                }
                continue;
            }

            Board board = Board.fromFen(currentFen);
            boolean isWhiteMove = currentFen.contains(" w ");

            // Parse the SAN move using the engine
            com.chessadvisor.engine.Move engineMove =
                    com.chessadvisor.engine.Move.fromAlgebraic(token, board);
            Board newBoard = board.makeMove(engineMove);
            currentFen = chessEngineService.getFen(newBoard);

            if (isWhiteMove) {
                moveNumber++;
                currentGameMove = GameMove.builder()
                        .game(game)
                        .moveNumber(moveNumber)
                        .whiteMove(token)
                        .fen(currentFen)
                        .build();
                game.getMoves().add(currentGameMove);
            } else {
                if (currentGameMove != null) {
                    currentGameMove.setBlackMove(token);
                    currentGameMove.setFen(currentFen);
                }
            }
        }

        // Identify opening
        identifyOpening(game);

        game = gameRepository.save(game);
        log.info("Game imported with ID: {}", game.getId());

        return toGameResponse(game, currentFen);
    }

    @Transactional
    public void deleteGame(Long gameId, Long userId) {
        log.info("Deleting game {} for user {}", gameId, userId);

        Game game = findGameByIdAndUserId(gameId, userId);
        gameRepository.delete(game);

        log.info("Game {} deleted", gameId);
    }

    // --- Private helper methods ---

    private Game findGameByIdAndUserId(Long gameId, Long userId) {
        return gameRepository.findByIdAndUserId(gameId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Game not found with ID: " + gameId));
    }

    private String getCurrentFen(Game game) {
        List<GameMove> moves = game.getMoves();
        if (moves == null || moves.isEmpty()) {
            return STARTING_FEN;
        }
        GameMove lastMove = moves.get(moves.size() - 1);
        return lastMove.getFen() != null ? lastMove.getFen() : STARTING_FEN;
    }

    private String buildPgn(List<GameMove> moves) {
        StringBuilder pgn = new StringBuilder();
        for (GameMove move : moves) {
            if (pgn.length() > 0) {
                pgn.append(" ");
            }
            pgn.append(move.getMoveNumber()).append(".");
            if (move.getWhiteMove() != null) {
                pgn.append(" ").append(move.getWhiteMove());
            }
            if (move.getBlackMove() != null) {
                pgn.append(" ").append(move.getBlackMove());
            }
        }
        return pgn.toString();
    }

    private String buildFullPgn(Game game) {
        StringBuilder pgn = new StringBuilder();
        pgn.append("[Event \"Chess Advisor Game\"]\n");
        pgn.append("[Site \"Chess Advisor\"]\n");
        pgn.append("[Date \"").append(game.getCreatedAt().toLocalDate()).append("\"]\n");
        pgn.append("[White \"").append(
                game.getPlayerColor() == com.chessadvisor.entity.PlayerColor.WHITE ? "Player" : "Engine")
                .append("\"]\n");
        pgn.append("[Black \"").append(
                game.getPlayerColor() == com.chessadvisor.entity.PlayerColor.BLACK ? "Player" : "Engine")
                .append("\"]\n");

        String result = game.getFinalEvaluation() != null ? game.getFinalEvaluation() : "*";
        pgn.append("[Result \"").append(result).append("\"]\n");

        if (game.getOpeningName() != null) {
            pgn.append("[Opening \"").append(game.getOpeningName()).append("\"]\n");
        }
        pgn.append("\n");

        pgn.append(buildPgn(game.getMoves()));
        if (!game.getMoves().isEmpty()) {
            pgn.append(" ");
        }
        pgn.append(result);

        return pgn.toString();
    }

    private String[] parsePgnMoves(String pgn) {
        // Strip PGN headers (lines starting with '[')
        String moveText = pgn.replaceAll("\\[.*?\\]", "").trim();
        // Strip comments
        moveText = moveText.replaceAll("\\{.*?\\}", "");
        // Strip variations
        moveText = moveText.replaceAll("\\(.*?\\)", "");
        // Normalize whitespace
        moveText = moveText.replaceAll("\\s+", " ").trim();
        // Split into tokens
        return moveText.split("\\s+");
    }

    private void identifyOpening(Game game) {
        String moveSequence = buildPgn(game.getMoves());
        if (moveSequence.isEmpty()) {
            return;
        }

        List<Opening> matchingOpenings = openingRepository.findBestMatchingOpening(moveSequence);
        if (!matchingOpenings.isEmpty()) {
            game.setOpeningName(matchingOpenings.get(0).getName());
        }
    }

    /**
     * Replay moves from the beginning to get the FEN after a specific point.
     * If whiteOnly is true and we're on the last move, only replay white's move.
     */
    private String replayToGetFen(List<GameMove> moves, boolean whiteOnlyOnLast) {
        String fen = STARTING_FEN;
        for (int i = 0; i < moves.size(); i++) {
            GameMove gm = moves.get(i);
            boolean isLast = (i == moves.size() - 1);

            if (gm.getWhiteMove() != null) {
                Board board = Board.fromFen(fen);
                com.chessadvisor.engine.Move whiteMove =
                        com.chessadvisor.engine.Move.fromAlgebraic(gm.getWhiteMove(), board);
                Board afterWhite = board.makeMove(whiteMove);
                fen = chessEngineService.getFen(afterWhite);
            }

            if (gm.getBlackMove() != null && !(isLast && whiteOnlyOnLast)) {
                Board board = Board.fromFen(fen);
                com.chessadvisor.engine.Move blackMove =
                        com.chessadvisor.engine.Move.fromAlgebraic(gm.getBlackMove(), board);
                Board afterBlack = board.makeMove(blackMove);
                fen = chessEngineService.getFen(afterBlack);
            }
        }
        return fen;
    }

    private GameResponse toGameResponse(Game game, String currentFen) {
        List<MoveResponse> moveResponses = game.getMoves().stream()
                .map(m -> MoveResponse.builder()
                        .moveNumber(m.getMoveNumber())
                        .whiteMove(m.getWhiteMove())
                        .blackMove(m.getBlackMove())
                        .fen(m.getFen())
                        .evaluation(m.getEvaluation())
                        .build())
                .collect(Collectors.toList());

        return GameResponse.builder()
                .id(game.getId())
                .playerColor(game.getPlayerColor().name())
                .status(game.getStatus().name())
                .pgn(game.getPgn())
                .openingName(game.getOpeningName())
                .currentFen(currentFen)
                .moves(moveResponses)
                .createdAt(game.getCreatedAt() != null ?
                        game.getCreatedAt().format(DATE_FORMATTER) : null)
                .build();
    }

    private GameListResponse toGameListResponse(Game game) {
        int totalMoves = 0;
        if (game.getMoves() != null) {
            for (GameMove m : game.getMoves()) {
                if (m.getWhiteMove() != null) totalMoves++;
                if (m.getBlackMove() != null) totalMoves++;
            }
        }

        return GameListResponse.builder()
                .id(game.getId())
                .playerColor(game.getPlayerColor().name())
                .status(game.getStatus().name())
                .openingName(game.getOpeningName())
                .totalMoves(totalMoves)
                .createdAt(game.getCreatedAt() != null ?
                        game.getCreatedAt().format(DATE_FORMATTER) : null)
                .build();
    }
}
