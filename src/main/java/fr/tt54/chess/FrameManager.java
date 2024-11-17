package fr.tt54.chess;

import fr.tt54.chess.bots.AbstractChessBot;
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
    private IntegerChessBoard displayedBoard2;

    private AbstractChessBot bot;

    public void showFrame(AbstractChessBoard displayedBoard){
        this.displayedBoard = displayedBoard;

        frame = new Frame("Chess", 800, 800);
        panel = new ChessPanel(displayedBoard);
        GraphicManager.enable(frame, panel);
    }

    public void showFrame(IntegerChessBoard displayedBoard){
        frame = new Frame("Chess", 800, 800);
        GraphicManager.enable(frame, panel2 = new ChessPanel2(this.displayedBoard2 = displayedBoard));

        movePlayedOnBoard();
    }

    public void showGame(AbstractChessBoard displayedBoard){
        this.displayedBoard = displayedBoard;
        this.panel.setBoard(this.displayedBoard);
    }

    public void setBot(AbstractChessBot bot){
        this.bot = bot;
    }

    public boolean isBotEnabled(){
        return this.bot != null;
    }

    public boolean isBotTurn(){
        return this.displayedBoard2.isWhiteToPlay() == this.bot.isWhite();
    }

    public void movePlayedOnBoard(){
        panel2.refreshBoard();

        System.out.println("ok ?");
        System.out.println(this.displayedBoard2.isWhiteToPlay());
        if(this.isBotEnabled() && this.isBotTurn()){
            System.out.println("all");
            this.bot.playMove(this.displayedBoard2);
            panel2.refreshBoard();
        }
    }

}
