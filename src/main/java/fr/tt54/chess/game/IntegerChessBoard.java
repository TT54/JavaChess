package fr.tt54.chess.game;

import fr.tt54.chess.ChessMain;
import fr.tt54.chess.utils.Tuple;

import java.util.*;

public class IntegerChessBoard {

    private static final int[][] standardMoves = new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int[][] knightMoves = new int[][] {{2, 1}, {2, -1}, {-2, 1}, {-2, -1}, {1, 2}, {1, -2}, {-1, 2}, {-1, -2}};


    private int moveCount;
    private final int[] board = new int[64]; // [position]
    private boolean whiteToPlay;
    private int castles = 0; // 4 bits int : 0000 with this order: white king side castle, ..., black queen side castle
    private int enPassant;
    private int halfMovesRule;

    private final Map<ChessPiece, Set<Integer>> piecesPositions = new HashMap<>();

    private PartialIntegerList[] squaresAttackers;
    private int[] pins;
    private int[] checks;
    private int whiteKingPosition;
    private int blackKingPosition;

    public IntegerChessBoard(){
        this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    public IntegerChessBoard(String fen){
        loadFen(fen);
    }


    public List<Integer> getAllowedMoves(){
        squaresAttackers = new PartialIntegerList[64];
        pins = new int[64];
        checks = new int[64];

        List<Integer> attackerMoves = new ArrayList<>();
        List<Integer> allowedMoves = new ArrayList<>();

        fillPseudoLegalMoves(attackerMoves, !whiteToPlay, true);

        PartialIntegerList kingAttackers = getKingAttackers(whiteToPlay);
        if(kingAttackers == null){
            List<Integer> validMoves = new ArrayList<>();

            for (ChessPiece piece : ChessPiece.getColoredPieces(whiteToPlay)) {
                for (int position : getPiecePositions(piece)) {
                    switch (piece){
                        case BLACK_BISHOP, BLACK_ROOK, BLACK_QUEEN, WHITE_BISHOP, WHITE_ROOK, WHITE_QUEEN -> addStandardMoves(allowedMoves, piece, position, false);
                        case BLACK_KNIGHT, WHITE_KNIGHT -> addKnightMoves(allowedMoves, piece, position, false);
                        case BLACK_PAWN, WHITE_PAWN -> addPawnMoves(allowedMoves, piece, position, false);
                        case BLACK_KING, WHITE_KING -> fillLegalKingMoves(validMoves, piece, position, true);
                    }
                }
            }

            for(int move : allowedMoves){
                if(isEnPassantMove(move)){
                    if(isLegalMove(move)){
                        validMoves.add(move);
                    }
                } else if(isMoveValidWithPins(move)){
                    validMoves.add(move);
                }
            }

            allowedMoves = validMoves;
        } else if(kingAttackers.size() == 1){
            int attackerPos = kingAttackers.getElement(0);
            ChessPiece attacker = getPiece(attackerPos);

            List<Integer> validMoves = new ArrayList<>();

            for (ChessPiece piece : ChessPiece.getColoredPieces(whiteToPlay)) {
                for (int position : getPiecePositions(piece)) {
                    switch (piece){
                        case BLACK_BISHOP, BLACK_ROOK, BLACK_QUEEN, WHITE_BISHOP, WHITE_ROOK, WHITE_QUEEN -> addStandardMoves(allowedMoves, piece, position, false);
                        case BLACK_KNIGHT, WHITE_KNIGHT -> addKnightMoves(allowedMoves, piece, position, false);
                        case BLACK_PAWN, WHITE_PAWN -> addPawnMoves(allowedMoves, piece, position, false);
                        case BLACK_KING, WHITE_KING -> fillLegalKingMoves(validMoves, piece, position, false);
                    }
                }
            }

            for(int move : allowedMoves){
                if(getMoveFinalPosition(move) == attackerPos && isMoveValidWithPins(move)){
                    validMoves.add(move);
                } else if(getMoveEnPassantPosition(move) == attackerPos && isLegalMove(move)){
                    validMoves.add(move);
                } else if(attacker.canCheckBeBlocked() && isMoveValidWithPins(move) && isMoveValidWithChecks(move)){
                    validMoves.add(move);
                }
            }

            allowedMoves = validMoves;
        } else {
            fillLegalKingMoves(allowedMoves, whiteToPlay ? ChessPiece.WHITE_KING : ChessPiece.BLACK_KING, whiteToPlay ? whiteKingPosition : blackKingPosition, false);
        }

        return allowedMoves;
    }

    private boolean isMoveValidWithChecks(int move) {
        int finalPosition = (move & 0x00000fc0) >>> 6;
        return checks[finalPosition] > 0;
    }

    private boolean isMoveValidWithPins(int move){
        int initialPosition = move & 0x00000003f;
        int finalPosition = (move & 0x00000fc0) >>> 6;

        int pinValue = pins[initialPosition];
        if(pinValue > 0){
            return pins[finalPosition] == pinValue;
        }
        return true;
    }

    private PartialIntegerList getKingAttackers(boolean white){
        return squaresAttackers[white ? whiteKingPosition : blackKingPosition];
    }

    /**
     * Warning : this function should be called after getAllowedMoves() !
     * @return If the current player is in check
     */
    public boolean isKingInCheck(){
        PartialIntegerList attackers = getKingAttackers(whiteToPlay);
        return attackers != null && attackers.size() > 0;
    }

    private void fillPseudoLegalMoves(List<Integer> moves, boolean white, boolean shouldFillAttackedSquares){
        for (ChessPiece piece : ChessPiece.getColoredPieces(white)) {
            for (int position : getPiecePositions(piece)) {
                switch (piece){
                    case BLACK_BISHOP, BLACK_ROOK, BLACK_QUEEN, WHITE_BISHOP, WHITE_ROOK, WHITE_QUEEN -> addStandardMoves(moves, piece, position, shouldFillAttackedSquares);
                    case BLACK_KNIGHT, WHITE_KNIGHT -> addKnightMoves(moves, piece, position, shouldFillAttackedSquares);
                    case BLACK_PAWN, WHITE_PAWN -> addPawnMoves(moves, piece, position, shouldFillAttackedSquares);
                    case BLACK_KING, WHITE_KING -> addPseudoLegalKingMovesWithoutCastles(moves, piece, position, shouldFillAttackedSquares);
                }
            }
        }
    }

    private void addStandardMoves(List<Integer> allowedMoves, ChessPiece piece, int position, boolean shouldFillAttackedSquares){
        int row = getRow(position);
        int column = getColumn(position);

        for(int i = piece.getBeginStandardOffset(); i <= piece.getEndStandardOffset(); i++){
            int targetRow = row + standardMoves[i][0];
            int targetColumn = column + standardMoves[i][1];

            while (isInBoard(targetRow, targetColumn)) {
                int targetPosition = getPosition(targetRow, targetColumn);
                ChessPiece targetPiece = getPiece(targetPosition);
                if(shouldFillAttackedSquares) {
                    addAttackedSquare(targetPosition, position);
                }

                if(targetPiece != null){
                    if(targetPiece.isWhite() != piece.isWhite()){
                        allowedMoves.add(getStandardMove(position, targetPosition, piece, targetPiece));

                        if(targetPiece == ChessPiece.getKing(!piece.isWhite()) && shouldFillAttackedSquares){
                            // This piece attacks the king : this is a check
                            int checkRow = targetRow + standardMoves[i][0];
                            int checkColumn = targetColumn + standardMoves[i][1];

                            if(isInBoard(checkRow, checkColumn)){
                                // We mark the square behind the king as an attacked square
                                addAttackedSquare(getPosition(checkRow, checkColumn), position);
                            }
                            checkRow = targetRow;
                            checkColumn = targetColumn;

                            while(checkRow != row || checkColumn != column){
                                int checkPosition = getPosition(checkRow, checkColumn);
                                checks[checkPosition] = position + 1; // We add +1 to keep 0 as a non-checked square
                                checkRow -= standardMoves[i][0];
                                checkColumn -= standardMoves[i][1];
                            }
                        }
                    }
                    break;
                }

                allowedMoves.add(getStandardMove(position, targetPosition, piece, targetPiece));

                targetRow += standardMoves[i][0];
                targetColumn += standardMoves[i][1];
            }

            if(shouldFillAttackedSquares && isInBoard(targetRow, targetColumn) && getPiece(targetRow, targetColumn).isWhite() != piece.isWhite()){
                // This targeted piece could be pinned

                targetRow += standardMoves[i][0];
                targetColumn += standardMoves[i][1];
                while(isInBoard(targetRow, targetColumn)){
                    ChessPiece targetPiece = getPiece(targetRow, targetColumn);
                    if(targetPiece != null){
                        if(targetPiece == ChessPiece.getKing(!piece.isWhite())){
                            // There's a pin

                            // We should add the list of squares pinned by this piece
                            while(targetRow != row || targetColumn != column){
                                targetRow -= standardMoves[i][0];
                                targetColumn -= standardMoves[i][1];
                                int targetPosition = getPosition(targetRow, targetColumn);
                                pins[targetPosition] = position + 1; // We add +1 to keep 0 as a non-pinned square
                            }
                        }
                        break;
                    }
                    targetRow += standardMoves[i][0];
                    targetColumn += standardMoves[i][1];
                }
            }
        }
    }

    private void addKnightMoves(List<Integer> allowedMoves, ChessPiece piece, int position, boolean shouldFillAttackedSquares){
        int row = getRow(position);
        int column = getColumn(position);

        for (int[] knightMove : knightMoves) {
            int targetRow = row + knightMove[0];
            int targetColumn = column + knightMove[1];

            if (isInBoard(targetRow, targetColumn)) {
                int targetPosition = getPosition(targetRow, targetColumn);
                ChessPiece targetPiece = getPiece(targetPosition);
                if(shouldFillAttackedSquares) {
                    addAttackedSquare(targetPosition, position);
                }

                if (targetPiece != null) {
                    if (targetPiece.isWhite() != piece.isWhite()) {
                        allowedMoves.add(getStandardMove(position, targetPosition, piece, targetPiece));
                    }
                } else {
                    allowedMoves.add(getStandardMove(position, targetPosition, piece, null));
                }
            }
        }
    }

    private void addPawnMoves(List<Integer> allowedMoves, ChessPiece piece, int position, boolean shouldFillAttackedSquares){
        int sign = piece.isWhite() ? 1 : -1;
        int row = getRow(position);
        int column = getColumn(position);

        // Adding advance 1 square move
        int targetRow = row + sign;
        int targetColumn = column;
        int targetPosition = getPosition(targetRow, targetColumn);
        if(isInBoard(targetRow, targetColumn) && getPiece(targetPosition) == null){
            if(!checkPromotionMoves(allowedMoves, piece, position, targetPosition)){
                allowedMoves.add(getStandardMove(position, targetPosition, piece, null));
            }
        }

        // Adding capture moves
        for(int i = -1; i < 2; i+=2){
            targetColumn = column + i;
            targetPosition = getPosition(targetRow, targetColumn);
            if(isInBoard(targetRow, targetColumn)){
                if(shouldFillAttackedSquares) {
                    addAttackedSquare(targetPosition, position);
                }
                ChessPiece targetPiece = getPiece(targetPosition);
                if(targetPiece != null && targetPiece.isWhite() != piece.isWhite()){
                    if(!checkPromotionMoves(allowedMoves, piece, position, targetPosition)){
                        allowedMoves.add(getStandardMove(position, targetPosition, piece, targetPiece));
                    }
                }
            }
        }

        // Adding en passant moves
        if(enPassant > 0 && row == (piece.isWhite() ? 4 : 3)) {
            for (int i = -1; i < 2; i++) {
                targetColumn = column + i;
                targetPosition = getPosition(row, targetColumn);
                if(targetPosition == enPassant && isInBoard(targetRow, targetColumn)){
                    allowedMoves.add(getEnPassantMove(position, targetPosition + (piece.isWhite() ? 8 : -8), piece, getPiece(this.enPassant)));
                }
            }
        }

        // Adding advance 2 squares move
        if(row == (piece.isWhite() ? 1 : 6)){
            targetRow = row + 2 * sign;
            targetColumn = column;
            targetPosition = getPosition(targetRow, targetColumn);
            if(getPiece(targetPosition) == null && getPiece(row + sign, column) == null){
                allowedMoves.add(getTwoSquaresPawnMove(position, targetPosition, piece));
            }
        }
    }

    private boolean checkPromotionMoves(List<Integer> allowedMoves, ChessPiece piece, int position, int targetPosition){
        if(getRow(targetPosition) == (piece.isWhite() ? 7 : 0)){
            for(ChessPiece newPiece : ChessPiece.getColoredPromotionPieces(piece.isWhite())){
                allowedMoves.add(getPromotionMove(position, targetPosition, piece, getPiece(targetPosition), newPiece));
            }
            return true;
        }
        return false;
    }

    private void addPseudoLegalKingMovesWithoutCastles(List<Integer> allowedMoves, ChessPiece piece, int position, boolean shouldFillAttackedSquares){
        int row = getRow(position);
        int column = getColumn(position);

        // Adding standard moves
        for (int[] standardMove : standardMoves) {
            int targetRow = row + standardMove[0];
            int targetColumn = column + standardMove[1];

            if (isInBoard(targetRow, targetColumn)) {
                int targetPosition = getPosition(targetRow, targetColumn);

                if(shouldFillAttackedSquares) {
                    addAttackedSquare(targetPosition, position);
                }

                ChessPiece targetPiece = getPiece(targetRow, targetColumn);
                if (targetPiece == null || targetPiece.isWhite() != piece.isWhite()) {
                    allowedMoves.add(getStandardMove(position, targetPosition, piece, targetPiece));
                }
            }
        }
    }

    private void fillLegalKingMoves(List<Integer> allowedMoves, ChessPiece piece, int position, boolean addCastle){
        int row = getRow(position);
        int column = getColumn(position);

        // Adding standard moves
        for (int[] standardMove : standardMoves) {
            int targetRow = row + standardMove[0];
            int targetColumn = column + standardMove[1];

            if (isInBoard(targetRow, targetColumn)) {
                int targetPos = getPosition(targetRow, targetColumn);
                ChessPiece targetPiece = getPiece(targetRow, targetColumn);
                if ((targetPiece == null || targetPiece.isWhite() != piece.isWhite()) && squaresAttackers[targetPos] == null && checks[targetPos] == 0) {
                    allowedMoves.add(getStandardMove(position, targetPos, piece, targetPiece));
                }
            }
        }

        // Adding castle
        if(addCastle) {
            if(piece.isWhite()){
                if ((castles & 0b1000) != 0) {
                    // Adding castle king side
                    if (getPiece(5) == null && getPiece(6) == null && squaresAttackers[5] == null && squaresAttackers[6] == null) {
                        allowedMoves.add(getCastleMove(position, 6, piece, true));
                    }
                }
                if ((castles & 0b0100) != 0) {
                    // Adding castle queen side
                    if (getPiece(1) == null && getPiece(2) == null && getPiece( 3) == null && squaresAttackers[2] == null && squaresAttackers[3] == null) {
                        allowedMoves.add(getCastleMove(position, 2, piece, false));
                    }
                }
            } else {
                if ((castles & 0b0010) != 0) {
                    // Adding castle king side
                    if (getPiece(61) == null && getPiece(62) == null && squaresAttackers[61] == null && squaresAttackers[62] == null) {
                        allowedMoves.add(getCastleMove(position, 62, piece, true));
                    }
                }
                if ((castles & 0b0001) != 0) {
                    // Adding castle queen side
                    if (getPiece(57) == null && getPiece(58) == null && getPiece(59) == null && squaresAttackers[58] == null && squaresAttackers[59] == null) {
                        allowedMoves.add(getCastleMove(position, 58, piece, false));
                    }
                }
            }
        }
    }

    private boolean isLegalMove(int move){
        playMove(move);
        int kingPos = whiteToPlay ? blackKingPosition : whiteKingPosition;
        boolean attacked = isAttackedBy(kingPos, whiteToPlay);
        undoMove(move);

        return !attacked;
    }

    private boolean isAttackedBy(int position, boolean white){
        int row = getRow(position);
        int column = getColumn(position);
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

    public void playMove(int move){
        int playerSign = whiteToPlay ? 1 : -1;
        int opponentSign = -playerSign;

        int initialPosition = move & 0x0000003f;
        int finalPosition = (move & 0x00000fc0) >>> 6;

        int unsignedMovedPieceId = (move & 0x00007000) >>> 12;
        int unsignedCapturedPieceId = (move & 0x00038000) >>> 15;
        ChessPiece movedPiece = ChessPiece.getPiece(playerSign * unsignedMovedPieceId);
        ChessPiece capturedPiece = unsignedCapturedPieceId == 0 ? null : ChessPiece.getPiece(opponentSign * unsignedCapturedPieceId);

        int extraDatas = (move & 0xf0000000) >>> 28;

        boolean promote = (extraDatas & 0b1000) != 0;

        if(promote){
            if(capturedPiece != null){
                removePiecePosition(capturedPiece, finalPosition);
                if(capturedPiece.getUnsignedId() == 4){
                    // A rook was captured
                    if(capturedPiece.isWhite()){
                        if(finalPosition == 0){
                            castles &= 0b1011;
                        } else if(finalPosition == 7){
                            castles &= 0b0111;
                        }
                    } else{
                        if(finalPosition == 56){
                            castles &= 0b1110;
                        } else if(finalPosition == 63){
                            castles &= 0b1101;
                        }
                    }
                }
            }

            int unsignedPromotedPieceId = extraDatas & 0b111;
            ChessPiece promotedPiece = ChessPiece.getPiece(playerSign * unsignedPromotedPieceId);
            this.removePiecePosition(movedPiece, initialPosition);
            this.addPiecePosition(promotedPiece, finalPosition);
        } else {
            boolean enPassant = (extraDatas & 0b0110) == 0b0100;
            boolean castle = (extraDatas & 0b0110) == 0b0110;
            boolean twoSquaresPawnMove = (extraDatas & 0b0110) == 0b0010;

            if(enPassant){
                // Managing an en passant capture
                this.removePiecePosition(capturedPiece, this.enPassant);
            } else if(capturedPiece != null){
                // Managing a non-en passant capture
                this.removePiecePosition(capturedPiece, finalPosition);
                if(capturedPiece.getUnsignedId() == 4){
                    // A rook was captured
                    if(capturedPiece.isWhite()){
                        if(finalPosition == 0){
                            castles &= 0b1011;
                        } else if(finalPosition == 7){
                            castles &= 0b0111;
                        }
                    } else{
                        if(finalPosition == 56){
                            castles &= 0b1110;
                        } else if(finalPosition == 63){
                            castles &= 0b1101;
                        }
                    }
                }
            } else if(castle){
                int castleId = (extraDatas & 0b1) + 2 * (whiteToPlay ? 0 : 1);
                switch (castleId){
                    case 0 -> {castles &= 0b0011; switchPiecePosition(ChessPiece.WHITE_ROOK, 0, 3);}
                    case 1 -> {castles &= 0b0011; switchPiecePosition(ChessPiece.WHITE_ROOK, 7, 5);}
                    case 2 -> {castles &= 0b1100; switchPiecePosition(ChessPiece.BLACK_ROOK, 56, 59);}
                    case 3 -> {castles &= 0b1100; switchPiecePosition(ChessPiece.BLACK_ROOK, 63, 61);}
                }
            }

            // Pawn moves two squares
            if(twoSquaresPawnMove){
                this.enPassant = finalPosition;
            } else {
                this.enPassant = 0;
            }

            // Prevent castle
            if(movedPiece.getUnsignedId() == 6){
                // King move
                castles &= whiteToPlay ? 0b0011 : 0b1100;
            } else if(movedPiece.getUnsignedId() == 4){
                // Rook move
                if(initialPosition == 0){
                    castles &= 0b1011;
                } else if(initialPosition == 7){
                    castles &= 0b0111;
                } else if(initialPosition == 56){
                    castles &= 0b1110;
                } else if(initialPosition == 63){
                    castles &= 0b1101;
                }
            }

            this.switchPiecePosition(movedPiece, initialPosition, finalPosition);
        }

        this.whiteToPlay = !this.whiteToPlay;
    }

    public void undoMove(int move){
        int playerSign = whiteToPlay ? -1 : 1;
        int opponentSign = -playerSign;

        this.castles = (move & 0x0f000000) >>> 24;
        this.enPassant = (move & 0x00fc0000) >>> 18;

        int initialPosition = move & 0x0000003f;
        int finalPosition = (move & 0x00000fc0) >>> 6;

        int unsignedMovedPieceId = (move & 0x00007000) >>> 12;
        int unsignedCapturedPieceId = (move & 0x00038000) >>> 15;
        ChessPiece movedPiece = ChessPiece.getPiece(playerSign * unsignedMovedPieceId);
        ChessPiece capturedPiece = unsignedCapturedPieceId == 0 ? null : ChessPiece.getPiece(opponentSign * unsignedCapturedPieceId);

        int extraDatas = (move & 0xf0000000) >>> 28;

        boolean promote = (extraDatas & 0b1000) != 0;

        if(promote){
            int unsignedPromotedPieceId = extraDatas & 0b111;
            ChessPiece promotedPiece = ChessPiece.getPiece(playerSign * unsignedPromotedPieceId);
            this.removePiecePosition(promotedPiece, finalPosition);
            this.addPiecePosition(movedPiece, initialPosition);
            if(unsignedCapturedPieceId > 0){
                this.addPiecePosition(capturedPiece, finalPosition);
            }
        } else {
            boolean enPassant = (extraDatas & 0b0110) == 0b0100;
            boolean castle = (extraDatas & 0b0110) == 0b0110;

            this.switchPiecePosition(movedPiece, finalPosition, initialPosition);

            // Managing a capture
            if(unsignedCapturedPieceId > 0){
                if(enPassant){
                    this.addPiecePosition(capturedPiece, this.enPassant);
                } else {
                    this.addPiecePosition(capturedPiece, finalPosition);
                }
            }

            if(castle){
                int castleId = (extraDatas & 0b1) + 2 * (whiteToPlay ? 1 : 0);
                switch (castleId){
                    case 0 -> {switchPiecePosition(ChessPiece.WHITE_ROOK, 3, 0);}
                    case 1 -> {switchPiecePosition(ChessPiece.WHITE_ROOK, 5, 7);}
                    case 2 -> {switchPiecePosition(ChessPiece.BLACK_ROOK, 59, 56);}
                    case 3 -> {switchPiecePosition(ChessPiece.BLACK_ROOK, 61, 63);}
                }
            }
        }

        this.whiteToPlay = !this.whiteToPlay;
    }

    private void addAttackedSquare(int squarePosition, int attackerPosition){
        if(squaresAttackers[squarePosition] == null){
            squaresAttackers[squarePosition] = new PartialIntegerList();
        }
        squaresAttackers[squarePosition].addElement(attackerPosition);
    }

    public ChessPiece getPiece(int row, int column){
        return getPiece(getPosition(row, column));
    }

    public ChessPiece getPiece(int position){
        return ChessPiece.getPiece(board[position]);
    }

    public Set<Integer> getPiecePositions(ChessPiece piece){
        return piecesPositions.getOrDefault(piece, new HashSet<>());
    }

    public Map<ChessPiece, Set<Integer>> getPiecesPositions(){
        return piecesPositions;
    }

    private void addPiecePosition(ChessPiece piece, int position){
        Set<Integer> positions = getPiecePositions(piece);
        positions.add(position);
        piecesPositions.put(piece, positions);
        board[position] = piece.getId();

        if(piece.getUnsignedId() == 6){
            if(piece.isWhite()){
                whiteKingPosition = position;
            } else{
                blackKingPosition = position;
            }
        }
    }

    private void removePiecePosition(ChessPiece piece, int position){
        Set<Integer> positions = getPiecePositions(piece);
        positions.remove(position);
        piecesPositions.put(piece, positions);
        board[position] = 0;
    }

    private void switchPiecePosition(ChessPiece piece, int previousPosition, int newPosition){
        Set<Integer> positions = getPiecePositions(piece);
        positions.remove(previousPosition);
        positions.add(newPosition);
        piecesPositions.put(piece, positions);
        board[previousPosition] = 0;
        board[newPosition] = piece.getId();

        if(piece.getUnsignedId() == 6){
            if(piece.isWhite()){
                whiteKingPosition = newPosition;
            } else{
                blackKingPosition = newPosition;
            }
        }
    }

    public void loadFen(String fen){
        piecesPositions.clear();
        for(int i = 0; i < 64; i++){
            board[i] = 0;
        }

        castles = 0;

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
                    addPiecePosition(piece, getPosition(row, column));
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
                    case WHITE_KING -> castles |= 0b1000;
                    case WHITE_QUEEN -> castles |= 0b0100;
                    case BLACK_KING -> castles |= 0b0010;
                    case BLACK_QUEEN -> castles |= 0b0001;
                }
            }
        }

