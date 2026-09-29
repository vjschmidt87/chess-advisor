package com.chessadvisor.engine;

import java.util.Arrays;

/**
 * Chess board representation using an 8x8 int array.
 * <p>
 * Piece encoding: 0 = empty, positive = white, negative = black.
 * Absolute values: 1=pawn, 2=knight, 3=bishop, 4=rook, 5=queen, 6=king.
 * <p>
 * Board orientation: row 0 = rank 8 (black's back rank), row 7 = rank 1 (white's back rank).
 * Column 0 = a-file, column 7 = h-file.
 */
public class Board {

    // Piece constants
    public static final int EMPTY  = 0;
    public static final int PAWN   = 1;
    public static final int KNIGHT = 2;
    public static final int BISHOP = 3;
    public static final int ROOK   = 4;
    public static final int QUEEN  = 5;
    public static final int KING   = 6;

    private final int[][] squares;

    // Castling rights
    private boolean whiteKingSide;
    private boolean whiteQueenSide;
    private boolean blackKingSide;
    private boolean blackQueenSide;

    // En passant target square (-1 means none)
    private int enPassantRow;
    private int enPassantCol;

    // Side to move
    private boolean whiteToMove;

    // Clocks
    private int halfMoveClock;
    private int fullMoveNumber;

    /**
     * Create a board with the standard starting position.
     */
    public Board() {
        squares = new int[8][8];
        setupStartingPosition();
    }

    /**
     * Copy constructor -- deep clone.
     */
    public Board(Board other) {
        this.squares = new int[8][8];
        for (int r = 0; r < 8; r++) {
            System.arraycopy(other.squares[r], 0, this.squares[r], 0, 8);
        }
        this.whiteKingSide = other.whiteKingSide;
        this.whiteQueenSide = other.whiteQueenSide;
        this.blackKingSide = other.blackKingSide;
        this.blackQueenSide = other.blackQueenSide;
        this.enPassantRow = other.enPassantRow;
        this.enPassantCol = other.enPassantCol;
        this.whiteToMove = other.whiteToMove;
        this.halfMoveClock = other.halfMoveClock;
        this.fullMoveNumber = other.fullMoveNumber;
    }

    /**
     * Create an empty board (no pieces).
     */
    public static Board empty() {
        Board b = new Board();
        for (int r = 0; r < 8; r++) {
            Arrays.fill(b.squares[r], EMPTY);
        }
        b.whiteKingSide = false;
        b.whiteQueenSide = false;
        b.blackKingSide = false;
        b.blackQueenSide = false;
        b.enPassantRow = -1;
        b.enPassantCol = -1;
        b.whiteToMove = true;
        b.halfMoveClock = 0;
        b.fullMoveNumber = 1;
        return b;
    }

    private void setupStartingPosition() {
        // Clear
        for (int r = 0; r < 8; r++) {
            Arrays.fill(squares[r], EMPTY);
        }

        // Black pieces (row 0 = rank 8)
        squares[0][0] = -ROOK;
        squares[0][1] = -KNIGHT;
        squares[0][2] = -BISHOP;
        squares[0][3] = -QUEEN;
        squares[0][4] = -KING;
        squares[0][5] = -BISHOP;
        squares[0][6] = -KNIGHT;
        squares[0][7] = -ROOK;
        for (int c = 0; c < 8; c++) {
            squares[1][c] = -PAWN;
        }

        // White pieces (row 7 = rank 1)
        squares[7][0] = ROOK;
        squares[7][1] = KNIGHT;
        squares[7][2] = BISHOP;
        squares[7][3] = QUEEN;
        squares[7][4] = KING;
        squares[7][5] = BISHOP;
        squares[7][6] = KNIGHT;
        squares[7][7] = ROOK;
        for (int c = 0; c < 8; c++) {
            squares[6][c] = PAWN;
        }

        // Initial state
        whiteKingSide = true;
        whiteQueenSide = true;
        blackKingSide = true;
        blackQueenSide = true;
        enPassantRow = -1;
        enPassantCol = -1;
        whiteToMove = true;
        halfMoveClock = 0;
        fullMoveNumber = 1;
    }

    // --- Getters / Setters ---

    public int getPiece(int row, int col) {
        return squares[row][col];
    }

    public void setPiece(int row, int col, int piece) {
        squares[row][col] = piece;
    }

    public static boolean isWhitePiece(int piece) {
        return piece > 0;
    }

    public static boolean isBlackPiece(int piece) {
        return piece < 0;
    }

    public boolean isWhiteToMove() { return whiteToMove; }
    public void setWhiteToMove(boolean whiteToMove) { this.whiteToMove = whiteToMove; }

