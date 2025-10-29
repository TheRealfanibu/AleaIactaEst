import org.graphstream.algorithm.ConnectedComponents;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.DefaultGraph;

public class GraphTest {

    public static void main(String[] args) {
        DefaultGraph graph = new DefaultGraph("test graph");

        graph.addNode("1");
        graph.addNode("2");
        graph.addNode("3");

        graph.addEdge("1-2", "1", "2");
        graph.addEdge("2-3", "2", "3");

        ConnectedComponents components = new ConnectedComponents(graph);

        System.out.println(components.defaultResult());

        Node node = graph.getNode("2");
        graph.removeNode(node);

        System.out.println(components.defaultResult());

        graph.addNode(node.getId());

        System.out.println(components.defaultResult());

        graph.addEdge("1-2", "1", "2");

        System.out.println(components.defaultResult());
        System.out.println(graph.getNode("2").hashCode());
        System.out.println(node.hashCode());

//        Solver solver = new Solver();
//
//        Board board = new Board();
//
//        Piece piece = board.getAllPieces().get(3);
//
//        board.placePieceOnBoard(piece, piece.getOrientations()[0], 0, 0);
//
//        System.setProperty("org.graphstream.ui", "swing");
//
//        solver.initConnectionGraph(board);

    }

    /*public static void main(String[] args) {

    }*/
}
