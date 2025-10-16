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

        int[] diceOccurrences = countOccurrencesOfNumbers(diceNumbers);
        List<Piece> availablePieces = board.getAvailablePieces();
        availablePieces.sort(Comparator.comparingInt(Piece::getAmountOccupations));

        solveWithCurrentBoard(board, availablePieces, diceOccurrences);
        mainFrame.updateSolutionStats(solutions.size());
    }

    public void solveWithCurrentBoard(Board board, List<Piece> availablePieces, int[] diceOccurrences) {

        if(availablePieces.isEmpty()) {
            if (isValidSolution(board, diceOccurrences)) {
                solutions.add(board.copy());
                mainFrame.updateSolutionStats(solutions.size());
            }
            return;
        }

        Piece nextPiece = availablePieces.remove(0);
        for(PieceOrientation orientation : nextPiece.getOrientations()) {
            for (int rowOffset = 0; rowOffset < 7 - orientation.getHeight(); rowOffset++) {
                for (int columnOffset = 0; columnOffset < 7 - orientation.getWidth(); columnOffset++) {
                    if (!solving) {
                        return;
                    }

                    if (fitsInPlace(board, orientation, rowOffset, columnOffset)) {
                            board.placePieceOnBoard(nextPiece, orientation, rowOffset, columnOffset);
                            solveWithCurrentBoard(board, availablePieces, diceOccurrences);
                            board.removeLastPieceFromBoard();
                    }
                }
            }
        }
        availablePieces.add(0, nextPiece);
    }

    public void stop() {
        solving = false;
    }

    private boolean isValidSolution(Board board, int[] diceOccurrences) {
        List<Integer> fieldNumbers = board.getAllFields()
                .stream()
                .filter(field -> !field.isOccupied() && field.getNumber() != 0)
                .map(Field::getNumber)
                .toList();
        int[] solutionNumberOccurrences = countOccurrencesOfNumbers(fieldNumbers);
        return Arrays.equals(diceOccurrences, solutionNumberOccurrences);
    }

    private int[] countOccurrencesOfNumbers(List<Integer> numbers) {
        int[] occurrences = new int[7];
        for(int number : numbers) {
            occurrences[number]++;
        }
        return occurrences;
    }

    private boolean fitsInPlace(Board board, PieceOrientation orientation, int rowOffset, int columnOffset) {
        for(FieldPosition pieceField : orientation.getPositions()) {
            if (board.getFieldOnBoard(pieceField.row() + rowOffset, pieceField.column() + columnOffset).isOccupied()) {
                return false;
            }
        }
        return true;
    }

    public boolean isSolving() {
        return solving;
    }

    public List<Board> getSolutions() {
        return solutions;
    }
}
