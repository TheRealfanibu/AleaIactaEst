public class Field {

    private boolean occupied;

    private final FieldPosition position;
    private final int number;

    public Field(int row, int column, int number) {
        this.number = number;
        position = new FieldPosition(row, column);
        occupied = false;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public int getNumber() {
        return number;
    }

    public FieldPosition getPosition() {
        return position;
    }

}
