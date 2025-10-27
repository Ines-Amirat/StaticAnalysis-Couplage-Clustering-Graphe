package org.analysis.clustering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Nœud du dendrogramme (binaire). */
public class ClusterNode {
    public final ClusterNode left;
    public final ClusterNode right;
    public final List<String> items; // classes dans ce cluster (noms simples)
    public final double height;      // distance (1 - similarité) à la fusion

    /** Feuille. */
    public static ClusterNode leaf(String item) {
        List<String> it = new ArrayList<>();
        it.add(item);
        return new ClusterNode(null, null, it, 0.0);
    }

    /** Interne. */
    public static ClusterNode merge(ClusterNode a, ClusterNode b, double height) {
        List<String> it = new ArrayList<>(a.items.size() + b.items.size());
        it.addAll(a.items);
        it.addAll(b.items);
        return new ClusterNode(a, b, it, height);
    }

    private ClusterNode(ClusterNode left, ClusterNode right, List<String> items, double height) {
        this.left = left;
        this.right = right;
        this.items = Collections.unmodifiableList(items);
        this.height = height;
    }

    public boolean isLeaf() { return left == null && right == null; }
}
