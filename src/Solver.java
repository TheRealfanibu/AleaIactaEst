import java.util.*;

public class Solver {

    private final List<Board> solutions = new ArrayList<>();

    private final MainFrame mainFrame;

    private boolean solving = false;


    public Solver(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    public void solve(Board board, List<Integer> diceNumbers) {
        solving = true;
        solutions.clear();

        int[] diceOccurrences = board.countDiceNumbers(diceNumbers.stream());
        diceOccurrences[0] = 1; // one field must be empty

        int[] visibleDiceNumbers = board.countVisibleDiceNumbers();

        List<Piece> availablePieces = board.getAvailablePieces();
        availablePieces.sort(Comparator.comparingInt(Piece::getAmountOccupations));

        solveWithCurrentBoard(board, availablePieces, diceOccurrences, visibleDiceNumbers);
        mainFrame.indicateSolvingFinished();
    }

    public void solveWithCurrentBoard(Board board, List<Piece> availablePieces, int[] diceOccurrences, int[] visibleDiceNumbers) {

        if(availablePieces.isEmpty()) {
            if (Arrays.equals(diceOccurrences, visibleDiceNumbers)) { // valid solution
                solutions.add(board.copy());
                mainFrame.updateSolutionStats();
            }
            return;
        }

        if(!areEnoughSolutionDiceNumbersAvailable(diceOccurrences, visibleDiceNumbers)) {
            return;
        }

        Piece nextPiece = availablePieces.remove(0);
        for(PieceOrientation orientation : nextPiece.getOrientations()) {
            for (int rowOffset = 0; rowOffset <= 7 - orientation.getHeight(); rowOffset++) {
                for (int columnOffset = 0; columnOffset <= 7 - orientation.getWidth(); columnOffset++) {
                    if (!solving) {
                        return;
                    }

                    if (board.fitsInPlace(orientation, rowOffset, columnOffset)) {
                            board.placePieceOnBoard(nextPiece, orientation, rowOffset, columnOffset);
                            int[] diceNumbersOccupied = board.countDiceNumbersOfFields(nextPiece.getOccupiedFields().stream());
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
        for (int diceNumber = 0; diceNumber < 7 ; diceNumber++) {
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