    public boolean isWhiteKingSide() { return whiteKingSide; }
    public void setWhiteKingSide(boolean v) { this.whiteKingSide = v; }
    public boolean isWhiteQueenSide() { return whiteQueenSide; }
    public void setWhiteQueenSide(boolean v) { this.whiteQueenSide = v; }
    public boolean isBlackKingSide() { return blackKingSide; }
    public void setBlackKingSide(boolean v) { this.blackKingSide = v; }
    public boolean isBlackQueenSide() { return blackQueenSide; }
    public void setBlackQueenSide(boolean v) { this.blackQueenSide = v; }

    public int getEnPassantRow() { return enPassantRow; }
    public int getEnPassantCol() { return enPassantCol; }
    public void setEnPassantTarget(int row, int col) {
        this.enPassantRow = row;
        this.enPassantCol = col;
    }

    public int getHalfMoveClock() { return halfMoveClock; }
    public void setHalfMoveClock(int v) { this.halfMoveClock = v; }
    public int getFullMoveNumber() { return fullMoveNumber; }
    public void setFullMoveNumber(int v) { this.fullMoveNumber = v; }

    /**
     * Find the king position for the given color.
     * @return int[] {row, col} or null if no king found
     */
    public int[] findKing(boolean white) {
        int target = white ? KING : -KING;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (squares[r][c] == target) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    // --- Make move ---

    /**
     * Return a new Board with the given move applied. Does not mutate this board.
     * Handles castling, en passant, promotion, and updates clocks / rights.
     */
    public Board makeMove(Move move) {
        Board next = new Board(this);

        int fromR = move.getFromRow();
        int fromC = move.getFromCol();
        int toR = move.getToRow();
        int toC = move.getToCol();
        int piece = next.squares[fromR][fromC];
        int absPiece = Math.abs(piece);
        boolean isCapture = next.squares[toR][toC] != 0 || move.isEnPassant();

        // Move piece
        next.squares[toR][toC] = piece;
        next.squares[fromR][fromC] = EMPTY;

        // En passant capture: remove the captured pawn
        if (move.isEnPassant()) {
            // The captured pawn is on the same row as the moving pawn's origin
            next.squares[fromR][toC] = EMPTY;
        }

        // Promotion
        if (move.getPromotionPiece() != 0) {
            int promoPiece = move.getPromotionPiece();
            if (piece < 0) promoPiece = -promoPiece;
            next.squares[toR][toC] = promoPiece;
        }

        // Castling: move the rook
        if (move.isCastling()) {
            if (toC == 6) {
                // King side
                next.squares[fromR][5] = next.squares[fromR][7];
                next.squares[fromR][7] = EMPTY;
            } else if (toC == 2) {
                // Queen side
                next.squares[fromR][3] = next.squares[fromR][0];
                next.squares[fromR][0] = EMPTY;
            }
        }

        // Update en passant target
        next.enPassantRow = -1;
        next.enPassantCol = -1;
        if (absPiece == PAWN && Math.abs(toR - fromR) == 2) {
            next.enPassantRow = (fromR + toR) / 2;
            next.enPassantCol = fromC;
        }

        // Update castling rights
        if (absPiece == KING) {
            if (piece > 0) {
                next.whiteKingSide = false;
                next.whiteQueenSide = false;
            } else {
                next.blackKingSide = false;
                next.blackQueenSide = false;
            }
        }
        // Rook moves or is captured
        if (fromR == 7 && fromC == 0) next.whiteQueenSide = false;
        if (fromR == 7 && fromC == 7) next.whiteKingSide = false;
        if (fromR == 0 && fromC == 0) next.blackQueenSide = false;
        if (fromR == 0 && fromC == 7) next.blackKingSide = false;
        if (toR == 7 && toC == 0) next.whiteQueenSide = false;
        if (toR == 7 && toC == 7) next.whiteKingSide = false;
        if (toR == 0 && toC == 0) next.blackQueenSide = false;
        if (toR == 0 && toC == 7) next.blackKingSide = false;

        // Update clocks
        if (absPiece == PAWN || isCapture) {
            next.halfMoveClock = 0;
        } else {
            next.halfMoveClock = this.halfMoveClock + 1;
        }
        if (!this.whiteToMove) {
            next.fullMoveNumber = this.fullMoveNumber + 1;
        }

        // Toggle side to move
        next.whiteToMove = !this.whiteToMove;

        return next;
    }

    // --- FEN ---

    /**
     * Generate FEN string from the current position.
     */
    public String toFen() {
        StringBuilder fen = new StringBuilder();

        // 1. Piece placement
        for (int r = 0; r < 8; r++) {
            int emptyCount = 0;
            for (int c = 0; c < 8; c++) {
                int piece = squares[r][c];
                if (piece == EMPTY) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(pieceToFenChar(piece));
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            if (r < 7) fen.append('/');
        }

        // 2. Active color
        fen.append(' ');
        fen.append(whiteToMove ? 'w' : 'b');

        // 3. Castling availability
        fen.append(' ');
        StringBuilder castling = new StringBuilder();
        if (whiteKingSide) castling.append('K');
        if (whiteQueenSide) castling.append('Q');
        if (blackKingSide) castling.append('k');
        if (blackQueenSide) castling.append('q');
        fen.append(castling.length() > 0 ? castling.toString() : "-");

        // 4. En passant target
        fen.append(' ');
        if (enPassantRow >= 0 && enPassantCol >= 0) {
            fen.append(Move.colToFile(enPassantCol));
            fen.append(Move.rowToRank(enPassantRow));
        } else {
            fen.append('-');
        }

        // 5. Halfmove clock
        fen.append(' ');
        fen.append(halfMoveClock);

        // 6. Fullmove number
        fen.append(' ');
        fen.append(fullMoveNumber);

        return fen.toString();
    }

    /**
     * Parse a FEN string and return a Board.
     */
    public static Board fromFen(String fen) {
        Board board = Board.empty();
        String[] parts = fen.trim().split("\\s+");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid FEN: " + fen);
        }

        // 1. Piece placement
        String[] ranks = parts[0].split("/");
        if (ranks.length != 8) {
            throw new IllegalArgumentException("Invalid FEN piece placement: " + parts[0]);
        }
        for (int r = 0; r < 8; r++) {
            int c = 0;
            for (char ch : ranks[r].toCharArray()) {
                if (Character.isDigit(ch)) {
                    c += ch - '0';
                } else {
                    board.squares[r][c] = fenCharToPiece(ch);
                    c++;
                }
            }
        }

        // 2. Active color
        board.whiteToMove = parts[1].equals("w");

        // 3. Castling
        String castling = parts[2];
        board.whiteKingSide = castling.contains("K");
        board.whiteQueenSide = castling.contains("Q");
        board.blackKingSide = castling.contains("k");
        board.blackQueenSide = castling.contains("q");

        // 4. En passant
        if (parts[3].equals("-")) {
            board.enPassantRow = -1;
            board.enPassantCol = -1;
        } else {
            board.enPassantCol = parts[3].charAt(0) - 'a';
            board.enPassantRow = 8 - (parts[3].charAt(1) - '0');
        }

        // 5. Halfmove clock (optional)
        if (parts.length > 4) {
            board.halfMoveClock = Integer.parseInt(parts[4]);
        }

        // 6. Fullmove number (optional)
        if (parts.length > 5) {
            board.fullMoveNumber = Integer.parseInt(parts[5]);
        }

        return board;
    }

    // --- FEN helpers ---

    private static char pieceToFenChar(int piece) {
        int abs = Math.abs(piece);
        char c;
        switch (abs) {
            case PAWN:   c = 'p'; break;
            case KNIGHT: c = 'n'; break;
            case BISHOP: c = 'b'; break;
            case ROOK:   c = 'r'; break;
            case QUEEN:  c = 'q'; break;
            case KING:   c = 'k'; break;
            default: return '?';
        }
        return piece > 0 ? Character.toUpperCase(c) : c;
    }

    private static int fenCharToPiece(char c) {
        int sign = Character.isUpperCase(c) ? 1 : -1;
        switch (Character.toLowerCase(c)) {
            case 'p': return sign * PAWN;
            case 'n': return sign * KNIGHT;
            case 'b': return sign * BISHOP;
            case 'r': return sign * ROOK;
            case 'q': return sign * QUEEN;
            case 'k': return sign * KING;
            default: throw new IllegalArgumentException("Unknown FEN char: " + c);
        }
    }

    // --- Helpers ---

    public static boolean isInBounds(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    /**
     * Return a human-readable text board for debugging.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  a b c d e f g h\n");
        for (int r = 0; r < 8; r++) {
            sb.append((8 - r)).append(' ');
            for (int c = 0; c < 8; c++) {
                int p = squares[r][c];
                if (p == EMPTY) {
                    sb.append(". ");
                } else {
                    sb.append(pieceToFenChar(p)).append(' ');
                }
            }
            sb.append(8 - r).append('\n');
        }
        sb.append("  a b c d e f g h\n");
        sb.append(whiteToMove ? "White to move" : "Black to move");
        return sb.toString();
    }
}
