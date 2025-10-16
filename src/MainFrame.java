import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;

public class MainFrame extends Application {
    public static final int FIELD_SIZE = 80;

    private static final int CANVAS_SIZE = 7 * FIELD_SIZE;

    private final Canvas canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
    private final GraphicsContext graphics = canvas.getGraphicsContext2D();

    private final Dice[] dices = new Dice[6];
    private final Board board = new Board();

    private final Solver solver = new Solver();


    @Override
    public void start(Stage stage) {
        initCanvas();

        BorderPane layout = new BorderPane();

        HBox centerBox = new HBox(30);
        centerBox.setAlignment(Pos.TOP_CENTER);

        VBox middleBox = new VBox(20);
        middleBox.setAlignment(Pos.CENTER);

        Font buttonFont = Font.font(20);

        Button solveButton = new Button("Solve");
        solveButton.setFont(buttonFont);
        solveButton.onActionProperty().set(e -> solveBoard());

        Button resetButton = new Button("Reset");
        resetButton.setFont(buttonFont);
        resetButton.onActionProperty().set(e -> resetBoard());

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(solveButton, resetButton);

        middleBox.getChildren().addAll(canvas, buttonBox);

        centerBox.getChildren().addAll(createDicePane(), middleBox);
        layout.setCenter(centerBox);


        layout.setPadding(new Insets(30));


        stage.setScene(new Scene(layout));
        stage.show();
    }

    private void resetBoard() {
        board.reset();
        drawCanvas();
    }

    private Pane createDicePane() {
        VBox vBox = new VBox(30);
        vBox.setAlignment(Pos.CENTER);
        for (int i = 0; i < 6; i++) {
            dices[i] = new Dice(i + 1);
            vBox.getChildren().add(dices[i]);
        }

        return vBox;
    }

    private void solveBoard() {
        List<Integer> diceNumbers = Arrays.stream(dices).map(Dice::getNumber).toList();
        solver.solve(board, diceNumbers);
        drawCanvas();
    }

    private void initCanvas() {
        drawCanvas();
        canvas.setOnMouseClicked(this::canvasOnClicked);
    }

    private void drawCanvas() {
        graphics.setFill(Dice.BACKGROUND_COLOR);
        graphics.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        graphics.setStroke(Color.BLACK);
        graphics.setLineWidth(10);
        graphics.strokeRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        graphics.setLineWidth(1);
        for (int i = 1; i < 7; i++) {
            graphics.strokeLine(i * FIELD_SIZE, 0, i* FIELD_SIZE, CANVAS_SIZE);
        }
        for (int i = 1; i < 7; i++) {
            graphics.strokeLine(0, i * FIELD_SIZE, CANVAS_SIZE, i * FIELD_SIZE);
        }

        drawBoard();
    }

    private void canvasOnClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            removePiece(mouseEvent.getX(), mouseEvent.getY());
            drawCanvas();
        }
    }

    private void removePiece(double mouseX, double mouseY) {
        int row = (int) (mouseY / FIELD_SIZE);
        int column = (int) (mouseX / FIELD_SIZE);

        Field clickedField = board.getFieldOnBoard(row, column);
        if (clickedField.isOccupied()) {
            board.removePieceFromBoard(clickedField.getOccupationPiece());
            drawBoard();
        }
    }

    private void drawBoard() {
        board.getPiecesOnBoard().forEach(piece -> piece.drawPiece(graphics,
                3, 2, FIELD_SIZE, 8, 6));
        board.getAllFields().stream()
                .filter(field -> !field.isOccupied())
                .forEach(this::drawField);
    }

    private void drawField(Field field) {
        if (field.getNumber() == 0)
            return;

        int xOffset = field.getTopLeftCornerXCoordinate();
        int yOffset = field.getTopLeftCornerYCoordinate();

        Dice.drawNumber(graphics, field.getNumber(), xOffset, yOffset);
    }

    public static void main(String[] args) {
        launch();
    }

}
