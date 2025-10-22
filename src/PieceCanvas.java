import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import java.util.List;

public class PieceCanvas extends Canvas {

    private final int fieldSize;

    private final Piece piece;

    private final Board board;

    public PieceCanvas(Piece piece, PieceOrientation orientation, int fieldSize) {
        super(orientation.getWidth() * fieldSize, orientation.getHeight() * fieldSize);

        this.fieldSize = fieldSize;
        this.piece = piece;

        board = new Board();
        piece.setBoard(board);
        board.placePieceOnBoard(piece, orientation, 0, 0);

        refresh();
    }

    public void refresh() {
        Piece pieceOnCanvas = board.getPiecesOnBoard().get(0);
        GraphicsContext graphics = getGraphicsContext2D();
        graphics.clearRect(0, 0, getWidth(), getHeight());
        pieceOnCanvas.drawPiece(graphics, List.of(), fieldSize);
    }

    public Board getBoard() {
        return board;
    }

    public Piece getPiece() {
        return piece;
    }
}
