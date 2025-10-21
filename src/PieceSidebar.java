import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PieceSidebar extends HBox {

    private static final int FIELD_SIZE = 50;

    private static final int SPACING = 10;

    private final List<Board> boards = new ArrayList<>();
    private final List<Canvas> canvases = new ArrayList<>();



    public PieceSidebar() {
        setAlignment(Pos.TOP_LEFT);
        setSpacing(SPACING);

        init();
    }

    private void init() {
        List<Piece> allPieces = PieceCollection.createPieceInstances();

        for (Piece piece : allPieces) {
            PieceOrientation orientation = piece.getOrientations()[0];

            Board board = new Board(FIELD_SIZE);
            piece.setBoard(board);
            board.placePieceOnBoard(piece, orientation, 0, 0);
            boards.add(board);

            Canvas canvas = new Canvas(orientation.getWidth() * FIELD_SIZE,
                    orientation.getHeight() * FIELD_SIZE);

            refreshCanvas(board, canvas);
            canvases.add(canvas);
        }
    }

    private void refreshCanvas(Board board, Canvas canvas) {
        Piece pieceOnCanvas = board.getPiecesOnBoard().get(0);
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        pieceOnCanvas.drawPiece(graphics, List.of(), 2, 1, FIELD_SIZE, 5, 5);
    }


    public void update(List<Piece> availablePieces) {
        List<Canvas> sidebarCanvases = availablePieces.stream()
                .sorted(Comparator.comparingInt(Piece::getId))
                .map(Piece::getId)
                .map(canvases::get)
                .toList();

        int threshold = 5;
        if (availablePieces.size() <= threshold) {
            VBox vBox = new VBox(SPACING);
            vBox.getChildren().setAll(sidebarCanvases);
            getChildren().setAll(vBox);
        } else {
            List<Canvas> leftCanvases = sidebarCanvases.stream().limit(threshold).toList();
            List<Canvas> rightCanvases = sidebarCanvases.stream().skip(threshold).toList();

            VBox leftVBox = new VBox(SPACING);
            leftVBox.getChildren().setAll(leftCanvases);
            VBox rightVBox = new VBox(SPACING);
            rightVBox.getChildren().setAll(rightCanvases);

            getChildren().setAll(leftVBox, rightVBox);
        }

    }
}
