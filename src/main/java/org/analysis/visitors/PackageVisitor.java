package org.analysis.visitors;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.analysis.processing.model.ProjectStats;

public class PackageVisitor extends ASTVisitor {
    private final ProjectStats stats;
    public String currentPackage = "";

    public PackageVisitor(ProjectStats stats){ this.stats = stats; }

    @Override public boolean visit(PackageDeclaration node) {
        currentPackage = node.getName().getFullyQualifiedName();
        stats.packages.add(currentPackage);
        return super.visit(node);
    }
}
