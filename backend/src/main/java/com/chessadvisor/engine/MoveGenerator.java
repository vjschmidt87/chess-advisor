package com.chessadvisor.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates all legal moves for a position, enforcing check-legality.
 * Also provides check, checkmate, and stalemate detection.
 */
public class MoveGenerator {

    // Direction arrays for sliding pieces
    private static final int[][] BISHOP_DIRS = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
    private static final int[][] ROOK_DIRS   = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    private static final int[][] QUEEN_DIRS  = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
    private static final int[][] KNIGHT_OFFSETS = {
        {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
        {1, -2}, {1, 2}, {2, -1}, {2, 1}
    };
    private static final int[][] KING_OFFSETS = {
        {-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}
    };

    /**
     * Generate all legal moves for the side to move.
     */
    public List<Move> generateLegalMoves(Board board) {
        List<Move> pseudoLegal = generatePseudoLegalMoves(board);
        List<Move> legal = new ArrayList<>();
        boolean isWhite = board.isWhiteToMove();

        for (Move m : pseudoLegal) {
            Board after = board.makeMove(m);
            if (!isInCheck(after, isWhite)) {
                legal.add(m);
            }
        }
        return legal;
    }

    /**
     * Generate legal moves from a specific square.
     */
    public List<Move> getLegalMovesForSquare(Board board, int row, int col) {
        List<Move> all = generateLegalMoves(board);
        List<Move> result = new ArrayList<>();
        for (Move m : all) {
            if (m.getFromRow() == row && m.getFromCol() == col) {
                result.add(m);
            }
        }
        return result;
    }

    /**
     * Check if the given side's king is in check.
     */
    public boolean isInCheck(Board board, boolean isWhite) {
        int[] kingPos = board.findKing(isWhite);
        if (kingPos == null) return false;
        return isSquareAttacked(board, kingPos[0], kingPos[1], !isWhite);
    }

    /**
     * Check if the current side to move is in checkmate.
     */
    public boolean isCheckmate(Board board) {
        boolean isWhite = board.isWhiteToMove();
        if (!isInCheck(board, isWhite)) return false;
        return generateLegalMoves(board).isEmpty();
    }

    /**
     * Check if the current side to move is in stalemate.
     */
    public boolean isStalemate(Board board) {
        boolean isWhite = board.isWhiteToMove();
        if (isInCheck(board, isWhite)) return false;
        return generateLegalMoves(board).isEmpty();
    }

    /**
     * Is the given square attacked by a piece of the specified color?
     */
    public boolean isSquareAttacked(Board board, int row, int col, boolean byWhite) {
        // Pawn attacks
        int pawnDir = byWhite ? 1 : -1; // White pawns attack upward (lower row index)
        int pawn = byWhite ? Board.PAWN : -Board.PAWN;
        for (int dc : new int[]{-1, 1}) {
            int pr = row + pawnDir;
            int pc = col + dc;
            if (Board.isInBounds(pr, pc) && board.getPiece(pr, pc) == pawn) {
                return true;
            }
        }

        // Knight attacks
        int knight = byWhite ? Board.KNIGHT : -Board.KNIGHT;
        for (int[] off : KNIGHT_OFFSETS) {
            int nr = row + off[0];
            int nc = col + off[1];
            if (Board.isInBounds(nr, nc) && board.getPiece(nr, nc) == knight) {
                return true;
            }
        }

        // King attacks
        int king = byWhite ? Board.KING : -Board.KING;
        for (int[] off : KING_OFFSETS) {
            int kr = row + off[0];
            int kc = col + off[1];
            if (Board.isInBounds(kr, kc) && board.getPiece(kr, kc) == king) {
                return true;
            }
        }

        // Sliding piece attacks: bishop/queen on diagonals
        int bishop = byWhite ? Board.BISHOP : -Board.BISHOP;
        int queen  = byWhite ? Board.QUEEN  : -Board.QUEEN;
        for (int[] dir : BISHOP_DIRS) {
            int r = row + dir[0];
            int c = col + dir[1];
            while (Board.isInBounds(r, c)) {
                int p = board.getPiece(r, c);
                if (p != Board.EMPTY) {
                    if (p == bishop || p == queen) return true;
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }

        // Sliding piece attacks: rook/queen on files/ranks
        int rook = byWhite ? Board.ROOK : -Board.ROOK;
        for (int[] dir : ROOK_DIRS) {
            int r = row + dir[0];
            int c = col + dir[1];
            while (Board.isInBounds(r, c)) {
                int p = board.getPiece(r, c);
                if (p != Board.EMPTY) {
                    if (p == rook || p == queen) return true;
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }

        return false;
    }

    // --- Pseudo-legal move generation ---

    private List<Move> generatePseudoLegalMoves(Board board) {
        List<Move> moves = new ArrayList<>();
        boolean isWhite = board.isWhiteToMove();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int piece = board.getPiece(r, c);
                if (piece == Board.EMPTY) continue;
                if (isWhite && !Board.isWhitePiece(piece)) continue;
                if (!isWhite && !Board.isBlackPiece(piece)) continue;

                int abs = Math.abs(piece);
                switch (abs) {
                    case Board.PAWN:
                        generatePawnMoves(board, r, c, isWhite, moves);
                        break;
                    case Board.KNIGHT:
                        generateKnightMoves(board, r, c, isWhite, moves);
                        break;
                    case Board.BISHOP:
                        generateSlidingMoves(board, r, c, isWhite, BISHOP_DIRS, moves);
                        break;
                    case Board.ROOK:
                        generateSlidingMoves(board, r, c, isWhite, ROOK_DIRS, moves);
                        break;
                    case Board.QUEEN:
                        generateSlidingMoves(board, r, c, isWhite, QUEEN_DIRS, moves);
                        break;
                    case Board.KING:
                        generateKingMoves(board, r, c, isWhite, moves);
                        break;
                }
            }
        }
        return moves;
    }

    private void generatePawnMoves(Board board, int row, int col, boolean isWhite, List<Move> moves) {
        int direction = isWhite ? -1 : 1; // White moves up (decreasing row), black moves down
        int startRow = isWhite ? 6 : 1;
        int promoRow = isWhite ? 0 : 7;

        // Single push
        int newRow = row + direction;
        if (Board.isInBounds(newRow, col) && board.getPiece(newRow, col) == Board.EMPTY) {
            if (newRow == promoRow) {
                addPromotionMoves(row, col, newRow, col, moves);
            } else {
                moves.add(new Move(row, col, newRow, col));
            }

            // Double push from starting position
            if (row == startRow) {
                int newRow2 = row + 2 * direction;
                if (board.getPiece(newRow2, col) == Board.EMPTY) {
                    moves.add(new Move(row, col, newRow2, col));
                }
            }
        }

        // Captures (including en passant)
        for (int dc : new int[]{-1, 1}) {
            int nc = col + dc;
            if (!Board.isInBounds(newRow, nc)) continue;

            int target = board.getPiece(newRow, nc);
            boolean isCapture = (isWhite && Board.isBlackPiece(target))
                             || (!isWhite && Board.isWhitePiece(target));

            if (isCapture) {
                if (newRow == promoRow) {
                    addPromotionMoves(row, col, newRow, nc, moves);
                } else {
                    moves.add(new Move(row, col, newRow, nc));
                }
            }

            // En passant
            if (newRow == board.getEnPassantRow() && nc == board.getEnPassantCol()) {
                moves.add(new Move(row, col, newRow, nc, 0, false, true));
            }
        }
    }

    private void addPromotionMoves(int fromRow, int fromCol, int toRow, int toCol, List<Move> moves) {
        moves.add(new Move(fromRow, fromCol, toRow, toCol, Board.QUEEN));
        moves.add(new Move(fromRow, fromCol, toRow, toCol, Board.ROOK));
        moves.add(new Move(fromRow, fromCol, toRow, toCol, Board.BISHOP));
        moves.add(new Move(fromRow, fromCol, toRow, toCol, Board.KNIGHT));
    }

    private void generateKnightMoves(Board board, int row, int col, boolean isWhite, List<Move> moves) {
        for (int[] off : KNIGHT_OFFSETS) {
            int nr = row + off[0];
            int nc = col + off[1];
            if (!Board.isInBounds(nr, nc)) continue;
            int target = board.getPiece(nr, nc);
            if (target == Board.EMPTY
                || (isWhite && Board.isBlackPiece(target))
                || (!isWhite && Board.isWhitePiece(target))) {
                moves.add(new Move(row, col, nr, nc));
            }
        }
    }

    private void generateSlidingMoves(Board board, int row, int col, boolean isWhite,
                                      int[][] directions, List<Move> moves) {
        for (int[] dir : directions) {
            int r = row + dir[0];
            int c = col + dir[1];
            while (Board.isInBounds(r, c)) {
                int target = board.getPiece(r, c);
                if (target == Board.EMPTY) {
                    moves.add(new Move(row, col, r, c));
                } else {
                    if ((isWhite && Board.isBlackPiece(target))
                        || (!isWhite && Board.isWhitePiece(target))) {
                        moves.add(new Move(row, col, r, c));
                    }
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }
    }

    private void generateKingMoves(Board board, int row, int col, boolean isWhite, List<Move> moves) {
        // Regular king moves
        for (int[] off : KING_OFFSETS) {
            int nr = row + off[0];
            int nc = col + off[1];
            if (!Board.isInBounds(nr, nc)) continue;
            int target = board.getPiece(nr, nc);
            if (target == Board.EMPTY
                || (isWhite && Board.isBlackPiece(target))
                || (!isWhite && Board.isWhitePiece(target))) {
                moves.add(new Move(row, col, nr, nc));
            }
        }

        // Castling
        if (isWhite) {
            if (board.isWhiteKingSide()) {
                if (canCastleKingSide(board, row, col, true)) {
                    moves.add(new Move(row, col, row, col + 2, 0, true, false));
                }
            }
            if (board.isWhiteQueenSide()) {
                if (canCastleQueenSide(board, row, col, true)) {
                    moves.add(new Move(row, col, row, col - 2, 0, true, false));
                }
            }
        } else {
            if (board.isBlackKingSide()) {
                if (canCastleKingSide(board, row, col, false)) {
                    moves.add(new Move(row, col, row, col + 2, 0, true, false));
                }
            }
            if (board.isBlackQueenSide()) {
                if (canCastleQueenSide(board, row, col, false)) {
                    moves.add(new Move(row, col, row, col - 2, 0, true, false));
                }
            }
        }
    }

    private boolean canCastleKingSide(Board board, int kingRow, int kingCol, boolean isWhite) {
        // Squares between king and rook must be empty
        if (board.getPiece(kingRow, kingCol + 1) != Board.EMPTY) return false;
        if (board.getPiece(kingRow, kingCol + 2) != Board.EMPTY) return false;

        // King must not be in check
        if (isSquareAttacked(board, kingRow, kingCol, !isWhite)) return false;

        // Squares the king passes through must not be attacked
        if (isSquareAttacked(board, kingRow, kingCol + 1, !isWhite)) return false;
        if (isSquareAttacked(board, kingRow, kingCol + 2, !isWhite)) return false;

        return true;
    }

    private boolean canCastleQueenSide(Board board, int kingRow, int kingCol, boolean isWhite) {
        // Squares between king and rook must be empty
        if (board.getPiece(kingRow, kingCol - 1) != Board.EMPTY) return false;
        if (board.getPiece(kingRow, kingCol - 2) != Board.EMPTY) return false;
        if (board.getPiece(kingRow, kingCol - 3) != Board.EMPTY) return false;

        // King must not be in check
        if (isSquareAttacked(board, kingRow, kingCol, !isWhite)) return false;

        // Squares the king passes through must not be attacked
        if (isSquareAttacked(board, kingRow, kingCol - 1, !isWhite)) return false;
        if (isSquareAttacked(board, kingRow, kingCol - 2, !isWhite)) return false;

        return true;
    }
}
