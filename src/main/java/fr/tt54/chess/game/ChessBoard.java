package fr.tt54.chess.game;

import fr.tt54.chess.utils.Tuple;

import java.util.*;

public class ChessBoard extends AbstractChessBoard{


    private static final int[][] standardMoves = new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int[][] knightMoves = new int[][] {{2, 1}, {2, -1}, {-2, 1}, {-2, -1}, {1, 2}, {1, -2}, {-1, 2}, {-1, -2}};


    private int move;
    private final int[][] board = new int[8][8]; // [row][column]
    private boolean whiteToPlay;
    private final boolean[][] castles = new boolean[2][2]; // [white|black][king|queen]
    private int enPassant;
    private int halfMovesRule;

    private final Map<ChessPiece, Set<Tuple<Integer, Integer>>> piecesPositions = new HashMap<>();

    public ChessBoard(){
        this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    public ChessBoard(String fen){
        loadFen(fen);
    }

    @Override
    public List<ChessMove> getAllowedMoves(){
        List<ChessMove> allowedMoves = new ArrayList<>();

        for (ChessPiece piece : ChessPiece.getColoredPieces(whiteToPlay)) {
            for (Tuple<Integer, Integer> position : new HashSet<>(getPiecePositions(piece))) {
                switch (piece){
                    case BLACK_BISHOP, BLACK_ROOK, BLACK_QUEEN, WHITE_BISHOP, WHITE_ROOK, WHITE_QUEEN -> addStandardMoves(allowedMoves, piece, position.x(), position.y());
                    case BLACK_KNIGHT, WHITE_KNIGHT -> addKnightMoves(allowedMoves, piece, position.x(), position.y());
                    case BLACK_PAWN, WHITE_PAWN -> addPawnMoves(allowedMoves, piece, position.x(), position.y());
                    case BLACK_KING, WHITE_KING -> addKingMoves(allowedMoves, piece, position.x(), position.y());
                }
            }
        }
        return allowedMoves;
    }

    private void addStandardMoves(List<ChessMove> allowedMoves, ChessPiece piece, int row, int column){
        for(int i = piece.getBeginStandardOffset(); i <= piece.getEndStandardOffset(); i++){
            int targetRow = row + standardMoves[i][0];
            int targetColumn = column + standardMoves[i][1];

            while (isInBoard(targetRow, targetColumn)) {
                ChessPiece targetPiece = getPiece(targetRow, targetColumn);
                ChessMove move = ChessMove.standardMove(piece, row, column, targetRow, targetColumn, targetPiece, this);

                if(targetPiece != null){
                    if(targetPiece.isWhite() != piece.isWhite() && isLegalMove(move)){
                        allowedMoves.add(move);
                    }
                    break;
                }

                if(isLegalMove(move)){
                    allowedMoves.add(move);
                }

                targetRow += standardMoves[i][0];
                targetColumn += standardMoves[i][1];
            }
        }
    }

    private void addKnightMoves(List<ChessMove> allowedMoves, ChessPiece piece, int row, int column){
        for (int[] knightMove : knightMoves) {
            int targetRow = row + knightMove[0];
            int targetColumn = column + knightMove[1];

            if (isInBoard(targetRow, targetColumn)) {
                ChessPiece targetPiece = getPiece(targetRow, targetColumn);
                ChessMove move = ChessMove.standardMove(piece, row, column, targetRow, targetColumn, targetPiece, this);

                if (targetPiece != null) {
                    if (targetPiece.isWhite() != piece.isWhite() && isLegalMove(move)) {
                        allowedMoves.add(move);
                    }
                } else {
                    if (isLegalMove(move)) {
                        allowedMoves.add(move);
                    }
                }
            }
        }
    }

    private void addPawnMoves(List<ChessMove> allowedMoves, ChessPiece piece, int row, int column){
        int sign = piece.isWhite() ? 1 : -1;

        // Adding advance 1 square move
        int targetRow = row + sign;
        int targetColumn = column;
        if(isInBoard(targetRow, targetColumn) && getPiece(targetRow, targetColumn) == null){
            ChessMove move = ChessMove.standardMove(piece, row, column, targetRow, targetColumn, null, this);
            if(isLegalMove(move)){
                if(!checkPromotionMoves(allowedMoves, piece, row, column, targetRow, targetColumn)){
                    allowedMoves.add(move);
                }
            }
        }

        // Adding capture moves
        for(int i = -1; i < 2; i+=2){
            targetColumn = column + i;
            if(isInBoard(targetRow, targetColumn)){
                ChessPiece targetPiece = getPiece(targetRow, targetColumn);
                if(targetPiece != null && targetPiece.isWhite() != piece.isWhite()){
                    ChessMove move = ChessMove.standardMove(piece, row, column, targetRow, targetColumn, targetPiece, this);
                    if(isLegalMove(move)){
                        if(!checkPromotionMoves(allowedMoves, piece, row, column, targetRow, targetColumn)){
                            allowedMoves.add(move);
                        }
                    }
                }
            }
        }

        // Adding en passant moves
        if(enPassant >= 0 && row == (piece.isWhite() ? 4 : 3)) {
            for (int i = -1; i < 2; i++) {
                targetColumn = column + i;
                if(getPosition(row, targetColumn) == enPassant && isInBoard(targetRow, targetColumn)){
                    ChessMove move = ChessMove.enPassantMove(this.enPassant, piece, row, column, targetRow, targetColumn, getPiece(this.enPassant), this);
                    if(isLegalMove(move)){
                        allowedMoves.add(move);
                    }
                }
            }
        }

        // Adding advance 2 squares move
        if(row == (piece.isWhite() ? 1 : 6)){
            targetRow = row + 2 * sign;
            targetColumn = column;
            if(getPiece(targetRow, targetColumn) == null && getPiece(row + sign, column) == null){
                ChessMove move = ChessMove.twoSquaresPawnMove(piece, row, column, targetRow, targetColumn, this);
                if(isLegalMove(move)){
                    allowedMoves.add(move);
                }
            }
        }
    }

    private boolean checkPromotionMoves(List<ChessMove> allowedMoves, ChessPiece piece, int row, int column, int targetRow, int targetColumn){
        if(targetRow == (piece.isWhite() ? 7 : 0)){
            for(ChessPiece newPiece : ChessPiece.getColoredPromotionPieces(piece.isWhite())){
                allowedMoves.add(ChessMove.promotionMove(piece, row, column, targetRow, targetColumn, getPiece(targetRow, targetColumn), newPiece, this));
            }
            return true;
        }
        return false;
    }

    private void addKingMoves(List<ChessMove> allowedMoves, ChessPiece piece, int row, int column){
        // Adding standard moves
        for (int[] standardMove : standardMoves) {
            int targetRow = row + standardMove[0];
            int targetColumn = column + standardMove[1];

            if (isInBoard(targetRow, targetColumn)) {
                ChessPiece targetPiece = getPiece(targetRow, targetColumn);
                if (targetPiece == null || targetPiece.isWhite() != piece.isWhite()) {
                    ChessMove move = ChessMove.standardMove(piece, row, column, targetRow, targetColumn, targetPiece, this);
                    if (isLegalMove(move)) {
                        allowedMoves.add(move);
                    }
                }
            }
        }

        // Adding castle
        int castleIndex = piece.isWhite() ? 0 : 1;
        if((castles[castleIndex][0] || castles[castleIndex][1]) && !isAttackedBy(row, column, !piece.isWhite())) {
            if (castles[castleIndex][0]) {
                // Adding castle king side
                if (getPiece(castleIndex * 7, 5) == null && getPiece(castleIndex * 7, 6) == null && !isAttackedBy(castleIndex * 7, 5, !piece.isWhite()) && !isAttackedBy(castleIndex * 7, 6, !piece.isWhite())){
                    allowedMoves.add(ChessMove.castleMove(true, piece, row, column, castleIndex * 7, 6, this));
                }
            }
            if(castles[castleIndex][1]){
                // Adding castle queen side
                if (getPiece(castleIndex * 7, 1) == null && getPiece(castleIndex * 7, 2) == null && getPiece(castleIndex * 7, 3) == null && !isAttackedBy(castleIndex * 7, 2, !piece.isWhite()) && !isAttackedBy(castleIndex * 7, 3, !piece.isWhite())){
                    allowedMoves.add(ChessMove.castleMove(false, piece, row, column, castleIndex * 7, 2, this));
                }
            }
        }
    }

    private boolean isLegalMove(ChessMove move){
        playMove(move);
        Tuple<Integer, Integer> kingPos = getPiecePositions(whiteToPlay ? ChessPiece.BLACK_KING : ChessPiece.WHITE_KING).stream().toList().getFirst();
        boolean attacked = isAttackedBy(kingPos.x(), kingPos.y(), whiteToPlay);
        undoMove(move);

        return !attacked;
    }

    private boolean isAttackedBy(int row, int column, boolean white){
        // Checking if a standard piece attacks this square
        for(int i = 0; i < standardMoves.length; i++){
            int attackingRow = row + standardMoves[i][0];
            int attackingColumn = column + standardMoves[i][1];

            if(isInBoard(attackingRow, attackingColumn)) {
                if (getPiece(attackingRow, attackingColumn) == (white ? ChessPiece.WHITE_KING : ChessPiece.BLACK_KING)) {
                    return true;
                }

                do{
                    ChessPiece piece = getPiece(attackingRow, attackingColumn);
                    if(piece != null){
                        if(piece.isWhite() == white && i >= piece.getBeginStandardOffset() && i <= piece.getEndStandardOffset()){
                            return true;
                        }
                        break;
                    }

                    attackingRow += standardMoves[i][0];
                    attackingColumn += standardMoves[i][1];
                } while (isInBoard(attackingRow, attackingColumn));
            }
        }

        // Checking if a knight attacks this square
        for (int[] knightMove : knightMoves) {
            int attackingRow = row + knightMove[0];
            int attackingColumn = column + knightMove[1];

            if (isInBoard(attackingRow, attackingColumn)) {
                ChessPiece piece = getPiece(attackingRow, attackingColumn);
                if (piece == (white ? ChessPiece.WHITE_KNIGHT : ChessPiece.BLACK_KNIGHT)) {
                    return true;
                }
            }
        }

        // Checking if a pawn attacks this square
        int offset = white ? -1 : 1;
        for(int i = -1; i < 2; i+=2){
            int attackingRow = row + offset;
            int attackingColumn = column + i;

            if (isInBoard(attackingRow, attackingColumn)) {
                ChessPiece piece = getPiece(attackingRow, attackingColumn);
                if(piece == (white ? ChessPiece.WHITE_PAWN : ChessPiece.BLACK_PAWN)){
                    return true;
                }
            }
        }

        // We do not check if this square is attacked with en passant rule

        // Nothing attacks the square
        return false;
    }

    @Override
    public void playMove(ChessMove move){
        board[move.initialRow()][move.initialColumn()] = 0;

        // Managing a non-en passant capture
        if(move.isCapture() && !move.isEnPassant()){
            this.removePiecePosition(move.capturedPiece(), move.finalRow(), move.finalColumn());
            if(Math.abs(move.capturedPiece().getId()) == 4){
                // A rook was captured
                if(move.capturedPiece().isWhite()){
                    if(move.finalRow() == 0){
                        if(move.finalColumn() == 0){
                            castles[0][1] = false;
                        } else if(move.finalColumn() == 7){
                            castles[0][0] = false;
                        }
                    }
                } else{
                    if(move.finalRow() == 7){
                        if(move.finalColumn() == 0){
                            castles[1][1] = false;
                        } else if(move.finalColumn() == 7){
                            castles[1][0] = false;
                        }
                    }
                }
            }
        }

        // Managing en passant
        if(move.isEnPassant()){
            int[] enPassantPos = getPosition(move.enPassant());
            board[enPassantPos[0]][enPassantPos[1]] = 0;
            this.removePiecePosition(move.capturedPiece(), enPassantPos[0], enPassantPos[1]);
        }

        if(!move.isPromotion()){
            // Switching piece position
            this.switchPiecePosition(move.movedPiece(), move.initialRow(), move.initialColumn(), move.finalRow(), move.finalColumn());
            board[move.finalRow()][move.finalColumn()] = move.movedPiece().getId();
        } else {
            // Promoting a piece
            this.removePiecePosition(move.movedPiece(), move.initialRow(), move.initialColumn());
            this.addPiecePosition(move.promotePiece(), move.finalRow(), move.finalColumn());
            board[move.finalRow()][move.finalColumn()] = move.promotePiece().getId();
        }

        // Pawn moves two squares
        if(move.isTwoSquaresPawnMove()){
            this.enPassant = getPosition(move.finalRow(), move.finalColumn());
        } else {
            this.enPassant = -1;
        }

        // Castle
        if(move.isCastle()){
            switch (move.castle()){
                case 0 -> {castles[0][0] = false; castles[0][1] = false; board[0][7] = 0; board[0][5] = ChessPiece.WHITE_ROOK.getId(); switchPiecePosition(ChessPiece.WHITE_ROOK, 0, 7, 0, 5);}
                case 1 -> {castles[0][0] = false; castles[0][1] = false; board[0][0] = 0; board[0][3] = ChessPiece.WHITE_ROOK.getId(); switchPiecePosition(ChessPiece.WHITE_ROOK, 0, 0, 0, 3);}
                case 2 -> {castles[1][0] = false; castles[1][1] = false; board[7][7] = 0; board[7][5] = ChessPiece.BLACK_ROOK.getId(); switchPiecePosition(ChessPiece.BLACK_ROOK, 7, 7, 7, 5);}
                case 3 -> {castles[1][0] = false; castles[1][1] = false; board[7][0] = 0; board[7][3] = ChessPiece.BLACK_ROOK.getId(); switchPiecePosition(ChessPiece.BLACK_ROOK, 7, 0, 7, 3);}
            }
        }

        // Prevent castle
        int castleIndex = move.movedPiece().isWhite() ? 0 : 1;
        if(Math.abs(move.movedPiece().getId()) == 6){
            // King move
            castles[castleIndex][0] = false;
            castles[castleIndex][1] = false;
        } else if(Math.abs(move.movedPiece().getId()) == 4){
            // Rook move
            if(move.initialRow() == castleIndex * 7){
                if(move.initialColumn() == 0){
                    // Prevent queen side castle
                    castles[castleIndex][1] = false;
                } else if(move.initialColumn() == 7){
                    // Prevent king side castle
                    castles[castleIndex][0] = false;
                }
            }
        }


        this.whiteToPlay = !this.whiteToPlay;
    }

    @Override
    public void undoMove(ChessMove move){
        this.castles[0][0] = move.previousWhiteKingCastle();
        this.castles[0][1] = move.previousWhiteQueenCastle();
        this.castles[1][0] = move.previousBlackKingCastle();
        this.castles[1][1] = move.previousBlackQueenCastle();
        this.enPassant = move.previousEnPassant();

        board[move.initialRow()][move.initialColumn()] = move.movedPiece().getId();

        // Managing a capture
        if(move.isCapture()){
            if(move.isEnPassant()){
                int[] enPassantPos = getPosition(move.enPassant());
                board[enPassantPos[0]][enPassantPos[1]] = move.capturedPiece().getId();
                board[move.finalRow()][move.finalColumn()] = 0;
                this.addPiecePosition(move.capturedPiece(), enPassantPos[0], enPassantPos[1]);
                this.removePiecePosition(move.movedPiece(), move.finalRow(), move.finalColumn());
            } else {
                board[move.finalRow()][move.finalColumn()] = move.capturedPiece().getId();
                this.addPiecePosition(move.capturedPiece(), move.finalRow(), move.finalColumn());
            }
        } else {
            board[move.finalRow()][move.finalColumn()] = 0;
        }

        if(!move.isPromotion()){
            // Switching piece position
            this.switchPiecePosition(move.movedPiece(), move.finalRow(), move.finalColumn(), move.initialRow(), move.initialColumn());
        } else {
            // Promoting a piece
            this.addPiecePosition(move.movedPiece(), move.initialRow(), move.initialColumn());
            this.removePiecePosition(move.promotePiece(), move.finalRow(), move.finalColumn());
        }

        // Castle
        if(move.isCastle()){
            switch (move.castle()){
                case 0 -> {board[0][7] = ChessPiece.WHITE_ROOK.getId(); board[0][5] = 0; switchPiecePosition(ChessPiece.WHITE_ROOK, 0, 5, 0, 7);}
                case 1 -> {board[0][0] = ChessPiece.WHITE_ROOK.getId(); board[0][3] = 0; switchPiecePosition(ChessPiece.WHITE_ROOK, 0, 3, 0, 0);}
                case 2 -> {board[7][7] = ChessPiece.BLACK_ROOK.getId(); board[7][5] = 0; switchPiecePosition(ChessPiece.BLACK_ROOK, 7, 5, 7, 7);}
                case 3 -> {board[7][0] = ChessPiece.BLACK_ROOK.getId(); board[7][3] = 0; switchPiecePosition(ChessPiece.BLACK_ROOK, 7, 3, 7, 0);}
            }
        }

        this.whiteToPlay = !this.whiteToPlay;
    }

    @Override
    public ChessPiece getPiece(int row, int column){
        return ChessPiece.getPiece(board[row][column]);
    }

    @Override
    public ChessPiece getPiece(int position){
        int[] pos = getPosition(position);
        return getPiece(pos[0], pos[1]);
    }

    @Override
    public Set<Tuple<Integer, Integer>> getPiecePositions(ChessPiece piece){
        return piecesPositions.getOrDefault(piece, new HashSet<>());
    }

    @Override
    public boolean isWhiteToPlay() {
        return whiteToPlay;
    }

    private void addPiecePosition(ChessPiece piece, int row, int column){
        Set<Tuple<Integer, Integer>> positions = getPiecePositions(piece);
        positions.add(new Tuple<>(row, column));
        piecesPositions.put(piece, positions);
    }

    private void removePiecePosition(ChessPiece piece, int row, int column){
        Set<Tuple<Integer, Integer>> positions = getPiecePositions(piece);
        positions.remove(new Tuple<>(row, column));
        piecesPositions.put(piece, positions);
    }

    private void switchPiecePosition(ChessPiece piece, int previousRow, int previousColumn, int newRow, int newColumn){
        Set<Tuple<Integer, Integer>> positions = getPiecePositions(piece);
        positions.remove(new Tuple<>(previousRow, previousColumn));
        positions.add(new Tuple<>(newRow, newColumn));
        piecesPositions.put(piece, positions);
    }

    public void loadFen(String fen){
        piecesPositions.clear();
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                board[i][j] = 0;
            }
        }

