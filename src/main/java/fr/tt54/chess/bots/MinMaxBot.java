package fr.tt54.chess.bots;

import fr.tt54.chess.game.ChessPiece;
import fr.tt54.chess.game.IntegerChessBoard;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class MinMaxBot extends AbstractChessBot{

    private static final int mateValue = 100000;

    private IntegerChessBoard board;
    private final int depth;

    public MinMaxBot(boolean white, int depth) {
        super(white);
        this.depth = depth;
    }

    @Override
    public void playMove(IntegerChessBoard board) {
        this.board = board;

        int bestMove = -1;
        if(this.white) {
            int max = Integer.MIN_VALUE;
            for (int move : board.getAllowedMoves()) {
                board.playMove(move);
                int eval = getMinMaxEval(depth-1, depth);
                board.undoMove(move);

                if(eval >= max){
                    max = eval;
                    bestMove = move;
                }
            }
        } else {
            int min = Integer.MAX_VALUE;
            for (int move : board.getAllowedMoves()) {
                board.playMove(move);
                int eval = getMinMaxEval(depth-1, depth);
                board.undoMove(move);

                if(eval <= min){
                    min = eval;
                    bestMove = move;
                }

                System.out.println(eval);
            }

            System.out.println("best is " + min);
        }

        if(bestMove != -1){
            board.playMove(bestMove);
        }
    }

    public int getMinMaxEval(int depth, int maxDepth){
        if(depth == 0){
            return evaluatePosition();
        }

        boolean evalWhite = board.isWhiteToPlay();

        List<Integer> moves = this.board.getAllowedMoves();
        if(moves.isEmpty()){
            return this.board.isKingInCheck() ? (evalWhite ? -100000 + (maxDepth - depth) : 100000 - (maxDepth - depth)) : 0;
        } else {
            if(evalWhite){
                // We should maximize the eval

                int max = Integer.MIN_VALUE;
                for(int move : moves){
                    this.board.playMove(move);
                    int eval = getMinMaxEval(depth - 1, maxDepth);
                    this.board.undoMove(move);

                    if(max < eval){
                        max = eval;
                    }
                }

                return max;
            } else {
                // We should minimize the eval

                int min = Integer.MAX_VALUE;
                for(int move : moves){
                    this.board.playMove(move);
                    int eval = getMinMaxEval(depth - 1, maxDepth);
                    this.board.undoMove(move);

                    if(min > eval){
                        min = eval;
                    }
                }

                return min;
            }
        }
    }

    public int evaluatePosition(){
        int eval = 0;
        for(Map.Entry<ChessPiece, Set<Integer>> piecePositions : this.board.getPiecesPositions().entrySet()){
            for(int position : piecePositions.getValue()){
                eval += pieceValue[piecePositions.getKey().getId() + 6];
            }
        }
        return eval;
    }

    private static final int[] pieceValue = {-1000, -900, -500, -320, -300, -100, 0, 100, 300, 320, 500, 900, 1000};
}
