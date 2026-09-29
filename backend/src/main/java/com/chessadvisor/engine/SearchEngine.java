package com.chessadvisor.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Chess search engine using minimax with alpha-beta pruning.
 * Includes move ordering optimization for better pruning efficiency.
 */
public class SearchEngine {

    public static final int DEFAULT_DEPTH = 4;
    private static final int CHECKMATE_SCORE = 100000;
    private static final int STALEMATE_SCORE = 0;

    private final Evaluator evaluator;
    private final MoveGenerator moveGenerator;
    private final MoveExplainer explainer;

    private int nodesSearched;

    public SearchEngine() {
        this.evaluator = new Evaluator();
        this.moveGenerator = new MoveGenerator();
        this.explainer = new MoveExplainer();
    }

    /**
     * Find the top N best moves for the current position.
     *
     * @param board  the current board state
     * @param depth  search depth (plies)
     * @param topN   number of best moves to return
     * @return list of MoveEvaluation sorted by score (best first for side to move)
     */
    public List<MoveEvaluation> findBestMoves(Board board, int depth, int topN) {
        nodesSearched = 0;
        List<Move> legalMoves = moveGenerator.generateLegalMoves(board);

        if (legalMoves.isEmpty()) {
            return new ArrayList<>();
        }

        // Order moves for better pruning
        legalMoves = orderMoves(board, legalMoves);

        List<MoveEvaluation> evaluations = new ArrayList<>();
        boolean maximizing = board.isWhiteToMove();

        for (Move move : legalMoves) {
            Board after = board.makeMove(move);
            int score = minimax(after, depth - 1,
                    Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, !maximizing);

            String explanation = explainer.analyzeMove(board, move);
            evaluations.add(new MoveEvaluation(move, score, explanation));
        }

        // Sort: best moves first (highest score for white, lowest for black)
        if (maximizing) {
            evaluations.sort(Comparator.comparingInt(MoveEvaluation::getScore).reversed());
        } else {
            evaluations.sort(Comparator.comparingInt(MoveEvaluation::getScore));
        }

        // Return top N
        int limit = Math.min(topN, evaluations.size());
        return evaluations.subList(0, limit);
    }

    /**
     * Convenience: find best moves with default depth.
     */
    public List<MoveEvaluation> findBestMoves(Board board, int topN) {
        return findBestMoves(board, DEFAULT_DEPTH, topN);
    }

    /**
     * Minimax with alpha-beta pruning.
     *
     * @param board      current position
     * @param depth      remaining depth to search
     * @param alpha      alpha bound (best score that the maximizer can guarantee)
     * @param beta       beta bound (best score that the minimizer can guarantee)
     * @param maximizing true if it's the maximizing player's turn (white)
     * @return evaluation score in centipawns
     */
    public int minimax(Board board, int depth, int alpha, int beta, boolean maximizing) {
        nodesSearched++;

        // Terminal node checks
        List<Move> legalMoves = moveGenerator.generateLegalMoves(board);

        if (legalMoves.isEmpty()) {
            if (moveGenerator.isInCheck(board, board.isWhiteToMove())) {
                // Checkmate: scored from the losing side's perspective
                // If it's white's turn and they're in checkmate, that's bad for white
                return maximizing ? -(CHECKMATE_SCORE + depth) : (CHECKMATE_SCORE + depth);
            } else {
                return STALEMATE_SCORE; // Stalemate
            }
        }

        if (depth <= 0) {
            return evaluator.evaluate(board);
        }

        // Order moves for better pruning
        legalMoves = orderMoves(board, legalMoves);

        if (maximizing) {
            int maxEval = Integer.MIN_VALUE + 1;
            for (Move move : legalMoves) {
                Board after = board.makeMove(move);
                int eval = minimax(after, depth - 1, alpha, beta, false);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) {
                    break; // Beta cutoff
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE - 1;
            for (Move move : legalMoves) {
                Board after = board.makeMove(move);
                int eval = minimax(after, depth - 1, alpha, beta, true);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) {
                    break; // Alpha cutoff
                }
            }
            return minEval;
        }
    }

    /**
     * Order moves for better alpha-beta pruning:
     * 1. Captures (ordered by MVV-LVA: Most Valuable Victim - Least Valuable Attacker)
     * 2. Checks
     * 3. Quiet moves
     */
    private List<Move> orderMoves(Board board, List<Move> moves) {
        List<Move> ordered = new ArrayList<>(moves);
        ordered.sort((a, b) -> {
            int scoreA = moveOrderScore(board, a);
            int scoreB = moveOrderScore(board, b);
            return Integer.compare(scoreB, scoreA); // Higher score first
        });
        return ordered;
    }

    private int moveOrderScore(Board board, Move move) {
        int score = 0;

        // Captures: MVV-LVA heuristic
        int capturedPiece = board.getPiece(move.getToRow(), move.getToCol());
        if (capturedPiece != Board.EMPTY) {
            int victimValue = Evaluator.materialValue(Math.abs(capturedPiece));
            int attackerValue = Evaluator.materialValue(Math.abs(board.getPiece(move.getFromRow(), move.getFromCol())));
            score += 10000 + victimValue - attackerValue / 100;
        }

        // En passant capture
        if (move.isEnPassant()) {
            score += 10000 + Evaluator.PAWN_VALUE;
        }

        // Promotion
        if (move.getPromotionPiece() != 0) {
            score += 9000 + Evaluator.materialValue(move.getPromotionPiece());
        }

        // Checks
        Board after = board.makeMove(move);
        if (moveGenerator.isInCheck(after, after.isWhiteToMove())) {
            score += 5000;
        }

        // Castling gets a small bonus
        if (move.isCastling()) {
            score += 500;
        }

        return score;
    }

    /**
     * Get the number of nodes searched in the last call.
     */
    public int getNodesSearched() {
        return nodesSearched;
    }

    // --- MoveEvaluation ---

    /**
     * Holds a move along with its evaluation score and a natural language explanation.
     */
    public static class MoveEvaluation {
        private final Move move;
        private final int score;
        private final String explanation;

        public MoveEvaluation(Move move, int score, String explanation) {
            this.move = move;
            this.score = score;
            this.explanation = explanation;
        }

        public Move getMove() { return move; }
        public int getScore() { return score; }
        public String getExplanation() { return explanation; }

        @Override
        public String toString() {
            return String.format("%s (score: %d) - %s", move.toCoordinateNotation(), score, explanation);
        }
    }
}
