import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RecommendedMove } from '@shared/models/chess.models';

@Component({
  selector: 'app-recommended-moves',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './recommended-moves.component.html',
  styleUrl: './recommended-moves.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RecommendedMovesComponent {
  @Input() moves: RecommendedMove[] = [];
  @Input() loading: boolean = false;
  @Output() moveHover = new EventEmitter<string | null>();
  @Output() moveSelect = new EventEmitter<string>();

  onMouseEnter(move: RecommendedMove): void {
    this.moveHover.emit(move.move);
  }

  onMouseLeave(): void {
    this.moveHover.emit(null);
  }

  onMoveClick(move: RecommendedMove): void {
    this.moveSelect.emit(move.move);
  }

  getScoreDisplay(score: number): string {
    if (Math.abs(score) > 9000) {
      return score > 0 ? '+M' : '-M';
    }
    const pawns = score / 100;
    return pawns > 0 ? `+${pawns.toFixed(1)}` : pawns.toFixed(1);
  }

  getScoreClass(score: number): string {
    if (score > 100) return 'score-winning';
    if (score > 0) return 'score-slight';
    if (score === 0) return 'score-equal';
    if (score > -100) return 'score-slight-losing';
    return 'score-losing';
  }

  getRankLabel(index: number): string {
    return ['Best', '2nd', '3rd'][index] || `${index + 1}th`;
  }

  trackByMove(index: number, move: RecommendedMove): string {
    return move.move;
  }
}
