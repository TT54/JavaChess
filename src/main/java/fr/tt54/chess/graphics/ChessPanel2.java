package fr.tt54.chess.graphics;

import fr.tt54.chess.ChessMain;
import fr.tt54.chess.FrameManager;
import fr.tt54.chess.game.AbstractChessBoard;
import fr.tt54.chess.game.ChessMove;
import fr.tt54.chess.game.ChessPiece;
import fr.tt54.chess.game.IntegerChessBoard;
import fr.tt54.chess.utils.Tuple;
import fr.ttgraphiclib.graphics.GraphicPanel;
import fr.ttgraphiclib.graphics.events.NodeClickedEvent;
import fr.ttgraphiclib.graphics.interfaces.ClickAction;
import fr.ttgraphiclib.graphics.nodes.GraphicNode;
import fr.ttgraphiclib.graphics.nodes.RectangleNode;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChessPanel2 extends GraphicPanel {

    private IntegerChessBoard board;

    private final Set<GraphicNode> moveIndicators = new HashSet<>();

    public ChessPanel2(IntegerChessBoard board) {
        setBoard(board);
    }

    public void setBoard(IntegerChessBoard board) {
        this.board = board;
        refreshBoard();
    }

    public void refreshBoard(){
        for(GraphicNode node : new ArrayList<>(this.getNodes())){
            this.removeNode(node);
            moveIndicators.remove(node);
        }

        int width = ChessMain.manager.frame.getWidth();
        int height = ChessMain.manager.frame.getHeight();

        int windowSize = Math.min(width, height);

        int pieceSize = windowSize / 9;
        int squareSize = windowSize / 9;

        Color blackSquareColor = new Color(137, 121, 110);
        Color whiteSquareColor = new Color(189, 178, 169);

        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                new RectangleNode(this, (i - 4) * squareSize + (squareSize - pieceSize) / 2, (j - 4) * squareSize + (squareSize - pieceSize) / 2, squareSize, squareSize, (i+j) % 2 == 1 ? blackSquareColor : whiteSquareColor);
            }
        }

        ChessPieceNode[] selected = new ChessPieceNode[1];
        for(ChessPiece piece : ChessPiece.values()){
            Set<Integer> positions = board.getPiecePositions(piece);

            for(int position : positions) {
                int row = IntegerChessBoard.getRow(position);
                int column = IntegerChessBoard.getColumn(position);
                ChessPieceNode node = new ChessPieceNode(this, piece,
                        (column - 4) * squareSize + (squareSize - pieceSize), (7 - row - 4) * squareSize + (squareSize - pieceSize),
                        pieceSize, pieceSize,
                        row, column);

                node.setClickAction(new ClickAction<NodeClickedEvent>() {
                    @Override
                    public void onClick(NodeClickedEvent nodeClickedEvent) {
                        List<Integer> moves = board.getAllowedMoves();

                        for(GraphicNode n : new HashSet<>(moveIndicators)){
                            removeNode(n);
                            moveIndicators.remove(n);
                        }

                        FrameManager manager = ChessMain.manager;
                        if(manager.isBotEnabled() && manager.isBotTurn()) {
                            selected[0] = null;
                            return;
                        }

                        if(selected[0] != node && node.getPiece().isWhite() == board.isWhiteToPlay()) {
                            moveIndicators.add(new RectangleNode(ChessPanel2.this,(column - 4) * squareSize + (squareSize - pieceSize), (7 - row - 4) * squareSize + (squareSize - pieceSize),
                                    squareSize, squareSize, new Color(200, 200, 0, 50)));
                            for (int move : moves) {
                                int pos = IntegerChessBoard.getMoveInitialPosition(move);
                                if (pos == IntegerChessBoard.getPosition(node.getRow(), node.getColumn())) {
                                    boolean promotion = move < 0;
                                    if(promotion){
                                        int promotedPiece = (move & 0x70000000) >>> 28;
                                        if(promotedPiece != 2) continue;
                                    }
                                    RectangleNode indicatorNode = getIndicatorNode(move, squareSize, pieceSize);
                                    moveIndicators.add(indicatorNode);
                                }
                            }
                            selected[0] = node;
                        } else {
                            selected[0] = null;
                        }
                    }
                });
            }
        }
    }

    private RectangleNode getIndicatorNode(int move, int squareSize, int pieceSize) {
        int pos = IntegerChessBoard.getMoveFinalPosition(move);
        int row = IntegerChessBoard.getRow(pos);
        int column = IntegerChessBoard.getColumn(pos);
        RectangleNode indicatorNode = new RectangleNode(this,  (column - 4) * squareSize + (squareSize - pieceSize), (7 - row - 4) * squareSize + (squareSize - pieceSize),
                squareSize, squareSize, new Color(200, 0, 0, 50));
        indicatorNode.setClickAction(nodeClickedEvent1 -> {
            board.playMove(move);
            ChessMain.manager.movePlayedOnBoard();
        });
        return indicatorNode;
    }
}
