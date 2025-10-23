import javafx.animation.RotateTransition;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.util.List;

public class PieceCanvas extends Canvas {
    private final Piece piece;
    private final Board board;

    private int rotationAngle = 0;

    private int orientationIndex;

    private final PieceOrientation originalPieceOrientation;
    private final int pieceRowOffset, pieceColumnOffset;

    public PieceCanvas(Piece pieceToCopy, PieceOrientation orientation, int fieldSize, boolean center) {
        this.piece = pieceToCopy.copy();
        this.originalPieceOrientation = orientation;

        board = new Board();
        piece.setBoard(board);

        if (center) {
            int maxDimension = Math.max(orientation.getWidth(), orientation.getHeight());
            pieceRowOffset = (maxDimension - orientation.getHeight()) / 2;
            pieceColumnOffset = (maxDimension - orientation.getWidth()) / 2;

            setWidth(maxDimension * fieldSize);
            setHeight(maxDimension * fieldSize);
        } else {
            pieceRowOffset = 0;
            pieceColumnOffset = 0;

            setWidth(orientation.getWidth() * fieldSize);
            setHeight(orientation.getHeight() * fieldSize);
        }
        board.placePieceOnBoard(piece, orientation, pieceRowOffset, pieceColumnOffset);
        orientationIndex = piece.getOrientationIndex();

        draw(fieldSize);
        setOnMouseClicked(this::rotate);
    }

    private void rotate(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            rotationAngle -= 90;
            orientationIndex--;
        } else if(mouseEvent.getButton() == MouseButton.SECONDARY) {
            rotationAngle += 90;
            orientationIndex++;
        } else {
            return;
        }

        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(0.25), this);
        rotateTransition.setToAngle(rotationAngle);
        rotateTransition.play();

        orientationIndex = Math.floorMod(orientationIndex, piece.getOrientations().length);
        piece.setOrientationOnBoard(piece.getOrientations()[orientationIndex]);
    }

    private void draw(int fieldSize) {
        GraphicsContext graphics = getGraphicsContext2D();
        graphics.scale((double) fieldSize / MainFrame.FIELD_SIZE, (double) fieldSize / MainFrame.FIELD_SIZE);
        piece.drawPiece(getGraphicsContext2D(), List.of());
    }

    public int getRotationAngle() {
        return rotationAngle;
    }

    public Board getBoard() {
        return board;
    }

    public Piece getPiece() {
        return piece;
    }

    public int getPieceRowOffset() {
        return pieceRowOffset;
    }

    public int getPieceColumnOffset() {
        return pieceColumnOffset;
    }

    public PieceOrientation getOriginalPieceOrientation() {
        return originalPieceOrientation;
    }
}
