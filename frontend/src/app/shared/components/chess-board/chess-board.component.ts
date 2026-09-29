import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChessLogicService } from '@core/services/chess-logic.service';
import { Square, Position, PieceType } from '@shared/models/chess.models';

@Component({
  selector: 'app-chess-board',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chess-board.component.html',
  styleUrl: './chess-board.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChessBoardComponent implements OnChanges {
  @Input() fen: string = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
  @Input() isFlipped: boolean = false;
  @Input() legalMoves: string[] = [];
  @Input() lastMove: { from: string; to: string } | null = null;
  @Input() checkSquare: string | null = null;
  @Input() recommendedSquares: string[] = [];
  @Input() disabled: boolean = false;
  @Input() playerColor: 'WHITE' | 'BLACK' = 'WHITE';

  @Output() squareClick = new EventEmitter<string>();
  @Output() pieceDrop = new EventEmitter<{ from: string; to: string }>();
  @Output() pieceSelected = new EventEmitter<string>();

  squares: Square[][] = [];
  selectedSquare: Position | null = null;
  draggedFrom: string | null = null;
  files = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
  ranks = ['8', '7', '6', '5', '4', '3', '2', '1'];

  constructor(private chessLogic: ChessLogicService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['fen'] || changes['isFlipped'] || changes['legalMoves'] ||
        changes['lastMove'] || changes['checkSquare'] || changes['recommendedSquares']) {
      this.updateBoard();
    }
  }

  private updateBoard(): void {
    this.squares = this.chessLogic.createBoardSquares(this.fen, this.isFlipped);

    // Apply highlights
    for (const row of this.squares) {
      for (const square of row) {
        const algebraic = this.chessLogic.positionToAlgebraic(square.row, square.col);

        // Legal moves
        square.isLegalMove = this.legalMoves.includes(algebraic);

        // Last move
        if (this.lastMove) {
          square.isLastMove = algebraic === this.lastMove.from || algebraic === this.lastMove.to;
        }

        // Check
        if (this.checkSquare) {
          square.isCheck = algebraic === this.checkSquare;
        }

        // Selected
        if (this.selectedSquare) {
          square.isSelected = square.row === this.selectedSquare.row && square.col === this.selectedSquare.col;
        }

        // Recommended moves
        square.isRecommended = this.recommendedSquares.includes(algebraic);
      }
    }
  }

  onSquareClick(square: Square): void {
    if (this.disabled) return;

    const algebraic = this.chessLogic.positionToAlgebraic(square.row, square.col);

    if (this.selectedSquare) {
      const fromAlgebraic = this.chessLogic.positionToAlgebraic(
        this.selectedSquare.row, this.selectedSquare.col
      );

      if (this.legalMoves.includes(algebraic)) {
        this.pieceDrop.emit({ from: fromAlgebraic, to: algebraic });
        this.selectedSquare = null;
      } else if (square.piece) {
        this.selectedSquare = { row: square.row, col: square.col };
        this.pieceSelected.emit(algebraic);
      } else {
        this.selectedSquare = null;
        this.legalMoves = [];
      }
    } else if (square.piece) {
      this.selectedSquare = { row: square.row, col: square.col };
      this.pieceSelected.emit(algebraic);
    }

    this.squareClick.emit(algebraic);
    this.updateBoard();
  }

  onDragStart(event: DragEvent, square: Square): void {
    if (this.disabled || !square.piece) {
      event.preventDefault();
      return;
    }
    const algebraic = this.chessLogic.positionToAlgebraic(square.row, square.col);
    this.draggedFrom = algebraic;
    this.selectedSquare = { row: square.row, col: square.col };
    this.pieceSelected.emit(algebraic);
    this.updateBoard();

    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
      event.dataTransfer.setData('text/plain', algebraic);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
  }

  onDrop(event: DragEvent, square: Square): void {
    event.preventDefault();
    if (this.draggedFrom) {
      const toAlgebraic = this.chessLogic.positionToAlgebraic(square.row, square.col);
      if (this.legalMoves.includes(toAlgebraic)) {
        this.pieceDrop.emit({ from: this.draggedFrom, to: toAlgebraic });
      }
      this.draggedFrom = null;
      this.selectedSquare = null;
      this.updateBoard();
    }
  }

  getPieceUnicode(piece: PieceType): string {
    return this.chessLogic.getPieceUnicode(piece);
  }

  getDisplayFiles(): string[] {
    return this.isFlipped ? [...this.files].reverse() : this.files;
  }

  getDisplayRanks(): string[] {
    return this.isFlipped ? [...this.ranks].reverse() : this.ranks;
  }

  trackByRow(index: number): number {
    return index;
  }

  trackBySquare(index: number, square: Square): string {
    return `${square.row}-${square.col}`;
  }
}
