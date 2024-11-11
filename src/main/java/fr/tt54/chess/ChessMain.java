package fr.tt54.chess;

import fr.tt54.chess.game.ChessBoard;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class ChessMain {

    public static final DecimalFormat longFormat = new DecimalFormat("#,###", new DecimalFormatSymbols(Locale.ENGLISH));
    public static FrameManager manager;

    public static void main(String[] args) {
        manager = new FrameManager();
        ChessBoard active = new ChessBoard("r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10");
        manager.showFrame(active);
    }
}