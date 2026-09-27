import { create } from 'zustand';

export type Player = 'PLAYER_1' | 'PLAYER_2';
export type MatchStatus = 'ONGOING' | 'WIN_PLAYER_1' | 'WIN_PLAYER_2' | 'DRAW';

export interface Position {
  x: number;
  y: number;
}

export interface MoveRequest {
  from: Position | null;
  to: Position;
}

export interface Piece {
  player: Player;
}

export interface GameStateResponse {
  roomId: string;
  matchStatus: MatchStatus;
  currentPlayer: Player | null;
  pieces: Record<string, Piece>;
  pieceCounts: Record<string, number>;
  player1Score: number;
  player2Score: number;
  winner: Player | null;
  [key: string]: unknown;
}

export interface GameStoreState {
  isConnected: boolean;
  roomId: string | null;
  errorMessage: string | null;
  gameState: GameStateResponse | null;

  setIsConnected: (connected: boolean) => void;
  setRoomId: (roomId: string | null) => void;
  setErrorMessage: (message: string | null) => void;
  setGameState: (gameState: GameStateResponse | null) => void;
  clearError: () => void;
  resetGame: () => void;
}

export const useGameStore = create<GameStoreState>((set) => ({
  isConnected: false,
  roomId: null,
  errorMessage: null,
  gameState: null,

  setIsConnected: (connected: boolean) => set({ isConnected: connected }),
  setRoomId: (roomId: string | null) => set({ roomId }),
  setErrorMessage: (message: string | null) => set({ errorMessage: message }),
  setGameState: (gameState: GameStateResponse | null) => set({ gameState }),
  clearError: () => set({ errorMessage: null }),
  resetGame: () => set({ roomId: null, gameState: null, errorMessage: null }),
}));