        if(args.length >= 4){
            if(args[3].equalsIgnoreCase("-")){
                this.enPassant = 0;
            } else {
                int value = getPosition(args[3]);
                this.enPassant = whiteToPlay ? value - 8 : value + 8;
            }
        }

        if(args.length >= 5){
            try {
                this.halfMovesRule = Integer.parseInt(args[4]);
            } catch (NumberFormatException ignore){}
        }

        if(args.length >= 6){
            try {
                this.moveCount = Integer.parseInt(args[5]);
            } catch (NumberFormatException ignore){}
        }
    }

    public static int getRow(int position){
        return position >>> 3;
    }

    public static int getColumn(int position){
        return position & 0b111;
    }

    public static int getPosition(int row, int column){
        return (row << 3) | column;
    }

    public static int getPosition(String pos){
        int row = pos.charAt(1) - '1';
        int column = pos.charAt(0) - 'a';
        return getPosition(row, column);
    }

    public static boolean isInBoard(int row, int column){
        return row >= 0 && row < 8 && column >= 0 && column < 8;
    }

    public int getStandardMove(int startingPosition, int finalPosition, ChessPiece movedPiece, ChessPiece capturedPiece){
        int i = (castles << 24) | (enPassant << 18) | (capturedPiece == null ? 0 : capturedPiece.getUnsignedId() << 15) | (movedPiece.getUnsignedId() << 12) | (finalPosition << 6) | startingPosition;
        return i;
    }

    public int getPromotionMove(int startingPosition, int finalPosition, ChessPiece movedPiece, ChessPiece capturedPiece, ChessPiece promotionResultPiece){
        return 0x80000000 | (promotionResultPiece.getUnsignedId() << 28) | (castles << 24) | (enPassant << 18) | (capturedPiece == null ? 0 : capturedPiece.getUnsignedId() << 15) | (movedPiece.getUnsignedId() << 12) | (finalPosition << 6) | startingPosition;
    }

    public int getCastleMove(int startingPosition, int finalPosition, ChessPiece movedPiece, boolean kingSide){
        return 0x60000000 | ((kingSide ? 1 : 0) << 28) | (castles << 24) | (enPassant << 18) | (movedPiece.getUnsignedId() << 12) | (finalPosition << 6) | startingPosition;
    }

    public int getEnPassantMove(int startingPosition, int finalPosition, ChessPiece movedPiece, ChessPiece capturedPiece){
        return 0x40000000 | (castles << 24) | (enPassant << 18) | (capturedPiece.getUnsignedId() << 15) | (movedPiece.getUnsignedId() << 12) | (finalPosition << 6) | startingPosition;
    }

    public int getTwoSquaresPawnMove(int startingPosition, int finalPosition, ChessPiece movedPiece){
        return 0x20000000 | (castles << 24) | (enPassant << 18) | (movedPiece.getUnsignedId() << 12) | (finalPosition << 6) | startingPosition;
    }

    public static boolean isEnPassantMove(int move){
        return (move & 0xf0000000) == 0x40000000;
    }

    public static int getMoveEnPassantPosition(int move){
        return (move & 0x00fc0000) >>> 18;
    }

    public static int getMoveInitialPosition(int move){
        return move & 0x0000003f;
    }

    public static int getMoveFinalPosition(int move){
        return (move & 0x00000fc0) >>> 6;
    }

    public int perft(int depth, int depthPrint){
        if(depth == 0){
            return 1;
        }

        int count = 0;
        for(int move : getAllowedMoves()){
            playMove(move);
            int moveCount = perft(depth - 1, depthPrint);
            undoMove(move);

            if(depthPrint == depth){
                int initialPos = getMoveInitialPosition(move);
                int finalPos = getMoveFinalPosition(move);
                boolean promotion = move < 0;
                String extra = "";
                if(promotion){
                    int promotedPiece = (move & 0x70000000) >>> 28;
                    extra = "" + ChessPiece.getPiece(promotedPiece).getFenChar();
                }
                System.out.println(AbstractChessBoard.getPositionString(getRow(initialPos), getColumn(initialPos)) + AbstractChessBoard.getPositionString(getRow(finalPos), getColumn(finalPos)) + extra + ": " + moveCount);
            }

            count += moveCount;
        }

        return count;
    }

    public static final String[] perftFens = new String[]{
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 ",
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -",
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
            "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10"
    };
    public static final int[][] perftExpectedResults = new int[][] {
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

    public boolean isWhiteToPlay() {
        return whiteToPlay;
    }

    public void printBoard(){
        for(int i = 7; i > -1; i--){
            for(int j = 0; j < 8; j++){
                int p = board[i * 8 + j];
                System.out.print(p >= 0 ? " " + p : p);
                if(j != 7) System.out.print(" |");
            }
            System.out.println();
        }
    }


    private static class PartialIntegerList{

        int[] array = new int[16];
        int size = 0;

        public PartialIntegerList() {}

        public void addElement(int value){
            array[size] = value;
            size++;
        }

        public int getElement(int index){
            return array[index];
        }

        public int size(){
            return size;
        }
    }
}
