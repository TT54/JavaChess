package fr.tt54.chess.game;

import java.util.HashMap;
import java.util.Map;

public enum ChessPiece{

    WHITE_PAWN(1, 'P', -1, -1),
    WHITE_KNIGHT(2, 'N', -1, -1),
    WHITE_BISHOP(3, 'B', 4, 7),
    WHITE_ROOK(4, 'R', 0, 3),
    WHITE_QUEEN(5, 'Q', 0, 7),
    WHITE_KING(6, 'K', -1, -1),
    BLACK_PAWN(-1, 'p', -1, -1),
    BLACK_KNIGHT(-2, 'n', -1, -1),
    BLACK_BISHOP(-3, 'b', 4, 7),
    BLACK_ROOK(-4, 'r', 0, 3),
    BLACK_QUEEN(-5, 'q', 0, 7),
    BLACK_KING(-6, 'k', -1, -1);


    private final int id;
    private final char fenChar;
    private final int beginStandardOffset;
    private final int endStandardOffset;

    ChessPiece(int id, char fenChar, int beginStandardOffset, int endStandardOffset) {
        this.id = id;
        this.fenChar = fenChar;
        this.beginStandardOffset = beginStandardOffset;
        this.endStandardOffset = endStandardOffset;
    }

    public int getId() {
        return id;
    }

    public char getFenChar() {
        return fenChar;
    }

    public boolean isWhite(){
        return this.id > 0;
    }

    public int getBeginStandardOffset() {
        return beginStandardOffset;
    }

    public int getEndStandardOffset() {
        return endStandardOffset;
    }

    private static final Map<Character, ChessPiece> pieceMap = new HashMap<>();
    private static final Map<Integer, ChessPiece> pieceIdMap = new HashMap<>();
    static {
        for(ChessPiece piece : values()){
            pieceMap.put(piece.fenChar, piece);
            pieceIdMap.put(piece.id, piece);
        }
    }
    public static ChessPiece getPiece(char c){
        return pieceMap.get(c);
    }
    public static ChessPiece getPiece(int id){
        return pieceIdMap.get(id);
    }

    public static ChessPiece[] getBlackPieces(){
        return new ChessPiece[] {BLACK_PAWN, BLACK_KNIGHT, BLACK_BISHOP, BLACK_ROOK, BLACK_QUEEN, BLACK_KING};
    }
    public static ChessPiece[] getWhitePieces(){
        return new ChessPiece[] {WHITE_PAWN, WHITE_KNIGHT, WHITE_BISHOP, WHITE_ROOK, WHITE_QUEEN, WHITE_KING};
    }
    public static ChessPiece[] getColoredPieces(boolean white){
        return white ? getWhitePieces() : getBlackPieces();
    }
    public static ChessPiece[] getBlackPromotionPieces(){
        return new ChessPiece[] {BLACK_KNIGHT, BLACK_BISHOP, BLACK_ROOK, BLACK_QUEEN};
    }
    public static ChessPiece[] getWhitePromotionPieces(){
        return new ChessPiece[] {WHITE_KNIGHT, WHITE_BISHOP, WHITE_ROOK, WHITE_QUEEN};
    }
    public static ChessPiece[] getColoredPromotionPieces(boolean white){
        return white ? getWhitePromotionPieces() : getBlackPromotionPieces();
    }
}
