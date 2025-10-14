import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MainFrame extends Application {
    public static final int FIELD_SIZE = 80;
    public static final int OUTER_PIECE_MARGIN = 7;
    public static final int INNER_PIECE_MARGIN = 14;

    private static final int CANVAS_SIZE = 7 * FIELD_SIZE;

    private final Canvas canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
    private final GraphicsContext graphics = canvas.getGraphicsContext2D();

    private final Dice[] dices = new Dice[6];
    private final Board board = new Board();

    private final Solver solver = new Solver();
    private int currentPieceDrawX;
    private int currentPieceDrawY;

    @Override
    public void start(Stage stage) {
        initCanvas();

        BorderPane layout = new BorderPane();
        layout.setCenter(canvas);
        layout.setBottom(createDicePane());
        layout.setPadding(new Insets(30));


        stage.setScene(new Scene(layout));
        stage.show();
    }

    private Pane createDicePane() {
        VBox vBox = new VBox(40);
        vBox.setAlignment(Pos.CENTER);
        vBox.setPadding(new Insets(30, 0, 30, 0));

        HBox hBox = new HBox(30);
        hBox.setAlignment(Pos.CENTER);
        for (int i = 0; i < 6; i++) {
            dices[i] = new Dice(i + 1);
            hBox.getChildren().add(dices[i]);
        }

        Button button = new Button("Solve");
        button.setFont(Font.font(30));
        button.onActionProperty().set(e -> solve());

        vBox.getChildren().addAll(hBox, button);
        return vBox;
    }

    private void solve() {
        List<Integer> diceNumbers = Arrays.stream(dices).map(Dice::getNumber).toList();
        solver.solve(board, diceNumbers);
        initCanvas();
        System.out.println("Solution found");
    }

    private void initCanvas() {
        graphics.setFill(Dice.BACKGROUND_COLOR);
        graphics.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        graphics.setStroke(Color.BLACK);
        graphics.setLineWidth(10);
        graphics.strokeRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        graphics.setLineWidth(1);
        for (int i = 1; i < 7; i++) {
            graphics.strokeLine(i * FIELD_SIZE, 0, i* FIELD_SIZE, CANVAS_SIZE);
        }
        for (int i = 1; i < 7; i++) {
            graphics.strokeLine(0, i * FIELD_SIZE, CANVAS_SIZE, i * FIELD_SIZE);
        }

        graphics.beginPath();

        drawBoard();
    }

    private void drawBoard() {
        board.getPiecesOnBoard().forEach(this::createPieceFigure);
        board.getAllFields().stream()
                .filter(field -> !field.isOccupied())
                .forEach(this::drawField);
    }

    private void drawField(Field field) {
        if (field.getNumber() == 0)
            return;

        int xOffset = field.getTopLeftCornerXCoordinate();
        int yOffset = field.getTopLeftCornerYCoordinate();

        Dice.drawNumber(graphics, field.getNumber(), xOffset, yOffset);
    }

    private void createPieceFigure(Piece piece) {

        graphics.setFill(Color.SADDLEBROWN);

        graphics.setLineWidth(3);
        createPieceShape(piece, OUTER_PIECE_MARGIN);
        graphics.fill();
        graphics.stroke();

        graphics.setLineWidth(2);
        createPieceShape(piece, INNER_PIECE_MARGIN);
        graphics.stroke();
    }

    private void createPieceShape(Piece piece, int gap) {
        List<Field> occupiedFields = piece.getOccupiedFields();
        if (occupiedFields.isEmpty()) {
            throw new IllegalStateException("Piece is not on board and is called for render");
        }

        PiecePosition startPosition = getStartPiecePosition(piece, occupiedFields);

        Field currentField = startPosition.field();
        Direction currentDirection = startPosition.direction();
        currentPieceDrawX = getStartX(currentField, currentDirection, gap);
        currentPieceDrawY = getStartY(currentField, currentDirection, gap);

        graphics.beginPath();
        graphics.moveTo(currentPieceDrawX, currentPieceDrawY);
        do {
            currentPieceDrawX = getEndX(currentField, currentDirection, gap); // big line
            currentPieceDrawY = getEndY(currentField, currentDirection, gap);
            graphics.lineTo(currentPieceDrawX, currentPieceDrawY);

            Direction adjacentDirection = currentDirection.next();

            if (isAdjacentFieldPartOfPiece(currentField, piece, adjacentDirection)) {
                PiecePosition nextPosition = drawAndFindNextPiecePosition(currentField, piece,
                        currentDirection, adjacentDirection, gap);
                currentField = nextPosition.field();
                currentDirection = nextPosition.direction();
            } else {
                currentDirection = adjacentDirection;
            }
        } while (currentField != startPosition.field() || currentDirection != startPosition.direction());
        graphics.closePath();
    }

    private PiecePosition drawAndFindNextPiecePosition(Field currentField, Piece piece,
                                                       Direction currentDirection, Direction adjacentDirection, int gap) {

        drawLineIntoDirection(adjacentDirection, gap);

        if (isDiagonalFieldPartOfPiece(currentField, piece, currentDirection, adjacentDirection)) {
            int arcX = currentPieceDrawX + adjacentDirection.columnOffset * gap;
            int arcY = currentPieceDrawY + adjacentDirection.rowOffset * gap;
            int diagColumnOffset = currentDirection.columnOffset + adjacentDirection.columnOffset;
            int diagRowOffset = currentDirection.rowOffset + adjacentDirection.rowOffset;
            currentPieceDrawX += diagColumnOffset * gap;
            currentPieceDrawY += diagRowOffset * gap;

            graphics.arcTo(arcX, arcY, currentPieceDrawX, currentPieceDrawY, gap);

            drawLineIntoDirection(currentDirection, gap);

            currentField = board.getFieldOnBoard(currentField.getRow() + diagRowOffset,
                    currentField.getColumn() + diagColumnOffset);

            currentDirection = currentDirection.previous();
        } else {
            drawLineIntoDirection(adjacentDirection, gap);

            currentField = board.getFieldOnBoard(currentField.getRow() + adjacentDirection.rowOffset,
                    currentField.getColumn() + adjacentDirection.columnOffset);
        }
        return new PiecePosition(currentField, currentDirection);
    }

    private void drawLineIntoDirection(Direction direction, int gap) {
        currentPieceDrawX += direction.columnOffset * gap;
        currentPieceDrawY += direction.rowOffset * gap;
        graphics.lineTo(currentPieceDrawX, currentPieceDrawY); // connection line
    }


    private PiecePosition getStartPiecePosition(Piece piece, List<Field> occupiedFields) {
        Field startField = null;
        Direction startDirection = null;
        outer: for (Field field : occupiedFields) {
            for (Direction direction : Direction.values()) {
                if (!isAdjacentFieldPartOfPiece(field, piece, direction)) {
                    startField = field;
                    startDirection = direction;
                    break outer;
                }
            }
        }
        Objects.requireNonNull(startField);
        Objects.requireNonNull(startDirection);
        return new PiecePosition(startField, startDirection);
    }

    private record PiecePosition(Field field, Direction direction) {
    }

    private void drawPiece(Piece piece) {
        List<Field> occupiedFields = piece.getOccupiedFields();
        if (occupiedFields.isEmpty()) {
            throw new IllegalStateException("Piece is not on board and is called for render");
        }

        graphics.setFill(Color.SADDLEBROWN);
        occupiedFields.forEach(field ->
                graphics.fillRect(
                        field.getTopLeftCornerXCoordinate() + OUTER_PIECE_MARGIN,
                        field.getTopLeftCornerYCoordinate()  + OUTER_PIECE_MARGIN,
                        FIELD_SIZE - 2 * OUTER_PIECE_MARGIN, FIELD_SIZE - 2 * OUTER_PIECE_MARGIN));

        drawPieceBounds(piece, true);
        drawPieceBounds(piece, false);
    }

    private void drawPieceBounds(Piece piece, boolean outer) {
        graphics.setStroke(Color.BLACK);
        graphics.setLineWidth(2);
        for (Field field : piece.getOccupiedFields()) {
            for (Direction direction : Direction.values()) {
                drawSideConnection(field, piece, direction, outer);
            }
        }
    }

    private void drawSideConnection(Field field, Piece piece, Direction baseDirection, boolean outer) {
        if (!isAdjacentFieldPartOfPiece(field, piece, baseDirection)) {
            int gap = outer ? OUTER_PIECE_MARGIN : INNER_PIECE_MARGIN;

            int gapLineStartX = getStartX(field, baseDirection, gap);
            int gapLineStartY = getStartY(field, baseDirection, gap);
            int gapLineEndX = getEndX(field, baseDirection, gap);
            int gapLineEndY = getEndY(field, baseDirection, gap);

            graphics.strokeLine(gapLineStartX, gapLineStartY, gapLineEndX, gapLineEndY);

//            graphics.setLineWidth(1);
//            graphics.strokeLine(getStartX(field, baseDirection, 0), getStartY(field, baseDirection, 0),
//                    getEndX(field, baseDirection, 0), getEndY(field, baseDirection, 0));
//            graphics.setLineWidth(2);

            Direction firstAdjacentDirection = baseDirection == Direction.DOWN || baseDirection == Direction.UP
                    ? Direction.LEFT : Direction.UP;
            Direction secondAdjacentDirection = baseDirection == Direction.DOWN || baseDirection == Direction.UP
                    ? Direction.RIGHT : Direction.DOWN;
            drawOneSideConnection(field, piece, baseDirection, firstAdjacentDirection, gapLineStartX, gapLineStartY, outer, gap);
            drawOneSideConnection(field, piece, baseDirection, secondAdjacentDirection, gapLineEndX, gapLineEndY, outer, gap);
        }
    }

    private int getStartX(Field field, Direction direction, int gap) {
        return direction == Direction.UP || direction == Direction.LEFT ?
                field.getTopLeftCornerXCoordinate() + gap :
                field.getTopLeftCornerXCoordinate() + FIELD_SIZE - gap;
    }

    private int getEndX(Field field, Direction direction, int gap) {
        return direction == Direction.DOWN || direction == Direction.LEFT ?
                field.getTopLeftCornerXCoordinate() + gap :
                field.getTopLeftCornerXCoordinate() + FIELD_SIZE - gap;
    }

    private int getStartY(Field field, Direction direction, int gap) {
        return direction == Direction.UP || direction == Direction.RIGHT ?
                field.getTopLeftCornerYCoordinate() + gap :
                field.getTopLeftCornerYCoordinate() + FIELD_SIZE - gap;
    }

    private int getEndY(Field field, Direction direction, int gap) {
        return direction == Direction.LEFT || direction == Direction.UP ?
                field.getTopLeftCornerYCoordinate() + gap :
                field.getTopLeftCornerYCoordinate() + FIELD_SIZE - gap;
    }

    private void drawOneSideConnection(Field field, Piece piece, Direction baseDirection, Direction adjacentDirection, int startX, int startY, boolean outer, int gap) {
        if (isAdjacentFieldPartOfPiece(field, piece, adjacentDirection)) {
            int endX = startX + adjacentDirection.columnOffset * gap;
            int endY = startY + adjacentDirection.rowOffset * gap;
            graphics.strokeLine(startX, startY, endX, endY);

            if (outer) {
                int rectX = Math.min(startX, endX);
                int rectY = Math.min(startY, endY);
                int width = startX == endX ? FIELD_SIZE - 2 * OUTER_PIECE_MARGIN : OUTER_PIECE_MARGIN;
                int height = startY == endY ? FIELD_SIZE - 2 * OUTER_PIECE_MARGIN : OUTER_PIECE_MARGIN;
                if (baseDirection == Direction.DOWN) {
                    rectY -= height;
                } else if (baseDirection == Direction.RIGHT) {
                    rectX -= width;
                }
                graphics.fillRect(rectX, rectY, width, height);
            }

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
        UP(-1, 0),
        RIGHT(0, 1),
        DOWN(1, 0);

        public final int rowOffset;
        public final int columnOffset;

        Direction(int rowOffset, int columnOffset) {
            this.rowOffset = rowOffset;
            this.columnOffset = columnOffset;
        }

        public Direction next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public Direction previous() {
            int index = ordinal() == 0 ? values().length - 1 : ordinal() - 1;
            return values()[index];
        }
    }
}
