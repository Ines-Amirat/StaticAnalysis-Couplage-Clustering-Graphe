package org.analysis.gui;

import org.analysis.parsing.SourceParser;
import org.analysis.processing.FileExplorer;
import org.analysis.processing.StatisticsService;
import org.analysis.processing.model.ProjectStats;

import java.nio.file.Path;

import static java.rmi.server.RemoteRef.packagePrefix;

public class AnalysisRunner {

    public record AnalysisResult(StatisticsService.Answers answers, ProjectStats stats) {}

    public static AnalysisResult analyze(String rootDir, int xThreshold, String includePrefix) {
        Path root = Path.of((rootDir == null || rootDir.isBlank()) ? "src/main/java" : rootDir);

        // (facultatif) tu peux garder cette ligne si tu l’utilises ailleurs
        var files = FileExplorer.listJavaFiles(root);

        SourceParser parser = new SourceParser();

        ProjectStats stats;
        try {
            // ⚠️ passer le bon paramètre includePrefix (et non packagePrefix)
            stats = parser.parseAll(root, includePrefix);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Échec d'analyse du dossier: " + root, e);
        }

        var answers = new StatisticsService().compute(stats, xThreshold);
        return new AnalysisResult(answers, stats);
    }

}
