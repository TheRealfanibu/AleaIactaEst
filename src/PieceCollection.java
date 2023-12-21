import java.util.Arrays;
import java.util.stream.IntStream;

public class PieceCollection {

    public static final Piece[] ALL_PIECES = {
            // Thunder-Piece
            createPiece(
                    new int[]{0, 1, 1, 1, 2},
                    new int[]{0, 0, 1, 2, 2}, true
            ),
            // small L-piece
            createPiece(
                    new int[]{0, 0, 1, 2},
                    new int[]{0, 1, 1, 1}, false
            ),
            // big L-piece
            createPiece(
                    new int[]{0, 1, 1, 1, 1},
                    new int[]{0, 0, 1, 2, 3}, false),
            // W-piece
            createPiece(
                    new int[]{2,2,1,1,0},
                    new int[]{0,1,1,2,2}, false
            ),
            // block-piece
            createPiece(
                    new int[]{0,0,0,1,1},
                    new int[]{0,1,2,1,2}, false
            ),
            // C-piece
            createPiece(
                    new int[]{0,1,1,1,0},
                    new int[]{0,0,1,2,2}, false
            ),
            // chair-piece
            createPiece(
                    new int[]{1,1,2,1,0},
                    new int[]{0,1,1,2,2}, false
            ),
            // stair-piece
            createPiece(
                    new int[]{1,1,1,0,0},
                    new int[]{0,1,2,2,3}, false
            ),
            // straight-piece
            createPiece(
                    new int[]{0,0,0},
                    new int[]{0,1,2}, true
            )
    };

    private static Piece createPiece(int[] rows, int[] columns, boolean symmetric) {
        FieldPosition[] fieldPositions = IntStream.range(0, rows.length)
                .mapToObj(i -> new FieldPosition(rows[i], columns[i]))
                .toArray(FieldPosition[]::new);

        int amountOrientations = symmetric ? 2 : 4;
        PieceOrientation[] orientations = new PieceOrientation[amountOrientations];

        orientations[0] = new PieceOrientation(fieldPositions);

        for (int i = 1; i < amountOrientations; i++) {
            FieldPosition[] rotatedPositions = new FieldPosition[fieldPositions.length];

            for (int j = 0; j < fieldPositions.length; j++) {
                FieldPosition oldPosition = fieldPositions[j];
                int newColumn = -oldPosition.row();
                int newRow = oldPosition.column();

                rotatedPositions[j] = new FieldPosition(newRow, newColumn);
            }
            int minRow = Arrays.stream(rotatedPositions).mapToInt(FieldPosition::row).min().orElseThrow();
            int minColumn = Arrays.stream(rotatedPositions).mapToInt(FieldPosition::column).min().orElseThrow();

            FieldPosition[] shiftedRotatedPositions = Arrays.stream(rotatedPositions)
                    .map(oldPos -> new FieldPosition(oldPos.row() - minRow, oldPos.column() - minColumn))
                    .toArray(FieldPosition[]::new);


            orientations[i] = new PieceOrientation(shiftedRotatedPositions);
            fieldPositions = shiftedRotatedPositions;
        }
        return new Piece(fieldPositions.length, orientations);
    }
}
