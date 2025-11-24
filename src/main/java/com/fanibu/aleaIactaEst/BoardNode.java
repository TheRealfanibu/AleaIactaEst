package com.fanibu.aleaIactaEst;

import org.graphstream.graph.implementations.AbstractGraph;
import org.graphstream.graph.implementations.SingleNode;

public class BoardNode extends SingleNode {
    private Field field;

    protected BoardNode(AbstractGraph graph, String id) {
        super(graph, id);
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Field getField() {
        return field;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BoardNode nodes = (BoardNode) o;

        return id.equals(nodes.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
