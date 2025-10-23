import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

public class PieceCanvas extends Canvas {
    private final int fieldSize;

    private final Piece piece;
    private final Board board;

    private int rotationAngle = 0;

    public PieceCanvas(Piece pieceToCopy, PieceOrientation orientation, int fieldSize, boolean center) {
        GraphicsContext graphics = getGraphicsContext2D();
        graphics.scale((double) fieldSize / MainFrame.FIELD_SIZE, (double) fieldSize / MainFrame.FIELD_SIZE);

        this.fieldSize = fieldSize;
        this.piece = pieceToCopy.copy();

        board = new Board();
        piece.setBoard(board);

        if (center) {
            int maxDimension = Math.max(orientation.getWidth(), orientation.getHeight());
            int rowOffset = (maxDimension - orientation.getHeight()) / 2;
            int columnOffset = (maxDimension - orientation.getWidth()) / 2;
            board.placePieceOnBoard(piece, orientation, rowOffset, columnOffset);

            setWidth(maxDimension * fieldSize);
            setHeight(maxDimension * fieldSize);
        } else {
            board.placePieceOnBoard(piece, orientation, 0,0);

            setWidth(orientation.getWidth() * fieldSize);
            setHeight(orientation.getHeight() * fieldSize);
        }


        refresh();
        setOnMouseClicked(this::rotate);
    }

    private void rotate(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            rotationAngle -= 90;
        } else if(event.getButton() == MouseButton.SECONDARY) {
            rotationAngle += 90;
        } else {
            return;
        }

        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(0.25), this);
        rotateTransition.setToAngle(rotationAngle);
        rotateTransition.play();

    }

    public void refresh() {
        Piece pieceOnCanvas = board.getPiecesOnBoard().get(0);

        pieceOnCanvas.drawPiece(getGraphicsContext2D(), List.of());
    }

    public Board getBoard() {
        return board;
    }

    public Piece getPiece() {
        return piece;
    }
}
