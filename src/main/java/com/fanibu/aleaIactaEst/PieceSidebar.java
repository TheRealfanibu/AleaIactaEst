package com.fanibu.aleaIactaEst;

import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;

import java.util.ArrayList;
import java.util.Arrays;
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
            PieceCanvas canvas = new PieceCanvas(piece, orientation, FIELD_SIZE, true);

            canvas.setOnDragDetected(this::onPieceDrag);
            pieceCanvasList.add(canvas);
        }
    }


    private void onPieceDrag(MouseEvent e) {
        int row = (int) (e.getY() / FIELD_SIZE);
        int column = (int) (e.getX() / FIELD_SIZE);

        PieceCanvas pieceCanvas = (PieceCanvas) e.getSource();
        Board board = pieceCanvas.getBoard();
        if (board.getFieldOnBoard(row, column).isOccupiedByPiece()) {
            Transform mouseCoordsRotate = new Rotate(pieceCanvas.getRotationAngle(),
                    pieceCanvas.getWidth() / 2, pieceCanvas.getHeight() / 2);
            Point2D absoluteMouseCoords = mouseCoordsRotate.transform(e.getX(), e.getY());

            PieceOrientation originalOrientation = pieceCanvas.getOriginalPieceOrientation();
            double centerField = (Math.max(originalOrientation.getWidth(), originalOrientation.getHeight()) - 1) / 2d;
            Transform fieldsRotate = new Rotate(pieceCanvas.getRotationAngle(), centerField, centerField);

            List<Point2D> transformedFields = Arrays.stream(originalOrientation.getPositions())
                    .map(pos -> fieldsRotate.transform(
                            pos.column() + pieceCanvas.getPieceColumnOffset(),
                            pos.row() + pieceCanvas.getPieceRowOffset()))
                    .toList();

            double minPieceColumn = transformedFields.stream().mapToDouble(Point2D::getX).min().orElseThrow();
            double minPieceRow = transformedFields.stream().mapToDouble(Point2D::getY).min().orElseThrow();

            double minPieceX = minPieceColumn * FIELD_SIZE;
            double minPieceY = minPieceRow * FIELD_SIZE;

            double scale = (double) MainFrame.FIELD_SIZE / FIELD_SIZE;

            int offsetX = (int) ((absoluteMouseCoords.getX() - minPieceX) * scale);
            int offsetY = (int) ((absoluteMouseCoords.getY() - minPieceY) * scale);

            mainFrame.addFloatingPieceView(pieceCanvas.getPiece(), offsetX, offsetY);

            pieceCanvas.startFullDrag();
        }

        e.consume();
    }


    public void update(List<Piece> availablePieces) {
        List<? extends Canvas> sidebarCanvases = availablePieces.stream()
                .sorted(Comparator.comparingInt(Piece::getId))
                .map(Piece::getId)
                .map(pieceCanvasList::get)
                .toList();

        int splittingThreshold = 3;
        int columns = (availablePieces.size() - 1) / splittingThreshold + 1;

        getChildren().clear();
        for (long i = 0; i < columns; i++) {
            VBox column = new VBox(SPACING);
            List<? extends Canvas> canvases = sidebarCanvases.stream()
                    .skip(i * splittingThreshold)
                    .limit(splittingThreshold)
                    .toList();
            column.getChildren().setAll(canvases);
            getChildren().add(column);
        }
    }
}
