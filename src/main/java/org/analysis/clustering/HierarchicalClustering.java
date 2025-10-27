package org.analysis.clustering;

import java.util.*;

/**
 * Clustering hiérarchique agglomératif avec "average linkage".
 * Similarité fournie par une fonction S(a,b) symétrique dans [0,1].
 */
public class HierarchicalClustering {

    /** Interface pour fournir S(a,b). */
    public interface Similarity {
        double s(String a, String b); // symétrique, 0..1
    }

    /** Construit le dendrogramme à partir des labels (ex: noms simples de classes). */
    public ClusterNode cluster(List<String> labels, Similarity sim) {
        // clusters actifs
        List<ClusterNode> clusters = new ArrayList<>();
        for (String l : labels) clusters.add(ClusterNode.leaf(l));

        // cache des similarités entre clusters : clé (i,j) -> S
        Map<Long, Double> cache = new HashMap<>();

        while (clusters.size() > 1) {
            // trouver le meilleur couple (max S)
            double bestS = -1;
            int bi = -1, bj = -1;

            for (int i = 0; i < clusters.size(); i++) {
                for (int j = i + 1; j < clusters.size(); j++) {
                    double sij = similarityBetween(clusters.get(i), clusters.get(j), sim, cache);
                    if (sij > bestS) {
                        bestS = sij; bi = i; bj = j;
                    }
                }
            }

            ClusterNode a = clusters.get(bi);
            ClusterNode b = clusters.get(bj);
            double height = 1.0 - bestS; // distance visuelle

            ClusterNode c = ClusterNode.merge(a, b, height);
            // remplacer a et b par c
            if (bj > bi) { clusters.remove(bj); clusters.remove(bi); }
            else         { clusters.remove(bi); clusters.remove(bj); }
            clusters.add(c);
        }
        return clusters.get(0);
    }

    // moyenne des similarités pairwise (average linkage)
    private double similarityBetween(ClusterNode c1, ClusterNode c2, Similarity sim,
                                     Map<Long, Double> cache) {
        long key = key(c1.items, c2.items);
        Double cached = cache.get(key);
        if (cached != null) return cached;

        double sum = 0; int n = 0;
        for (String a : c1.items) {
            for (String b : c2.items) {
                sum += sim.s(a, b);
                n++;
            }
        }
        double avg = (n == 0) ? 0.0 : (sum / n);
        cache.put(key, avg);
        return avg;
    }

    private long key(List<String> a, List<String> b) {
        // clé pauvre mais déterministe : hashCodes ordonnés.
        int h1 = a.toString().hashCode();
        int h2 = b.toString().hashCode();
        long x = Integer.toUnsignedLong(Math.min(h1, h2));
        long y = Integer.toUnsignedLong(Math.max(h1, h2));
        return (x << 32) ^ y;
    }
}
