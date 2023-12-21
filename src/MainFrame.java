import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;

public class MainFrame extends Application {
    private static final int FIELD_SIZE = 100;

    private static final int FIELD_SIZE_HALF = FIELD_SIZE / 2;

    private static final int DICE_CIRCLE_SIZE = 15;

    private static final int DICE_CIRCLE_SIZE_HALF = DICE_CIRCLE_SIZE / 2;

    private static final int BORDER_CIRCLE_GAP = 15;
    private static final int CANVAS_SIZE = 7 * FIELD_SIZE;

    private final Canvas canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
    private final GraphicsContext graphics = canvas.getGraphicsContext2D();

    private final Board board = new Board();

    @Override
    public void start(Stage stage) throws Exception {
        initCanvas();

        drawBoard();

        BorderPane layout = new BorderPane(canvas);
        layout.setPadding(new Insets(30));

        stage.setScene(new Scene(layout));
        stage.show();
    }

    private void initCanvas() {
        graphics.setFill(Color.BEIGE);
        graphics.fillRect(0,0, CANVAS_SIZE, CANVAS_SIZE);
        graphics.setFill(Color.BLACK);
        graphics.setLineWidth(3);
        graphics.strokeRect(0,0, CANVAS_SIZE, CANVAS_SIZE);
    }

    private void drawBoard() {
        board.getPiecesOnBoard().forEach(this::drawPiece);
        board.getAllFields().stream()
                .filter(field -> !field.isOccupied())
                .forEach(this::drawField);
    }

    private void drawField(Field field) {
        if (field.getNumber() == 0)
            return;

        int xCoordinateOffset = field.getPosition().column() * FIELD_SIZE;
        int yCoordinateOffset = field.getPosition().row() * FIELD_SIZE;

        graphics.setFill(Color.BLACK);
        if (field.getNumber() != 1 && field.getNumber() != 3) {
            graphics.fillOval(xCoordinateOffset + BORDER_CIRCLE_GAP, yCoordinateOffset + BORDER_CIRCLE_GAP,
                    DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
            graphics.fillOval(xCoordinateOffset + FIELD_SIZE - BORDER_CIRCLE_GAP - DICE_CIRCLE_SIZE,
                    yCoordinateOffset + FIELD_SIZE - BORDER_CIRCLE_GAP - DICE_CIRCLE_SIZE,
                    DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
        }
        if (field.getNumber() != 1 && field.getNumber() != 2) {
            graphics.fillOval(xCoordinateOffset + FIELD_SIZE - BORDER_CIRCLE_GAP - DICE_CIRCLE_SIZE,
                    yCoordinateOffset + BORDER_CIRCLE_GAP, DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
            graphics.fillOval(xCoordinateOffset + BORDER_CIRCLE_GAP,
                    yCoordinateOffset + FIELD_SIZE - BORDER_CIRCLE_GAP - DICE_CIRCLE_SIZE,
                    DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
        }
        if (field.getNumber() % 2 == 1) {
            graphics.fillOval(xCoordinateOffset + FIELD_SIZE_HALF - DICE_CIRCLE_SIZE_HALF,
                    yCoordinateOffset + FIELD_SIZE_HALF - DICE_CIRCLE_SIZE_HALF,
                    DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
        }
        if (field.getNumber() == 6) {
            graphics.fillOval(xCoordinateOffset + BORDER_CIRCLE_GAP,
                    yCoordinateOffset + FIELD_SIZE_HALF - DICE_CIRCLE_SIZE_HALF,
                    DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
            graphics.fillOval(xCoordinateOffset + FIELD_SIZE - BORDER_CIRCLE_GAP - DICE_CIRCLE_SIZE,
                    yCoordinateOffset + FIELD_SIZE_HALF - DICE_CIRCLE_SIZE_HALF,
                    DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
        }

    }

    private void drawPiece(Piece piece) {
        List<Field> occupiedFields = piece.getOccupiedFields();
        if (occupiedFields.isEmpty()) {
            throw new IllegalStateException("Piece is not on board and is called for render");
        }

        graphics.setFill(Color.SADDLEBROWN);
        occupiedFields.forEach(field -> {
            graphics.fillRect(field.getPosition().column() * FIELD_SIZE, field.getPosition().row() * FIELD_SIZE, FIELD_SIZE, FIELD_SIZE);
        });
    }

    public static void main(String[] args) {
        launch();
    }
}
