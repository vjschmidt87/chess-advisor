import { Component, OnInit, DestroyRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BehaviorSubject, catchError, of, switchMap } from 'rxjs';
import { ChessBoardComponent } from '@shared/components/chess-board/chess-board.component';
import { MoveHistoryComponent } from '@shared/components/move-history/move-history.component';
import { EvaluationBarComponent } from '@shared/components/evaluation-bar/evaluation-bar.component';
import { RecommendedMovesComponent } from '@shared/components/recommended-moves/recommended-moves.component';
import { NotationGuideComponent } from '@shared/components/notation-guide/notation-guide.component';
import { GameService } from '@core/services/game.service';
import { AnalysisService } from '@core/services/analysis.service';
import { ChessLogicService } from '@core/services/chess-logic.service';
import { AnalysisResult, GameInfo, MoveNotation, RecommendedMove } from '@shared/models/chess.models';

@Component({
  selector: 'app-game-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ChessBoardComponent,
    MoveHistoryComponent,
    EvaluationBarComponent,
    RecommendedMovesComponent,
    NotationGuideComponent
  ],
  templateUrl: './game-page.component.html',
  styleUrl: './game-page.component.scss'
})
export class GamePageComponent implements OnInit {
  private destroyRef = inject(DestroyRef);

  currentFen: string;
  isFlipped = false;
  playerColor: 'WHITE' | 'BLACK' = 'WHITE';
  gameId: number | null = null;
  moves: MoveNotation[] = [];
  currentMoveIndex = -1;
  evaluation = 0;
  recommendedMoves: RecommendedMove[] = [];
  legalMoves: string[] = [];
  lastMove: { from: string; to: string } | null = null;
  checkSquare: string | null = null;
  recommendedSquares: string[] = [];
  openingName = '';
  isAnalyzing = false;
  isCheck = false;
  isCheckmate = false;
  isStalemate = false;
  showNewGameDialog = true;
  showImportDialog = false;
  importPgn = '';
  errorMessage = '';

  private analysisSubject = new BehaviorSubject<string>('');

  constructor(
    private gameService: GameService,
    private analysisService: AnalysisService,
    private chessLogic: ChessLogicService
  ) {
    this.currentFen = this.chessLogic.getInitialFen();
  }

