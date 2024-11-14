package fr.tt54.chess;

import fr.tt54.chess.game.AbstractChessBoard;
import fr.tt54.chess.game.ChessBoard;
import fr.tt54.chess.game.IntegerChessBoard;
import fr.tt54.chess.graphics.ChessPanel;
import fr.tt54.chess.graphics.ChessPanel2;
import fr.ttgraphiclib.GraphicManager;
import fr.ttgraphiclib.thread.Frame;

public class FrameManager {

    public ChessPanel panel;
    public ChessPanel2 panel2;
    public Frame frame;

    private AbstractChessBoard displayedBoard;

    public void showFrame(AbstractChessBoard displayedBoard){
        this.displayedBoard = displayedBoard;

        frame = new Frame("Chess", 800, 800);
        panel = new ChessPanel(displayedBoard);
        GraphicManager.enable(frame, panel);
    }

    public void showFrame(IntegerChessBoard displayedBoard){
        frame = new Frame("Chess", 800, 800);
        GraphicManager.enable(frame, panel2 = new ChessPanel2(displayedBoard));
    }

    public void showGame(AbstractChessBoard displayedBoard){
        this.displayedBoard = displayedBoard;
        this.panel.setBoard(this.displayedBoard);
    }

}
