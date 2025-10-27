package org.analysis;

import org.analysis.parsing.SpoonSourceParser;
import org.analysis.processing.model.ProjectStats;
import org.analysis.clustering.*;
import org.analysis.gui.DendrogramSwing;

import java.nio.file.Path;
import java.util.*;

/** Clustering + modules (Spoon) avec average linkage et contraintes CP / M/2. */
public class ModulesAppMainSpoon {

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length > 0 ? args[0] : "src/main/java");
        String pkgPrefix = (args.length > 1) ? args[1] : "org.analysis.codesource";
        double CP = (args.length > 2) ? Double.parseDouble(args[2]) : 0.20;

        ProjectStats stats = new SpoonSourceParser().parseAll(root, pkgPrefix);
        Map<String, Set<String>> call = stats.callGraph.asMap();

        // matrice de similarité S(a,b) = (wAB + wBA) / T (a,b = noms simples)
        CouplingMatrix cm = CouplingMatrix.fromCallGraph(call, pkgPrefix);

        // clustering hiérarchique
        List<String> labels = new ArrayList<>(cm.classes);
        labels.sort(Comparator.naturalOrder());
        HierarchicalClustering hc = new HierarchicalClustering();
        ClusterNode rootDendro = hc.cluster(labels, cm::s);

        // extraction de modules (<= M/2 et mean > CP)
        ModuleExtractor extractor = new ModuleExtractor();
        ModuleExtractor.Modules modules = extractor.extract(rootDendro, CP, cm::s);

        // console
        System.out.println("Classes (" + labels.size() + "): " + labels);
        System.out.println("CP = " + CP + " ; max modules = " + (labels.size()/2));
        System.out.println("\n== Modules (Spoon) ==");
        List<String> rightPanel = new ArrayList<>();
        int k = 1;
        for (List<String> g : modules.groups) {
            double mean = modules.meanCoupling.get(g);
            String line = String.format(Locale.US, "Module %d (|C|=%d, mean=%.3f): %s",
                    k++, g.size(), mean, String.join(", ", g));
            System.out.println(line);
            rightPanel.add(line);
        }

        // UI
        DendrogramSwing.show(rootDendro, rightPanel);
    }

    /** Matrice de couplage symétrique depuis callGraph (méthode->méthode). */
    static class CouplingMatrix {
        final Set<String> classes = new LinkedHashSet<>();                 // noms simples
        final Map<String, Map<String, Double>> s = new HashMap<>();        // S(a,b)

        double s(String a, String b) {
            if (a.equals(b)) return 0.0;
            return s.getOrDefault(a, Map.of()).getOrDefault(b, 0.0);
        }

        static CouplingMatrix fromCallGraph(Map<String, Set<String>> callGraph, String includePrefix) {
            CouplingMatrix cm = new CouplingMatrix();
            Map<String, Map<String, Integer>> w = new HashMap<>(); // w(A->B)
            int T = 0;

            for (var e : callGraph.entrySet()) {
                String caller = e.getKey();          // fqcn#method
                String callerFqn = clsOf(caller);
                if (!includePrefix.isBlank() && !callerFqn.startsWith(includePrefix)) continue;

                for (String callee : e.getValue()) {
                    String calleeFqn = clsOf(callee);
                    if (!includePrefix.isBlank() && !calleeFqn.startsWith(includePrefix)) continue;
                    if (callerFqn.equals(calleeFqn)) continue; // intra classe ignoré

                    String A = simple(callerFqn);
                    String B = simple(calleeFqn);
                    cm.classes.add(A); cm.classes.add(B);

                    w.computeIfAbsent(A, k -> new HashMap<>()).merge(B, 1, Integer::sum);
                    T++;
                }
            }

            for (String a : cm.classes) cm.s.put(a, new HashMap<>());
            for (String A : cm.classes) {
                for (String B : cm.classes) {
                    if (A.equals(B)) continue;
                    int wAB = w.getOrDefault(A, Map.of()).getOrDefault(B, 0);
                    int wBA = w.getOrDefault(B, Map.of()).getOrDefault(A, 0);
                    double sim = (T == 0) ? 0.0 : ((wAB + wBA) / (double) T);
                    cm.s.get(A).put(B, sim);
                    cm.s.get(B).put(A, sim);
                }
            }
            return cm;
        }

        private static String clsOf(String sig) {
            int i = sig.indexOf('#');
            return (i < 0) ? sig : sig.substring(0, i);
        }
        private static String simple(String fqn) {
            int d = fqn.lastIndexOf('.');
            return (d >= 0) ? fqn.substring(d + 1) : fqn;
        }
    }
}
