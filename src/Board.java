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

        Piece piece = PieceCollection.ALL_PIECES[1];

        placePieceOnBoard(piece, piece.getOrientations()[3], 1,2);

        piecesOnBoard.add(piece);
    }

    private void placePieceOnBoard(Piece piece, PieceOrientation orientation, int rowOffset, int columnOffset) {
        var occupiedFields = Arrays.stream(orientation.getPositions())
                .map(piecePosition -> boardFields[piecePosition.row() + rowOffset][piecePosition.column() + columnOffset])
                .toList();

        occupiedFields.forEach(field -> field.setOccupied(true));
        piece.setOccupiedFields(occupiedFields);
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

    public Field[][] getBoardFields() {
        return boardFields;
    }

    public List<Field> getAllFields() {
        return allFields;
    }
}
