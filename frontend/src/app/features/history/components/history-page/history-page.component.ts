import { Component, ChangeDetectionStrategy, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { GameService } from '@core/services/game.service';
import { GameListItem } from '@shared/models/chess.models';
import { BehaviorSubject, switchMap, finalize } from 'rxjs';

@Component({
  selector: 'app-history-page',
  standalone: true,
  imports: [RouterLink, DatePipe],
  template: `
    <div class="history-page">
      <div class="page-header">
        <h1>Game History</h1>
        <a routerLink="/game" class="btn btn-primary">New Game</a>
      </div>

      @if (loading) {
        <div class="loading">Loading games...</div>
      } @else if (error) {
        <div class="error">
          <p>{{ error }}</p>
          <button class="btn btn-secondary" (click)="load$.next()">Retry</button>
        </div>
      } @else if (games.length === 0) {
        <div class="empty">
          <p>No games yet. Start your first game!</p>
          <a routerLink="/game" class="btn btn-primary">Play Now</a>
        </div>
      } @else {
        <div class="games-list">
          @for (game of games; track game.id) {
            <div class="game-card">
              <div class="game-color">
                <span class="piece" [class.white]="game.playerColor === 'WHITE'" [class.black]="game.playerColor === 'BLACK'">
                  {{ game.playerColor === 'WHITE' ? '♔' : '♚' }}
                </span>
              </div>
              <div class="game-info">
                <div class="game-title">
                  @if (game.openingName) {
                    <span class="opening">{{ game.openingName }}</span>
                  } @else {
                    <span class="opening">Game #{{ game.id }}</span>
                  }
                </div>
                <div class="game-meta">
                  <span class="moves">{{ game.totalMoves }} moves</span>
                  <span class="separator">·</span>
                  <span class="status badge-{{ game.status.toLowerCase() }}">{{ formatStatus(game.status) }}</span>
                  <span class="separator">·</span>
                  <span class="date">{{ game.createdAt | date:'mediumDate' }}</span>
                </div>
              </div>
              <div class="game-actions">
                <a [routerLink]="['/game']" [queryParams]="{ id: game.id }" class="btn btn-secondary btn-sm">Review</a>
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
  styleUrl: './history-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HistoryPageComponent implements OnInit {
  private readonly gameService = inject(GameService);
  private readonly destroyRef = inject(DestroyRef);

  games: GameListItem[] = [];
  loading = true;
  error: string | null = null;

  load$ = new BehaviorSubject<void>(undefined);

  ngOnInit(): void {
    this.load$
      .pipe(
        switchMap(() => {
          this.loading = true;
          this.error = null;
          return this.gameService
            .getUserGames()
            .pipe(finalize(() => (this.loading = false)));
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (games) => (this.games = games),
        error: (err) => (this.error = err.message || 'Failed to load games'),
      });
  }

  formatStatus(status: string): string {
    return status.replace('_', ' ').toLowerCase().replace(/^\w/, c => c.toUpperCase());
  }
}
