package org.analysis;

import org.analysis.parsing.SourceParser;
import org.analysis.processing.model.ProjectStats;
import org.analysis.gui.CouplingGraphSwing;               // UI
import org.analysis.gui.CouplingGraphSwing.EdgeInfo;     // Info d’arête (poids + ratio)

import java.nio.file.Path;
import java.util.*;

public class CouplingAppMain {

    // Classes du domaine à garder
    private static final Set<String> KEEP = Set.of(
            "org.analysis.codesource.BoutiqueService",
            "org.analysis.codesource.ProduitElectronique",
            "org.analysis.codesource.Produit"
    );

    // true = graphe non orienté (A—B), false = orienté (A->B)
    private static final boolean UNDIRECTED = false;

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length > 0 ? args[0] : "src/main/java");
        String pkgPrefix = "org.analysis.codesource";

        // 1) Parse et graphe d'appels méthode->méthode
        ProjectStats stats = new SourceParser().parseAll(root, pkgPrefix);
        Map<String, Set<String>> g = stats.callGraph.asMap();

        // 2) Agrégation classe->classe (inter-classes)
        Map<String, Integer> edgeWeights = new LinkedHashMap<>(); // "A->B" ou "A|B"
        int total = 0;

        for (var e : g.entrySet()) {
            String caller = e.getKey();                 // ex: org.x.Y#foo
            String callerClass = clsOf(caller);
            if (!KEEP.contains(callerClass)) continue;

            for (String callee : e.getValue()) {
                String calleeClass = clsOf(callee);
                if (!KEEP.contains(calleeClass)) continue;
                if (callerClass.equals(calleeClass)) continue; // on ignore intra-classes

                String key = UNDIRECTED
                        ? undirectedKey(callerClass, calleeClass)
                        : callerClass + "->" + calleeClass;

                edgeWeights.merge(key, 1, Integer::sum);
                total++;
            }
        }

        // ===== copie finale pour les lambdas =====
        final int T = total;

        // 3) Impression console (poids + couplage)
        System.out.println("=== Graphe de couplage (" + (UNDIRECTED ? "non orienté" : "orienté") + ") ===");
        System.out.println("Total des relations inter-classes T = " + T);

        edgeWeights.entrySet().stream()
                .sorted((a,b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(e -> {
                    double c = (T == 0) ? 0d : (e.getValue() / (double) T);
                    String nice = String.format(Locale.US, "%.3f", c);
                    System.out.println(" " + labelSimple(e.getKey()) + " : w=" + e.getValue() + "  (c=" + nice + ")");
                });

        // 4) Données pour l’UI
        Map<String, Map<String, EdgeInfo>> ui = new LinkedHashMap<>();
        for (var e : edgeWeights.entrySet()) {
            int w = e.getValue();
            double c = (T == 0) ? 0d : (w / (double) T);
            EdgeInfo info = new EdgeInfo(w, c);

            if (UNDIRECTED) {
                String[] ab = e.getKey().split("\\|");
                add(ui, simple(ab[0]), simple(ab[1]), info);
                add(ui, simple(ab[1]), simple(ab[0]), info); // dessiner visuellement les 2 sens
            } else {
                String[] ab = e.getKey().split("->");
                add(ui, simple(ab[0]), simple(ab[1]), info);
            }
        }

        // 5) Fenêtre Swing
        CouplingGraphSwing.show(ui);
    }

    private static void add(Map<String, Map<String, EdgeInfo>> ui, String a, String b, EdgeInfo info) {
        ui.computeIfAbsent(a, k -> new LinkedHashMap<>()).put(b, info);
        ui.computeIfAbsent(b, k -> new LinkedHashMap<>()); // s'assurer du nœud cible
    }

    private static String clsOf(String sig) {
        int i = sig.indexOf('#');
        String fqn = (i < 0) ? sig : sig.substring(0, i);
        return fqn;
    }

    private static String simple(String fqn) {
        int d = fqn.lastIndexOf('.');
        return (d >= 0) ? fqn.substring(d + 1) : fqn;
    }

    private static String undirectedKey(String a, String b) {
        int cmp = a.compareTo(b);
        return (cmp <= 0) ? (a + "|" + b) : (b + "|" + a);
    }

    private static String labelSimple(String key) {
        if (key.contains("->")) {
            String[] ab = key.split("->");
            return simple(ab[0]) + " -> " + simple(ab[1]);
        } else if (key.contains("|")) {
            String[] ab = key.split("\\|");
            return simple(ab[0]) + " — " + simple(ab[1]);
        }
        return key;
    }
}
