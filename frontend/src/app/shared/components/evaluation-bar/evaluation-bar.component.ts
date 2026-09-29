import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-evaluation-bar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './evaluation-bar.component.html',
  styleUrl: './evaluation-bar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EvaluationBarComponent {
  @Input() evaluation: number = 0;
  @Input() isFlipped: boolean = false;

  get whitePercentage(): number {
    // Convert centipawn evaluation to percentage (sigmoid-like)
    const clamped = Math.max(-1000, Math.min(1000, this.evaluation));
    const percentage = 50 + (clamped / 1000) * 50;
    return this.isFlipped ? 100 - percentage : percentage;
  }

  get displayEvaluation(): string {
    if (Math.abs(this.evaluation) > 9000) {
      return this.evaluation > 0 ? '+M' : '-M';
    }
    const pawns = this.evaluation / 100;
    return pawns > 0 ? `+${pawns.toFixed(1)}` : pawns.toFixed(1);
  }

  get isWhiteAdvantage(): boolean {
    return this.evaluation > 0;
  }
}
