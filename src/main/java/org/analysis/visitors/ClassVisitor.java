package org.analysis.visitors;

import org.eclipse.jdt.core.dom.*;
import org.analysis.processing.model.ProjectStats;
import org.analysis.processing.model.ClassInfo;

public class ClassVisitor extends ASTVisitor {
    private final ProjectStats stats;
    private final PackageVisitor pkgVisitor;

    public ClassVisitor(ProjectStats stats, PackageVisitor pkgVisitor){
        this.stats = stats;
        this.pkgVisitor = pkgVisitor;
    }

    @Override public boolean visit(TypeDeclaration node) {
        String pkg = pkgVisitor.currentPackage;
        String cls = node.getName().getIdentifier();
        ClassInfo ci = stats.getOrCreate(pkg, cls);
        ci.methodCount += node.getMethods().length;
        ci.fieldCount  += node.getFields().length;
        return super.visit(node);
    }
}
