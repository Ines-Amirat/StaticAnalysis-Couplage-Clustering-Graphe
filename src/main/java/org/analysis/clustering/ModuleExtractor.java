package org.analysis.clustering;

import java.util.*;

/**
 * Découpe le dendrogramme en modules selon :
 *  - <= M/2 modules
 *  - modules = branches du dendrogramme
 *  - moyenne de couplage interne > CP
 */
public class ModuleExtractor {

    /** Interface pour fournir S(a,b) symétrique (même que pour clustering). */
    public interface Similarity {
        double s(String a, String b); // 0..1
    }

    /** Résultat : liste de modules (chaque module = liste de classes) */
    public static class Modules {
        public final List<List<String>> groups;
        public final Map<List<String>, Double> meanCoupling;
        public Modules(List<List<String>> groups, Map<List<String>, Double> mean) {
            this.groups = groups;
            this.meanCoupling = mean;
        }
    }

    /** Extraire les modules depuis la racine, avec seuil CP et limite M/2. */
    public Modules extract(ClusterNode root, double cpThreshold, Similarity s) {
        int M = root.items.size();
        int maxModules = Math.max(1, M / 2);

        List<ClusterNode> accepted = new ArrayList<>();
        Deque<ClusterNode> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            ClusterNode c = stack.pop();

            double mean = meanInternalCoupling(c.items, s);
            boolean good = (mean > cpThreshold);

            if ((good || c.isLeaf()) || accepted.size() + stack.size() + 1 >= maxModules) {
                // on garde ce cluster comme module
                accepted.add(c);
            } else {
                // on découpe si possible
                if (c.left != null) stack.push(c.left);
                if (c.right != null) stack.push(c.right);
                // si feuille, on la gardera de toute façon (cas rare)
            }
        }

        // produire la sortie
        List<List<String>> groups = new ArrayList<>();
        Map<List<String>, Double> meanMap = new LinkedHashMap<>();
        for (ClusterNode c : accepted) {
            List<String> items = new ArrayList<>(c.items);
            Collections.sort(items);
            groups.add(items);
            meanMap.put(items, meanInternalCoupling(items, s));
        }
        return new Modules(groups, meanMap);
    }

    /** Moyenne des S(a,b) pour toutes les paires a<b dans items. */
    public static double meanInternalCoupling(List<String> items, Similarity s) {
        if (items.size() <= 1) return 0.0;
        double sum = 0; int n = 0;
        for (int i = 0; i < items.size(); i++) {
            for (int j = i + 1; j < items.size(); j++) {
                sum += s.s(items.get(i), items.get(j));
                n++;
            }
        }
        return (n == 0) ? 0.0 : (sum / n);
    }
}
