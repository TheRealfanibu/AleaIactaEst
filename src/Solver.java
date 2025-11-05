import org.graphstream.algorithm.ConnectedComponents;
import org.graphstream.algorithm.ConnectedComponents.ConnectedComponent;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.AbstractGraph;
import org.graphstream.graph.implementations.DefaultGraph;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Solver {

    private static final Comparator<Piece> PIECE_ORDER = Comparator.comparingInt(Piece::getNumOccupations)
            .thenComparingInt(Piece::getMaxDimension)
            .thenComparingInt(Piece::getMinDimension).reversed();

    private static final int CONNECTIVITY_CHECK_AT_PIECE = 3;

    private static final int THREAD_SPLIT_AT_PIECE = 2;

    private final MainFrame mainFrame;

    private boolean solving = false;

    private final boolean searchOnlyOneSolution;
    private boolean checkConnectivity;

    private boolean multiThreading;

    private final List<Board> solutions;

    private final Map<Set<Node>, FieldComponentProperty> fieldComponentProperties;

    private AtomicInteger prunedTreesCounter, notPrunedTreesCounter;

    private ExecutorService threadExecutor;


    public Solver() {
        this(null, true);
    }

    public Solver(MainFrame mainFrame) {
        this(mainFrame, false);
    }

    public Solver(MainFrame mainFrame, boolean searchOnlyOneSolution) {
        this.searchOnlyOneSolution = searchOnlyOneSolution;
        this.mainFrame = mainFrame;

        if (THREAD_SPLIT_AT_PIECE > 0) { // do multi-threading
            solutions = Collections.synchronizedList(new ArrayList<>());
            fieldComponentProperties = new ConcurrentHashMap<>();
        } else {
            solutions = new ArrayList<>();
            fieldComponentProperties = new HashMap<>();
        }
    }

    public Field getNextBestDicePosition(Stream<Field> unoccupiedFields) {
        Map<Field, List<Board>> solutionsPerField = unoccupiedFields.collect(Collectors.toMap(
                field -> field,
                field -> solutions.parallelStream()
                        .filter(board -> !board.getFieldOnBoard(field.getRow(), field.getColumn()).isOccupied())
                        .toList()));

        Map.Entry<Field, List<Board>> bestEntry = solutionsPerField.entrySet().stream()
                .max(Comparator.comparingInt(entry -> entry.getValue().size())).orElseThrow();

        solutions.clear();
        solutions.addAll(bestEntry.getValue());

        return bestEntry.getKey();
    }

    public PiecePositionSolutions getNextBestPiecePosition(List<Piece> availablePieces) {
        List<PiecePositionSolutions> piecePositionSolutions = new LinkedList<>();
        for (Piece piece : availablePieces) {
            int pieceId = piece.getId();
            Map<PieceOrientation, List<Board>> solutionsPerOrientation = groupSolutionsBy(solutions, pieceId, Piece::getOrientationOnBoard);

            for (PieceOrientation orientation : piece.getOrientations()) {
                List<Board> orientationSolutions = solutionsPerOrientation.get(orientation);
                if (orientationSolutions != null) {
                    Map<FieldPosition, List<Board>> solutionsPerPosition = groupSolutionsBy(orientationSolutions, pieceId,
                            solutionPiece -> new FieldPosition(solutionPiece.getRowOffsetOnBoard(), solutionPiece.getColumnOffsetOnBoard()));

                    Stream<PiecePositionSolutions> positionSolutions = solutionsPerPosition.keySet().parallelStream()
                            .map(fieldPosition -> new PiecePositionSolutions(piece, orientation, fieldPosition, solutionsPerPosition.get(fieldPosition)));
                    piecePositionSolutions.add(getBestPiecePositionSolutions(positionSolutions));
                }
            }
        }
        PiecePositionSolutions bestPosition = getBestPiecePositionSolutions(piecePositionSolutions.stream());

        solutions.clear();
        solutions.addAll(bestPosition.solutions());

        return bestPosition;
    }

    private PiecePositionSolutions getBestPiecePositionSolutions(Stream<PiecePositionSolutions> pps) {
        return pps.max(Comparator.comparingInt(x -> x.solutions().size())).orElseThrow();
    }

    private <T> Map<T, List<Board>> groupSolutionsBy(List<Board> solutions, int pieceId, Function<Piece, T> grouper) {
        return solutions.parallelStream().collect(Collectors.groupingBy(board -> {
            Piece piece = board.getAllPieces().get(pieceId);
            return grouper.apply(piece);
        }));
    }

    public record PiecePositionSolutions(Piece piece, PieceOrientation orientation, FieldPosition position,
                                         List<Board> solutions) {
    }


    public void solve(Board board, List<Integer> diceNumbers, List<Integer> fixedDiceNumbers) {
        long startTime = System.currentTimeMillis();

        multiThreading = fixedDiceNumbers.size() <= 4 && board.getPiecesOnBoard().size() < THREAD_SPLIT_AT_PIECE;

        prunedTreesCounter = new AtomicInteger();
        notPrunedTreesCounter = new AtomicInteger();

        board = board.copy();
        solving = true;
        solutions.clear();

        int[] diceOccurrences = Board.countDiceNumbers(diceNumbers.stream());
        diceOccurrences[0] = 1;
        checkConnectivity = Arrays.stream(diceOccurrences).anyMatch(x -> x > 1); // will not prune many trees if dices are 1-2-3-4-5-6 --> not worth the cost

        int[] fixedDiceOccurrences = Board.countDiceNumbers(fixedDiceNumbers.stream());
        int[] visibleDiceNumbers = board.countVisibleDiceNumbers();

        List<Piece> availablePieces = board.getAvailablePieces();
        availablePieces.sort(PIECE_ORDER);

        threadExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 2);

        solveWithCurrentBoard(board, availablePieces, visibleDiceNumbers, diceOccurrences, fixedDiceOccurrences);

        threadExecutor.shutdown();
        try {
            boolean terminated = threadExecutor.awaitTermination(300, TimeUnit.SECONDS);
            if (!terminated) {
                System.err.println("Solver timeout");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        if (mainFrame != null)
            mainFrame.indicateSolvingFinished();

        int total = prunedTreesCounter.get() + notPrunedTreesCounter.get();
        if (total > 0) {
            System.out.println("prune ratio: " + (double) prunedTreesCounter.get() / total);
            System.out.println("total trees pruned: " + prunedTreesCounter);
            System.out.println("total trees: " + total);
        }
        System.out.println("field components computed: " + fieldComponentProperties.size());
        System.out.println("Solving took: " + (System.currentTimeMillis() - startTime) / 1000d + "s");
    }

    private void solveWithCurrentBoard(Board board, List<Piece> availablePieces, int[] visibleDiceNumbers,
                                       int[] diceOccurrences, int[] fixedDiceOccurrences) {
        if (availablePieces.isEmpty()) {
            if (Arrays.equals(diceOccurrences, visibleDiceNumbers)) { // valid solution
                solutions.add(board.copy());

                if (searchOnlyOneSolution) {
                    stop();
                }
            }
            return;
        }

        if (!areEnoughSolutionDiceNumbersAvailable(diceOccurrences, visibleDiceNumbers)) {
            return;
        }

        if (checkConnectivity && board.getPiecesOnBoard().size() == CONNECTIVITY_CHECK_AT_PIECE) {
            if (areFieldComponentsCompatible(board, availablePieces, diceOccurrences, fixedDiceOccurrences)) {
                notPrunedTreesCounter.incrementAndGet();
            } else {
                prunedTreesCounter.incrementAndGet();
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

                        if (multiThreading && board.getPiecesOnBoard().size() == THREAD_SPLIT_AT_PIECE) {
                            Board boardCopy = board.copy();
                            List<Piece> availablePiecesCopy = boardCopy.getAvailablePieces();
                            availablePiecesCopy.sort(PIECE_ORDER);
                            int[] visibleDiceNumbersCopy = Arrays.copyOf(visibleDiceNumbers, visibleDiceNumbers.length);
                            threadExecutor.submit(() -> solveWithCurrentBoard(boardCopy, availablePiecesCopy, visibleDiceNumbersCopy,
                                    diceOccurrences, fixedDiceOccurrences));
                        } else {
                            solveWithCurrentBoard(board, availablePieces, visibleDiceNumbers, diceOccurrences, fixedDiceOccurrences);
                        }

                        updateVisibleDiceNumbers(visibleDiceNumbers, diceNumbersOccupied, true);
                        board.removeLastPieceFromBoard();
                    }
                }
            }
        }
        availablePieces.add(0, nextPiece);
    }

    public Graph initConnectivityGraph(Board board) {
        List<Field> unoccupiedFields = board.getUnoccupiedFields().toList();

        Graph connectionGraph = new DefaultGraph("ConnectionGraph-" + Thread.currentThread().getName(),
                true, false, unoccupiedFields.size(), 2 * unoccupiedFields.size());
        connectionGraph.setNodeFactory((id, graph) -> new BoardNode((AbstractGraph) graph, id));

        addFieldsToConnectivityGraph(connectionGraph, unoccupiedFields);

        for (Field field : unoccupiedFields) {
            addEdgeToGraphIfNeighborConnected(connectionGraph, board, field, 1, 0);
            addEdgeToGraphIfNeighborConnected(connectionGraph, board, field, 0, 1);
        }
        return connectionGraph;
    }

    private void addFieldsToConnectivityGraph(Graph connectionGraph, List<Field> fields) {
        fields.forEach(field -> {
            BoardNode node = (BoardNode) connectionGraph.addNode(field.getId());
            node.setField(field);
        });
    }

    private void addEdgeToGraphIfNeighborConnected(Graph connectionGraph, Board board, Field field, int rowOffset, int columnOffset) {
        int row = field.getRow() + rowOffset;
        int column = field.getColumn() + columnOffset;
        if (!Board.isOutOfBounds(row, column) &&
                !board.getFieldOnBoard(row, column).isOccupied()) {
            Field connectedField = board.getFieldOnBoard(row, column);
            connectionGraph.addEdge(field.getId() + "-" + connectedField.getId(), field.getId(), connectedField.getId());
        }
    }


    private boolean areFieldComponentsCompatible(Board board, List<Piece> availablePieces,
                                                 int[] diceNumbers, int[] fixedDiceOccurrences) {
        Graph connectionGraph = initConnectivityGraph(board);
        ConnectedComponents connectedFields = new ConnectedComponents(connectionGraph);
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
