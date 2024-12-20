package fr.tt54.chess;

import fr.tt54.chess.bots.AlphaBetaBot;
import fr.tt54.chess.bots.MinMaxBot;
import fr.tt54.chess.game.AbstractChessBoard;
import fr.tt54.chess.game.ChessBoard;
import fr.tt54.chess.game.IntegerChessBoard;
import fr.tt54.chess.game.QuickChessBoard;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class ChessMain {

    public static final DecimalFormat longFormat = new DecimalFormat("#,###", new DecimalFormatSymbols(Locale.ENGLISH));
    public static FrameManager manager;

    public static void main(String[] args) {
        manager = new FrameManager();
        manager.setBot(new AlphaBetaBot(false, 4));

        IntegerChessBoard active = new IntegerChessBoard();
        System.out.println(active.isWhiteToPlay());
        manager.showFrame(active);


    }

    public static void compareChessBoards(){
        System.out.println("Standard implementation perft");
        ChessBoard board1 = new ChessBoard();
        board1.launchPerftChecks();
        System.out.println();

        System.out.println("Quick implementation perft");
        QuickChessBoard board = new QuickChessBoard();
        board.launchPerftChecks();
        System.out.println();

        System.out.println("Quick integer implementation perft");
        IntegerChessBoard b = new IntegerChessBoard();
        b.launchPerftChecks();
    }
}