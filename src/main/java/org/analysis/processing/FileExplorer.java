package org.analysis.processing;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

/** Explore récursivement un dossier et récupère tous les fichiers .java. */
public class FileExplorer {

    public static List<Path> listJavaFiles(Path root) {
        if (root == null) throw new IllegalArgumentException("root is null");
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(p -> Files.isRegularFile(p))
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du parcours du dossier: " + root, e);
        }
    }
}
