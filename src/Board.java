import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Board {

    private static final int[][] NUMBERS = {
            {2,5,4,3,6,0,1},
            {1,0,6,4,2,3,5},
            {5,6,0,2,3,1,4},
            {0,4,1,6,5,2,3},
            {4,3,5,0,1,6,2},
            {3,1,2,5,0,4,6},
            {6,2,3,1,4,5,0}
    };

    private final Field[][] boardFields;
    private final List<Field> allFields;

    private final List<Piece> piecesOnBoard = new LinkedList<>();

    public Board() {
        boardFields = new Field[7][7];
        allFields = new LinkedList<>();
        initField();

        Piece piece1 = PieceCollection.ALL_PIECES[1];
        Piece piece2 = PieceCollection.ALL_PIECES[4];
        Piece piece3 = PieceCollection.ALL_PIECES[3];

        placePieceOnBoard(piece1, piece1.getOrientations()[3], 3,1);
        placePieceOnBoard(piece2, piece2.getOrientations()[1], 4,3);
        placePieceOnBoard(piece3, piece3.getOrientations()[1], 4,0);
    }

    private void placePieceOnBoard(Piece piece, PieceOrientation orientation, int rowOffset, int columnOffset) {
        var occupiedFields = Arrays.stream(orientation.getPositions())
                .map(partialPiecePos -> getFieldOnBoard(partialPiecePos.row() + rowOffset, partialPiecePos.column() + columnOffset))
                .toList();

        occupiedFields.forEach(field -> field.setOccupationPiece(piece));
        piece.setOccupiedFields(occupiedFields);

        piecesOnBoard.add(piece);
    }

    private void initField() {
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                Field field = new Field(i, j, NUMBERS[i][j]);
                boardFields[i][j] = field;
                allFields.add(field);
            }
        }
    }

    public List<Piece> getPiecesOnBoard() {
        return piecesOnBoard;
    }

    public Field getFieldOnBoard(int row, int column) {
        return boardFields[row][column];
    }

    public List<Field> getAllFields() {
        return allFields;
    }
}
