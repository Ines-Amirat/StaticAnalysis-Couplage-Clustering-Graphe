package org.analysis.visitors;

import org.analysis.processing.model.ProjectStats;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** Construit le graphe d’appel INTER-CLASSES (callerClass != calleeClass). */
public class CallGraphVisitor extends ASTVisitor {

    private final ProjectStats stats;
    private final PackageVisitor pkg;

    private final Deque<String> classStack = new ArrayDeque<>();
    private final Deque<String> methodStack = new ArrayDeque<>();

    public CallGraphVisitor(ProjectStats stats, PackageVisitor pkg) {
        this.stats = Objects.requireNonNull(stats);
        this.pkg = Objects.requireNonNull(pkg);
    }

    private String curClass()  { return classStack.isEmpty() ? null : classStack.peek(); }
    private String curMethod() { return methodStack.isEmpty() ? null : methodStack.peek(); }
    private static String sig(String classFqn, String method) {
        if (classFqn == null || method == null) return null;
        return classFqn + "#" + method;
    }

    /* ----- Contexte classe/méthode ----- */
    @Override public boolean visit(TypeDeclaration node) {
        classStack.push(ProjectStats.fullName(pkg.currentPackage, node.getName().getIdentifier()));
        return super.visit(node);
    }
    @Override public void endVisit(TypeDeclaration node) {
        if (!classStack.isEmpty()) classStack.pop();
    }

    @Override public boolean visit(MethodDeclaration node) {
        methodStack.push(node.getName().getIdentifier());
        String me = sig(curClass(), curMethod());
        if (me != null) stats.callGraph.addEdge(me, me); // crée le nœud
        return super.visit(node);
    }
    @Override public void endVisit(MethodDeclaration node) {
        if (!methodStack.isEmpty()) methodStack.pop();
    }

    /* ----- Appels "classiques" ----- */
    @Override public boolean visit(MethodInvocation node) {
        String caller = sig(curClass(), curMethod());
        if (caller == null) return true;

        IMethodBinding mb = node.resolveMethodBinding();
        if (mb != null && mb.getDeclaringClass() != null) {
            String calleeClass = mb.getDeclaringClass().getQualifiedName();
            String callee = sig(calleeClass, mb.getName());
            if (callee != null && calleeClass != null && !calleeClass.equals(curClass())) {
                stats.callGraph.addEdge(caller, callee); // only inter-classes
            }
        }
        return super.visit(node);
    }

    @Override public boolean visit(SuperMethodInvocation node) {
        String caller = sig(curClass(), curMethod());
        if (caller == null) return true;

        IMethodBinding mb = node.resolveMethodBinding();
        if (mb != null && mb.getDeclaringClass() != null) {
            String calleeClass = mb.getDeclaringClass().getQualifiedName();
            String callee = sig(calleeClass, mb.getName());
            if (callee != null && !calleeClass.equals(curClass())) {
                stats.callGraph.addEdge(caller, callee);
            }
        }
        return super.visit(node);
    }

    /* ----- Constructeurs ----- */
    @Override public boolean visit(ClassInstanceCreation node) {
        String caller = sig(curClass(), curMethod());
        if (caller == null) return super.visit(node);

        IMethodBinding mb = node.resolveConstructorBinding();
        if (mb != null && mb.getDeclaringClass() != null) {
            String calleeClass = mb.getDeclaringClass().getQualifiedName();
            String callee = sig(calleeClass, "<init>");
            if (callee != null && !calleeClass.equals(curClass())) {
                stats.callGraph.addEdge(caller, callee);
            }
        }
        return super.visit(node);
    }

    @Override public boolean visit(ConstructorInvocation node) {
        String caller = sig(curClass(), curMethod());
        if (caller == null) return super.visit(node);

        IMethodBinding mb = node.resolveConstructorBinding();
        if (mb != null && mb.getDeclaringClass() != null) {
            String calleeClass = mb.getDeclaringClass().getQualifiedName();
            String callee = sig(calleeClass, "<init>");
            if (callee != null && !calleeClass.equals(curClass())) {
                stats.callGraph.addEdge(caller, callee);
            }
        }
        return super.visit(node);
    }
}
