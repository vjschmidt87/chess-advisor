package com.chessadvisor.service;

import com.chessadvisor.dto.response.AnalysisResponse;
import com.chessadvisor.dto.response.RecommendedMove;
import com.chessadvisor.engine.Board;
import com.chessadvisor.engine.Evaluator;
import com.chessadvisor.engine.Move;
import com.chessadvisor.engine.MoveExplainer;
import com.chessadvisor.engine.MoveGenerator;
import com.chessadvisor.engine.OpeningBook;
import com.chessadvisor.engine.SearchEngine;
import com.chessadvisor.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChessEngineService {

    private final MoveGenerator moveGenerator;
    private final SearchEngine searchEngine;
    private final Evaluator evaluator;
    private final MoveExplainer moveExplainer;
    private final OpeningBook openingBook;

    public ChessEngineService() {
        this.moveGenerator = new MoveGenerator();
        this.searchEngine = new SearchEngine();
        this.evaluator = new Evaluator();
        this.moveExplainer = new MoveExplainer();
        this.openingBook = new OpeningBook();
    }

    /**
     * Analyze a chess position given a FEN string.
     * Returns evaluation, top 3 recommended moves, and game state flags.
     */
    public AnalysisResponse analyzePosition(String fen) {
        log.debug("Analyzing position: {}", fen);

        Board board = Board.fromFen(fen);
        int evalCentipawns = evaluator.evaluate(board);
        double evaluation = evalCentipawns / 100.0;
        List<Move> legalMoves = moveGenerator.generateLegalMoves(board);

        boolean isWhite = board.isWhiteToMove();
        boolean inCheck = moveGenerator.isInCheck(board, isWhite);
        boolean isCheckmate = inCheck && legalMoves.isEmpty();
        boolean isStalemate = !inCheck && legalMoves.isEmpty();

        // Find top 3 recommended moves
        List<RecommendedMove> recommendedMoves = new ArrayList<>();
        if (!legalMoves.isEmpty()) {
            List<SearchEngine.MoveEvaluation> topMoves = searchEngine.findBestMoves(board, 3);
            for (SearchEngine.MoveEvaluation moveEval : topMoves) {
                Move move = moveEval.getMove();
                double moveScore = moveEval.getScore() / 100.0;
                String san = move.toAlgebraicNotation(board);
                String explanation = moveExplainer.analyzeMove(board, move);

                recommendedMoves.add(RecommendedMove.builder()
                        .move(san)
                        .score(moveScore)
                        .explanation(explanation)
                        .build());
            }
        }

        // Identify opening (not available from FEN alone, return null)
        String openingName = null;

        return AnalysisResponse.builder()
                .currentFen(fen)
                .evaluation(evaluation)
                .openingName(openingName)
                .recommendedMoves(recommendedMoves)
                .isCheck(inCheck)
                .isCheckmate(isCheckmate)
                .isStalemate(isStalemate)
                .build();
    }

    /**
     * Make a move on the board and return the resulting board state.
     */
    public Board makeMove(String fen, String from, String to, String promotion) {
        log.debug("Making move: {} -> {} (promotion: {}) from FEN: {}", from, to, promotion, fen);

        Board board = Board.fromFen(fen);
        int fromRow = squareToRow(from);
        int fromCol = squareToCol(from);
        int toRow = squareToRow(to);
        int toCol = squareToCol(to);

        int promotionPiece = parsePromotionPiece(promotion);

        // Find the matching legal move (handles special moves like castling, en passant)
        List<Move> legalMoves = moveGenerator.generateLegalMoves(board);
        Move matchingMove = legalMoves.stream()
                .filter(m -> m.getFromRow() == fromRow && m.getFromCol() == fromCol
                        && m.getToRow() == toRow && m.getToCol() == toCol
                        && (promotionPiece == 0 || m.getPromotionPiece() == promotionPiece))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        String.format("Illegal move: %s to %s", from, to)));

        return board.makeMove(matchingMove);
    }

    /**
     * Validate whether a move is legal in the given position.
     */
    public boolean validateMove(String fen, String from, String to) {
        Board board = Board.fromFen(fen);
        int fromRow = squareToRow(from);
        int fromCol = squareToCol(from);
        int toRow = squareToRow(to);
        int toCol = squareToCol(to);

        List<Move> legalMoves = moveGenerator.generateLegalMoves(board);
        return legalMoves.stream()
                .anyMatch(m -> m.getFromRow() == fromRow && m.getFromCol() == fromCol
                        && m.getToRow() == toRow && m.getToCol() == toCol);
    }

    /**
     * Get all legal target squares for a piece on the given square.
     */
    public List<String> getLegalMoves(String fen, String square) {
        Board board = Board.fromFen(fen);
        int row = squareToRow(square);
        int col = squareToCol(square);

        List<Move> legalMoves = moveGenerator.generateLegalMoves(board);
        return legalMoves.stream()
                .filter(m -> m.getFromRow() == row && m.getFromCol() == col)
                .map(m -> colToSquareLetter(m.getToCol()) + "" + rowToSquareNumber(m.getToRow()))
                .collect(Collectors.toList());
    }

    /**
     * Convert a Board to its FEN string representation.
     */
    public String getFen(Board board) {
        return board.toFen();
    }

    /**
     * Get the SAN notation for a move in the given position.
     */
    public String getMoveNotation(String fen, String from, String to, String promotion) {
        Board board = Board.fromFen(fen);
        int fromRow = squareToRow(from);
        int fromCol = squareToCol(from);
        int toRow = squareToRow(to);
        int toCol = squareToCol(to);
        int promotionPiece = parsePromotionPiece(promotion);

        List<Move> legalMoves = moveGenerator.generateLegalMoves(board);
        Move matchingMove = legalMoves.stream()
                .filter(m -> m.getFromRow() == fromRow && m.getFromCol() == fromCol
                        && m.getToRow() == toRow && m.getToCol() == toCol
                        && (promotionPiece == 0 || m.getPromotionPiece() == promotionPiece))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        String.format("Illegal move: %s to %s", from, to)));

        return matchingMove.toAlgebraicNotation(board);
    }

    // --- Coordinate conversion helpers ---

    /**
     * Convert algebraic square notation (e.g., "e4") to row index.
     * Row 0 = rank 8 (top), Row 7 = rank 1 (bottom).
     */
    private int squareToRow(String square) {
        if (square == null || square.length() != 2) {
            throw new BadRequestException("Invalid square notation: " + square);
        }
        char rank = square.charAt(1);
        if (rank < '1' || rank > '8') {
            throw new BadRequestException("Invalid rank in square: " + square);
        }
        return 8 - (rank - '0');
    }

    /**
     * Convert algebraic square notation (e.g., "e4") to column index.
     * Column 0 = file 'a', Column 7 = file 'h'.
     */
    private int squareToCol(String square) {
        if (square == null || square.length() != 2) {
            throw new BadRequestException("Invalid square notation: " + square);
        }
        char file = square.charAt(0);
        if (file < 'a' || file > 'h') {
            throw new BadRequestException("Invalid file in square: " + square);
        }
        return file - 'a';
    }

    private char colToSquareLetter(int col) {
        return (char) ('a' + col);
    }

    private int rowToSquareNumber(int row) {
        return 8 - row;
    }

    private int parsePromotionPiece(String promotion) {
        if (promotion == null || promotion.isEmpty()) {
            return 0;
        }
        char c = Character.toUpperCase(promotion.charAt(0));
        switch (c) {
            case 'Q': return Board.QUEEN;
            case 'R': return Board.ROOK;
            case 'B': return Board.BISHOP;
            case 'N': return Board.KNIGHT;
            default: throw new BadRequestException("Invalid promotion piece: " + promotion);
        }
    }

    /**
     * Identify the opening name from a list of SAN moves.
     * Returns the opening name if found, null otherwise.
     */
    public String identifyOpening(List<String> moveHistory) {
        if (moveHistory == null || moveHistory.isEmpty()) {
            return null;
        }
        Optional<String[]> result = openingBook.identifyOpening(moveHistory);
        return result.map(arr -> arr[0]).orElse(null);
    }
}
