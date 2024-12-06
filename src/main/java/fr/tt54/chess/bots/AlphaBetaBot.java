package fr.tt54.chess.bots;

import fr.tt54.chess.game.ChessPiece;
import fr.tt54.chess.game.IntegerChessBoard;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class AlphaBetaBot extends AbstractChessBot{

    private IntegerChessBoard board;
    private final int depth;

    private int evaluatedPositions = 0;

    public AlphaBetaBot(boolean white, int depth) {
        super(white);
        this.depth = depth;
    }

    @Override
    public void playMove(IntegerChessBoard board) {
        this.evaluatedPositions = 0;
        this.board = board;

        long time = System.currentTimeMillis();

        int bestMove = -1;
        if(this.white) {
            int max = Integer.MIN_VALUE;
            for (int move : board.getAllowedMoves()) {
                board.playMove(move);
                int eval = getAlphaBetaEval(depth-1, depth, Integer.MIN_VALUE, Integer.MAX_VALUE);
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
                int eval = getAlphaBetaEval(depth-1, depth, Integer.MIN_VALUE, Integer.MAX_VALUE);
                board.undoMove(move);

                if(eval <= min){
                    min = eval;
                    bestMove = move;
                }
            }
        }

        if(bestMove != -1){
            board.playMove(bestMove);
        }

        System.out.println("Evaluated : " + evaluatedPositions + " positions in " + (System.currentTimeMillis() - time) + "ms");
    }

    public int getAlphaBetaEval(int depth, int maxDepth, int alpha, int beta){
        if(depth == 0){
            this.evaluatedPositions++;
            return evaluatePosition();
        }

        boolean evalWhite = board.isWhiteToPlay();

        List<Integer> moves = this.board.getAllowedMoves();
        if(moves.isEmpty()){
            this.evaluatedPositions++;
            return this.board.isKingInCheck() ? (evalWhite ? -100000 + (maxDepth - depth) : 100000 - (maxDepth - depth)) : 0;
        } else {
            if(evalWhite){
                // We should maximize the eval

                int max = Integer.MIN_VALUE;
                for(int move : moves){
                    this.board.playMove(move);
                    max = Math.max(max, getAlphaBetaEval(depth - 1, maxDepth, alpha, beta));
                    this.board.undoMove(move);

                    if(max >= beta){
                        return max;
                    }
                    alpha = Math.max(alpha, max);
                }

                return max;
            } else {
                // We should minimize the eval

                int min = Integer.MAX_VALUE;
                for(int move : moves){
                    this.board.playMove(move);
                    min = Math.min(min, getAlphaBetaEval(depth - 1, maxDepth, alpha, beta));
                    this.board.undoMove(move);

                    if(alpha >= min){
                        return min;
                    }
                    beta = Math.min(beta, min);
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
