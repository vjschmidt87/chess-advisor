package com.chessadvisor.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates natural language explanations for chess moves by analyzing
 * what each move accomplishes tactically and positionally.
 */
public class MoveExplainer {

    private final MoveGenerator moveGenerator = new MoveGenerator();

    /**
     * Analyze a move and return a natural language explanation.
     */
    public String analyzeMove(Board board, Move move) {
        List<String> observations = new ArrayList<>();

        int piece = board.getPiece(move.getFromRow(), move.getFromCol());
        int absPiece = Math.abs(piece);
        boolean isWhite = piece > 0;
        Board after = board.makeMove(move);

        // Castling
        if (move.isCastling()) {
            if (move.getToCol() > move.getFromCol()) {
                observations.add("Castles kingside for king safety");
            } else {
                observations.add("Castles queenside for king safety");
            }
        }

        // Capture
        int captured = board.getPiece(move.getToRow(), move.getToCol());
        if (captured != Board.EMPTY) {
            String capturedName = pieceName(Math.abs(captured));
            String square = squareName(move.getToRow(), move.getToCol());
            observations.add("Captures the " + capturedName + " on " + square);
            evaluateTrade(absPiece, Math.abs(captured), observations);
        } else if (move.isEnPassant()) {
            String square = squareName(move.getToRow(), move.getToCol());
            observations.add("Captures the pawn on " + square + " en passant");
        }

        // Promotion
        if (move.getPromotionPiece() != 0) {
            observations.add("Promotes to " + pieceName(move.getPromotionPiece()));
        }

        // Check
        boolean givesCheck = moveGenerator.isInCheck(after, !isWhite);
        if (givesCheck) {
            if (moveGenerator.isCheckmate(after)) {
                observations.add("Delivers checkmate!");
            } else {
                observations.add("Gives check to the king");
            }
        }

        // Development (moving piece from back rank or second rank toward center)
        if (!move.isCastling()) {
            checkDevelopment(board, move, absPiece, isWhite, observations);
        }

        // Center control
        checkCenterControl(move, absPiece, observations);

        // Pawn advancement
        if (absPiece == Board.PAWN) {
            checkPawnAdvancement(move, isWhite, observations);
        }

        // Fork detection
        checkFork(after, move, isWhite, observations);

        // Pin detection
        checkPin(after, move, isWhite, observations);

        // File opening
        checkFileOpening(board, after, move, observations);

        // King safety improvement (e.g., moving pieces to protect king)
        if (!move.isCastling()) {
            checkKingSafety(board, after, isWhite, observations);
        }

        if (observations.isEmpty()) {
            observations.add("Repositions the " + pieceName(absPiece));
        }

        return String.join(". ", observations);
    }

    private void evaluateTrade(int attackerType, int victimType, List<String> observations) {
        int attackerValue = Evaluator.materialValue(attackerType);
        int victimValue = Evaluator.materialValue(victimType);

        if (victimValue > attackerValue + 50) {
            observations.add("Wins material (" + pieceName(victimType) + " for " + pieceName(attackerType) + ")");
        } else if (attackerValue > victimValue + 50) {
            observations.add("Sacrifices the " + pieceName(attackerType) + " for the " + pieceName(victimType));
        } else if (attackerType != Board.KING && victimType != Board.KING) {
            observations.add("Equal trade");
        }
    }

    private void checkDevelopment(Board board, Move move, int absPiece, boolean isWhite,
                                   List<String> observations) {
        if (absPiece == Board.PAWN || absPiece == Board.KING) return;

        int homeRow = isWhite ? 7 : 0;
        boolean wasOnHomeRank = move.getFromRow() == homeRow;

        if (wasOnHomeRank) {
            observations.add("Develops the " + pieceName(absPiece) + " toward the center");
        }
    }

