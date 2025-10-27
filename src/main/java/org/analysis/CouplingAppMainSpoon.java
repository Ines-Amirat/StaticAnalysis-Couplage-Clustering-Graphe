package org.analysis;

import org.analysis.parsing.SpoonSourceParser;
import org.analysis.processing.model.ProjectStats;
import org.analysis.gui.CouplingGraphSwing;
import org.analysis.gui.CouplingGraphSwing.EdgeInfo;

import java.nio.file.Path;
import java.util.*;

/** Couplage (classe->classe) basé sur Spoon. */
public class CouplingAppMainSpoon {

    // limite d’analyse (adapte si besoin)
    private static final Set<String> KEEP_FQCN = Set.of(
            "org.analysis.codesource.BoutiqueService",
            "org.analysis.codesource.ProduitElectronique",
            "org.analysis.codesource.Produit"
    );

    private static final boolean UNDIRECTED = false; // graphe orienté par défaut

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length > 0 ? args[0] : "src/main/java");
        String pkgPrefix = (args.length > 1) ? args[1] : "org.analysis.codesource";

        ProjectStats stats = new SpoonSourceParser().parseAll(root, pkgPrefix);
        Map<String, Set<String>> call = stats.callGraph.asMap();

        // agrégation classe->classe
        Map<String, Integer> edgeW = new LinkedHashMap<>(); // "A->B" ou "A|B"
        int total = 0;

        for (var e : call.entrySet()) {
            String callerSig = e.getKey();         // fqcn#method
            String callerCls = clsOf(callerSig);   // fqcn
            if (!KEEP_FQCN.contains(callerCls)) continue;

            for (String calleeSig : e.getValue()) {
                String calleeCls = clsOf(calleeSig);
                if (!KEEP_FQCN.contains(calleeCls)) continue;
                if (callerCls.equals(calleeCls)) continue; // ignore intra-classe

                String key = UNDIRECTED
                        ? undirectedKey(callerCls, calleeCls)
                        : callerCls + "->" + calleeCls;

                edgeW.merge(key, 1, Integer::sum);
                total++;
            }
        }

        final int T = total;

        System.out.println("=== Couplage (Spoon) " + (UNDIRECTED ? "non orienté" : "orienté") + " ===");
        System.out.println("T = " + T);
        edgeW.entrySet().stream()
                .sorted((a,b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(en -> {
                    double c = (T == 0) ? 0d : (en.getValue() / (double) T);
                    System.out.printf(Locale.US, " %s : w=%d  (c=%.3f)%n",
                            labelSimple(en.getKey()), en.getValue(), c);
                });

        // données UI
        Map<String, Map<String, EdgeInfo>> ui = new LinkedHashMap<>();
        for (var en : edgeW.entrySet()) {
            int w = en.getValue();
            double c = (T == 0) ? 0d : (w / (double) T);
            EdgeInfo info = new EdgeInfo(w, c);

            if (UNDIRECTED) {
                String[] ab = en.getKey().split("\\|");
                add(ui, simple(ab[0]), simple(ab[1]), info);
                add(ui, simple(ab[1]), simple(ab[0]), info);
            } else {
                String[] ab = en.getKey().split("->");
                add(ui, simple(ab[0]), simple(ab[1]), info);
            }
        }

        CouplingGraphSwing.show(ui);
    }

    private static void add(Map<String, Map<String, EdgeInfo>> ui, String a, String b, EdgeInfo info) {
        ui.computeIfAbsent(a, k -> new LinkedHashMap<>()).put(b, info);
        ui.computeIfAbsent(b, k -> new LinkedHashMap<>());
    }
    private static String clsOf(String sig) {
        int i = sig.indexOf('#');
        return (i < 0) ? sig : sig.substring(0, i);
    }
    private static String simple(String fqn) {
        int d = fqn.lastIndexOf('.');
        return (d >= 0) ? fqn.substring(d + 1) : fqn;
    }
    private static String undirectedKey(String a, String b) {
        return (a.compareTo(b) <= 0) ? (a + "|" + b) : (b + "|" + a);
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
