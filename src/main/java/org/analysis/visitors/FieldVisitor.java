package org.analysis.visitors;

import org.eclipse.jdt.core.dom.*;
import org.analysis.processing.model.ProjectStats;
import org.analysis.processing.model.ClassInfo;

public class FieldVisitor extends ASTVisitor {
    private final ProjectStats stats;
    private final PackageVisitor pkgVisitor;

    public FieldVisitor(ProjectStats stats, PackageVisitor pkgVisitor){
        this.stats = stats; this.pkgVisitor = pkgVisitor;
    }

    @Override public boolean visit(FieldDeclaration node) {
        String cls = enclosingTypeSimpleName(node);
        String pkg = pkgVisitor.currentPackage;
        ClassInfo ci = stats.getOrCreate(pkg, cls);
        ci.fieldCount += node.fragments().size();
        return super.visit(node);
    }

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
            ASTNode p = acd.getParent();
            String base = "Anonymous";
            if (p instanceof ClassInstanceCreation cic) base = cic.getType().toString();
            return base + "$anon";
        }
        return "Unknown";
    }
}
