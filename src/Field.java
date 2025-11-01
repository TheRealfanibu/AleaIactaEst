public class Field {

    private Piece occupationPiece;

    private final FieldPosition position;
    private final int number;

    private final String id;

    private Dice fixedDice;

    public Field(int row, int column, int number) {
        this.number = number;
        position = new FieldPosition(row, column);
        id = String.valueOf(row * Board.DIM + column);
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

    public String getId() {
        return id;
    }

    public boolean isDiceFixed() {
        return fixedDice != null;
    }

    public void setFixedDice(Dice fixedDice) {
        this.fixedDice = fixedDice;
    }

    public Dice getFixedDice() {
        return fixedDice;
    }

    @Override
    public String toString() {
        return position.toString();
    }
}
