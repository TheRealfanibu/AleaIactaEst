import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class Board {


    private static final int[][] NUMBERS = { // original board (no solution for 6x2)
            {1, 5, 4, 3, 2, 6, 0},
            {0, 3, 1, 2, 6, 4, 5},
            {6, 2, 3, 5, 1, 0, 4},
            {3, 4, 2, 6, 0, 5, 1},
            {4, 6, 0, 1, 5, 2, 3},
            {5, 0, 6, 4, 3, 1, 2},
            {2, 1, 5, 0, 4, 3, 6}
    };

//        private static final int[][] NUMBERS = { // example board with solutions for all dice combinations
//            {2,0,5,1,6,3,4},
//            {1,5,4,3,0,6,2},
//            {4,6,0,2,3,1,5},
//            {3,4,2,6,5,0,1},
//            {5,3,6,4,1,2,0},
//            {0,1,3,5,2,4,6},
//            {6,2,1,0,4,5,3},
//    };

    public static final int DIM = NUMBERS.length; // on normal board = 7


    private final Field[][] boardFields = new Field[DIM][DIM];
    private final List<Field> allFields = new LinkedList<>();
    private final List<Field> fixedFields = new LinkedList<>();

    private final List<Piece> allPieces = PieceCollection.createPieceInstances();
    private final List<Piece> piecesOnBoard = new LinkedList<>();

    public Board() {
        initField();

        allPieces.forEach(piece -> piece.setBoard(this));
    }

    public void addFixedDice(Dice dice, Field field) {
        dice.setFixedField(field);
        field.setFixedDice(dice);
        fixedFields.add(field);
    }

    public void removeFixedDice(Dice dice, Field field, boolean remove) {
        dice.setFixedField(null);
        field.setFixedDice(null);
        if(remove) {
            fixedFields.remove(field);
        }
    }

    public Stream<Field> getFieldsNotOccupiedByPiece() {
        return allFields.stream().filter(field -> !field.isOccupiedByPiece());
    }

    public Stream<Field> getUnoccupiedFields() {
        return allFields.stream().filter(field -> !field.isOccupied());
    }

    public static boolean isOutOfBounds(int row, int column) {
        return row < 0 || row > 6 || column < 0 || column > 6;
    }

    public int[] countVisibleDiceNumbers() {
        return countDiceNumbersOfFields(getFieldsNotOccupiedByPiece());
    }

    public static int[] countDiceNumbersOfFields(Stream<Field> fieldStream) {
        return countDiceNumbers(fieldStream.map(Field::getNumber));
    }

    public static int[] countDiceNumbers(Stream<Integer> numbers) {
        int[] diceNumbers = new int[DIM];
        numbers.forEach(number -> diceNumbers[number]++);
        return diceNumbers;
    }

    public void placePieceOnBoard(Piece piece, PieceOrientation orientation, int rowOffset, int columnOffset) {
        List<Field> occupiedFields = new ArrayList<>(piece.getNumOccupations());
        for (FieldPosition partialPiecePos : orientation.getPositions()) {
            occupiedFields.add(getFieldOnBoard(partialPiecePos.row() + rowOffset, partialPiecePos.column() + columnOffset));
        }

        occupiedFields.forEach(field -> field.setOccupationPiece(piece));
        piece.setRowOffsetOnBoard(rowOffset);
        piece.setColumnOffsetOnBoard(columnOffset);
        piece.setOrientationOnBoard(orientation);
        piece.setOccupiedFields(occupiedFields);

        piecesOnBoard.add(piece);
    }

    public void removePieceFromBoard(Piece piece) {
        piecesOnBoard.remove(piece);
        removePieceFieldInfo(piece);
    }

    public void removeLastPieceFromBoard() {
        Piece piece = piecesOnBoard.remove(piecesOnBoard.size() - 1);
        removePieceFieldInfo(piece);
    }

    private void removePieceFieldInfo(Piece piece) {
        piece.getOccupiedFields().forEach(field -> field.setOccupationPiece(null));
    }


    private void initField() {
        for (int i = 0; i < DIM; i++) {
            for (int j = 0; j < DIM; j++) {
                Field field = new Field(i, j, NUMBERS[i][j]);
                boardFields[i][j] = field;
                allFields.add(field);
            }
        }
    }

    public void reset() {
        piecesOnBoard.forEach(this::removePieceFieldInfo);
        piecesOnBoard.clear();
    }

    public void resetFixedFields() {
        fixedFields.forEach(field -> removeFixedDice(field.getFixedDice(), field, false));
        fixedFields.clear();
    }

    public List<Piece> getPiecesOnBoard() {
        return piecesOnBoard;
    }

    public List<Piece> getAvailablePieces() {
        List<Piece> availablePieces = new LinkedList<>(allPieces);
        availablePieces.removeAll(piecesOnBoard);
        return availablePieces;
    }

    public Field getFieldOnBoard(int row, int column) {
        return boardFields[row][column];
    }

    public List<Piece> getAllPieces() {
        return allPieces;
    }

    public Board copy() {
        Board copyBoard = new Board();
        for (Piece piece : piecesOnBoard) {
            Piece copyPiece = copyBoard.allPieces.get(piece.getId());
            copyBoard.placePieceOnBoard(copyPiece, piece.getOrientationOnBoard(),
                    piece.getRowOffsetOnBoard(), piece.getColumnOffsetOnBoard());
        }
        fixedFields.forEach(field -> copyBoard.addFixedDice(field.getFixedDice(),
                copyBoard.getFieldOnBoard(field.getRow(), field.getColumn())));
        return copyBoard;
    }

    public boolean fitsInPlace(PieceOrientation orientation, int rowOffset, int columnOffset) {
        for (FieldPosition pieceField : orientation.getPositions()) {
            int row = pieceField.row() + rowOffset;
            int column = pieceField.column() + columnOffset;
            if (isOutOfBounds(row, column) || getFieldOnBoard(row, column).isOccupied()) {
                return false;
            }
        }
        return true;
    }
}
