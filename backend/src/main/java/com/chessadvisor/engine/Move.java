package com.chessadvisor.engine;

import java.util.List;
import java.util.Objects;

/**
 * Represents a chess move with origin, destination, and special move flags.
 */
public class Move {

    private final int fromRow;
    private final int fromCol;
    private final int toRow;
    private final int toCol;
    private final int promotionPiece; // 0 if none, otherwise piece code (2=knight, 3=bishop, 4=rook, 5=queen)
    private final boolean isCastling;
    private final boolean isEnPassant;

    public Move(int fromRow, int fromCol, int toRow, int toCol) {
        this(fromRow, fromCol, toRow, toCol, 0, false, false);
    }

    public Move(int fromRow, int fromCol, int toRow, int toCol, int promotionPiece) {
        this(fromRow, fromCol, toRow, toCol, promotionPiece, false, false);
    }

    public Move(int fromRow, int fromCol, int toRow, int toCol,
                int promotionPiece, boolean isCastling, boolean isEnPassant) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.promotionPiece = promotionPiece;
        this.isCastling = isCastling;
        this.isEnPassant = isEnPassant;
    }

    // --- Getters ---

    public int getFromRow() { return fromRow; }
    public int getFromCol() { return fromCol; }
    public int getToRow() { return toRow; }
    public int getToCol() { return toCol; }
    public int getPromotionPiece() { return promotionPiece; }
    public boolean isCastling() { return isCastling; }
    public boolean isEnPassant() { return isEnPassant; }

    // --- Algebraic notation ---

    /**
     * Convert this move to Standard Algebraic Notation (SAN).
     * Examples: e4, Nf3, Bxe5, O-O, O-O-O, e8=Q, Nbd2, R1e1, Qh4+, Qxf7#
     */
    public String toAlgebraicNotation(Board board) {
        int piece = board.getPiece(fromRow, fromCol);
        int absPiece = Math.abs(piece);

        // Castling
        if (isCastling) {
            if (toCol > fromCol) {
                return "O-O";
            } else {
                return "O-O-O";
            }
        }

        StringBuilder sb = new StringBuilder();

        boolean isCapture = board.getPiece(toRow, toCol) != 0 || isEnPassant;

        if (absPiece == Board.PAWN) {
            // Pawn moves
            if (isCapture) {
                sb.append(colToFile(fromCol));
                sb.append('x');
            }
            sb.append(colToFile(toCol));
            sb.append(rowToRank(toRow));
            if (promotionPiece != 0) {
                sb.append('=');
                sb.append(pieceToLetter(promotionPiece));
            }
        } else {
            // Non-pawn piece
            sb.append(pieceToLetter(absPiece));

            // Disambiguation
            String disambiguation = getDisambiguation(board, piece, absPiece);
            sb.append(disambiguation);

            if (isCapture) {
                sb.append('x');
            }
            sb.append(colToFile(toCol));
            sb.append(rowToRank(toRow));
        }

        // Apply move and check for check/checkmate
        Board newBoard = board.makeMove(this);
        boolean isWhiteMoving = piece > 0;
        boolean opponentIsWhite = !isWhiteMoving;
        MoveGenerator gen = new MoveGenerator();
        if (gen.isInCheck(newBoard, opponentIsWhite)) {
            List<Move> legalMoves = gen.generateLegalMoves(newBoard);
            if (legalMoves.isEmpty()) {
                sb.append('#');
            } else {
                sb.append('+');
            }
        }

        return sb.toString();
    }

    private String getDisambiguation(Board board, int piece, int absPiece) {
        MoveGenerator gen = new MoveGenerator();
        List<Move> allMoves = gen.generateLegalMoves(board);
        boolean needFile = false;
        boolean needRank = false;
        boolean ambiguous = false;

        for (Move other : allMoves) {
            if (other.equals(this)) continue;
            if (other.toRow == this.toRow && other.toCol == this.toCol) {
                int otherPiece = board.getPiece(other.fromRow, other.fromCol);
                if (Math.abs(otherPiece) == absPiece && otherPiece == piece) {
                    ambiguous = true;
                    if (other.fromCol == this.fromCol) {
                        needRank = true;
                    }
                    if (other.fromRow == this.fromRow) {
                        needFile = true;
                    }
                }
            }
        }

        if (!ambiguous) return "";

        // If neither file nor rank alone disambiguates, use file (or both if truly needed)
        if (!needFile && !needRank) {
            needFile = true;
        }

        StringBuilder dis = new StringBuilder();
        if (needFile) {
            dis.append(colToFile(fromCol));
        }
        if (needRank) {
            dis.append(rowToRank(fromRow));
        }
        return dis.toString();
    }

    /**
     * Parse Standard Algebraic Notation into a Move object.
     * Supports: e4, Nf3, Bxe5, O-O, O-O-O, e8=Q, Nbd2, R1e1, etc.
     */
    public static Move fromAlgebraic(String notation, Board board) {
        if (notation == null || notation.isEmpty()) {
            throw new IllegalArgumentException("Empty notation");
        }

        // Strip check/checkmate indicators
        String clean = notation.replaceAll("[+#!?]", "").trim();

        // Castling
        if (clean.equals("O-O") || clean.equals("0-0")) {
            int row = board.isWhiteToMove() ? 7 : 0;
            return new Move(row, 4, row, 6, 0, true, false);
        }
        if (clean.equals("O-O-O") || clean.equals("0-0-0")) {
            int row = board.isWhiteToMove() ? 7 : 0;
            return new Move(row, 4, row, 2, 0, true, false);
        }

        MoveGenerator gen = new MoveGenerator();
        List<Move> legalMoves = gen.generateLegalMoves(board);

        // Promotion piece
        int promoPiece = 0;
        if (clean.contains("=")) {
            int eqIdx = clean.indexOf('=');
            char promoChar = clean.charAt(eqIdx + 1);
            promoPiece = letterToPiece(promoChar);
            clean = clean.substring(0, eqIdx);
        }

        // Determine if it's a pawn move or piece move
        boolean isPawnMove = Character.isLowerCase(clean.charAt(0));

        if (isPawnMove) {
            // Pawn move: e4, exd5, e8 (promotion already stripped)
            boolean isCapture = clean.contains("x");
            clean = clean.replace("x", "");

            int toCol = fileToCol(clean.charAt(clean.length() - 2));
            int toRow = rankToRow(clean.charAt(clean.length() - 1));

            int fromColFilter = -1;
            if (clean.length() > 2) {
                fromColFilter = fileToCol(clean.charAt(0));
            }

            for (Move m : legalMoves) {
                int piece = board.getPiece(m.getFromRow(), m.getFromCol());
                if (Math.abs(piece) != Board.PAWN) continue;
                if (m.getToRow() != toRow || m.getToCol() != toCol) continue;
                if (fromColFilter >= 0 && m.getFromCol() != fromColFilter) continue;
                if (promoPiece != 0 && m.getPromotionPiece() != promoPiece) continue;
                if (promoPiece == 0 && m.getPromotionPiece() != 0) continue;
                return m;
            }
        } else {
            // Piece move: Nf3, Bxe5, Nbd2, R1e1, Qd1d3
            char pieceChar = clean.charAt(0);
            int targetPiece = letterToPiece(pieceChar);
            clean = clean.substring(1); // remove piece letter
            clean = clean.replace("x", "");

            // Last two chars are destination
            int toCol = fileToCol(clean.charAt(clean.length() - 2));
            int toRow = rankToRow(clean.charAt(clean.length() - 1));

            // Disambiguation characters before destination
            String disambig = clean.substring(0, clean.length() - 2);
            int fileFilter = -1;
            int rankFilter = -1;
            for (char c : disambig.toCharArray()) {
                if (c >= 'a' && c <= 'h') {
                    fileFilter = fileToCol(c);
                } else if (c >= '1' && c <= '8') {
                    rankFilter = rankToRow(c);
                }
            }

            for (Move m : legalMoves) {
                int piece = board.getPiece(m.getFromRow(), m.getFromCol());
                if (Math.abs(piece) != targetPiece) continue;
                if (m.getToRow() != toRow || m.getToCol() != toCol) continue;
                if (fileFilter >= 0 && m.getFromCol() != fileFilter) continue;
                if (rankFilter >= 0 && m.getFromRow() != rankFilter) continue;
                return m;
            }
        }

        throw new IllegalArgumentException("Cannot parse move: " + notation
            + " (no matching legal move found)");
    }

    // --- Utility conversions ---

    public static char colToFile(int col) {
        return (char) ('a' + col);
    }

    public static int rowToRank(int row) {
        return 8 - row;
    }

    public static char pieceToLetter(int absPiece) {
        switch (absPiece) {
            case Board.KNIGHT: return 'N';
            case Board.BISHOP: return 'B';
            case Board.ROOK:   return 'R';
            case Board.QUEEN:  return 'Q';
            case Board.KING:   return 'K';
            default: return '?';
        }
    }

    public static int letterToPiece(char c) {
        switch (Character.toUpperCase(c)) {
            case 'N': return Board.KNIGHT;
            case 'B': return Board.BISHOP;
            case 'R': return Board.ROOK;
            case 'Q': return Board.QUEEN;
            case 'K': return Board.KING;
            default: throw new IllegalArgumentException("Unknown piece letter: " + c);
        }
    }

    public static int fileToCol(char file) {
        return file - 'a';
    }

    public static int rankToRow(char rank) {
        return 8 - (rank - '0');
    }

    /**
     * Return coordinate notation (e.g., "e2e4", "e7e8q")
     */
    public String toCoordinateNotation() {
        StringBuilder sb = new StringBuilder();
        sb.append(colToFile(fromCol));
        sb.append(rowToRank(fromRow));
        sb.append(colToFile(toCol));
        sb.append(rowToRank(toRow));
        if (promotionPiece != 0) {
            sb.append(Character.toLowerCase(pieceToLetter(promotionPiece)));
        }
        return sb.toString();
    }

    // --- equals / hashCode ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        return fromRow == move.fromRow
            && fromCol == move.fromCol
            && toRow == move.toRow
            && toCol == move.toCol
            && promotionPiece == move.promotionPiece;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromRow, fromCol, toRow, toCol, promotionPiece);
    }

    @Override
    public String toString() {
        return toCoordinateNotation();
    }
}
