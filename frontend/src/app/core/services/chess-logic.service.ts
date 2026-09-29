import { Injectable } from '@angular/core';
import { PieceType, Square, Position } from '@shared/models/chess.models';

@Injectable({ providedIn: 'root' })
export class ChessLogicService {

  private readonly INITIAL_FEN = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';

  private readonly PIECE_UNICODE: Record<string, string> = {
    'K': '♔', 'Q': '♕', 'R': '♖', 'B': '♗', 'N': '♘', 'P': '♙',
    'k': '♚', 'q': '♛', 'r': '♜', 'b': '♝', 'n': '♞', 'p': '♟'
  };

  getInitialFen(): string {
    return this.INITIAL_FEN;
  }

  parseFen(fen: string): PieceType[][] {
    const board: PieceType[][] = [];
    const rows = fen.split(' ')[0].split('/');
    for (const row of rows) {
      const boardRow: PieceType[] = [];
      for (const char of row) {
        if (isNaN(Number(char))) {
          boardRow.push(char as PieceType);
        } else {
          for (let i = 0; i < Number(char); i++) {
            boardRow.push('');
          }
        }
      }
      board.push(boardRow);
    }
    return board;
  }

  isWhiteTurn(fen: string): boolean {
    return fen.split(' ')[1] === 'w';
  }

  getPieceUnicode(piece: PieceType): string {
    return this.PIECE_UNICODE[piece] || '';
  }

  isWhitePiece(piece: PieceType): boolean {
    return piece !== '' && piece === piece.toUpperCase();
  }

  isBlackPiece(piece: PieceType): boolean {
    return piece !== '' && piece === piece.toLowerCase();
  }

  positionToAlgebraic(row: number, col: number): string {
    const file = String.fromCharCode('a'.charCodeAt(0) + col);
    const rank = (8 - row).toString();
    return file + rank;
  }

  algebraicToPosition(square: string): Position {
    return {
      row: 8 - parseInt(square[1]),
      col: square.charCodeAt(0) - 'a'.charCodeAt(0)
    };
  }

  createBoardSquares(fen: string, isFlipped: boolean): Square[][] {
    const pieces = this.parseFen(fen);
    const squares: Square[][] = [];

    for (let row = 0; row < 8; row++) {
      const boardRow: Square[] = [];
      for (let col = 0; col < 8; col++) {
        const actualRow = isFlipped ? 7 - row : row;
        const actualCol = isFlipped ? 7 - col : col;
        boardRow.push({
          row: actualRow,
          col: actualCol,
          piece: pieces[actualRow][actualCol],
          isLight: (actualRow + actualCol) % 2 === 0,
          isSelected: false,
          isLegalMove: false,
          isLastMove: false,
          isCheck: false,
          isRecommended: false
        });
      }
      squares.push(boardRow);
    }
    return squares;
  }
}
