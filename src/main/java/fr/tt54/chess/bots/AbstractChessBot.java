package fr.tt54.chess.bots;

import fr.tt54.chess.game.IntegerChessBoard;

public abstract class AbstractChessBot {

    protected boolean white;

    public AbstractChessBot(boolean white) {
        this.white = white;
    }

    public abstract void playMove(IntegerChessBoard board);

    public boolean isWhite() {
        return white;
    }
}
