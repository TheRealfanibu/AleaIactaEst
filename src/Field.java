public class Field {

    private Piece occupationPiece;

    private final FieldPosition position;
    private final int number;

    public Field(int row, int column, int number) {
        this.number = number;
        position = new FieldPosition(row, column);
    }

    public int getTopLeftCornerXCoordinate() {
        return position.column() * MainFrame.FIELD_SIZE;
    }

    public int getTopLeftCornerYCoordinate() {
        return position.row() * MainFrame.FIELD_SIZE;
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