    private void checkCenterControl(Move move, int absPiece, List<String> observations) {
        int toR = move.getToRow();
        int toC = move.getToCol();
        // Center squares: d4, d5, e4, e5 (rows 3-4, cols 3-4)
        boolean isCenterSquare = (toR == 3 || toR == 4) && (toC == 3 || toC == 4);
        // Extended center: c3-f6
        boolean isExtendedCenter = toR >= 2 && toR <= 5 && toC >= 2 && toC <= 5;

        if (isCenterSquare) {
            observations.add("Controls key central squares");
        } else if (isExtendedCenter && (absPiece == Board.KNIGHT || absPiece == Board.BISHOP)) {
            observations.add("Occupies a strong central position");
        }
    }

    private void checkPawnAdvancement(Move move, boolean isWhite, List<String> observations) {
        int promoRow = isWhite ? 0 : 7;
        int distanceToPromo = Math.abs(move.getToRow() - promoRow);
        if (distanceToPromo <= 2 && distanceToPromo > 0) {
            observations.add("Advances the pawn toward promotion");
        }
    }

    private void checkFork(Board after, Move move, boolean isWhite, List<String> observations) {
        int piece = after.getPiece(move.getToRow(), move.getToCol());
        int absPiece = Math.abs(piece);

        List<String> attacked = new ArrayList<>();
        int toR = move.getToRow();
        int toC = move.getToCol();

        // Check what valuable pieces the moved piece now attacks
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int target = after.getPiece(r, c);
                if (target == Board.EMPTY) continue;
                if (isWhite && target > 0) continue; // Same side
                if (!isWhite && target < 0) continue;

                int absTarget = Math.abs(target);
                if (absTarget == Board.PAWN) continue; // Don't count pawns as fork targets

                if (doesAttack(after, toR, toC, r, c, absPiece, isWhite)) {
                    attacked.add(pieceName(absTarget));
                }
            }
        }

        if (attacked.size() >= 2) {
            observations.add("Forks the " + String.join(" and ", attacked));
        }
    }

    /**
     * Simplified check: does piece at (fromR,fromC) attack (toR,toC)?
     */
    private boolean doesAttack(Board board, int fromR, int fromC, int toR, int toC,
                                int absPiece, boolean isWhite) {
        int dr = toR - fromR;
        int dc = toC - fromC;

        switch (absPiece) {
            case Board.PAWN: {
                int dir = isWhite ? -1 : 1;
                return dr == dir && Math.abs(dc) == 1;
            }
            case Board.KNIGHT: {
                int adr = Math.abs(dr);
                int adc = Math.abs(dc);
                return (adr == 2 && adc == 1) || (adr == 1 && adc == 2);
            }
            case Board.BISHOP: {
                if (Math.abs(dr) != Math.abs(dc) || dr == 0) return false;
                return isClearPath(board, fromR, fromC, toR, toC);
            }
            case Board.ROOK: {
                if (dr != 0 && dc != 0) return false;
                return isClearPath(board, fromR, fromC, toR, toC);
            }
            case Board.QUEEN: {
                if (dr == 0 || dc == 0 || Math.abs(dr) == Math.abs(dc)) {
                    return isClearPath(board, fromR, fromC, toR, toC);
                }
                return false;
            }
            case Board.KING: {
                return Math.abs(dr) <= 1 && Math.abs(dc) <= 1 && (dr != 0 || dc != 0);
            }
        }
        return false;
    }

    private boolean isClearPath(Board board, int fromR, int fromC, int toR, int toC) {
        int dr = Integer.signum(toR - fromR);
        int dc = Integer.signum(toC - fromC);
        int r = fromR + dr;
        int c = fromC + dc;
        while (r != toR || c != toC) {
            if (board.getPiece(r, c) != Board.EMPTY) return false;
            r += dr;
            c += dc;
        }
        return true;
    }

    private void checkPin(Board after, Move move, boolean isWhite, List<String> observations) {
        // Check if the moved piece creates a pin along its attack lines
        int piece = after.getPiece(move.getToRow(), move.getToCol());
        int absPiece = Math.abs(piece);

        // Only sliding pieces and queens can pin
        if (absPiece != Board.BISHOP && absPiece != Board.ROOK && absPiece != Board.QUEEN) return;

        int[][] directions;
        if (absPiece == Board.BISHOP) {
            directions = new int[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        } else if (absPiece == Board.ROOK) {
            directions = new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        } else {
            directions = new int[][]{{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
        }

        int toR = move.getToRow();
        int toC = move.getToCol();

        for (int[] dir : directions) {
            int firstPieceType = Board.EMPTY;
            int r = toR + dir[0];
            int c = toC + dir[1];
            boolean foundFirst = false;

            while (Board.isInBounds(r, c)) {
                int target = after.getPiece(r, c);
                if (target != Board.EMPTY) {
                    if (!foundFirst) {
                        // First piece found on this ray
                        boolean isEnemy = (isWhite && target < 0) || (!isWhite && target > 0);
                        if (!isEnemy) break; // Same color, no pin possible
                        firstPieceType = Math.abs(target);
                        foundFirst = true;
                    } else {
                        // Second piece found: check if it's the enemy king
                        boolean isEnemy = (isWhite && target < 0) || (!isWhite && target > 0);
                        if (isEnemy && Math.abs(target) == Board.KING) {
                            observations.add("Pins the " + pieceName(firstPieceType) + " to the king");
                        }
                        break;
                    }
                }
                r += dir[0];
                c += dir[1];
            }
        }
    }

    private void checkFileOpening(Board before, Board after, Move move, List<String> observations) {
        int piece = before.getPiece(move.getFromRow(), move.getFromCol());
        if (Math.abs(piece) != Board.PAWN) return;

        // Check if the file the pawn left is now open
        int col = move.getFromCol();
        boolean fileOpen = true;
        for (int r = 0; r < 8; r++) {
            int p = after.getPiece(r, col);
            if (Math.abs(p) == Board.PAWN) {
                fileOpen = false;
                break;
            }
        }
        if (fileOpen) {
            observations.add("Opens the " + Move.colToFile(col) + "-file");
        }
    }

    private void checkKingSafety(Board before, Board after, boolean isWhite, List<String> observations) {
        // Simple check: see if pawn shield around king improved
        int[] kingPos = after.findKing(isWhite);
        if (kingPos == null) return;

        int kr = kingPos[0];
        int kc = kingPos[1];

        // King should ideally be on back rank corners (rows 7/0, cols 0-2 or 5-7)
        boolean isOnBackRank = (isWhite && kr == 7) || (!isWhite && kr == 0);
        boolean isInCorner = kc <= 2 || kc >= 5;

        if (isOnBackRank && isInCorner) {
            // Count pawn shield
            int shieldBefore = countPawnShield(before, isWhite);
            int shieldAfter = countPawnShield(after, isWhite);
            if (shieldAfter > shieldBefore) {
                observations.add("Improves king safety");
            }
        }
    }

    private int countPawnShield(Board board, boolean isWhite) {
        int[] kingPos = board.findKing(isWhite);
        if (kingPos == null) return 0;

        int kr = kingPos[0];
        int kc = kingPos[1];
        int pawnDir = isWhite ? -1 : 1;
        int pawn = isWhite ? Board.PAWN : -Board.PAWN;
        int count = 0;

        for (int dc = -1; dc <= 1; dc++) {
            int r = kr + pawnDir;
            int c = kc + dc;
            if (Board.isInBounds(r, c) && board.getPiece(r, c) == pawn) {
                count++;
            }
        }
        return count;
    }

    // --- Utility ---

    public static String pieceName(int absPiece) {
        switch (absPiece) {
            case Board.PAWN:   return "pawn";
            case Board.KNIGHT: return "knight";
            case Board.BISHOP: return "bishop";
            case Board.ROOK:   return "rook";
            case Board.QUEEN:  return "queen";
            case Board.KING:   return "king";
            default: return "piece";
        }
    }

    public static String squareName(int row, int col) {
        return "" + Move.colToFile(col) + Move.rowToRank(row);
    }
}
