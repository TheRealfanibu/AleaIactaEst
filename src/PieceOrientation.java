import java.util.Arrays;

public class PieceOrientation {

    private final FieldPosition[] positions;

    private final int width;

    private final int height;

    public PieceOrientation(FieldPosition[] positions) {
        this.positions = positions;

        width = Arrays.stream(positions).mapToInt(FieldPosition::column).max().orElseThrow() + 1;
        height = Arrays.stream(positions).mapToInt(FieldPosition::row).max().orElseThrow() + 1;
    }

    public FieldPosition[] getPositions() {
        return positions;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
