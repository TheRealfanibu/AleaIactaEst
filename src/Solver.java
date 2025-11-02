import org.graphstream.algorithm.ConnectedComponents;
import org.graphstream.algorithm.ConnectedComponents.ConnectedComponent;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.AbstractGraph;
import org.graphstream.graph.implementations.DefaultGraph;

import java.util.*;

public class Solver {


    private final List<Board> solutions = new ArrayList<>();

    private final MainFrame mainFrame;

    private boolean solving = false;

    private final boolean searchOnlyOneSolution;

    private static final int CONNECTIVITY_CHECK_AT_PIECE = 3;

    private final Graph connectionGraph;

    private ConnectedComponents connectedFields;

    private final Map<Set<Node>, FieldComponentProperty> fieldComponentProperties = new HashMap<>();

    private int prunedTreesCounter, notPrunedTreesCounter;


    public Solver() {
        this(null, true);
    }

    public Solver(MainFrame mainFrame) {
        this(mainFrame, false);
    }

    public Solver(MainFrame mainFrame, boolean searchOnlyOneSolution) {
        this.searchOnlyOneSolution = searchOnlyOneSolution;
        this.mainFrame = mainFrame;

        connectionGraph = new DefaultGraph("ConnectionGraph", true, false);
        connectionGraph.setNodeFactory((id, graph) -> new BoardNode((AbstractGraph) graph, id));
    }

    public void solve(Board board, List<Integer> diceNumbers, List<Integer> fixedDiceNumbers) {
        long startTime = System.currentTimeMillis();

        prunedTreesCounter = 0;
        notPrunedTreesCounter = 0;

        board = board.copy();
        solving = true;
        solutions.clear();

        int[] diceOccurrences = Board.countDiceNumbers(diceNumbers.stream());
        diceOccurrences[0] = 1;

        int[] fixedDiceOccurrences = Board.countDiceNumbers(fixedDiceNumbers.stream());
        int[] visibleDiceNumbers = board.countVisibleDiceNumbers();

        List<Piece> availablePieces = board.getAvailablePieces();

        Comparator<Piece> pieceSorter = Comparator.comparingInt(Piece::getNumOccupations)
                .thenComparingInt(Piece::getMaxDimension)
                .thenComparingInt(Piece::getMinDimension).reversed();

        availablePieces.sort(pieceSorter);

        solveWithCurrentBoard(board, availablePieces, diceOccurrences, visibleDiceNumbers, fixedDiceOccurrences);
        if (mainFrame != null)
            mainFrame.indicateSolvingFinished();

        int total = prunedTreesCounter + notPrunedTreesCounter;
        if (total > 0) {
            System.out.println("prune ratio: " + (double) prunedTreesCounter / total);
            System.out.println("total trees pruned: " + prunedTreesCounter);
        }
        System.out.println("field components computed: " + fieldComponentProperties.size());
        System.out.println("Solving took: " + (System.currentTimeMillis() - startTime) / 1000d + "s");
    }

    public void initConnectivityGraph(Board board) {
        connectionGraph.clear();

        List<Field> unoccupiedFields = board.getUnoccupiedFields().toList();

        addFieldsToConnectivityGraph(unoccupiedFields);

        for (Field field : unoccupiedFields) {
            addEdgeToGraphIfNeighborConnected(board, field, 1, 0);
            addEdgeToGraphIfNeighborConnected(board, field, 0, 1);
        }

    }

