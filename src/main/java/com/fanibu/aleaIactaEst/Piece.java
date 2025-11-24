package com.fanibu.aleaIactaEst;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Piece {

    private static final int OUTER_STROKE = 3;
    private static final int INNER_STROKE = 2;

    private static final int OUTER_MARGIN = 8;
    private static final int INNER_MARGIN = 6;

    private final int numOccupations;
    private final int minDimension, maxDimension;
    private final PieceOrientation[] orientations;
    private final int id;

    private int rowOffsetOnBoard;
    private int columnOffsetOnBoard;
    private PieceOrientation orientationOnBoard;
    private List<Field> occupiedFields;

    private int currentDrawX;
    private int currentDrawY;

    private Board board;

    public Piece(int numOccupations, PieceOrientation[] orientations, int id) {
        this.numOccupations = numOccupations;
        this.orientations = orientations;

        PieceOrientation orientation = orientations[0];
        this.minDimension = Math.min(orientation.getHeight(), orientation.getWidth());
        this.maxDimension = Math.max(orientation.getHeight(), orientation.getWidth());
        this.id = id;
    }

    public Piece copy() {
        return new Piece(numOccupations, orientations, id);
    }

    public void drawPiece(GraphicsContext graphics, List<Piece> fixedPiecesOnBoard) {
        Color fillColor = fixedPiecesOnBoard.contains(this) ? Color.SANDYBROWN : Color.SADDLEBROWN;
        graphics.setFill(fillColor);

        graphics.setLineWidth(OUTER_STROKE);
        createPieceShape(graphics, OUTER_MARGIN);
        graphics.fill();
        graphics.stroke();

        graphics.setLineWidth(INNER_STROKE);
        for (int i = 0; i < 2; i++) {
            createPieceShape(graphics, OUTER_MARGIN + (i + 1) * INNER_MARGIN);
            graphics.stroke();
        }
    }

    private void createPieceShape(GraphicsContext graphics, int margin) {
        PiecePosition startPosition = getStartPiecePosition();

        Field currentField = startPosition.field();
        Direction currentDirection = startPosition.direction();
        currentDrawX = getStartX(currentField, currentDirection, margin);
        currentDrawY = getStartY(currentField, currentDirection, margin);

        graphics.beginPath();
        graphics.moveTo(currentDrawX, currentDrawY);
        do {
            currentDrawX = getEndX(currentField, currentDirection, margin); // big line
            currentDrawY = getEndY(currentField, currentDirection, margin);
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


    private int getStartX(Field field, Direction direction, int gap) {
        return direction == Direction.UP || direction == Direction.LEFT ?
                field.getTopLeftCornerXCoordinate() + gap :
                field.getTopLeftCornerXCoordinate() + MainFrame.FIELD_SIZE - gap;
    }

    private int getEndX(Field field, Direction direction, int gap) {
        return direction == Direction.DOWN || direction == Direction.LEFT ?
                field.getTopLeftCornerXCoordinate() + gap :
                field.getTopLeftCornerXCoordinate() + MainFrame.FIELD_SIZE - gap;
    }

    private int getStartY(Field field, Direction direction, int gap) {
        return direction == Direction.UP || direction == Direction.RIGHT ?
                field.getTopLeftCornerYCoordinate() + gap :
                field.getTopLeftCornerYCoordinate() + MainFrame.FIELD_SIZE - gap;
    }

    private int getEndY(Field field, Direction direction, int gap) {
        return direction == Direction.LEFT || direction == Direction.UP ?
                field.getTopLeftCornerYCoordinate() + gap :
                field.getTopLeftCornerYCoordinate() + MainFrame.FIELD_SIZE - gap;
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

        return !Board.isOutOfBounds(row, column) && board.getFieldOnBoard(row, column).getOccupationPiece() == this;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public int getNumOccupations() {
        return numOccupations;
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

    public int getId() {
        return id;
    }

    public PieceOrientation getOrientationOnBoard() {
        return orientationOnBoard;
    }

    public void setOrientationOnBoard(PieceOrientation orientationOnBoard) {
        this.orientationOnBoard = orientationOnBoard;
    }

    public int getRowOffsetOnBoard() {
        return rowOffsetOnBoard;
    }

    public void setRowOffsetOnBoard(int rowOffsetOnBoard) {
        this.rowOffsetOnBoard = rowOffsetOnBoard;
    }

    public int getColumnOffsetOnBoard() {
        return columnOffsetOnBoard;
    }

    public void setColumnOffsetOnBoard(int columnOffsetOnBoard) {
        this.columnOffsetOnBoard = columnOffsetOnBoard;
    }

    public int getOrientationIndex() {
        return Arrays.asList(orientations).indexOf(orientationOnBoard);
    }

    public int getMinDimension() {
        return minDimension;
    }

    public int getMaxDimension() {
        return maxDimension;
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Piece) {
            return ((Piece) obj).id == id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
