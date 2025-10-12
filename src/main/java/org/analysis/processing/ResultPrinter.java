package org.analysis.processing;

/** Affiche proprement les résultats dans la console. */
public class ResultPrinter {

    public static void print(StatisticsService.Answers a) {
        System.out.println("=== Résultats de l'analyse ===");
        System.out.println("1)  # Classes                         : " + a.nbClasses());
        System.out.println("2)  # Lignes de code (LOC)            : " + a.nbLOC());
        System.out.println("3)  # Méthodes                        : " + a.nbMethods());
        System.out.println("4)  # Packages                        : " + a.nbPackages());
        System.out.println("5)  Moyenne méthodes / classe         : " + a.avgMethodsPerClass());
        System.out.println("6)  Moyenne LOC / méthode             : " + a.avgLocPerMethod());
        System.out.println("7)  Moyenne attributs / classe        : " + a.avgFieldsPerClass());
        System.out.println("8)  Top 10% classes (# méthodes)      : " + a.top10pctByMethods());
        System.out.println("9)  Top 10% classes (# attributs)     : " + a.top10pctByFields());
        System.out.println("10) Intersection (8 ∩ 9)              : " + a.intersectionTop10pct());
        System.out.println("11) Classes avec > X méthodes         : " + a.classesMoreThanXMethods());
        System.out.println("12) Top 10% méthodes par classe (LOC) :");
        a.top10pctLongestMethodsPerClass().forEach((cls, list) ->
                System.out.println("    - " + cls + " -> " + list));
        System.out.println("13) Max # paramètres d'une méthode    : " + a.maxParameters());
    }
}
