import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Board {

    private static final int[][] NUMBERS = {
            {1,5,4,3,2,6,0},
            {0,3,1,2,6,4,5},
            {6,2,3,5,1,0,4},
            {3,4,2,6,0,5,1},
            {4,6,0,1,5,2,3},
            {5,0,6,4,3,1,2},
            {2,1,5,0,4,3,6}
    };

    private final Field[][] boardFields;
    private final List<Field> allFields;

    private final List<Piece> piecesOnBoard = new LinkedList<>();

    public Board() {
        boardFields = new Field[7][7];
        allFields = new LinkedList<>();
        initField();

        Arrays.stream(PieceCollection.ALL_PIECES).forEach(piece -> piece.setBoard(this));

        /*Piece thunder = PieceCollection.ALL_PIECES[0];
        Piece smallL = PieceCollection.ALL_PIECES[1];
        Piece bigL = PieceCollection.ALL_PIECES[2];
        Piece w = PieceCollection.ALL_PIECES[3];
        Piece block = PieceCollection.ALL_PIECES[4];
        Piece c = PieceCollection.ALL_PIECES[5];
        Piece chair = PieceCollection.ALL_PIECES[6];
        Piece stair = PieceCollection.ALL_PIECES[7];
        Piece straight = PieceCollection.ALL_PIECES[8];

        placePieceOnBoard(thunder, thunder.getOrientations()[1], 2, 0);
        placePieceOnBoard(smallL, smallL.getOrientations()[1], 5, 4);
        placePieceOnBoard(bigL, bigL.getOrientations()[1], 0,0);
        placePieceOnBoard(w, w.getOrientations()[0], 4,0);
        placePieceOnBoard(block, block.getOrientations()[0], 0,4);
        placePieceOnBoard(c, c.getOrientations()[3], 2, 5);
        placePieceOnBoard(chair, chair.getOrientations()[1], 2,2);
        placePieceOnBoard(stair, stair.getOrientations()[2],5, 2 );
        placePieceOnBoard(straight, straight.getOrientations()[0], 1,1);*/
    }

    public void placePieceOnBoard(Piece piece, PieceOrientation orientation, int rowOffset, int columnOffset) {
        var occupiedFields = Arrays.stream(orientation.getPositions())
                .map(partialPiecePos -> getFieldOnBoard(partialPiecePos.row() + rowOffset, partialPiecePos.column() + columnOffset))
                .toList();

        occupiedFields.forEach(field -> field.setOccupationPiece(piece));
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
        piece.setOccupiedFields(null);
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

    public void reset() {
        piecesOnBoard.clear();
        allFields.forEach(field -> field.setOccupationPiece(null));
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
