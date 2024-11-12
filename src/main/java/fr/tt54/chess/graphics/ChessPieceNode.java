package fr.tt54.chess.graphics;

import fr.tt54.chess.game.ChessBoard;
import fr.tt54.chess.game.ChessPiece;
import fr.ttgraphiclib.graphics.GraphicPanel;
import fr.ttgraphiclib.graphics.nodes.ImageNode;

import java.net.URL;

public class ChessPieceNode extends ImageNode {

    private final ChessPiece piece;
    private final int row;
    private final int column;

    public ChessPieceNode(GraphicPanel panel, ChessPiece piece, double x, double y, double width, double height, int row, int column) {
        super(panel, x, y, width, height, getPieceImage(piece));
        this.piece = piece;
        this.row = row;
        this.column = column;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public ChessPiece getPiece() {
        return piece;
    }

    public static URL getPieceImage(ChessPiece piece) {
        return ChessPieceNode.class.getResource("/" + piece.name().toLowerCase() + ".png");
    }

    public int getPiecePosition() {
        return ChessBoard.getPosition(row, column);
    }
}
