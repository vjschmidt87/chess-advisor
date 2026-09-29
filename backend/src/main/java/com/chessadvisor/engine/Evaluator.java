package com.chessadvisor.engine;

import java.util.List;

/**
 * Position evaluator returning a score in centipawns (positive = white advantage).
 * Evaluates: material, piece-square tables, king safety, pawn structure, center control, mobility.
 */
public class Evaluator {

    // Material values in centipawns
    public static final int PAWN_VALUE   = 100;
    public static final int KNIGHT_VALUE = 320;
    public static final int BISHOP_VALUE = 330;
    public static final int ROOK_VALUE   = 500;
    public static final int QUEEN_VALUE  = 900;
    public static final int KING_VALUE   = 20000;

    // Penalties / bonuses
    private static final int DOUBLED_PAWN_PENALTY  = -20;
    private static final int ISOLATED_PAWN_PENALTY = -15;
    private static final int BISHOP_PAIR_BONUS     = 30;
    private static final int MOBILITY_WEIGHT       = 2; // centipawns per legal move

    // --- Piece-Square Tables (from White's perspective, row 0 = rank 8, row 7 = rank 1) ---

    private static final int[][] PAWN_PST = {
        {  0,  0,  0,  0,  0,  0,  0,  0},
        { 50, 50, 50, 50, 50, 50, 50, 50},
        { 10, 10, 20, 30, 30, 20, 10, 10},
        {  5,  5, 10, 25, 25, 10,  5,  5},
        {  0,  0,  0, 20, 20,  0,  0,  0},
        {  5, -5,-10,  0,  0,-10, -5,  5},
        {  5, 10, 10,-20,-20, 10, 10,  5},
        {  0,  0,  0,  0,  0,  0,  0,  0}
    };

    private static final int[][] KNIGHT_PST = {
        {-50,-40,-30,-30,-30,-30,-40,-50},
        {-40,-20,  0,  0,  0,  0,-20,-40},
        {-30,  0, 10, 15, 15, 10,  0,-30},
        {-30,  5, 15, 20, 20, 15,  5,-30},
        {-30,  0, 15, 20, 20, 15,  0,-30},
        {-30,  5, 10, 15, 15, 10,  5,-30},
        {-40,-20,  0,  5,  5,  0,-20,-40},
        {-50,-40,-30,-30,-30,-30,-40,-50}
    };

    private static final int[][] BISHOP_PST = {
        {-20,-10,-10,-10,-10,-10,-10,-20},
        {-10,  0,  0,  0,  0,  0,  0,-10},
        {-10,  0, 10, 10, 10, 10,  0,-10},
        {-10,  5,  5, 10, 10,  5,  5,-10},
        {-10,  0,  5, 10, 10,  5,  0,-10},
        {-10, 10, 10, 10, 10, 10, 10,-10},
        {-10,  5,  0,  0,  0,  0,  5,-10},
        {-20,-10,-10,-10,-10,-10,-10,-20}
    };

    private static final int[][] ROOK_PST = {
        {  0,  0,  0,  0,  0,  0,  0,  0},
        {  5, 10, 10, 10, 10, 10, 10,  5},
        { -5,  0,  0,  0,  0,  0,  0, -5},
        { -5,  0,  0,  0,  0,  0,  0, -5},
        { -5,  0,  0,  0,  0,  0,  0, -5},
        { -5,  0,  0,  0,  0,  0,  0, -5},
        { -5,  0,  0,  0,  0,  0,  0, -5},
        {  0,  0,  0,  5,  5,  0,  0,  0}
    };

    private static final int[][] QUEEN_PST = {
        {-20,-10,-10, -5, -5,-10,-10,-20},
        {-10,  0,  0,  0,  0,  0,  0,-10},
        {-10,  0,  5,  5,  5,  5,  0,-10},
        { -5,  0,  5,  5,  5,  5,  0, -5},
        {  0,  0,  5,  5,  5,  5,  0, -5},
        {-10,  5,  5,  5,  5,  5,  0,-10},
        {-10,  0,  5,  0,  0,  0,  0,-10},
        {-20,-10,-10, -5, -5,-10,-10,-20}
    };

