package org.analysis;

import org.analysis.parsing.SourceParser;
import org.analysis.processing.FileExplorer;
import org.analysis.processing.ResultPrinter;
import org.analysis.processing.StatisticsService;
import org.analysis.processing.model.ProjectStats;

import java.nio.file.Path;
import java.util.List;

public class App {

    /**
     * Usage :
     *  - arg0 (optionnel) : chemin du dossier à analyser (défaut: "src/main/java")
     *  - arg1 (optionnel) : X pour "classes qui possèdent plus de X méthodes" (défaut: 3)
     *
     * Exemples :
     *   java -jar target/ton-jar.jar
     *   java -jar target/ton-jar.jar ./src/main/java 5
     */
    public static void main(String[] args) {
        try {
            Path root = Path.of(args.length > 0 ? args[0] : "src/main/java"); // au lieu de .../org/example/codebase

            int x = (args.length > 1) ? Integer.parseInt(args[1]) : 3;

            // 1) Lister les fichiers .java
            List<Path> javaFiles = FileExplorer.listJavaFiles(root);
            if (javaFiles.isEmpty()) {
                System.out.println("[INFO] Aucun fichier .java trouvé sous : " + root.toAbsolutePath());
                return;
            }

            // 2) Parser + collecter les infos
            SourceParser parser = new SourceParser();
            ProjectStats stats = parser.parseFiles(javaFiles);

            // 3) Calculer les 13 métriques
            StatisticsService.Answers answers = new StatisticsService().compute(stats, x);

            // 4) Afficher joliment
            ResultPrinter.print(answers);

        } catch (Exception e) {
            System.err.println("[ERREUR] " + e.getClass().getSimpleName() + " : " + e.getMessage());
            e.printStackTrace(System.err);
            System.err.println("\nUsage: App [dossier_source] [X]");
        }
    }
}
