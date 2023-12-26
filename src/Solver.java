import java.util.*;

public class Solver {

    private int counter = 0;

    public void solve(Board board, List<Integer> dices) {
        int[] diceOccurrences = countOccurrencesOfNumbers(dices);
        List<Piece> availablePieces = new LinkedList<>(List.of(PieceCollection.ALL_PIECES));
        availablePieces.sort(Comparator.comparingInt(Piece::getAmountOccupations));

        solveWithCurrentBoard(board, availablePieces, diceOccurrences);
    }

    public void solveWithCurrentBoard(Board board, List<Piece> availablePieces, int[] diceOccurrences) {
        if(availablePieces.isEmpty()) {
            if (isValidSolution(board, diceOccurrences)) {
                counter++;
                if(counter % 100 == 0) {
                    System.out.println(counter);
                }
            }
            return;
        }

        Piece nextPiece = availablePieces.remove(0);
        for(PieceOrientation orientation : nextPiece.getOrientations()) {
            for (int rowOffset = 0; rowOffset < 7 - orientation.getHeight(); rowOffset++) {
                for (int columnOffset = 0; columnOffset < 7 - orientation.getWidth(); columnOffset++) {
                    if (fitsInPlace(board, orientation, rowOffset, columnOffset)) {
                        board.placePieceOnBoard(nextPiece, orientation, rowOffset, columnOffset);
                        solveWithCurrentBoard(board, availablePieces, diceOccurrences);
                        if (counter > 0) {
                            return;
                        }
                        board.removeLastPieceFromBoard();
                    }
                }
            }
        }
        availablePieces.add(0, nextPiece);
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


}
