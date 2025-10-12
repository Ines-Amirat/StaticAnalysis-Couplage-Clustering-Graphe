package org.analysis.visitors;

import org.eclipse.jdt.core.dom.*;
import org.analysis.processing.model.ProjectStats;

import java.util.ArrayDeque;
import java.util.Deque;

/** Construit le graphe d'appel en gérant aussi les classes anonymes. */
public class CallGraphVisitor extends ASTVisitor {
    private final ProjectStats stats;
    private final PackageVisitor pkgVisitor;

    private final Deque<String> classStack = new ArrayDeque<>();
    private final Deque<String> methodStack = new ArrayDeque<>();

    public CallGraphVisitor(ProjectStats stats, PackageVisitor pkgVisitor) {
        this.stats = stats;
        this.pkgVisitor = pkgVisitor;
    }

    private String fq(String simpleClass){ return ProjectStats.fullName(pkgVisitor.currentPackage, simpleClass); }
    private String curClass(){ return classStack.isEmpty() ? "" : classStack.peek(); }
    private String curMethod(){ return methodStack.isEmpty() ? "" : methodStack.peek(); }
    private static String sig(String fqCls, String m){ return (fqCls==null||fqCls.isBlank()) ? m : fqCls + "#" + m; }

    /* ==== Contexte classe/méthode ==== */

    @Override public boolean visit(TypeDeclaration node) {
        classStack.push(fq(node.getName().getIdentifier()));
        return true;
    }
    @Override public void endVisit(TypeDeclaration node) { classStack.pop(); }

    @Override public boolean visit(EnumDeclaration node) {
        classStack.push(fq(node.getName().getIdentifier()));
        return true;
    }
    @Override public void endVisit(EnumDeclaration node) { classStack.pop(); }

    @Override public boolean visit(AnonymousClassDeclaration node) {
        // Nom synthétique basé sur le type instancié : new Foo() { ... } -> Foo$anon
        String name = "Anonymous$anon";
        ASTNode p = node.getParent();
        if (p instanceof ClassInstanceCreation cic) name = cic.getType().toString() + "$anon";
        classStack.push(fq(name));
        return true;
    }
    @Override public void endVisit(AnonymousClassDeclaration node) { classStack.pop(); }

    @Override public boolean visit(MethodDeclaration node) {
        methodStack.push(node.getName().getIdentifier());
        return true;
    }
    @Override public void endVisit(MethodDeclaration node) { methodStack.pop(); }

    /* ==== Collecte des appels ==== */

    @Override public boolean visit(MethodInvocation node) {
        String caller = sig(curClass(), curMethod());

        // si qualifié (Foo.bar()) essayer de déduire la classe cible
        String calleeOwner = curClass();
        Expression expr = node.getExpression();
        if (expr instanceof SimpleName sn) {
            calleeOwner = fq(sn.getIdentifier());
        } else if (expr instanceof ThisExpression) {
            calleeOwner = curClass();
        } // sinon, appel non qualifié: même classe

        String callee = sig(calleeOwner, node.getName().getIdentifier());
        stats.callGraph.addEdge(caller, callee);
        return true;
    }

    @Override public boolean visit(SuperMethodInvocation node) {
        String caller = sig(curClass(), curMethod());
        String callee = sig(curClass(), node.getName().getIdentifier());
        stats.callGraph.addEdge(caller, callee);
        return true;
    }

    @Override public boolean visit(ClassInstanceCreation node) {
        String caller = sig(curClass(), curMethod());
        String callee = sig(fq(node.getType().toString()), "<init>");
        stats.callGraph.addEdge(caller, callee);
        return true;
    }

    @Override public boolean visit(ConstructorInvocation node) {
        String caller = sig(curClass(), curMethod());
        stats.callGraph.addEdge(caller, sig(curClass(), "<init>"));
        return true;
    }
}
