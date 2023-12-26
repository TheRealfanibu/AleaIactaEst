import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.stage.Stage;

import java.util.List;

public class MainFrame extends Application {
    public static final int FIELD_SIZE = 100;

    private static final int FIELD_SIZE_HALF = FIELD_SIZE / 2;

    private static final int DICE_CIRCLE_SIZE = 15;

    private static final int DICE_CIRCLE_SIZE_HALF = DICE_CIRCLE_SIZE / 2;

    private static final int BORDER_CIRCLE_GAP = 15;
    private static final int CANVAS_SIZE = 7 * FIELD_SIZE;

    private final Canvas canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
    private final GraphicsContext graphics = canvas.getGraphicsContext2D();

    private final Board board = new Board();

    @Override
    public void start(Stage stage) {
        initCanvas();

        List<Integer> dices = List.of(1,5,2,2,1,4);
        new Solver().solve(board, dices);
        drawBoard();

        BorderPane layout = new BorderPane(canvas);
        layout.setPadding(new Insets(30));

        stage.setScene(new Scene(layout));
        stage.show();
    }

    private void initCanvas() {
        graphics.setFill(Color.BEIGE);
        graphics.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        graphics.setFill(Color.BLACK);
        graphics.setLineWidth(3);
        graphics.strokeRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        graphics.setLineWidth(1);
        for (int i = 0; i < 7; i++) {
            graphics.strokeLine(i * FIELD_SIZE, 0, i* FIELD_SIZE, CANVAS_SIZE);
        }
        for (int i = 0; i < 7; i++) {
            graphics.strokeLine(0, i * FIELD_SIZE, CANVAS_SIZE, i * FIELD_SIZE);
        }
    }

    private void drawBoard() {
        board.getPiecesOnBoard().forEach(this::drawPiece);
        board.getAllFields().stream()
                .filter(field -> !field.isOccupied())
                .forEach(this::drawField);
    }

