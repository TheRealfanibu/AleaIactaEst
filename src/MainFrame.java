import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
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
    private Board board = new Board();

    private final Solver solver = new Solver(this);

    private Button solveButton;
    private Button previusSolutionButton, nextSolutionButton;
    private Label solutionLabel;

    private List<Piece> fixedPiecesOnBoard;

    private boolean anySolutionFound = false;
    private int currentSolutionNumber;
    private int numberSolutionsFound;


    private void updateSolution() {
        board = solver.getSolutions().get(currentSolutionNumber - 1);
        drawBoard();
        updateSolutionObjects();
    }

    public void indicateSolvingFinished() {
        if (solver.isSolving()) {
            updateSolutionStats();
            String solveButtonText = numberSolutionsFound == 0 ? "No solution found." : "Solved.";
            Platform.runLater(() -> solveButton.setText(solveButtonText));
        }

    }

    public void updateSolutionStats() {
        this.numberSolutionsFound = solver.getSolutions().size();

        Platform.runLater(() -> {
            synchronized (this) {
                if (solver.isSolving()) {
                    if (!anySolutionFound) {
                        anySolutionFound = true;
                        if (numberSolutionsFound > 0) {
                            currentSolutionNumber = 1;
                            board = solver.getSolutions().get(0); // todo: can raise exception with next run
                            drawBoard();
                        }
                    }

                    Platform.runLater(this::updateSolutionObjects);
                }
            }
        });

    }


    private synchronized void updateSolutionObjects() {
        previusSolutionButton.setDisable(currentSolutionNumber <= 1);
        nextSolutionButton.setDisable(currentSolutionNumber >= numberSolutionsFound);

        String solutionNumbers = anySolutionFound ? currentSolutionNumber + "/" + numberSolutionsFound : "-/-";
        solutionLabel.setText("Solution: " + solutionNumbers);
    }

    private void resetBoard() {
        resetSolutionObjects();
        board.reset();
        drawBoard();
    }

    private void solveBoard() {
        solveButton.setDisable(true);
        solveButton.setText("Solving...");

        fixedPiecesOnBoard = board.getPiecesOnBoard();

        List<Integer> diceNumbers = Arrays.stream(dices).map(Dice::getNumber).toList();
        new Thread(() -> solver.solve(board.copy(), diceNumbers)).start();
    }

    public void resetSolutionObjects() {
        solver.stop();
        anySolutionFound = false;
        numberSolutionsFound = 0;
        currentSolutionNumber = 0;

        solveButton.setDisable(false);
        solveButton.setText("Solve");

        updateSolutionObjects();
    }



    private void initCanvas() {
        drawBoard();
        canvas.setOnMouseClicked(this::canvasOnClicked);
    }

    private synchronized void drawBoard() {
        graphics.setFill(Dice.BACKGROUND_COLOR);
        graphics.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        graphics.setStroke(Color.BLACK);
        graphics.setLineWidth(10);
        graphics.strokeRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        graphics.setLineWidth(1);
        for (int i = 1; i < 7; i++) {
            graphics.strokeLine(i * FIELD_SIZE, 0, i * FIELD_SIZE, CANVAS_SIZE);
        }
        for (int i = 1; i < 7; i++) {
            graphics.strokeLine(0, i * FIELD_SIZE, CANVAS_SIZE, i * FIELD_SIZE);
        }

        drawPiecesAndNumbers();
    }

    private void canvasOnClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            removePiece(mouseEvent.getX(), mouseEvent.getY());
            drawBoard();
        }
    }

    private void removePiece(double mouseX, double mouseY) {
        int row = (int) (mouseY / FIELD_SIZE);
        int column = (int) (mouseX / FIELD_SIZE);

        Field clickedField = board.getFieldOnBoard(row, column);
        if (clickedField.isOccupied()) {
            board.removePieceFromBoard(clickedField.getOccupationPiece());
            resetSolutionObjects();
        }
    }

    private void drawPiecesAndNumbers() {
        board.getPiecesOnBoard().forEach(piece -> piece.drawPiece(graphics, fixedPiecesOnBoard,
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

    @Override
    public void start(Stage stage) {
        initCanvas();

        BorderPane layout = new BorderPane();

        HBox centerBox = new HBox(30);
        centerBox.setAlignment(Pos.TOP_LEFT);

        VBox middleBox = new VBox(20);
        middleBox.setAlignment(Pos.CENTER);

        Pane buttonBox = createButtonBox();

        middleBox.getChildren().addAll(canvas, buttonBox);

        centerBox.getChildren().addAll(createDicePane(), middleBox);
        layout.setCenter(centerBox);

        layout.setPadding(new Insets(30));

        stage.setScene(new Scene(layout));
        stage.show();
    }

    private Pane createDicePane() {
        VBox vBox = new VBox(30);
        vBox.setAlignment(Pos.TOP_CENTER);
        for (int i = 0; i < 6; i++) {
            dices[i] = new Dice(this, i + 1);
            vBox.getChildren().add(dices[i]);
        }

        return vBox;
    }

    private Pane createButtonBox() {

        Font buttonFont = Font.font(20);

        solveButton = new Button("Solve");
        solveButton.setFont(buttonFont);
        solveButton.onActionProperty().set(e -> solveBoard());

        Button resetButton = new Button("Reset");
        resetButton.setFont(buttonFont);
        resetButton.onActionProperty().set(e -> resetBoard());

        HBox upperBox = new HBox(10);
        upperBox.setAlignment(Pos.CENTER);
        upperBox.getChildren().addAll(solveButton, resetButton);

        HBox solutionBox = new HBox(10);
        solutionBox.setAlignment(Pos.CENTER);

        previusSolutionButton = createSolutionButton(buttonFont, "Previous");
        previusSolutionButton.onActionProperty().set(e -> {
            currentSolutionNumber--;
            updateSolution();
        });

        nextSolutionButton = createSolutionButton(buttonFont, "Next");
        nextSolutionButton.onActionProperty().set(e -> {
            currentSolutionNumber++;
            updateSolution();
        });

        solutionLabel = new Label();
        solutionLabel.setFont(buttonFont);
        updateSolutionObjects();

        solutionBox.getChildren().addAll(solutionLabel, previusSolutionButton, nextSolutionButton);

        VBox buttonBox = new VBox(upperBox, solutionBox);
        buttonBox.setSpacing(10);

        return buttonBox;
    }

    private Button createSolutionButton(Font font, String text) {
        Button solutionButton = new Button(text);
        solutionButton.setFont(font);
        solutionButton.setDisable(true);
        return solutionButton;
    }


    public static void main(String[] args) {
        launch();
    }
}