        castles[0][0] = false;
        castles[0][1] = false;
        castles[1][0] = false;
        castles[1][1] = false;

        String[] args = fen.split(" ");

        String[] pos = args[0].split("/");
        assert pos.length != 8;

        int column = 0;
        int row = 7;
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < pos[i].length(); j++){
                char c = pos[i].charAt(j);
                if(c >= '0' && c <= '9'){
                    column += c - '0';
                } else {
                    ChessPiece piece = ChessPiece.getPiece(c);
                    board[row][column] = piece.getId();

                    addPiecePosition(piece, row, column);

                    column++;
                }
            }
            column = 0;
            row--;
        }

        if(args.length >= 2){
            whiteToPlay = args[1].equalsIgnoreCase("w");
        }

        if(args.length >= 3){
            for(int i = 0; i < args[2].length(); i++){
                ChessPiece piece = ChessPiece.getPiece(args[2].charAt(i));
                if(piece == null) continue;
                switch (piece){
                    case WHITE_KING -> castles[0][0] = true;
                    case WHITE_QUEEN -> castles[0][1] = true;
                    case BLACK_KING -> castles[1][0] = true;
                    case BLACK_QUEEN -> castles[1][1] = true;
                }
            }
        }

        if(args.length >= 4){
            if(args[3].equalsIgnoreCase("-")){
                enPassant = -1;
            } else {
                enPassant = getPosition(getPosition(args[3]));
            }
        }

        if(args.length >= 5){
            try {
                this.halfMovesRule = Integer.parseInt(args[4]);
            } catch (NumberFormatException ignore){}
        }

        if(args.length >= 6){
            try {
                this.move = Integer.parseInt(args[5]);
            } catch (NumberFormatException ignore){}
        }
    }

    public void printBoard(){
        for(int i = 7; i > -1; i--){
            for(int j = 0; j < 8; j++){
                int p = board[i][j];
                System.out.print(p >= 0 ? " " + p : p);
                if(j != 7) System.out.print(" |");
            }
            System.out.println();
        }
        System.out.println("Coup n°" + move);
        System.out.println("Aux " + (whiteToPlay ? "blancs" : "noirs") + " de jouer");
        System.out.println("Petit roque blanc : " + castles[0][0]);
        System.out.println("Grand roque blanc : " + castles[0][1]);
        System.out.println("Petit roque noir : " + castles[1][0]);
        System.out.println("Grand roque noir : " + castles[1][1]);
        System.out.println("Prise en passant " + (enPassant < 0 ? "Impossible" : getPositionString(enPassant)));
        System.out.println("Règle des 100 coups " + halfMovesRule);
    }

    public int getEnPassant() {
        return enPassant;
    }

    public boolean[][] getCastles() {
        return castles;
    }
}
