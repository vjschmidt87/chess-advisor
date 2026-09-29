export interface Position {
  row: number;
  col: number;
}

export interface ChessMove {
  from: Position;
  to: Position;
  promotion?: string;
}

export interface MoveNotation {
  moveNumber: number;
  whiteMove: string;
  blackMove?: string;
  fen: string;
  evaluation?: string;
}

export interface RecommendedMove {
  move: string;
  score: number;
  explanation: string;
}

export interface AnalysisResult {
  currentFen: string;
  evaluation: number;
  openingName: string;
  recommendedMoves: RecommendedMove[];
  isCheck: boolean;
  isCheckmate: boolean;
  isStalemate: boolean;
}

export interface GameInfo {
  id: number;
  playerColor: 'WHITE' | 'BLACK';
  status: 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED';
  pgn: string;
  openingName: string;
  currentFen: string;
  moves: MoveNotation[];
  createdAt: string;
}

export interface GameListItem {
  id: number;
  playerColor: 'WHITE' | 'BLACK';
  status: string;
  openingName: string;
  totalMoves: number;
  createdAt: string;
}

export type PieceType = 'K' | 'Q' | 'R' | 'B' | 'N' | 'P' | 'k' | 'q' | 'r' | 'b' | 'n' | 'p' | '';

export interface Square {
  row: number;
  col: number;
  piece: PieceType;
  isLight: boolean;
  isSelected: boolean;
  isLegalMove: boolean;
  isLastMove: boolean;
  isCheck: boolean;
  isRecommended: boolean;
}

export interface BoardState {
  squares: Square[][];
  fen: string;
  isWhiteTurn: boolean;
  isFlipped: boolean;
}
