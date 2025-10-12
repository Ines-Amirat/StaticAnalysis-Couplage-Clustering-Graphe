package org.analysis.gui;

import org.analysis.parsing.SourceParser;
import org.analysis.processing.FileExplorer;
import org.analysis.processing.StatisticsService;
import org.analysis.processing.model.ProjectStats;

import java.nio.file.Path;

public class AnalysisRunner {

    public record AnalysisResult(StatisticsService.Answers answers, ProjectStats stats) {}

    public static AnalysisResult analyze(String rootDir, int xThreshold, String includePrefix) {
        Path root = Path.of((rootDir == null || rootDir.isBlank()) ? "src/main/java" : rootDir);

        var files = FileExplorer.listJavaFiles(root);
        var parser = new SourceParser();
        ProjectStats stats = (includePrefix == null || includePrefix.isBlank())
                ? parser.parseFiles(files)
                : parser.parseFilesFiltered(files, includePrefix);

        var answers = new StatisticsService().compute(stats, xThreshold);
        return new AnalysisResult(answers, stats);
    }
}
