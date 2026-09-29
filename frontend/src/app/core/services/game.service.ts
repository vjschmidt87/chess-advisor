import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { AnalysisResult, GameInfo, GameListItem } from '@shared/models/chess.models';

@Injectable({ providedIn: 'root' })
export class GameService {
  private readonly API_URL = `${environment.apiUrl}/games`;

  constructor(private http: HttpClient) {}

  createGame(playerColor: 'WHITE' | 'BLACK'): Observable<GameInfo> {
    return this.http.post<GameInfo>(this.API_URL, { playerColor });
  }

  getGame(id: number): Observable<GameInfo> {
    return this.http.get<GameInfo>(`${this.API_URL}/${id}`);
  }

  getUserGames(): Observable<GameListItem[]> {
    return this.http.get<GameListItem[]>(this.API_URL);
  }

  makeMove(gameId: number, from: string, to: string, promotion?: string): Observable<AnalysisResult> {
    return this.http.post<AnalysisResult>(`${this.API_URL}/${gameId}/move`, { from, to, promotion });
  }

  undoMove(gameId: number): Observable<GameInfo> {
    return this.http.post<GameInfo>(`${this.API_URL}/${gameId}/undo`, {});
  }

  exportPgn(gameId: number): Observable<string> {
    return this.http.get(`${this.API_URL}/${gameId}/export`, { responseType: 'text' });
  }

  importGame(pgn: string): Observable<GameInfo> {
    return this.http.post<GameInfo>(`${this.API_URL}/import`, { pgn });
  }

  deleteGame(gameId: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${gameId}`);
  }
}
