package org.analysis.processing;

import org.analysis.processing.model.ClassInfo;
import org.analysis.processing.model.MethodInfo;
import org.analysis.processing.model.ProjectStats;

import java.util.*;
import java.util.stream.Collectors;

/** Calcule les 13 métriques demandées à partir du ProjectStats. */
public class StatisticsService {

    /** Conteneur immuable pour toutes les réponses. */
    public record Answers(
            int nbClasses,                        // 1
            int nbLOC,                            // 2
            int nbMethods,                        // 3
            int nbPackages,                       // 4
            double avgMethodsPerClass,            // 5
            double avgLocPerMethod,               // 6
            double avgFieldsPerClass,             // 7
            List<String> top10pctByMethods,       // 8
            List<String> top10pctByFields,        // 9
            List<String> intersectionTop10pct,    // 10
            List<String> classesMoreThanXMethods, // 11
            Map<String, List<String>> top10pctLongestMethodsPerClass, // 12
            int maxParameters                     // 13
    ) {}

    /** Calcule toutes les métriques. */
    public Answers compute(ProjectStats s, int xThreshold) {
        Objects.requireNonNull(s, "ProjectStats manquant");

        int nbClasses  = s.classes.size();          // 1
        int nbLOC      = s.totalLOC;                // 2
        int nbMethods  = s.methods.size();          // 3
        int nbPackages = s.packages.size();         // 4

        double avgMethodsPerClass = nbClasses == 0 ? 0.0 :
                s.classes.values().stream().mapToInt(c -> c.methodCount).average().orElse(0.0); // 5

        double avgLocPerMethod = nbMethods == 0 ? 0.0 :
                s.methods.stream().mapToInt(m -> m.loc).average().orElse(0.0);                   // 6

        double avgFieldsPerClass = nbClasses == 0 ? 0.0 :
                s.classes.values().stream().mapToInt(c -> c.fieldCount).average().orElse(0.0);  // 7

        // 8) Top 10% classes par # méthodes
        List<String> topByMethods = topPercent(
                s.classes.values().stream()
                        .sorted(Comparator.comparingInt((ClassInfo c) -> c.methodCount).reversed())
                        .map(ci -> ProjectStats.fullName(ci.packageName, ci.className))
                        .toList(),
                0.10
        );

        // 9) Top 10% classes par # attributs
        List<String> topByFields = topPercent(
                s.classes.values().stream()
                        .sorted(Comparator.comparingInt((ClassInfo c) -> c.fieldCount).reversed())
                        .map(ci -> ProjectStats.fullName(ci.packageName, ci.className))
                        .toList(),
                0.10
        );

        // 10) Intersection
        Set<String> inter = new LinkedHashSet<>(topByMethods);
        inter.retainAll(topByFields);

        // 11) Classes avec > X méthodes
        List<String> moreThanX = s.classes.values().stream()
                .filter(c -> c.methodCount > xThreshold)
                .map(ci -> ProjectStats.fullName(ci.packageName, ci.className))
                .sorted()
                .toList();

        // 12) Pour chaque classe: top 10% des méthodes par LOC
        Map<String, List<String>> longestPerClass = new LinkedHashMap<>();
        Map<String, List<MethodInfo>> byClass = s.methods.stream()
                .collect(Collectors.groupingBy(mi -> mi.className, LinkedHashMap::new, Collectors.toList()));
        for (var e : byClass.entrySet()) {
            List<MethodInfo> sorted = e.getValue().stream()
                    .sorted(Comparator.comparingInt((MethodInfo m) -> m.loc).reversed())
                    .toList();
            int k = Math.max(1, (int) Math.ceil(sorted.size() * 0.10));
            longestPerClass.put(e.getKey(),
                    sorted.subList(0, k).stream()
                            .map(m -> m.methodName + " (" + m.loc + " loc)")
                            .toList()
            );
        }

        // 13) Max parameters
        int maxParams = s.methods.stream().mapToInt(m -> m.parameterCount).max().orElse(0);

        return new Answers(
                nbClasses,
                nbLOC,
                nbMethods,
                nbPackages,
                round2(avgMethodsPerClass),
                round2(avgLocPerMethod),
                round2(avgFieldsPerClass),
                topByMethods,
                topByFields,
                new ArrayList<>(inter),
                moreThanX,
                longestPerClass,
                maxParams
        );
    }

    /* Helpers */

    private static List<String> topPercent(List<String> ordered, double p) {
        if (ordered == null || ordered.isEmpty()) return List.of();
        int k = Math.max(1, (int) Math.ceil(ordered.size() * p));
        return ordered.subList(0, k);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
