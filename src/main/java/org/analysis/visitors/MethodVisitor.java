package org.analysis.visitors;

import org.eclipse.jdt.core.dom.*;
import org.analysis.processing.model.MethodInfo;
import org.analysis.processing.model.ProjectStats;

public class MethodVisitor extends ASTVisitor {
    private final ProjectStats stats;
    private final PackageVisitor pkgVisitor;

    public MethodVisitor(ProjectStats stats, PackageVisitor pkgVisitor){
        this.stats = stats; this.pkgVisitor = pkgVisitor;
    }

    @Override public boolean visit(MethodDeclaration node) {
        String cls = enclosingTypeSimpleName(node);
        String pkg = pkgVisitor.currentPackage;

        int params = node.parameters().size();

        // LOC méthode (approx.)
        int loc = 0;
        if (node.getBody() != null) {
            String[] lines = node.getBody().toString().split("\\R");
            for (String l : lines) {
                String s = l.replaceAll("//.*", "").trim();
                if (!s.isEmpty()) loc++;
            }
        }
        stats.methods.add(new MethodInfo(ProjectStats.fullName(pkg, cls),
                node.getName().getIdentifier(), params, loc));
        return super.visit(node);
    }

    /** Récupère un nom de type robuste (Type, Enum, ou Anonyme). */
    private static String enclosingTypeSimpleName(ASTNode n){
        ASTNode t = n;
        while (t != null &&
                !(t instanceof TypeDeclaration) &&
                !(t instanceof EnumDeclaration) &&
                !(t instanceof AnonymousClassDeclaration)) {
            t = t.getParent();
        }
        if (t instanceof TypeDeclaration td) return td.getName().getIdentifier();
        if (t instanceof EnumDeclaration ed) return ed.getName().getIdentifier();
        if (t instanceof AnonymousClassDeclaration acd) {
            // Essayer de fabriquer un nom lisible à partir de "new X() { ... }"
            ASTNode p = acd.getParent();
            String base = "Anonymous";
            if (p instanceof ClassInstanceCreation cic) base = cic.getType().toString();
            return base + "$anon";
        }
        return "Unknown";
    }
}
