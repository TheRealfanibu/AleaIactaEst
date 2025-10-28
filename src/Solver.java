import org.graphstream.algorithm.ConnectedComponents;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.DefaultGraph;

import java.util.*;

public class Solver {

    private final List<Board> solutions = new ArrayList<>();

    private final MainFrame mainFrame;

    private boolean solving = false;

    private final boolean searchOnlyOneSolution;

    private Graph connectionGraph;

    private ConnectedComponents connectedFields;

    public Solver() {
        searchOnlyOneSolution = true;
        mainFrame = null;
    }

    public Solver(MainFrame mainFrame) {
        searchOnlyOneSolution = false;
        this.mainFrame = mainFrame;
    }

    public void initConnectionGraph(Board board) {
        List<Field> unoccupiedFields = board.getUnoccupiedFields();

        connectionGraph = new DefaultGraph("ConnectionGraph", true, false,
                unoccupiedFields.size(), unoccupiedFields.size() * 4);

        unoccupiedFields.forEach(field -> connectionGraph.addNode(String.valueOf(field.getId())));

        for (Field field : unoccupiedFields) {
            addEdgeToGraphIfNeighborConnected(board, field, 1, 0);
            addEdgeToGraphIfNeighborConnected(board, field, 0, 1);
        }
        connectedFields = new ConnectedComponents(connectionGraph);
    }

    private void addEdgeToGraphIfNeighborConnected(Board board, Field field, int rowOffset, int columnOffset) {

        int row = field.getRow() + rowOffset;
        int column = field.getColumn() + columnOffset;
        if (!Board.isOutOfBounds(row, column) &&
                !board.getFieldOnBoard(row, column).isOccupied()) {
            Field connectedField = board.getFieldOnBoard(row, column);
            connectionGraph.addEdge(field.getId() + "-" + connectedField.getId(), field.getId(), connectedField.getId());
        }
    }


    public void solve(Board board, List<Integer> diceNumbers) {
        board = board.copy();
        solving = true;
        solutions.clear();


        int[] diceOccurrences = Board.countDiceNumbers(diceNumbers.stream());
        diceOccurrences[0] = 1; // one field must be empty

        int[] visibleDiceNumbers = board.countVisibleDiceNumbers();

        List<Piece> availablePieces = board.getAvailablePieces();
        availablePieces.sort(Comparator.comparingInt(Piece::getAmountOccupations));

        solveWithCurrentBoard(board, availablePieces, diceOccurrences, visibleDiceNumbers);
        if (mainFrame != null)
            mainFrame.indicateSolvingFinished();
    }


    public void solveWithCurrentBoard(Board board, List<Piece> availablePieces, int[] diceOccurrences,
                                      int[] visibleDiceNumbers) {

        if (availablePieces.isEmpty()) {
            if (Arrays.equals(diceOccurrences, visibleDiceNumbers)) { // valid solution
                solutions.add(board.copy());

                if (searchOnlyOneSolution) {
                    stop();
                }
                if (mainFrame != null)
                    mainFrame.updateSolutionStats();
            }
            return;
        }

        if (!areEnoughSolutionDiceNumbersAvailable(diceOccurrences, visibleDiceNumbers)) {
            return;
        }

        Piece nextPiece = availablePieces.remove(0);
        for (PieceOrientation orientation : nextPiece.getOrientations()) {
            for (int rowOffset = 0; rowOffset <= 7 - orientation.getHeight(); rowOffset++) {
                for (int columnOffset = 0; columnOffset <= 7 - orientation.getWidth(); columnOffset++) {
                    if (!solving) {
                        return;
                    }

                    if (board.fitsInPlace(orientation, rowOffset, columnOffset)) {
                        board.placePieceOnBoard(nextPiece, orientation, rowOffset, columnOffset);
                        int[] diceNumbersOccupied = Board.countDiceNumbersOfFields(nextPiece.getOccupiedFields().stream());
                        updateVisibleDiceNumbers(visibleDiceNumbers, diceNumbersOccupied, false);

                        solveWithCurrentBoard(board, availablePieces, diceOccurrences, visibleDiceNumbers);

                        updateVisibleDiceNumbers(visibleDiceNumbers, diceNumbersOccupied, true);
                        board.removeLastPieceFromBoard();
                    }
                }
            }
        }
        availablePieces.add(0, nextPiece);
    }

    private void updateVisibleDiceNumbers(int[] visibleDiceNumbers, int[] diceNumbers, boolean add) {
        for (int diceNumber = 0; diceNumber < 7; diceNumber++) {
            if (add) {
                visibleDiceNumbers[diceNumber] += diceNumbers[diceNumber];
            } else {
                visibleDiceNumbers[diceNumber] -= diceNumbers[diceNumber];
            }
        }
    }

    private boolean areEnoughSolutionDiceNumbersAvailable(int[] diceOccurrences, int[] visibleDiceNumbers) {
        for (int diceNumber = 0; diceNumber < 7; diceNumber++) {
            if (visibleDiceNumbers[diceNumber] < diceOccurrences[diceNumber]) { // too many number fields occupied
                return false;
            }
        }
        return true;
    }

    public void stop() {
        solving = false;
    }

    public boolean isSolving() {
        return solving;
    }

    public List<Board> getSolutions() {
        return solutions;
    }
}
