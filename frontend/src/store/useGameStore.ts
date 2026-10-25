import { create } from 'zustand';

export type Player = 'PLAYER_1' | 'PLAYER_2';
export type MatchStatus = 'ONGOING' | 'WIN_PLAYER_1' | 'WIN_PLAYER_2' | 'DRAW';

export interface Position {
  x: number;
  y: number;
}

export interface MoveRequest {
  from: Position | null;
  to?: Position | null;
  direction?: 'LEFT' | 'RIGHT' | string;
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
  gameType: string;
  [key: string]: unknown;
}

export interface GameStoreState {
  isConnected: boolean;
  roomId: string | null;
  errorMessage: string | null;
  gameState: GameStateResponse | null;
  myPlayer: Player | null;
  roomGameType: string | null;
  opponentLeft: boolean;
  isBotMatch: boolean;

  setIsConnected: (connected: boolean) => void;
  setRoomId: (roomId: string | null) => void;
  setErrorMessage: (message: string | null) => void;
  setGameState: (gameState: GameStateResponse | null) => void;
  setMyPlayer: (player: Player | null) => void;
  setRoomGameType: (type: string | null) => void;
  setOpponentLeft: (opponentLeft: boolean) => void;
  setIsBotMatch: (isBotMatch: boolean) => void;
  clearError: () => void;
  resetGame: () => void;
}

export const useGameStore = create<GameStoreState>((set) => ({
  isConnected: false,
  roomId: null,
  errorMessage: null,
  gameState: null,
  myPlayer: null,
  roomGameType: null,
  opponentLeft: false,
  isBotMatch: false,

  setIsConnected: (connected: boolean) => set({ isConnected: connected }),
  setRoomId: (roomId: string | null) => set({ roomId }),
  setErrorMessage: (message: string | null) => set({ errorMessage: message }),
  setGameState: (gameState: GameStateResponse | null) => set({ gameState }),
  setMyPlayer: (player: Player | null) => set({ myPlayer: player }),
  setRoomGameType: (type: string | null) => set({ roomGameType: type }),
  setOpponentLeft: (opponentLeft: boolean) => set({ opponentLeft }),
  setIsBotMatch: (isBotMatch: boolean) => set({ isBotMatch }),
  clearError: () => set({ errorMessage: null }),
  resetGame: () => set({ roomId: null, gameState: null, errorMessage: null, myPlayer: null, roomGameType: null, opponentLeft: false, isBotMatch: false }),
}));