    private void drawField(Field field) {
        if (field.getNumber() == 0)
            return;

        int xCoordinateOffset = field.getTopLeftCornerXCoordinate();
        int yCoordinateOffset = field.getTopLeftCornerYCoordinate();

        graphics.setFill(Color.BLACK);
        if (field.getNumber() != 1 && field.getNumber() != 3) {
            graphics.fillOval(xCoordinateOffset + BORDER_CIRCLE_GAP, yCoordinateOffset + BORDER_CIRCLE_GAP,
                    DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
            graphics.fillOval(xCoordinateOffset + FIELD_SIZE - BORDER_CIRCLE_GAP - DICE_CIRCLE_SIZE,
                    yCoordinateOffset + FIELD_SIZE - BORDER_CIRCLE_GAP - DICE_CIRCLE_SIZE,
                    DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
        }
        if (field.getNumber() != 1 && field.getNumber() != 2) {
            graphics.fillOval(xCoordinateOffset + FIELD_SIZE - BORDER_CIRCLE_GAP - DICE_CIRCLE_SIZE,
                    yCoordinateOffset + BORDER_CIRCLE_GAP, DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
            graphics.fillOval(xCoordinateOffset + BORDER_CIRCLE_GAP,
                    yCoordinateOffset + FIELD_SIZE - BORDER_CIRCLE_GAP - DICE_CIRCLE_SIZE,
                    DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
        }
        if (field.getNumber() % 2 == 1) {
            graphics.fillOval(xCoordinateOffset + FIELD_SIZE_HALF - DICE_CIRCLE_SIZE_HALF,
                    yCoordinateOffset + FIELD_SIZE_HALF - DICE_CIRCLE_SIZE_HALF,
                    DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
        }
        if (field.getNumber() == 6) {
            graphics.fillOval(xCoordinateOffset + BORDER_CIRCLE_GAP,
                    yCoordinateOffset + FIELD_SIZE_HALF - DICE_CIRCLE_SIZE_HALF,
                    DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
            graphics.fillOval(xCoordinateOffset + FIELD_SIZE - BORDER_CIRCLE_GAP - DICE_CIRCLE_SIZE,
                    yCoordinateOffset + FIELD_SIZE_HALF - DICE_CIRCLE_SIZE_HALF,
                    DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
        }

    }

    private void drawPiece(Piece piece) {
        List<Field> occupiedFields = piece.getOccupiedFields();
        if (occupiedFields.isEmpty()) {
            throw new IllegalStateException("Piece is not on board and is called for render");
        }

        graphics.setFill(Color.SADDLEBROWN);
        occupiedFields.forEach(field ->
                graphics.fillRect(field.getTopLeftCornerXCoordinate(), field.getTopLeftCornerYCoordinate(),
                        FIELD_SIZE, FIELD_SIZE));
        drawPieceBounds(piece, 10);
        drawPieceBounds(piece, 17);
    }

    private void drawPieceBounds(Piece piece, int gap) {
        graphics.setStroke(Color.BLACK);
        graphics.setLineWidth(2);
        for (Field field : piece.getOccupiedFields()) {
            for (Direction direction : Direction.values()) {
                drawSideConnection(field, piece, direction, gap);
            }
        }
    }

    private void drawSideConnection(Field field, Piece piece, Direction baseDirection, int gap) {
        if (!isAdjacentFieldPartOfPiece(field, piece, baseDirection)) {
            int gapLineStartX = getStartX(field, baseDirection, gap);
            int gapLineStartY = getStartY(field, baseDirection, gap);
            int gapLineEndX = getEndX(field, baseDirection, gap);
            int gapLineEndY = getEndY(field, baseDirection, gap);

            graphics.strokeLine(gapLineStartX, gapLineStartY, gapLineEndX, gapLineEndY);

            /*graphics.setLineWidth(1);
            graphics.strokeLine(getStartX(field, baseDirection, 0), getStartY(field, baseDirection, 0),
                    getEndX(field, baseDirection, 0), getEndY(field, baseDirection, 0));
            graphics.setLineWidth(2);*/

            Direction firstAdjacentDirection = baseDirection == Direction.DOWN || baseDirection == Direction.UP
                    ? Direction.LEFT : Direction.UP;
            Direction secondAdjacentDirection = baseDirection == Direction.DOWN || baseDirection == Direction.UP
                    ? Direction.RIGHT : Direction.DOWN;
            drawOneSideConnection(field, piece, baseDirection, firstAdjacentDirection, gapLineStartX, gapLineStartY, gap);
            drawOneSideConnection(field, piece, baseDirection, secondAdjacentDirection, gapLineEndX, gapLineEndY, gap);
        }
    }

    private int getStartX(Field field, Direction direction, int gap) {
        return direction != Direction.RIGHT ? field.getTopLeftCornerXCoordinate() + gap :
                field.getTopLeftCornerXCoordinate() + FIELD_SIZE - gap;
    }

    private int getEndX(Field field, Direction direction, int gap) {
        return direction != Direction.LEFT ? field.getTopLeftCornerXCoordinate() + FIELD_SIZE - gap :
                field.getTopLeftCornerXCoordinate() + gap;
    }

    private int getStartY(Field field, Direction direction, int gap) {
        return direction != Direction.DOWN ? field.getTopLeftCornerYCoordinate() + gap :
                field.getTopLeftCornerYCoordinate() + FIELD_SIZE - gap;
    }

    private int getEndY(Field field, Direction direction, int gap) {
        return direction != Direction.UP ? field.getTopLeftCornerYCoordinate() + FIELD_SIZE - gap :
                field.getTopLeftCornerYCoordinate() + gap;
    }

    private void drawOneSideConnection(Field field, Piece piece, Direction baseDirection, Direction adjacentDirection, int startX, int startY, int gap) {
        if (isAdjacentFieldPartOfPiece(field, piece, adjacentDirection)) {
            graphics.strokeLine(startX, startY, startX + adjacentDirection.columnOffset * gap, startY + adjacentDirection.rowOffset * gap);
            drawArcConnection(field, piece, baseDirection, adjacentDirection, gap);
        }
    }

    private void drawArcConnection(Field field, Piece piece, Direction baseDirection, Direction adjacentDirection,
                                   int gap) {
        if ((baseDirection == Direction.DOWN || baseDirection == Direction.UP) &&
                isDiagonalFieldPartOfPiece(field, piece, baseDirection, adjacentDirection)) {
            int arcStartX = field.getTopLeftCornerXCoordinate() - gap;
            int arcStartY = field.getTopLeftCornerYCoordinate() - gap;
            int startAngle;

            if (baseDirection == Direction.DOWN) {
                arcStartY += FIELD_SIZE;
            }
            if (adjacentDirection == Direction.RIGHT) {
                arcStartX += FIELD_SIZE;
            }
            if (baseDirection == Direction.DOWN && adjacentDirection == Direction.RIGHT) {
                startAngle = 0;
            } else if (baseDirection == Direction.DOWN && adjacentDirection == Direction.LEFT) {
                startAngle = 90;
            } else if (baseDirection == Direction.UP && adjacentDirection == Direction.LEFT) {
                startAngle = 180;
            } else { // baseDirection == Direction.UP && adjacentDirection == Direction.RIGHT
                startAngle = 270;
            }

            graphics.strokeArc(arcStartX, arcStartY, 2 * gap, 2 * gap, startAngle, 90, ArcType.OPEN);
        }
    }


    private boolean isDiagonalFieldPartOfPiece(Field field, Piece piece, Direction baseDirection, Direction adjacentDirection) {
        return isFieldPartOfPiece(field, piece,
                baseDirection.rowOffset + adjacentDirection.rowOffset, baseDirection.columnOffset + adjacentDirection.columnOffset);
    }

    private boolean isAdjacentFieldPartOfPiece(Field field, Piece piece, Direction direction) {
        return isFieldPartOfPiece(field, piece, direction.rowOffset, direction.columnOffset);
    }

    private boolean isFieldPartOfPiece(Field field, Piece piece, int rowOffset, int columnOffset) {
        int row = field.getRow() + rowOffset;
        int column = field.getColumn() + columnOffset;

        return !isOutOfBounds(row, column) && board.getFieldOnBoard(row, column)
                .getOccupationPiece() == piece;
    }

    private boolean isOutOfBounds(int row, int column) {
        return row < 0 || row > 6 || column < 0 || column > 6;
    }


    public static void main(String[] args) {
        launch();
    }

    private enum Direction {
        LEFT(0, -1),
        RIGHT(0, 1),
        UP(-1, 0),
        DOWN(1, 0);

        public final int rowOffset;
        public final int columnOffset;

        Direction(int rowOffset, int columnOffset) {
            this.rowOffset = rowOffset;
            this.columnOffset = columnOffset;
        }
    }
}
