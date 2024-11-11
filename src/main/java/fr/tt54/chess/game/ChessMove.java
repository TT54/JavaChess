package fr.tt54.chess.game;

public record ChessMove(int castle, int enPassant, boolean twoSquaresPawnMove, ChessPiece promotePiece,
                        ChessPiece movedPiece, ChessPiece capturedPiece,
                        int initialRow, int initialColumn, int finalRow, int finalColumn,
                        int previousEnPassant, boolean previousWhiteKingCastle, boolean previousWhiteQueenCastle, boolean previousBlackKingCastle, boolean previousBlackQueenCastle) {

    public boolean isCastle(){
        return castle >= 0;
    }

    public boolean isEnPassant(){
        return enPassant >= 0;
    }

    public boolean isTwoSquaresPawnMove(){
        return twoSquaresPawnMove;
    }

    public boolean isPromotion(){
        return promotePiece != null;
    }

    public boolean isCapture(){
        return capturedPiece != null;
    }

    public static ChessMove standardMove(ChessPiece movedPiece, int initialRow, int initialColumn, int finalRow, int finalColumn, ChessPiece capturedPiece, AbstractChessBoard board) {
        return new ChessMove(-1, -1, false, null, movedPiece, capturedPiece, initialRow, initialColumn, finalRow, finalColumn, board.getEnPassant(), board.getCastles()[0][0], board.getCastles()[0][1], board.getCastles()[1][0], board.getCastles()[1][1]);
    }

    public static ChessMove enPassantMove(int enPassant, ChessPiece movedPiece, int initialRow, int initialColumn, int finalRow, int finalColumn, ChessPiece capturedPiece, AbstractChessBoard board) {
        return new ChessMove(-1, enPassant, false, null, movedPiece, capturedPiece, initialRow, initialColumn, finalRow, finalColumn, board.getEnPassant(), board.getCastles()[0][0], board.getCastles()[0][1], board.getCastles()[1][0], board.getCastles()[1][1]);
    }

    public static ChessMove castleMove(boolean kingSide, ChessPiece movedPiece, int initialRow, int initialColumn, int finalRow, int finalColumn, AbstractChessBoard board) {
        return new ChessMove((movedPiece == ChessPiece.WHITE_KING ? 0 : 2) + (kingSide ? 0 : 1), -1, false, null, movedPiece, null, initialRow, initialColumn, finalRow, finalColumn, board.getEnPassant(), board.getCastles()[0][0], board.getCastles()[0][1], board.getCastles()[1][0], board.getCastles()[1][1]);
    }

    public static ChessMove twoSquaresPawnMove(ChessPiece movedPiece, int initialRow, int initialColumn, int finalRow, int finalColumn, AbstractChessBoard board){
        return new ChessMove(-1, -1, true, null, movedPiece, null, initialRow, initialColumn, finalRow, finalColumn, board.getEnPassant(), board.getCastles()[0][0], board.getCastles()[0][1], board.getCastles()[1][0], board.getCastles()[1][1]);
    }

    public static ChessMove promotionMove(ChessPiece movedPiece, int initialRow, int initialColumn, int finalRow, int finalColumn, ChessPiece capturedPiece, ChessPiece promotePiece, AbstractChessBoard board){
        return new ChessMove(-1, -1, false, promotePiece, movedPiece, capturedPiece, initialRow, initialColumn, finalRow, finalColumn, board.getEnPassant(), board.getCastles()[0][0], board.getCastles()[0][1], board.getCastles()[1][0], board.getCastles()[1][1]);
    }

}