    private void addFieldsToConnectivityGraph(List<Field> fields) {
        fields.forEach(field -> {
            BoardNode node = (BoardNode) connectionGraph.addNode(field.getId());
            node.setField(field);
        });
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


    public void solveWithCurrentBoard(Board board, List<Piece> availablePieces, int[] diceNumbers,
                                      int[] visibleDiceNumbers, int[] fixedDiceOccurrences) {

        if (availablePieces.isEmpty()) {
            if (Arrays.equals(diceNumbers, visibleDiceNumbers)) { // valid solution
                solutions.add(board.copy());

                if (searchOnlyOneSolution) {
                    stop();
                }
                if (mainFrame != null)
                    mainFrame.updateSolutionStats();
            }
            return;
        }

        if (!areEnoughSolutionDiceNumbersAvailable(diceNumbers, visibleDiceNumbers)) {
            return;
        }

        int numPiecesOnBoard = board.getPiecesOnBoard().size();
        if (numPiecesOnBoard == CONNECTIVITY_CHECK_AT_PIECE) {
            initConnectivityGraph(board);
            boolean fieldComponentsCompatible =
                    areFieldComponentsCompatible(board, availablePieces, diceNumbers, fixedDiceOccurrences);
            connectedFields.terminate();
            if (fieldComponentsCompatible) {
                notPrunedTreesCounter++;
            } else {
                prunedTreesCounter++;
                return;
            }
        }

        Piece nextPiece = availablePieces.remove(0);
        for (PieceOrientation orientation : nextPiece.getOrientations()) {
            for (int rowOffset = 0; rowOffset <= Board.DIM - orientation.getHeight(); rowOffset++) {
                for (int columnOffset = 0; columnOffset <= Board.DIM - orientation.getWidth(); columnOffset++) {
                    if (!solving) {
                        return;
                    }

                    if (board.fitsInPlace(orientation, rowOffset, columnOffset)) {
                        board.placePieceOnBoard(nextPiece, orientation, rowOffset, columnOffset);

                        int[] diceNumbersOccupied = Board.countDiceNumbersOfFields(nextPiece.getOccupiedFields().stream());
                        updateVisibleDiceNumbers(visibleDiceNumbers, diceNumbersOccupied, false);

                        solveWithCurrentBoard(board, availablePieces, diceNumbers, visibleDiceNumbers, fixedDiceOccurrences);

                        updateVisibleDiceNumbers(visibleDiceNumbers, diceNumbersOccupied, true);
                        board.removeLastPieceFromBoard();
                    }
                }
            }
        }
        availablePieces.add(0, nextPiece);
    }

    private boolean areFieldComponentsCompatible(Board board, List<Piece> availablePieces,
                                                 int[] diceNumbers, int[] fixedDiceOccurrences) {
        connectedFields = new ConnectedComponents(connectionGraph);
        if (connectedFields.getConnectedComponentsCount() == 1) {
            return true;
        }

        ConnectedComponent biggestComponent = connectedFields.getGiantComponent();

        int[] componentFixedDiceOccurrences = Arrays.copyOf(fixedDiceOccurrences, fixedDiceOccurrences.length);
        for (ConnectedComponent fieldComponent : connectedFields) {
            if (fieldComponent.getNodeCount() >= biggestComponent.getNodeCount())
                continue;

            Set<Node> nodes = fieldComponent.getNodeSet();
            FieldComponentProperty fcp = fieldComponentProperties.computeIfAbsent(nodes,
                    nodes1 -> computeFieldComponentProperties(board, nodes1));

            boolean fittable = fcp.fittablePieces.stream()
                    .anyMatch(availablePieces::contains);

            if (!fittable) {
                for (int i = 0; i < Board.DIM; i++) {
                    componentFixedDiceOccurrences[i] += fcp.diceNumbers[i];
                    if (componentFixedDiceOccurrences[i] > diceNumbers[i]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean doesPieceFitIntoComponent(Board board, Piece piece,
                                              int width, int height, int startRow, int startColumn) {
        for (PieceOrientation orientation : piece.getOrientations()) {
            for (int rowOffset = 0; rowOffset <= height - orientation.getHeight(); rowOffset++) {
                for (int columnOffset = 0; columnOffset <= width - orientation.getWidth(); columnOffset++) {
                    if (board.fitsInPlace(orientation,
                            startRow + rowOffset, startColumn + columnOffset)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private FieldComponentProperty computeFieldComponentProperties(Board board, Set<Node> nodes) {
        List<Field> fields = nodes.stream().map(node -> ((BoardNode) node).getField()).toList();
        int minRow = fields.stream().mapToInt(Field::getRow).min().orElseThrow();
        int maxRow = fields.stream().mapToInt(Field::getRow).max().orElseThrow();
        int minColumn = fields.stream().mapToInt(Field::getColumn).min().orElseThrow();
        int maxColumn = fields.stream().mapToInt(Field::getColumn).max().orElseThrow();

        int width = maxColumn - minColumn + 1;
        int height = maxRow - minRow + 1;

        List<Piece> fittablePieces = board.getAllPieces().stream()
                .filter(piece -> doesPieceFitIntoComponent(board, piece, width, height, minRow, minColumn))
                .toList();

        return new FieldComponentProperty(fittablePieces, Board.countDiceNumbersOfFields(fields.stream()));
    }

    private void updateVisibleDiceNumbers(int[] visibleDiceNumbers, int[] diceNumbers, boolean add) {
        for (int diceNumber = 0; diceNumber < Board.DIM; diceNumber++) {
            if (add) {
                visibleDiceNumbers[diceNumber] += diceNumbers[diceNumber];
            } else {
                visibleDiceNumbers[diceNumber] -= diceNumbers[diceNumber];
            }
        }
    }

    private boolean areEnoughSolutionDiceNumbersAvailable(int[] diceOccurrences, int[] visibleDiceNumbers) {
        for (int diceNumber = 0; diceNumber < Board.DIM; diceNumber++) {
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

    private record FieldComponentProperty(List<Piece> fittablePieces, int[] diceNumbers) {

    }
}