    private static final int[][] KING_MIDDLEGAME_PST = {
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-20,-30,-30,-40,-40,-30,-30,-20},
        {-10,-20,-20,-20,-20,-20,-20,-10},
        { 20, 20,  0,  0,  0,  0, 20, 20},
        { 20, 30, 10,  0,  0, 10, 30, 20}
    };

    private static final int[][] KING_ENDGAME_PST = {
        {-50,-40,-30,-20,-20,-30,-40,-50},
        {-30,-20,-10,  0,  0,-10,-20,-30},
        {-30,-10, 20, 30, 30, 20,-10,-30},
        {-30,-10, 30, 40, 40, 30,-10,-30},
        {-30,-10, 30, 40, 40, 30,-10,-30},
        {-30,-10, 20, 30, 30, 20,-10,-30},
        {-30,-30,  0,  0,  0,  0,-30,-30},
        {-50,-30,-30,-30,-30,-30,-30,-50}
    };

    /**
     * Evaluate the position. Returns score in centipawns.
     * Positive = white advantage, negative = black advantage.
     */
    public int evaluate(Board board) {
        int score = 0;

        score += evaluateMaterial(board);
        score += evaluatePieceSquareTables(board);
        score += evaluatePawnStructure(board);
        score += evaluateMobility(board);
        score += evaluateBishopPair(board);

        return score;
    }

    /**
     * Count total material on the board (for endgame detection).
     */
    private int totalMaterial(Board board) {
        int total = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int piece = board.getPiece(r, c);
                if (piece == Board.EMPTY) continue;
                int abs = Math.abs(piece);
                if (abs != Board.KING) {
                    total += materialValue(abs);
                }
            }
        }
        return total;
    }

    private boolean isEndgame(Board board) {
        // Simple heuristic: endgame if total non-king material <= 2600
        // (roughly both sides have a rook + minor piece or less)
        return totalMaterial(board) <= 2600;
    }

    private int evaluateMaterial(Board board) {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int piece = board.getPiece(r, c);
                if (piece == Board.EMPTY) continue;
                int abs = Math.abs(piece);
                int value = materialValue(abs);
                score += (piece > 0) ? value : -value;
            }
        }
        return score;
    }

    public static int materialValue(int absPiece) {
        switch (absPiece) {
            case Board.PAWN:   return PAWN_VALUE;
            case Board.KNIGHT: return KNIGHT_VALUE;
            case Board.BISHOP: return BISHOP_VALUE;
            case Board.ROOK:   return ROOK_VALUE;
            case Board.QUEEN:  return QUEEN_VALUE;
            case Board.KING:   return KING_VALUE;
            default: return 0;
        }
    }

    private int evaluatePieceSquareTables(Board board) {
        int score = 0;
        boolean endgame = isEndgame(board);

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int piece = board.getPiece(r, c);
                if (piece == Board.EMPTY) continue;
                int abs = Math.abs(piece);

                int pstValue;
                if (piece > 0) {
                    // White: table is already from white's perspective
                    pstValue = getPstValue(abs, r, c, endgame);
                } else {
                    // Black: mirror the row (7 - r)
                    pstValue = -getPstValue(abs, 7 - r, c, endgame);
                }
                score += pstValue;
            }
        }
        return score;
    }

    private int getPstValue(int absPiece, int row, int col, boolean endgame) {
        switch (absPiece) {
            case Board.PAWN:   return PAWN_PST[row][col];
            case Board.KNIGHT: return KNIGHT_PST[row][col];
            case Board.BISHOP: return BISHOP_PST[row][col];
            case Board.ROOK:   return ROOK_PST[row][col];
            case Board.QUEEN:  return QUEEN_PST[row][col];
            case Board.KING:   return endgame ? KING_ENDGAME_PST[row][col] : KING_MIDDLEGAME_PST[row][col];
            default: return 0;
        }
    }

    private int evaluatePawnStructure(Board board) {
        int score = 0;

        // Count pawns per file for each side
        int[] whitePawnsPerFile = new int[8];
        int[] blackPawnsPerFile = new int[8];

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int piece = board.getPiece(r, c);
                if (piece == Board.PAWN) {
                    whitePawnsPerFile[c]++;
                } else if (piece == -Board.PAWN) {
                    blackPawnsPerFile[c]++;
                }
            }
        }

        // Doubled pawns
        for (int c = 0; c < 8; c++) {
            if (whitePawnsPerFile[c] > 1) {
                score += DOUBLED_PAWN_PENALTY * (whitePawnsPerFile[c] - 1);
            }
            if (blackPawnsPerFile[c] > 1) {
                score -= DOUBLED_PAWN_PENALTY * (blackPawnsPerFile[c] - 1);
            }
        }

        // Isolated pawns (no friendly pawn on adjacent files)
        for (int c = 0; c < 8; c++) {
            if (whitePawnsPerFile[c] > 0) {
                boolean hasNeighbor = (c > 0 && whitePawnsPerFile[c - 1] > 0)
                                   || (c < 7 && whitePawnsPerFile[c + 1] > 0);
                if (!hasNeighbor) {
                    score += ISOLATED_PAWN_PENALTY * whitePawnsPerFile[c];
                }
            }
            if (blackPawnsPerFile[c] > 0) {
                boolean hasNeighbor = (c > 0 && blackPawnsPerFile[c - 1] > 0)
                                   || (c < 7 && blackPawnsPerFile[c + 1] > 0);
                if (!hasNeighbor) {
                    score -= ISOLATED_PAWN_PENALTY * blackPawnsPerFile[c];
                }
            }
        }

        return score;
    }

    private int evaluateMobility(Board board) {
        MoveGenerator gen = new MoveGenerator();

        // White mobility
        Board whiteBoard = new Board(board);
        whiteBoard.setWhiteToMove(true);
        int whiteMobility = gen.generateLegalMoves(whiteBoard).size();

        // Black mobility
        Board blackBoard = new Board(board);
        blackBoard.setWhiteToMove(false);
        int blackMobility = gen.generateLegalMoves(blackBoard).size();

        return MOBILITY_WEIGHT * (whiteMobility - blackMobility);
    }

    private int evaluateBishopPair(Board board) {
        int whiteBishops = 0;
        int blackBishops = 0;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int piece = board.getPiece(r, c);
                if (piece == Board.BISHOP) whiteBishops++;
                else if (piece == -Board.BISHOP) blackBishops++;
            }
        }

        int score = 0;
        if (whiteBishops >= 2) score += BISHOP_PAIR_BONUS;
        if (blackBishops >= 2) score -= BISHOP_PAIR_BONUS;
        return score;
    }
}
