import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.*;
import java.util.function.ToIntFunction;

public class MainFrame extends Application {

    public static final int FIELD_SIZE = 80;

    private static final int CANVAS_SIZE = Board.DIM * FIELD_SIZE;

    private Pane rootPane;

    private final Canvas canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
    private final GraphicsContext graphics = canvas.getGraphicsContext2D();

    private final Dice[] dices = new Dice[6];
    private Board board = new Board();

    private final PieceSidebar pieceSidebar = new PieceSidebar(this);

    private final Solver solver = new Solver(this);

    private Button solveButton;
    private Button firstSolutionButton, previusSolutionButton, nextSolutionButton, lastSolutionButton;
    private Label solutionLabel;

    private List<Piece> fixedPiecesOnBoard = new ArrayList<>();

    private boolean showSolution = true;
    private Board withoutSolutionBoard;

    private boolean anySolutionFound = false;
    private int currentSolutionNumber;
    private int numberSolutionsFound;

    private Canvas floatingPieceCanvas = null;
    private int floatingPieceOffsetX;
    private int floatingPieceOffsetY;

    private int dragPieceId;
    private PieceOrientation dragPieceOrientation;
    private Field dragFieldOrigin;
    private Field dragFieldToBePlaced;


    private void startPieceDragOnBoard(MouseEvent mouseEvent) {
        int row = (int) (mouseEvent.getY() / FIELD_SIZE);
        int column =  (int) (mouseEvent.getX() / FIELD_SIZE);
        System.out.println(row + " " + column);

        Field draggedField = board.getFieldOnBoard(row, column);
        if(draggedField.isOccupied()) {
            Piece draggedPiece = draggedField.getOccupationPiece();

            int minPieceRow = getMinAttributeField(draggedPiece.getOccupiedFields(), Field::getRow);
            int minPieceColumn = getMinAttributeField(draggedPiece.getOccupiedFields(), Field::getColumn);
            Field minField = board.getFieldOnBoard(minPieceRow, minPieceColumn);
            int offsetX = (int) (mouseEvent.getX() - minField.getTopLeftCornerXCoordinate());
            int offsetY = (int) (mouseEvent.getY() - minField.getTopLeftCornerYCoordinate());

            board.removePieceFromBoard(draggedPiece);
            dragFieldOrigin = minField;

            addFloatingPieceView(draggedPiece, offsetX, offsetY);
            drawBoard();
            canvas.startFullDrag();
        }
    }

    private int getMinAttributeField(List<Field> fields, ToIntFunction<Field> attributeFunc) {
        return fields.stream().mapToInt(attributeFunc).min().orElseThrow();
    }

    public void addFloatingPieceView(Piece piece, int offsetX, int offsetY) {
        floatingPieceCanvas = new PieceCanvas(piece, piece.getOrientationOnBoard(), FIELD_SIZE, false);
        floatingPieceCanvas.setOpacity(0.7);

        floatingPieceOffsetX = offsetX;
        floatingPieceOffsetY = offsetY;

        dragPieceId = piece.getId();
        dragPieceOrientation = piece.getOrientationOnBoard();

        floatingPieceCanvas.setVisible(false);
        rootPane.getChildren().add(floatingPieceCanvas);

        Platform.runLater(() -> { // to avoid the piece popping up in the top left corner
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            floatingPieceCanvas.setVisible(true);
        });
    }

    private boolean isPiecePlaceableOnCanvas(MouseDragEvent event) {
        Bounds canvasCoords = canvas.localToScene(canvas.getBoundsInLocal());
        double canvasMouseX = event.getX() - canvasCoords.getMinX();
        double canvasMouseY = event.getY() - canvasCoords.getMinY();
        int rowToBePlaced = (int) Math.round((canvasMouseY - floatingPieceOffsetY) / FIELD_SIZE);
        int columnToBePlaced = (int) Math.round((canvasMouseX - floatingPieceOffsetX) / FIELD_SIZE);

        if (board.fitsInPlace(dragPieceOrientation, rowToBePlaced, columnToBePlaced)) {
            dragFieldToBePlaced = board.getFieldOnBoard(rowToBePlaced, columnToBePlaced);
            int fieldSceneX = (int) (canvasCoords.getMinX() + dragFieldToBePlaced.getTopLeftCornerXCoordinate());
            int fieldSceneY = (int) (canvasCoords.getMinY() + dragFieldToBePlaced.getTopLeftCornerYCoordinate());
            floatingPieceCanvas.relocate(fieldSceneX, fieldSceneY);
            return true;
        }
        return false;
    }

