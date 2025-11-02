import java.util.Arrays;
import java.util.List;

public class SolutionChecker {

    private final Board board = new Board();

    private final Solver solver = new Solver();

    private boolean allSameValue;

    public SolutionChecker() {
        int[] dices = new int[6];
        for (int i = 1; i <= dices.length; i++) {
            Arrays.fill(dices, i);
            checkSolution(dices);
        }


        checkAllSubsolutions(dices, 1, 0);
    }

    private void checkAllSubsolutions(int[] dices, int startValue, int index) {
        if (index == dices.length) {
            if (allSameValue) {
                allSameValue = false;
            } else {
                checkSolution(dices);
            }
            return;
        }

        for (int i = startValue; i <= 6 ; i++) {
            dices[index] = i;
            if (index == 0) {
                allSameValue = true;
            }
            checkAllSubsolutions(dices, i, index + 1);
        }
    }

    private void checkSolution(int[] dices) {
        List<Integer> diceList = Arrays.stream(dices).boxed().toList();
        solver.solve(board, diceList, List.of());
        if (solver.getSolutions().isEmpty()) {
            System.err.println(diceList + ": No solution found!");
        } else {
            System.out.println(diceList + ": Solution found.");
        }
    }

    public static void main(String[] args) {
        new SolutionChecker();
    }
}
