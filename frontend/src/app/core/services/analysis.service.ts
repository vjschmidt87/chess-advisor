import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { AnalysisResult } from '@shared/models/chess.models';

@Injectable({ providedIn: 'root' })
export class AnalysisService {
  private readonly API_URL = `${environment.apiUrl}/analysis`;

  constructor(private http: HttpClient) {}

  analyzePosition(fen: string): Observable<AnalysisResult> {
    return this.http.post<AnalysisResult>(`${this.API_URL}/position`, { fen });
  }

  getLegalMoves(fen: string, square: string): Observable<string[]> {
    return this.http.post<string[]>(`${this.API_URL}/legal-moves`, { fen, square });
  }
}
