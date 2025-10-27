package org.analysis.parsing;

import org.analysis.processing.model.ProjectStats;
import org.analysis.visitors.CallGraphVisitor;
import org.analysis.visitors.ClassVisitor;
import org.analysis.visitors.PackageVisitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class SourceParser {

    /** Parcourt tout un dossier (avec filtre de package optionnel). */
    public ProjectStats parseAll(Path root, String packagePrefix) throws IOException {
        ProjectStats stats = new ProjectStats();
        try (var stream = Files.walk(root)) {
            for (Path p : (Iterable<Path>) stream.filter(f -> f.toString().endsWith(".java"))::iterator) {
                parseOneFileInto(stats, root, p, packagePrefix);
            }
        }
        return stats;
    }

    /** Compatibilité avec AnalysisRunner : liste précise de fichiers. */
    public ProjectStats parseFiles(List<Path> javaFiles) throws IOException {
        return parseFiles(javaFiles, null);
    }

    public ProjectStats parseFiles(List<Path> javaFiles, String packagePrefix) throws IOException {
        if (javaFiles == null || javaFiles.isEmpty()) return new ProjectStats();
        // racine déduite du premier fichier
        Path root = javaFiles.get(0).toAbsolutePath().getParent();
        while (root != null && !root.getFileName().toString().equals("java")) {
            root = root.getParent();
        }
        if (root == null) root = Path.of("src/main/java").toAbsolutePath();

        ProjectStats stats = new ProjectStats();
        for (Path p : javaFiles) {
            if (p == null || !p.toString().endsWith(".java")) continue;
            parseOneFileInto(stats, root, p, packagePrefix);
        }
        return stats;
    }

    /* -------------------- core -------------------- */

    private void parseOneFileInto(ProjectStats stats, Path projectSourceRoot, Path file, String packagePrefix) throws IOException {
        String src = Files.readString(file, StandardCharsets.UTF_8);

        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(src.toCharArray());

        // ✅ Options de compilation (Java 21 OK, ou ajuste si besoin)
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_17, options);
        parser.setCompilerOptions(options);

        // ✅ Très important pour que JDT sache où résoudre les types/méthodes
        //    - classpath minimal (vide ici)
        //    - sourcepath = racine du code source (ex: .../src/main/java)
        //    - encodings
        //    - includeRunningVMBootclasspath = true (donc JRE/JDK courants visibles)
        String[] classpathEntries = new String[] { /* vide => on résout au moins la JRE */ };
        String[] sourcepathEntries = new String[] { projectSourceRoot.toAbsolutePath().toString() };
        String[] encodings = new String[] { "UTF-8" };
        parser.setEnvironment(classpathEntries, sourcepathEntries, encodings, /* includeRunningVMBootclasspath */ true);

        // ✅ Un nom d'unité est requis quand on fournit un environment
        parser.setUnitName(file.getFileName().toString());

        // ✅ Activer bindings + recoveries
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setStatementsRecovery(true);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        // Filtrage package (si demandé)
        var pkg = cu.getPackage();
        String pkgName = (pkg == null || pkg.getName() == null) ? "" : pkg.getName().getFullyQualifiedName();
        if (packagePrefix != null && !packagePrefix.isBlank() && !pkgName.startsWith(packagePrefix)) {
            return;
        }

        PackageVisitor pv = new PackageVisitor(stats);
        cu.accept(pv);
        cu.accept(new ClassVisitor(stats, pv));
        cu.accept(new CallGraphVisitor(stats, pv));
    }
}
