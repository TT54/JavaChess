package fr.tt54.chess.graphics;

import fr.tt54.chess.ChessMain;
import fr.tt54.chess.game.AbstractChessBoard;
import fr.tt54.chess.game.ChessBoard;
import fr.tt54.chess.game.ChessMove;
import fr.tt54.chess.game.ChessPiece;
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

public class ChessPanel extends GraphicPanel {

    private AbstractChessBoard board;

    private final Set<GraphicNode> moveIndicators = new HashSet<>();

    public ChessPanel(AbstractChessBoard board) {
        setBoard(board);
    }

    public void setBoard(AbstractChessBoard board) {
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
            Set<Tuple<Integer, Integer>> positions = board.getPiecePositions(piece);

            for(Tuple<Integer, Integer> position : positions) {
                int row = position.x();
                int column = position.y();
                ChessPieceNode node = new ChessPieceNode(this, piece,
                        (column - 4) * squareSize + (squareSize - pieceSize), (7 - row - 4) * squareSize + (squareSize - pieceSize),
                        pieceSize, pieceSize,
                        row, column);

                node.setClickAction(new ClickAction<NodeClickedEvent>() {
                    @Override
                    public void onClick(NodeClickedEvent nodeClickedEvent) {
                        List<ChessMove> moves = board.getAllowedMoves();

                        for(GraphicNode n : new HashSet<>(moveIndicators)){
                            removeNode(n);
                            moveIndicators.remove(n);
                        }

                        if(selected[0] != node && node.getPiece().isWhite() == board.isWhiteToPlay()) {
                            moveIndicators.add(new RectangleNode(ChessPanel.this,(column - 4) * squareSize + (squareSize - pieceSize), (7 - row - 4) * squareSize + (squareSize - pieceSize),
                                    squareSize, squareSize, new Color(200, 200, 0, 50)));
                            for (ChessMove move : moves) {
                                if (move.initialRow() == node.getRow() && move.initialColumn() == node.getColumn()) {
                                    if(!move.isPromotion() || Math.abs(move.promotePiece().getId()) == 5){
                                        RectangleNode indicatorNode = getIndicatorNode(move, squareSize, pieceSize);
                                        moveIndicators.add(indicatorNode);
                                    }
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

    private RectangleNode getIndicatorNode(ChessMove move, int squareSize, int pieceSize) {
        RectangleNode indicatorNode = new RectangleNode(this,  (move.finalColumn() - 4) * squareSize + (squareSize - pieceSize), (7 - move.finalRow() - 4) * squareSize + (squareSize - pieceSize),
                squareSize, squareSize, new Color(200, 0, 0, 50));
        indicatorNode.setClickAction(nodeClickedEvent1 -> {
            board.playMove(move);
            this.refreshBoard();
        });
        return indicatorNode;
    }
}
