import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Objects;

public class Piece {

    private final int amountOccupations;
    private final PieceOrientation[] orientations;

    private List<Field> occupiedFields;

    private int currentDrawX;
    private int currentDrawY;

    private Board board;

    public Piece(int amountOccupations, PieceOrientation[] orientations) {
        this.amountOccupations = amountOccupations;
        this.orientations = orientations;
    }

    public void drawPiece(GraphicsContext graphics, int outerStroke, int innerStroke,
                          int fieldSize, int outerMargin, int innerMargin) {
        graphics.setFill(Color.SADDLEBROWN);

        graphics.setLineWidth(outerStroke);
        createPieceShape(graphics, fieldSize, outerMargin);
        graphics.fill();
        graphics.stroke();

        graphics.setLineWidth(innerStroke);
        createPieceShape(graphics, fieldSize, innerMargin);
        graphics.stroke();
    }

    private void createPieceShape(GraphicsContext graphics, int fieldSize, int margin) {
        PiecePosition startPosition = getStartPiecePosition();

        Field currentField = startPosition.field();
        Direction currentDirection = startPosition.direction();
        currentDrawX = getStartX(currentField, currentDirection, fieldSize, margin);
        currentDrawY = getStartY(currentField, currentDirection, fieldSize, margin);

        graphics.beginPath();
        graphics.moveTo(currentDrawX, currentDrawY);
        do {
            currentDrawX = getEndX(currentField, currentDirection, fieldSize, margin); // big line
            currentDrawY = getEndY(currentField, currentDirection, fieldSize, margin);
            graphics.lineTo(currentDrawX, currentDrawY);

            Direction adjacentDirection = currentDirection.next();

            if (isAdjacentFieldPartOfPiece(currentField, adjacentDirection)) {
                PiecePosition nextPosition = drawAndFindNextPiecePosition(graphics, currentField, currentDirection, adjacentDirection, margin);
                currentField = nextPosition.field();
                currentDirection = nextPosition.direction();
            } else {
                currentDirection = adjacentDirection;
            }
        } while (currentField != startPosition.field() || currentDirection != startPosition.direction());
        graphics.closePath();
    }

    private PiecePosition drawAndFindNextPiecePosition(GraphicsContext graphics, Field currentField,
                                                       Direction currentDirection, Direction adjacentDirection, int margin) {

        drawLineIntoDirection(graphics, adjacentDirection, margin);

        if (isDiagonalFieldPartOfPiece(currentField, currentDirection, adjacentDirection)) {
            int arcX = currentDrawX + adjacentDirection.columnOffset * margin;
            int arcY = currentDrawY + adjacentDirection.rowOffset * margin;
            int diagColumnOffset = currentDirection.columnOffset + adjacentDirection.columnOffset;
            int diagRowOffset = currentDirection.rowOffset + adjacentDirection.rowOffset;
            currentDrawX += diagColumnOffset * margin;
            currentDrawY += diagRowOffset * margin;

            graphics.arcTo(arcX, arcY, currentDrawX, currentDrawY, margin);

            drawLineIntoDirection(graphics, currentDirection, margin);

            currentField = board.getFieldOnBoard(currentField.getRow() + diagRowOffset,
                    currentField.getColumn() + diagColumnOffset);

            currentDirection = currentDirection.previous();
        } else {
            drawLineIntoDirection(graphics, adjacentDirection, margin);

            currentField = board.getFieldOnBoard(currentField.getRow() + adjacentDirection.rowOffset,
                    currentField.getColumn() + adjacentDirection.columnOffset);
        }
        return new PiecePosition(currentField, currentDirection);
    }

    private void drawLineIntoDirection(GraphicsContext graphics, Direction direction, int gap) {
        currentDrawX += direction.columnOffset * gap;
        currentDrawY += direction.rowOffset * gap;
        graphics.lineTo(currentDrawX, currentDrawY); // connection line
    }


    private PiecePosition getStartPiecePosition() {
        Field startField = null;
        Direction startDirection = null;
        outer:
        for (Field field : occupiedFields) {
            for (Direction direction : Direction.values()) {
                if (!isAdjacentFieldPartOfPiece(field, direction)) {
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


    private int getStartX(Field field, Direction direction, int fieldSize, int gap) {
        return direction == Direction.UP || direction == Direction.LEFT ?
                field.getTopLeftCornerXCoordinate() + gap :
                field.getTopLeftCornerXCoordinate() + fieldSize - gap;
    }

    private int getEndX(Field field, Direction direction, int fieldSize, int gap) {
        return direction == Direction.DOWN || direction == Direction.LEFT ?
                field.getTopLeftCornerXCoordinate() + gap :
                field.getTopLeftCornerXCoordinate() + fieldSize - gap;
    }

    private int getStartY(Field field, Direction direction, int fieldSize, int gap) {
        return direction == Direction.UP || direction == Direction.RIGHT ?
                field.getTopLeftCornerYCoordinate() + gap :
                field.getTopLeftCornerYCoordinate() + fieldSize - gap;
    }

    private int getEndY(Field field, Direction direction, int fieldSize, int gap) {
        return direction == Direction.LEFT || direction == Direction.UP ?
                field.getTopLeftCornerYCoordinate() + gap :
                field.getTopLeftCornerYCoordinate() + fieldSize - gap;
    }

    private boolean isDiagonalFieldPartOfPiece(Field field, Direction baseDirection, Direction adjacentDirection) {
        return isFieldPartOfPiece(field, baseDirection.rowOffset + adjacentDirection.rowOffset, baseDirection.columnOffset + adjacentDirection.columnOffset);
    }

    private boolean isAdjacentFieldPartOfPiece(Field field, Direction direction) {
        return isFieldPartOfPiece(field, direction.rowOffset, direction.columnOffset);
    }

    private boolean isFieldPartOfPiece(Field field, int rowOffset, int columnOffset) {
        int row = field.getRow() + rowOffset;
        int column = field.getColumn() + columnOffset;

        return !isOutOfBounds(row, column) && board.getFieldOnBoard(row, column).getOccupationPiece() == this;
    }

    private boolean isOutOfBounds(int row, int column) {
        return row < 0 || row > 6 || column < 0 || column > 6;
    }


    public void setBoard(Board board) {
        this.board = board;
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
