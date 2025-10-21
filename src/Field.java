public class Field {

    private Piece occupationPiece;

    private final FieldPosition position;
    private final int number;

    private final int fieldSize;

    public Field(int row, int column, int number, int fieldSize) {
        this.number = number;
        this.fieldSize = fieldSize;
        position = new FieldPosition(row, column);
    }

    public int getTopLeftCornerXCoordinate() {
        return position.column() * fieldSize;
    }

    public int getTopLeftCornerYCoordinate() {
        return position.row() * fieldSize;
    }

    public boolean isOccupied() {
        return occupationPiece != null;
    }

    public void setOccupationPiece(Piece occupationPiece) {
        this.occupationPiece = occupationPiece;
    }

    public Piece getOccupationPiece() {
        return occupationPiece;
    }

    public int getNumber() {
        return number;
    }

    public int getRow() {
        return position.row();
    }

    public int getColumn() {
        return position.column();
    }

}
