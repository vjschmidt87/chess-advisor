import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MoveNotation } from '@shared/models/chess.models';

@Component({
  selector: 'app-move-history',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './move-history.component.html',
  styleUrl: './move-history.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MoveHistoryComponent {
  @Input() moves: MoveNotation[] = [];
  @Input() currentMoveIndex: number = -1;
  @Output() moveClick = new EventEmitter<number>();

  onMoveClick(index: number): void {
    this.moveClick.emit(index);
  }

  trackByMove(index: number): number {
    return index;
  }
}