  ngOnInit(): void {
    this.analysisSubject.pipe(
      switchMap(fen => {
        if (!fen) return of(null);
        this.isAnalyzing = true;
        return this.analysisService.analyzePosition(fen).pipe(
          catchError(err => {
            this.errorMessage = 'Failed to analyze position';
            this.isAnalyzing = false;
            return of(null);
          })
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(result => {
      if (result) {
        this.handleAnalysisResult(result);
      }
      this.isAnalyzing = false;
    });
  }

  startNewGame(color: 'WHITE' | 'BLACK'): void {
    this.playerColor = color;
    this.isFlipped = color === 'BLACK';
    this.showNewGameDialog = false;
    this.errorMessage = '';

    this.gameService.createGame(color).pipe(
      catchError(err => {
        this.errorMessage = 'Failed to create game. Playing locally.';
        return of(null);
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(game => {
      if (game) {
        this.gameId = game.id;
        this.currentFen = game.currentFen || this.chessLogic.getInitialFen();
      }
      this.resetBoard();
    });
  }

  private resetBoard(): void {
    this.currentFen = this.chessLogic.getInitialFen();
    this.moves = [];
    this.currentMoveIndex = -1;
    this.evaluation = 0;
    this.recommendedMoves = [];
    this.legalMoves = [];
    this.lastMove = null;
    this.checkSquare = null;
    this.recommendedSquares = [];
    this.openingName = '';
    this.isCheck = false;
    this.isCheckmate = false;
    this.isStalemate = false;
  }

  onPieceSelected(square: string): void {
    if (!this.gameId && !this.showNewGameDialog) {
      // Local play mode - get legal moves from backend
      this.analysisService.getLegalMoves(this.currentFen, square).pipe(
        catchError(() => of([]))
      ).subscribe(moves => {
        this.legalMoves = moves;
      });
      return;
    }

    this.analysisService.getLegalMoves(this.currentFen, square).pipe(
      catchError(() => of([]))
    ).subscribe(moves => {
      this.legalMoves = moves;
    });
  }

  onPieceDrop(event: { from: string; to: string }): void {
    this.legalMoves = [];
    this.errorMessage = '';

    if (this.gameId) {
      this.gameService.makeMove(this.gameId, event.from, event.to).pipe(
        catchError(err => {
          this.errorMessage = err.error?.message || 'Invalid move';
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(result => {
        if (result) {
          this.handleAnalysisResult(result);
          this.lastMove = event;
        }
      });
    } else {
      // Analyze the position after move
      this.lastMove = event;
      this.analysisSubject.next(this.currentFen);
    }
  }

  private handleAnalysisResult(result: AnalysisResult): void {
    this.currentFen = result.currentFen;
    this.evaluation = result.evaluation;
    this.recommendedMoves = result.recommendedMoves || [];
    this.openingName = result.openingName || '';
    this.isCheck = result.isCheck;
    this.isCheckmate = result.isCheckmate;
    this.isStalemate = result.isStalemate;
    this.checkSquare = null; // Would need to find king square if in check
    this.recommendedSquares = this.recommendedMoves.map(m => {
      // Extract destination square from move notation (simplified)
      const match = m.move.match(/[a-h][1-8]/g);
      return match ? match[match.length - 1] : '';
    }).filter(s => s !== '');
  }

  undoMove(): void {
    if (this.gameId && this.moves.length > 0) {
      this.gameService.undoMove(this.gameId).pipe(
        catchError(err => {
          this.errorMessage = 'Failed to undo move';
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(game => {
        if (game) {
          this.currentFen = game.currentFen || this.chessLogic.getInitialFen();
          this.moves = game.moves || [];
          this.lastMove = null;
          this.recommendedMoves = [];
          this.recommendedSquares = [];
        }
      });
    }
  }

  flipBoard(): void {
    this.isFlipped = !this.isFlipped;
  }

  resetGame(): void {
    this.showNewGameDialog = true;
    this.gameId = null;
    this.resetBoard();
  }

  exportGame(): void {
    if (!this.gameId) return;
    this.gameService.exportPgn(this.gameId).pipe(
      catchError(() => of(''))
    ).subscribe(pgn => {
      if (pgn) {
        navigator.clipboard.writeText(pgn);
        this.errorMessage = ''; // Could show success toast instead
      }
    });
  }

  showImport(): void {
    this.showImportDialog = true;
  }

  importGame(): void {
    if (!this.importPgn.trim()) return;
    this.gameService.importGame(this.importPgn).pipe(
      catchError(err => {
        this.errorMessage = 'Failed to import game';
        return of(null);
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(game => {
      if (game) {
        this.gameId = game.id;
        this.currentFen = game.currentFen || this.chessLogic.getInitialFen();
        this.moves = game.moves || [];
        this.playerColor = game.playerColor as 'WHITE' | 'BLACK';
        this.isFlipped = this.playerColor === 'BLACK';
        this.openingName = game.openingName || '';
        this.showImportDialog = false;
        this.showNewGameDialog = false;
        this.importPgn = '';
      }
    });
  }

  onMoveHistoryClick(index: number): void {
    // Navigate to a specific move in history
    this.currentMoveIndex = index;
  }

  onRecommendedMoveHover(move: string | null): void {
    if (move) {
      const match = move.match(/[a-h][1-8]/g);
      this.recommendedSquares = match ? [match[match.length - 1]] : [];
    } else {
      this.recommendedSquares = this.recommendedMoves.map(m => {
        const match = m.move.match(/[a-h][1-8]/g);
        return match ? match[match.length - 1] : '';
      }).filter(s => s !== '');
    }
  }

  getStatusText(): string {
    if (this.isCheckmate) return 'Checkmate!';
    if (this.isStalemate) return 'Stalemate!';
    if (this.isCheck) return 'Check!';
    return this.chessLogic.isWhiteTurn(this.currentFen) ? "White's turn" : "Black's turn";
  }
}