    private void dragPiece(MouseDragEvent event) {
        if (!isPiecePlaceableOnCanvas(event)) {
            dragFieldToBePlaced = null;
            int viewX = (int) (event.getX() - floatingPieceOffsetX);
            int viewY = (int) (event.getY() - floatingPieceOffsetY);
            floatingPieceCanvas.relocate(viewX, viewY);
        }
        event.consume();
    }

    private void dragPieceRelease(MouseDragEvent event) {
        Piece pieceToPlace = board.getAllPieces().get(dragPieceId);
        if (dragFieldToBePlaced != null) { // can place piece at position
            board.placePieceOnBoard(pieceToPlace, dragPieceOrientation,
                    dragFieldToBePlaced.getRow(), dragFieldToBePlaced.getColumn());
            if (dragFieldToBePlaced == dragFieldOrigin) {
                updatePieceSidebar();
                drawBoard();
            } else {
                removeDragPiece(pieceToPlace);
            }
        }
        if (dragFieldOrigin != null) { // piece got removed from position
            removeDragPiece(pieceToPlace);
        }

        rootPane.getChildren().remove(floatingPieceCanvas);
        event.consume();
    }

    private void removeDragPiece(Piece pieceToPlace) {
        fixedPiecesOnBoard.remove(pieceToPlace);
        updatePiecesAndResetSolution();
    }

    private void initFloatingPieceViewHandler() {
        rootPane.setOnMouseDragOver(this::dragPiece);

        rootPane.setOnMouseDragReleased(this::dragPieceRelease);

    }

    private void updatePiecesAndResetSolution() {
        updatePieceSidebar();
        resetSolutionObjects();
        drawBoard();
    }

    private void updatePieceSidebar() {
        pieceSidebar.update(board.getAvailablePieces());
    }

