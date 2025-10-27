package org.analysis;

import org.analysis.parsing.SourceParser;
import org.analysis.processing.model.ProjectStats;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class AppGrapheMain {

    private static final Set<String> KEEP = Set.of(
            "org.analysis.codesource.BoutiqueService",
            "org.analysis.codesource.ProduitElectronique",
            "org.analysis.codesource.Produit",
            "org.analysis.codesource.Vendable"
    );

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length > 0 ? args[0] : "src/main/java");
        String pkgPrefix = "org.analysis.codesource";

        ProjectStats stats = new SourceParser().parseAll(root, pkgPrefix);

        // ---------- impression console groupée comme tu l'as eue ----------
        Map<String, Set<String>> g = stats.callGraph.asMap();
        java.util.function.Function<String, String> clsOf = s -> {
            int i = s.indexOf('#'); return i < 0 ? s : s.substring(0, i);
        };

        Map<String, Set<String>> inter = new LinkedHashMap<>();
        for (var e : g.entrySet()) {
            String caller = e.getKey();
            String callerClass = clsOf.apply(caller);
            if (!KEEP.contains(callerClass)) continue;
            for (String callee : e.getValue()) {
                String calleeClass = clsOf.apply(callee);
                if (!KEEP.contains(calleeClass)) continue;
                if (callerClass.equals(calleeClass)) continue; // inter-classes only
                inter.computeIfAbsent(caller, k -> new LinkedHashSet<>()).add(callee);
            }
        }

        Map<String, Map<String, Set<String>>> byCallerClass = new LinkedHashMap<>();
        for (var e : inter.entrySet()) {
            String caller = e.getKey();
            String callerClass = clsOf.apply(caller);
            byCallerClass.computeIfAbsent(callerClass, k -> new LinkedHashMap<>())
                    .put(caller, e.getValue());
        }

        printGroup(byCallerClass, "org.analysis.codesource.ProduitElectronique",
                "ProduitElectronique (→ Produit)");
        printGroup(byCallerClass, "org.analysis.codesource.BoutiqueService",
                "BoutiqueService (→ Produit / ProduitElectronique)");

        // ---------- sous-graphe pour l'UI Swing ----------
        Map<String, Set<String>> sub = new LinkedHashMap<>();
        for (var e : inter.entrySet()) {
            String caller = e.getKey();
            for (String callee : e.getValue()) {
                sub.computeIfAbsent(caller, k -> new LinkedHashSet<>()).add(callee);
                sub.computeIfAbsent(callee, k -> new LinkedHashSet<>()); // assurer le nœud
            }
        }

        // ---------- ouvrir la fenêtre ----------
        org.analysis.gui.CallGraphSwing.show(sub);
    }

    private static void printGroup(Map<String, Map<String, Set<String>>> byCallerClass,
                                   String key, String title) {
        Map<String, Set<String>> m = byCallerClass.get(key);
        System.out.println();
        System.out.println(title);
        if (m == null || m.isEmpty()) {
            System.out.println("  (aucune arête)");
            return;
        }
        m.forEach((caller, callees) -> {
            String callerLabel = caller.substring(caller.lastIndexOf('.') + 1); // Classe#méth
            var targets = callees.stream()
                    .map(s -> s.substring(s.lastIndexOf('.') + 1)) // "Classe#meth"
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            System.out.println("  " + callerLabel + " →");
            System.out.println("    " + String.join(", ", targets));
        });
    }
}
