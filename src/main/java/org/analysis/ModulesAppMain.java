package org.analysis;

import org.analysis.clustering.ClusterNode;
import org.analysis.clustering.HierarchicalClustering;
import org.analysis.clustering.ModuleExtractor;
import org.analysis.gui.DendrogramSwing;
import org.analysis.parsing.SourceParser;
import org.analysis.processing.model.ProjectStats;

import java.nio.file.Path;
import java.util.*;

/**
 * Point d'entrée pour :
 *  1) construire la matrice de couplage classe<->classe (symétrisée),
 *  2) faire un clustering hiérarchique (average linkage),
 *  3) extraire des modules selon CP et M/2,
 *  4) afficher dendrogramme + modules.
 *
 * Args possibles :
 *   [0] rootDir (par défaut: src/main/java)
 *   [1] pkgPrefix (ex: org.analysis.codesource) - "" pour tout
 *   [2] CP (double, ex: 0.20)
 */
public class ModulesAppMain {

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length > 0 ? args[0] : "src/main/java");
        String pkgPrefix = (args.length > 1) ? args[1] : "org.analysis.codesource";
        double CP = (args.length > 2) ? Double.parseDouble(args[2]) : 0.20;

        // 1) Parser + graphe d'appels
        ProjectStats stats = new SourceParser().parseAll(root, pkgPrefix);
        Map<String, Set<String>> callGraph = stats.callGraph.asMap();

        // 2) Construire la matrice de couplage (symétrique) entre noms simples de classes
        CouplingMatrix cm = CouplingMatrix.fromCallGraph(callGraph, pkgPrefix);

        // 3) Clustering hierarchique
        List<String> labels = new ArrayList<>(cm.classes); // noms simples
        labels.sort(Comparator.naturalOrder());

        HierarchicalClustering hc = new HierarchicalClustering();
        ClusterNode rootDendro = hc.cluster(labels, cm::s);

        // 4) Extraction de modules
        ModuleExtractor extractor = new ModuleExtractor();
        ModuleExtractor.Modules modules = extractor.extract(rootDendro, CP, cm::s);

        // 5) Impression console
        System.out.println("Classes (" + labels.size() + ") : " + labels);
        System.out.println("Couplage S(a,b) en [0,1] basé sur w(A->B)+w(B->A) / T");
        System.out.println("Paramètre CP = " + CP + " ; limite modules = M/2 = " + (labels.size()/2));
        System.out.println("\n== Modules ==");
        List<String> rightPanel = new ArrayList<>();
        int idx = 1;
        for (List<String> g : modules.groups) {
            double mean = modules.meanCoupling.get(g);
            String line = String.format(Locale.US, "Module %d (|C|=%d, mean=%.3f): %s",
                    idx++, g.size(), mean, String.join(", ", g));
            System.out.println(line);
            rightPanel.add(line);
        }

        // 6) Dendrogramme + modules (fenêtre Swing)
        DendrogramSwing.show(rootDendro, rightPanel);
    }

    /** Matrice de couplage symétrique basée sur le graphe d'appels. */
    static class CouplingMatrix {
        final Set<String> classes = new LinkedHashSet<>();                     // noms simples
        final Map<String, Map<String, Double>> s = new HashMap<>();            // S(a,b)
        final Map<String, String> simpleToFqn = new HashMap<>();

        /** similarité entre noms simples (symétrique dans [0,1]). */
        double s(String a, String b) {
            if (a.equals(b)) return 0.0; // on ignore l'auto-couplage pour clustering
            return s.getOrDefault(a, Map.of()).getOrDefault(b, 0.0);
        }

        /** Construit depuis le callGraph méthode->méthode agrégé classe->classe. */
        static CouplingMatrix fromCallGraph(Map<String, Set<String>> callGraph, String includePrefix) {
            CouplingMatrix cm = new CouplingMatrix();

            // agrégation w(A->B)
            Map<String, Map<String, Integer>> w = new HashMap<>();
            int T = 0;

            for (var e : callGraph.entrySet()) {
                String caller = e.getKey();
                String callerFqn = clsOf(caller);
                if (!includePrefix.isBlank() && !callerFqn.startsWith(includePrefix)) continue;

                for (String callee : e.getValue()) {
                    String calleeFqn = clsOf(callee);
                    if (!includePrefix.isBlank() && !calleeFqn.startsWith(includePrefix)) continue;
                    if (callerFqn.equals(calleeFqn)) continue; // intra-classes ignoré

                    String A = simple(callerFqn);
                    String B = simple(calleeFqn);

                    cm.classes.add(A); cm.classes.add(B);
                    cm.simpleToFqn.putIfAbsent(A, callerFqn);
                    cm.simpleToFqn.putIfAbsent(B, calleeFqn);

                    w.computeIfAbsent(A, k -> new HashMap<>())
                            .merge(B, 1, Integer::sum);
                    T++;
                }
            }

            // Similarité symétrique S(A,B) = (wAB + wBA) / T
            for (String A : cm.classes) {
                cm.s.put(A, new HashMap<>());
            }
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
