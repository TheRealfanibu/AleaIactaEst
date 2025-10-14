import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class Dice extends Canvas {

    private static final int FIELD_SIZE = MainFrame.FIELD_SIZE;
    private static final int FIELD_SIZE_HALF = MainFrame.FIELD_SIZE / 2;

    private static final int DICE_CIRCLE_SIZE = 12;

    private static final int DICE_CIRCLE_SIZE_HALF = DICE_CIRCLE_SIZE / 2;

    private static final int BORDER_CIRCLE_GAP = 15;

    public static final Color BACKGROUND_COLOR = Color.BEIGE;

    private int number;

    private long lastEvent;


    public Dice(int number) {
        super(MainFrame.FIELD_SIZE, MainFrame.FIELD_SIZE);
        this.number = number;

        draw();
        setOnMouseClicked(this::onMouseClicked);
    }

    private void onMouseClicked(MouseEvent mouseEvent) {
        if(System.currentTimeMillis() - lastEvent < 100) {
            return;
        }

        lastEvent = System.currentTimeMillis();
        if(mouseEvent.getButton() == MouseButton.PRIMARY) {
            if (++number > 6) {
                number = 1;
            }
        } else if(mouseEvent.getButton() == MouseButton.SECONDARY) {
            if (--number < 1) {
                number = 6;
            }
        }
        draw();
    }

    private void draw() {
        GraphicsContext graphics = getGraphicsContext2D();
        graphics.setFill(BACKGROUND_COLOR);
        graphics.fillRect(0,0, FIELD_SIZE, FIELD_SIZE);
        graphics.setStroke(Color.BLACK);
        graphics.setLineWidth(2);
        graphics.strokeRect(0,0, FIELD_SIZE, FIELD_SIZE);

        drawNumber(graphics, number, 0,0);
    }

    public static void drawNumber(GraphicsContext graphics, int number, int xOffset, int yOffset) {
        graphics.setFill(Color.BLACK);
        if (number != 1 && number != 3) {
            graphics.fillOval(xOffset + BORDER_CIRCLE_GAP, yOffset + BORDER_CIRCLE_GAP,
                    DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
            graphics.fillOval(xOffset + FIELD_SIZE - BORDER_CIRCLE_GAP - DICE_CIRCLE_SIZE,
                    yOffset + FIELD_SIZE - BORDER_CIRCLE_GAP - DICE_CIRCLE_SIZE,
                    DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
        }
        if (number != 1 && number != 2) {
            graphics.fillOval(xOffset + FIELD_SIZE - BORDER_CIRCLE_GAP - DICE_CIRCLE_SIZE,
                    yOffset + BORDER_CIRCLE_GAP, DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
            graphics.fillOval(xOffset + BORDER_CIRCLE_GAP,
                    yOffset + FIELD_SIZE - BORDER_CIRCLE_GAP - DICE_CIRCLE_SIZE,
                    DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
        }
        if (number % 2 == 1) {
            graphics.fillOval(xOffset + FIELD_SIZE_HALF - DICE_CIRCLE_SIZE_HALF,
                    yOffset + FIELD_SIZE_HALF - DICE_CIRCLE_SIZE_HALF,
                    DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
        }
        if (number == 6) {
            graphics.fillOval(xOffset + BORDER_CIRCLE_GAP,
                    yOffset + FIELD_SIZE_HALF - DICE_CIRCLE_SIZE_HALF,
                    DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
            graphics.fillOval(xOffset + FIELD_SIZE - BORDER_CIRCLE_GAP - DICE_CIRCLE_SIZE,
                    yOffset + FIELD_SIZE_HALF - DICE_CIRCLE_SIZE_HALF,
                    DICE_CIRCLE_SIZE, DICE_CIRCLE_SIZE);
        }
    }

    public int getNumber() {
        return number;
    }
}