    private void updateSolution() {
        if (anySolutionFound) {
            board = showSolution ? solver.getSolutions().get(currentSolutionNumber - 1) : withoutSolutionBoard;
            updatePieceSidebar();
            drawBoard();
            updateSolutionObjects();
        }
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
                            fixedPiecesOnBoard = board.getPiecesOnBoard();
                            updateSolution();
                        }
                    }

                    updateSolutionObjects();
                }
            }
        });

    }


    private synchronized void updateSolutionObjects() {
        firstSolutionButton.setDisable(currentSolutionNumber <= 1);
        previusSolutionButton.setDisable(currentSolutionNumber <= 1);
        nextSolutionButton.setDisable(currentSolutionNumber >= numberSolutionsFound);
        lastSolutionButton.setDisable(currentSolutionNumber >= numberSolutionsFound);

        String solutionNumbers = anySolutionFound ? currentSolutionNumber + "/" + numberSolutionsFound : "-/-";
        solutionLabel.setText("Solution: " + solutionNumbers);
    }

    private void resetBoard() {
        board.reset();
        fixedPiecesOnBoard.clear();
        updatePiecesAndResetSolution();
    }

    private void solveBoard() {
        withoutSolutionBoard = board.copy();

        solveButton.setDisable(true);
        solveButton.setText("Solving...");

        List<Integer> diceNumbers = Arrays.stream(dices).map(Dice::getNumber).toList();
        new Thread(() -> solver.solve(board, diceNumbers)).start();
        //new Thread(() -> solver.initConnectionGraph(board)).start();
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


    private void canvasOnClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            selectFixedDicePosition(mouseEvent.getX(), mouseEvent.getY());
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            removePiece(mouseEvent.getX(), mouseEvent.getY());
        }
    }

    private void selectFixedDicePosition(double mouseX, double mouseY) {
        int row = (int) (mouseY / FIELD_SIZE);
        int column = (int)  (mouseX / FIELD_SIZE);

        Field field = board.getFieldOnBoard(row, column);
        if (!field.isOccupied()) {
            boolean diceChange = false;
            Dice alteredDice = null;

            if (field.isDiceFixed()) {
                alteredDice = field.getFixedDice();
                alteredDice.setFixedField(null);
                field.setFixedDice(null);
                diceChange = true;
            } else {
                Optional<Dice> optUnfixedDice = Arrays.stream(dices)
                        .filter(dice -> dice.getNumber() == field.getNumber() && !dice.isFixed())
                        .findFirst();

                if(optUnfixedDice.isPresent()) {
                    alteredDice = optUnfixedDice.get();
                    alteredDice.setFixedField(field);
                    field.setFixedDice(alteredDice);
                    diceChange = true;
                }
            }

            if (diceChange) {
                alteredDice.draw();
                drawBoard();
                resetSolutionObjects();
            }
        }
    }


    public synchronized void drawBoard() {
        graphics.setFill(Dice.BACKGROUND_COLOR);
        graphics.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);

        board.getAllFields().stream()
                .filter(field -> !field.isOccupied())
                .forEach(this::drawField);

        graphics.setStroke(Color.BLACK);
        graphics.setLineWidth(10);
        graphics.strokeRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        graphics.setLineWidth(1);
        for (int i = 1; i < Board.DIM; i++) {
            graphics.strokeLine(i * FIELD_SIZE, 0, i * FIELD_SIZE, CANVAS_SIZE);
        }
        for (int i = 1; i < Board.DIM; i++) {
            graphics.strokeLine(0, i * FIELD_SIZE, CANVAS_SIZE, i * FIELD_SIZE);
        }

        board.getPiecesOnBoard().forEach(piece -> piece.drawPiece(graphics, fixedPiecesOnBoard));
    }

    private void removePiece(double mouseX, double mouseY) {
        int row = (int) (mouseY / FIELD_SIZE);
        int column = (int) (mouseX / FIELD_SIZE);

        Field clickedField = board.getFieldOnBoard(row, column);
        if (clickedField.isOccupied()) {
            Piece occupationPiece = clickedField.getOccupationPiece();
            fixedPiecesOnBoard.remove(occupationPiece);
            board.removePieceFromBoard(occupationPiece);
            updatePiecesAndResetSolution();
        }
    }

    private void drawField(Field field) {
        int xOffset = field.getTopLeftCornerXCoordinate();
        int yOffset = field.getTopLeftCornerYCoordinate();

        if(field.isDiceFixed()) {
            graphics.setFill(Dice.FIXED_COLORS[field.getNumber() - 1]);
            graphics.fillRect(xOffset, yOffset, FIELD_SIZE, FIELD_SIZE);
        }

        if(field.getNumber() != 0) {
            Dice.drawNumber(graphics, field.getNumber(), xOffset, yOffset);
        }
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
        CheckBox showSolutionBox = new CheckBox("Show Solution");
        showSolutionBox.setFont(buttonFont);
        showSolutionBox.setSelected(showSolution);
        showSolutionBox.setOnAction(event -> {
            showSolution = showSolutionBox.isSelected();
            updateSolution();
        });

        solveButton = new Button("Solve");
        solveButton.setFont(buttonFont);
        solveButton.onActionProperty().set(e -> solveBoard());

        Button resetButton = new Button("Reset");
        resetButton.setFont(buttonFont);
        resetButton.onActionProperty().set(e -> resetBoard());

        HBox upperBox = new HBox(10);
        upperBox.setAlignment(Pos.CENTER);
        upperBox.getChildren().addAll(showSolutionBox, solveButton, resetButton);

        HBox solutionBox = new HBox(10);
        solutionBox.setAlignment(Pos.CENTER);

        firstSolutionButton = createSolutionButton("first", () -> currentSolutionNumber = 1);

        previusSolutionButton = createSolutionButton("previous", () -> currentSolutionNumber--);

        nextSolutionButton = createSolutionButton("next", () -> currentSolutionNumber++);

        lastSolutionButton = createSolutionButton("last", () -> currentSolutionNumber = numberSolutionsFound);

        solutionLabel = new Label();
        solutionLabel.setFont(buttonFont);
        updateSolutionObjects();

        solutionBox.getChildren().addAll(solutionLabel, firstSolutionButton, previusSolutionButton, nextSolutionButton, lastSolutionButton);

        VBox buttonBox = new VBox(upperBox, solutionBox);
        buttonBox.setSpacing(10);

        return buttonBox;
    }

    private Button createSolutionButton(String imageName, Runnable action) {
        Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream("res/" + imageName + ".png")));
        Button solutionButton = new Button();
        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(30);
        imageView.setFitWidth(30 * image.getWidth() / image.getHeight());
        solutionButton.setGraphic(imageView);

        solutionButton.setDisable(true);
        solutionButton.onActionProperty().set(e -> {
            action.run();
            updateSolution();
        });
        return solutionButton;
    }

    private void initCanvas() {
        updatePiecesAndResetSolution();
        canvas.setOnMouseClicked(this::canvasOnClicked);
        canvas.setOnDragDetected(this::startPieceDragOnBoard);
    }

    @Override
    public void start(Stage stage) {

        BorderPane layout = new BorderPane();

        HBox centerBox = new HBox(30);
        centerBox.setAlignment(Pos.TOP_LEFT);

        VBox middleBox = new VBox(20);
        middleBox.setAlignment(Pos.TOP_CENTER);

        Pane buttonBox = createButtonBox();
        initCanvas();

        middleBox.getChildren().addAll(canvas, buttonBox);

        centerBox.getChildren().addAll(createDicePane(), middleBox, pieceSidebar);
        layout.setCenter(centerBox);

        layout.setPadding(new Insets(30));

        rootPane = new Pane(layout);
        initFloatingPieceViewHandler();

        Scene scene = new Scene(rootPane);

        stage.setScene(scene);
        stage.show();
    }
}
