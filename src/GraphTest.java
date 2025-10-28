import org.graphstream.algorithm.ConnectedComponents;
import org.graphstream.graph.implementations.DefaultGraph;

public class GraphTest {

    public static void main(String[] args) {
        Solver solver = new Solver();

        Board board = new Board();

        Piece piece = board.getAllPieces().get(3);

        board.placePieceOnBoard(piece, piece.getOrientations()[0], 2, 3);

        System.setProperty("org.graphstream.ui", "swing");

        solver.initConnectionGraph(board);
    }
}
