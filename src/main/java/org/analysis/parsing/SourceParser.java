package org.analysis.parsing;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.analysis.processing.model.ProjectStats;
import org.analysis.visitors.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SourceParser {

    /** Analyse tous les fichiers fournis (sans filtre de package). */
    public ProjectStats parseFiles(List<Path> javaFiles) {
        ProjectStats stats = new ProjectStats();
        for (Path f : javaFiles) {
            String src = read(f);
            stats.totalLOC += countLOC(src);

            ASTParser parser = ASTParser.newParser(AST.JLS21);
            parser.setSource(src.toCharArray());
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setCompilerOptions(JavaCore.getOptions());

            CompilationUnit cu = (CompilationUnit) parser.createAST(null);

            PackageVisitor pv = new PackageVisitor(stats);
            cu.accept(pv);                                    // 1) package d'abord
            cu.accept(new ClassVisitor(stats, pv));
            cu.accept(new FieldVisitor(stats, pv));
            cu.accept(new MethodVisitor(stats, pv));
            cu.accept(new CallGraphVisitor(stats, pv));       // 2) graphe d’appel
        }
        return stats;
    }

    /** Analyse uniquement les unités dont le package matche un préfixe. */
    public ProjectStats parseFilesFiltered(List<Path> javaFiles, String includePackagePrefix) {
        ProjectStats stats = new ProjectStats();
        String prefix = includePackagePrefix == null ? "" : includePackagePrefix.trim();

        for (Path f : javaFiles) {
            String src = read(f);

            ASTParser parser = ASTParser.newParser(AST.JLS21);
            parser.setSource(src.toCharArray());
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setCompilerOptions(JavaCore.getOptions());

            CompilationUnit cu = (CompilationUnit) parser.createAST(null);

            PackageVisitor pv = new PackageVisitor(stats);
            cu.accept(pv);
            String pkg = (pv.currentPackage == null) ? "" : pv.currentPackage;

            if (!prefix.isEmpty() && !pkg.startsWith(prefix)) {
                continue;
            }

            stats.totalLOC += countLOC(src);
            cu.accept(new ClassVisitor(stats, pv));
            cu.accept(new FieldVisitor(stats, pv));
            cu.accept(new MethodVisitor(stats, pv));
            cu.accept(new CallGraphVisitor(stats, pv));       // AJOUT dans filtered
        }
        return stats;
    }

    /* ===== Helpers ===== */

    private static String read(Path p) {
        try { return Files.readString(p); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /** Compte grossièrement les LOC non vides (ignore // commentaires). */
    private static int countLOC(String src) {
        int loc = 0;
        for (String line : src.split("\\R")) {
            String s = line.replaceAll("//.*", "").trim();
            if (!s.isEmpty()) loc++;
        }
        return loc;
    }
}
