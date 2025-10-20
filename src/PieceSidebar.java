import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PieceSidebar extends VBox {

    private static final int PIECE_FIELD_SIZE = 50;

    private final List<Board> boards = new ArrayList<>();
    private List<Canvas> canvases = new ArrayList<>();

    public PieceSidebar() {
        setAlignment(Pos.TOP_CENTER);
        setSpacing(10);

        init();
    }

    private void init() {
        List<Piece> allPieces = PieceCollection.createPieceInstances();

        for (Piece piece : allPieces) {
            PieceOrientation orientation = piece.getOrientations()[0];

            Board board = new Board();
            piece.setBoard(board);
            board.placePieceOnBoard(piece, orientation, 0, 0);
            boards.add(board);

            Canvas canvas = new Canvas(orientation.getWidth() * PIECE_FIELD_SIZE,
                    orientation.getHeight() * PIECE_FIELD_SIZE);

            refreshCanvas(board, canvas);
            canvases.add(canvas);
        }
    }

    private void refreshCanvas(Board board, Canvas canvas) {
        Piece pieceOnCanvas = board.getPiecesOnBoard().get(0);
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.setFill(Color.GRAY);
        graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        pieceOnCanvas.drawPiece(graphics, List.of(), 2, 1, PIECE_FIELD_SIZE, 5, 5);
    }


    public void update(List<Piece> availablePieces) {
        List<Canvas> sidebarCanvases = availablePieces.stream()
                .sorted(Comparator.comparingInt(Piece::getId))
                .map(Piece::getId)
                .map(id -> canvases.get(id))
                .toList();



    }
}
