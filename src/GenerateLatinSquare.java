import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

public class GenerateLatinSquare {
    public static int[][] generateRandomLatinSquare(int n, Random rand) {
        // Step 1: Start with a cyclic Latin square
        int[][] square = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                square[i][j] = (i + j) % n;

        // Step 2: Randomly permute rows
        shuffleRows(square, rand);

        // Step 3: Randomly permute columns
        shuffleColumns(square, rand);

        // Step 4: Randomly permute symbols
        permuteSymbols(square, rand);

        return square;
    }

    private static void shuffleRows(int[][] square, Random rand) {
        for (int i = square.length - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int[] temp = square[i];
            square[i] = square[j];
            square[j] = temp;
        }
    }

    private static void shuffleColumns(int[][] square, Random rand) {
        int n = square.length;
        for (int i = n - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            for (int row = 0; row < n; row++) {
                int temp = square[row][i];
                square[row][i] = square[row][j];
                square[row][j] = temp;
            }
        }
    }

    private static void permuteSymbols(int[][] square, Random rand) {
        int n = square.length;
        int[] mapping = new int[n];
        for (int i = 0; i < n; i++) mapping[i] = i;
        // shuffle mapping
        for (int i = n - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int tmp = mapping[i];
            mapping[i] = mapping[j];
            mapping[j] = tmp;
        }
        // apply mapping
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                square[i][j] = mapping[square[i][j]];
    }

    public static void main(String[] args) {
        int n = 7;
        Random rand = new Random();
        int[][] square = generateRandomLatinSquare(n, rand);
        printSquare(square);
    }

    public static void printSquare(int[][] square) {
        for (int[] row : square) {
            System.out.println("{" + Arrays.stream(row).mapToObj(String::valueOf).collect(Collectors.joining(",")) + "},");
        }
        System.out.println();
    }

}
