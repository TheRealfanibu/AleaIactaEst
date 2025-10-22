import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PieceSidebar extends HBox {

    private static final int FIELD_SIZE = 50;

    private static final int SPACING = 10;

    private final List<Board> boards = new ArrayList<>();
    private final List<Canvas> canvases = new ArrayList<>();

    private final MainFrame mainFrame;

    public PieceSidebar(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

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
            canvas.setOnDragDetected(this::onPieceDrag);
            canvases.add(canvas);
        }
    }

    private void refreshCanvas(Board board, Canvas canvas) {
        Piece pieceOnCanvas = board.getPiecesOnBoard().get(0);
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        pieceOnCanvas.drawPiece(graphics, List.of(), 2, 1, FIELD_SIZE, 5, 4);
    }

    private void onPieceDrag(MouseEvent e) {
        int row = (int) (e.getY() / FIELD_SIZE);
        int column = (int) (e.getX() / FIELD_SIZE);

        Canvas source = (Canvas) e.getSource();
        Board board = boards.get(canvases.indexOf(source));

        if (board.getFieldOnBoard(row, column).isOccupied()) {
            double scaleWidth = (double) MainFrame.FIELD_SIZE / FIELD_SIZE;
            double scaleHeight = (double) MainFrame.FIELD_SIZE / FIELD_SIZE;

            WritableImage dragViewImage = new WritableImage((int) (source.getWidth() * scaleWidth),
                    (int) (source.getHeight() * scaleHeight));
            SnapshotParameters params = new SnapshotParameters();
            params.setTransform(Transform.scale(scaleWidth, scaleHeight));
            params.setFill(Color.TRANSPARENT);
            ImageView pieceView = new ImageView(source.snapshot(params, dragViewImage));
            pieceView.setOpacity(0.5);

            int offsetX = (int) (e.getX() * scaleWidth);
            int offsetY = (int) (e.getY() * scaleHeight);
            mainFrame.addFloatingPieceView(pieceView, offsetX, offsetY);

            Dragboard dragboard = pieceView.startDragAndDrop(TransferMode.MOVE);

            ClipboardContent content = new ClipboardContent();
            Piece piece = board.getPiecesOnBoard().get(0);
            content.putString(String.valueOf(piece.getId()));

            dragboard.setContent(content);
        }

        e.consume();
    }


    public void update(List<Piece> availablePieces) {
        List<Canvas> sidebarCanvases = availablePieces.stream()
                .sorted(Comparator.comparingInt(Piece::getId))
                .map(Piece::getId)
                .map(canvases::get)
                .toList();

        int splittingThreshold = 4;
        if (availablePieces.size() <= splittingThreshold) {
            VBox vBox = new VBox(SPACING);
            vBox.getChildren().setAll(sidebarCanvases);
            getChildren().setAll(vBox);
        } else {
            List<Canvas> leftCanvases = sidebarCanvases.stream().limit(splittingThreshold).toList();
            List<Canvas> rightCanvases = sidebarCanvases.stream().skip(splittingThreshold).toList();

            VBox leftVBox = new VBox(SPACING);
            leftVBox.getChildren().setAll(leftCanvases);
            VBox rightVBox = new VBox(SPACING);
            rightVBox.getChildren().setAll(rightCanvases);

            getChildren().setAll(leftVBox, rightVBox);
        }

    }
}
