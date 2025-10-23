import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PieceSidebar extends HBox {

    private static final int FIELD_SIZE = 60;

    private static final int SPACING = 15;

    private final List<PieceCanvas> pieceCanvasList = new ArrayList<>();

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
            PieceCanvas canvas = new PieceCanvas(piece, orientation, FIELD_SIZE);

            canvas.setOnDragDetected(this::onPieceDrag);
            pieceCanvasList.add(canvas);
        }
    }


    private void onPieceDrag(MouseEvent e) {
        int row = (int) (e.getY() / FIELD_SIZE);
        int column = (int) (e.getX() / FIELD_SIZE);

        PieceCanvas source = (PieceCanvas) e.getSource();
        Board board = source.getBoard();
        if (board.getFieldOnBoard(row, column).isOccupied()) {

            double scaleWidth = (double) MainFrame.FIELD_SIZE / FIELD_SIZE;
            double scaleHeight = (double) MainFrame.FIELD_SIZE / FIELD_SIZE;

            int offsetX = (int) (e.getX() * scaleWidth);
            int offsetY = (int) (e.getY() * scaleHeight);

            mainFrame.addFloatingPieceView(source.getPiece(), offsetX, offsetY);

            source.startFullDrag();
        }

        e.consume();
    }


    public void update(List<Piece> availablePieces) {
        List<? extends Canvas> sidebarCanvases = availablePieces.stream()
                .sorted(Comparator.comparingInt(Piece::getId))
                .map(Piece::getId)
                .map(pieceCanvasList::get)
                .toList();

        int splittingThreshold = 4;
        if (availablePieces.size() <= splittingThreshold) {
            VBox vBox = new VBox(SPACING);
            vBox.getChildren().setAll(sidebarCanvases);
            getChildren().setAll(vBox);
        } else {
            List<? extends Canvas> leftCanvases = sidebarCanvases.stream().limit(splittingThreshold).toList();
            List<? extends Canvas> rightCanvases = sidebarCanvases.stream().skip(splittingThreshold).toList();

            VBox leftVBox = new VBox(SPACING);
            leftVBox.getChildren().setAll(leftCanvases);
            VBox rightVBox = new VBox(SPACING);
            rightVBox.getChildren().setAll(rightCanvases);

            getChildren().setAll(leftVBox, rightVBox);
        }

    }
}
