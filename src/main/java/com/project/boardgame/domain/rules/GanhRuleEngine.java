package com.project.boardgame.domain.rules;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.MoveStep;
import com.project.boardgame.domain.models.Piece;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;

public class GanhRuleEngine implements GameRuleEngine {

    @Override
    public java.util.Optional<Player> checkWinner(BoardState board, Position lastMove) {
        // TODO: Cần implement logic thắng thua cho Cờ Gánh (sẽ làm ở lệnh sau)
        return java.util.Optional.empty();
    }

    @Override
    public boolean isValidMove(BoardState board, MoveStep move, Player player) {
        if (player != board.currentPlayer()) {
            return false;
        }
        
        Position from = move.from();
        Position to = move.to();

        // Check bounds
        if (!isWithinBounds(from) || !isWithinBounds(to)) {
            return false;
        }

        // Check from position belongs to player
        Piece pieceAtFrom = board.pieces().get(from);
        if (pieceAtFrom == null || pieceAtFrom.player() != player) {
            return false;
        }

        // Check to position is empty
        if (board.pieces().containsKey(to)) {
            return false;
        }

        // Check if move is adjacent
        int dx = Math.abs(to.x() - from.x());
        int dy = Math.abs(to.y() - from.y());

        if (dx > 1 || dy > 1 || (dx == 0 && dy == 0)) {
            return false; // Moving more than 1 step or not moving
        }

        if (dx == 1 && dy == 1) {
            // Diagonal move is only valid if (x + y) is even
            if ((from.x() + from.y()) % 2 != 0) {
                return false;
            }
        }

        // Enforce mandatory Gánh (Mở trap)
        if (hasAnyGanhMove(board, player)) {
            if (!moveResultsInGanh(board, move, player)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public BoardState executeMove(BoardState board, MoveStep move, Player player) {
        if (!isValidMove(board, move, player)) {
            throw new IllegalArgumentException("Invalid move");
        }

        java.util.Map<Position, Piece> newPieces = new java.util.HashMap<>(board.pieces());
        
        // Move piece
        newPieces.remove(move.from());
        newPieces.put(move.to(), new Piece(player));

        Position to = move.to();
        Player opponent = (player == Player.PLAYER_1) ? Player.PLAYER_2 : Player.PLAYER_1;

        // Check horizontal Ganh
        checkAndApplyGanh(newPieces, player, opponent, 
            new Position(to.x() - 1, to.y()), 
            new Position(to.x() + 1, to.y()));

        // Check vertical Ganh
        checkAndApplyGanh(newPieces, player, opponent, 
            new Position(to.x(), to.y() - 1), 
            new Position(to.x(), to.y() + 1));

        // Check diagonal Ganh (only if target cell has diagonals)
        if ((to.x() + to.y()) % 2 == 0) {
            checkAndApplyGanh(newPieces, player, opponent, 
                new Position(to.x() - 1, to.y() - 1), 
                new Position(to.x() + 1, to.y() + 1));
            
            checkAndApplyGanh(newPieces, player, opponent, 
                new Position(to.x() - 1, to.y() + 1), 
                new Position(to.x() + 1, to.y() - 1));
        }

        // Apply Chẹt (Bao vây) logic
        applyChet(newPieces, player, opponent);

        Player nextPlayer = player == Player.PLAYER_1 ? Player.PLAYER_2 : Player.PLAYER_1;
        return new BoardState(newPieces, nextPlayer);
    }

    private void applyChet(java.util.Map<Position, Piece> pieces, Player player, Player opponent) {
        java.util.Set<Position> visited = new java.util.HashSet<>();
        
        for (int x = 0; x <= 4; x++) {
            for (int y = 0; y <= 4; y++) {
                Position startPos = new Position(x, y);
                Piece piece = pieces.get(startPos);
                
                if (piece != null && piece.player() == opponent && !visited.contains(startPos)) {
                    java.util.List<Position> group = new java.util.ArrayList<>();
                    java.util.Queue<Position> queue = new java.util.LinkedList<>();
                    boolean hasLiberty = false;
                    
                    queue.add(startPos);
                    visited.add(startPos);
                    
                    while (!queue.isEmpty()) {
                        Position current = queue.poll();
                        group.add(current);
                        
                        for (Position adj : getAdjacentPositions(current)) {
                            Piece adjPiece = pieces.get(adj);
                            if (adjPiece == null) {
                                hasLiberty = true;
                            } else if (adjPiece.player() == opponent && !visited.contains(adj)) {
                                visited.add(adj);
                                queue.add(adj);
                            }
                        }
                    }
                    
                    if (!hasLiberty) {
                        for (Position p : group) {
                            pieces.put(p, new Piece(player));
                        }
                    }
                }
            }
        }
    }

    private java.util.List<Position> getAdjacentPositions(Position p) {
        java.util.List<Position> adj = new java.util.ArrayList<>();
        int[] dx = {-1, 0, 1, -1, 1, -1, 0, 1};
        int[] dy = {-1, -1, -1, 0, 0, 1, 1, 1};
        
        for (int i = 0; i < 8; i++) {
            int nx = p.x() + dx[i];
            int ny = p.y() + dy[i];
            Position np = new Position(nx, ny);
            if (isWithinBounds(np)) {
                if (dx[i] != 0 && dy[i] != 0) {
                    if ((p.x() + p.y()) % 2 == 0) {
                        adj.add(np);
                    }
                } else {
                    adj.add(np);
                }
            }
        }
        return adj;
    }

    private void checkAndApplyGanh(java.util.Map<Position, Piece> pieces, Player player, Player opponent, Position p1, Position p2) {
        if (isWithinBounds(p1) && isWithinBounds(p2)) {
            Piece piece1 = pieces.get(p1);
            Piece piece2 = pieces.get(p2);
            
            if (piece1 != null && piece1.player() == opponent && 
                piece2 != null && piece2.player() == opponent) {
                // Apply Ganh: convert opponent pieces
                pieces.put(p1, new Piece(player));
                pieces.put(p2, new Piece(player));
            }
        }
    }

    private boolean isWithinBounds(Position p) {
        return p.x() >= 0 && p.x() <= 4 && p.y() >= 0 && p.y() <= 4;
    }

    private boolean moveResultsInGanh(BoardState board, MoveStep move, Player player) {
        Position to = move.to();
        Player opponent = (player == Player.PLAYER_1) ? Player.PLAYER_2 : Player.PLAYER_1;
        
        java.util.Map<Position, Piece> pieces = board.pieces();
        
        // 1. Horizontal
        if (isOpponent(pieces, new Position(to.x() - 1, to.y()), opponent) &&
            isOpponent(pieces, new Position(to.x() + 1, to.y()), opponent)) {
            return true;
        }
        
        // 2. Vertical
        if (isOpponent(pieces, new Position(to.x(), to.y() - 1), opponent) &&
            isOpponent(pieces, new Position(to.x(), to.y() + 1), opponent)) {
            return true;
        }
        
        // 3. Diagonal
        if ((to.x() + to.y()) % 2 == 0) {
            if (isOpponent(pieces, new Position(to.x() - 1, to.y() - 1), opponent) &&
                isOpponent(pieces, new Position(to.x() + 1, to.y() + 1), opponent)) {
                return true;
            }
            if (isOpponent(pieces, new Position(to.x() - 1, to.y() + 1), opponent) &&
                isOpponent(pieces, new Position(to.x() + 1, to.y() - 1), opponent)) {
                return true;
            }
        }
        
        return false;
    }

    private boolean isOpponent(java.util.Map<Position, Piece> pieces, Position p, Player opponent) {
        if (!isWithinBounds(p)) return false;
        Piece piece = pieces.get(p);
        return piece != null && piece.player() == opponent;
    }

    private boolean hasAnyGanhMove(BoardState board, Player player) {
        java.util.Map<Position, Piece> pieces = board.pieces();
        for (java.util.Map.Entry<Position, Piece> entry : pieces.entrySet()) {
            if (entry.getValue().player() == player) {
                Position from = entry.getKey();
                for (Position to : getAdjacentPositions(from)) {
                    if (!pieces.containsKey(to)) {
                        MoveStep move = new MoveStep(from, to);
                        if (moveResultsInGanh(board, move, player)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
