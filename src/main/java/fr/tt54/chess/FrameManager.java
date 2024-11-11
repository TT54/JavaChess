package fr.tt54.chess;

import fr.tt54.chess.game.ChessBoard;
import fr.tt54.chess.graphics.ChessPanel;
import fr.ttgraphiclib.GraphicManager;
import fr.ttgraphiclib.thread.Frame;

public class FrameManager {

    public ChessPanel panel;
    public Frame frame;

    private ChessBoard displayedBoard;

    public void showFrame(ChessBoard displayedBoard){
        this.displayedBoard = displayedBoard;

        frame = new Frame("Chess", 800, 800);
        panel = new ChessPanel(displayedBoard);
        GraphicManager.enable(frame, panel);
    }

    public void showGame(ChessBoard displayedBoard){
        this.displayedBoard = displayedBoard;
        this.panel.setBoard(this.displayedBoard);
    }

}
