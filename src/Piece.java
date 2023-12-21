import java.util.List;

public class Piece {

    private final int amountOccupations;
    private final PieceOrientation[] orientations;

    private List<Field> occupiedFields;

    public Piece(int amountOccupations, PieceOrientation[] orientations) {
        this.amountOccupations = amountOccupations;
        this.orientations = orientations;
    }

    public int getAmountOccupations() {
        return amountOccupations;
    }

    public PieceOrientation[] getOrientations() {
        return orientations;
    }

    public List<Field> getOccupiedFields() {
        return occupiedFields;
    }

    public void setOccupiedFields(List<Field> occupiedFields) {
        this.occupiedFields = occupiedFields;
    }
}
