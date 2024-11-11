package fr.tt54.chess.game;

import fr.tt54.chess.ChessMain;
import fr.tt54.chess.FrameManager;
import fr.tt54.chess.utils.Tuple;

import java.util.List;
import java.util.Set;

public abstract class AbstractChessBoard {

    public abstract List<ChessMove> getAllowedMoves();
    public abstract void playMove(ChessMove move);
    public abstract void undoMove(ChessMove move);
    public abstract ChessPiece getPiece(int row, int column);
    public abstract ChessPiece getPiece(int position);
    public abstract Set<Tuple<Integer, Integer>> getPiecePositions(ChessPiece piece);

    public abstract int getEnPassant();
    public abstract boolean[][] getCastles();

    public abstract void loadFen(String fen);

    public int perft(int depth, int depthPrint){
        if(depth == 0){
            return 1;
        }

        int count = 0;
        for(ChessMove move : getAllowedMoves()){
            playMove(move);
            int moveCount = perft(depth - 1, depthPrint);
            undoMove(move);

            if(depthPrint == depth){
                String extra = move.isPromotion() ? move.promotePiece().getFenChar() + "" : "";
                System.out.println(getPositionString(move.initialRow(), move.initialColumn()) + getPositionString(move.finalRow(), move.finalColumn()) + extra + ": " + moveCount);
            }

            count += moveCount;
        }

        return count;
    }

    public long evaluatePerftTime(int depth, int printDepth){
        long time = System.currentTimeMillis();
        System.out.println("Perft result : " + perft(depth, printDepth));
        return System.currentTimeMillis() - time;
    }

    public void testPerformances(int depth){
        long time = System.currentTimeMillis();
        perft(5, -1);
        time = System.currentTimeMillis() - time;
        System.out.println("Time Elapsed : " + time + "ms");
    }

    private static final String[] perftFens = new String[]{
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 ",
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -",
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
            "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10"
    };
    private static final int[][] perftExpectedResults = new int[][] {
            {20, 400, 8902, 197281, 4865609, 119060324},
            {48, 2039, 97862, 4085603, 193690690},
            {14, 191, 2812, 43238, 674624, 11030083, 178633661},
            {6, 264, 9467, 422333, 15833292, 706045033},
            {44, 1486, 62379, 2103487, 89941194},
            {46, 2079, 89890, 3894594, 164075551}
    };


    /**
     *
     * @return An array containing for each perft position : -1 if the perft completely success, the rank of the first perft fail otherwise
     */
    public int[] launchPerftChecks(){
        int[] results = new int[perftFens.length];
        for(int i = 0; i < perftFens.length; i++){
            loadFen(perftFens[i]);
            ChessMain.manager.panel.setBoard(this);
            results[i] = -1;
            for(int j = 0; j < perftExpectedResults[i].length; j++){
                long elapsedTime = System.nanoTime();
                int result = perft(j+1, -1);
                elapsedTime = System.nanoTime() - elapsedTime;

                if(result != perftExpectedResults[i][j]){
                    results[i] = j+1;
                    System.out.println("Perft " + (i+1) + " at depth " + (j+1) + " failed (" + ChessMain.longFormat.format(result) + " instead of " + ChessMain.longFormat.format(perftExpectedResults[i][j]) + ") in " + ChessMain.longFormat.format(elapsedTime / 1000000) + "ms" + " (" + ChessMain.longFormat.format(result * 1000000000L / elapsedTime) + " nodes/s)");
                    break;
                }
                System.out.println("Perft " + (i+1) + " at depth " + (j+1) + " succeed (" + ChessMain.longFormat.format(result) + ") in " + ChessMain.longFormat.format(elapsedTime / 1000000) + "ms" + " (" + ChessMain.longFormat.format(result * 1000000000L / elapsedTime) + " nodes/s)");
            }
        }
        return results;
    }

    public static boolean isInBoard(int row, int column){
        return row >= 0 && row <= 7 && column >= 0 && column <= 7;
    }

    public static int[] getPosition(String pos){
        int row = pos.charAt(1) - '1';
        int column = pos.charAt(0) - 'a';
        System.out.println(pos);
        System.out.println(row + " - " + column);
        return new int[] {row, column};
    }

    public static int[] getPosition(int pos){
        return new int[] {pos / 8, pos % 8};
    }

    public static int getPosition(int row, int column){
        return row * 8 + column;
    }

    public static int getPosition(int[] pos){
        return getPosition(pos[0], pos[1]);
    }

    public static String getPositionString(int row, int column){
        return  "" + ((char) (column + 'a')) + ((char) (row + '1'));
    }

    public static String getPositionString(int position){
        int[] pos = getPosition(position);
        return getPositionString(pos[0], pos[1]);
    }
}
