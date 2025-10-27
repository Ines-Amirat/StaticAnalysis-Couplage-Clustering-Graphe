package org.analysis.parsing;

import org.analysis.processing.model.ProjectStats;
import org.analysis.processing.model.CallGraph;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;

/**
 * Parser basé sur Spoon.
 * - Construit un call-graph méthode->méthode (signature "fqcn#method").
 * - Renseigne ProjectStats et ALIMENTE CallGraph sans présumer de son API
 *   (utilise des tentatives via réflexion : addEdge/add/asMap).
 */
public class SpoonSourceParser {

    /** Analyse tout le dossier 'root' en filtrant par 'pkgPrefix' (si non vide). */
    public ProjectStats parseAll(Path root, String pkgPrefix) {
        // 1) Construire le modèle Spoon
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setNoClasspath(true);      // tolère les deps manquantes
        launcher.getEnvironment().setComplianceLevel(17);    // adapte si besoin (11/17/21)
        launcher.addInputResource(root.toString());
        launcher.buildModel();

        CtModel model = launcher.getModel();

        // 2) Collecte edges "callerSig" -> {calleeSig}
        final Map<String, Set<String>> edges = new LinkedHashMap<>();
        final Set<String> classes = new LinkedHashSet<>();

        CtScanner scanner = new CtScanner() {
            String currentCallerSig = null;   // "fqcn#method"
            String currentCallerClass = null; // "fqcn"

            @Override
            public <T> void visitCtMethod(CtMethod<T> m) {
                CtType<?> parent = m.getParent(CtType.class);
                if (parent == null) return;

                String classFqn = parent.getQualifiedName();
                if (!acceptPkg(classFqn, pkgPrefix)) return;

                String sig = classFqn + "#" + m.getSimpleName();
                currentCallerSig = sig;
                currentCallerClass = classFqn;
                classes.add(classFqn);

                edges.computeIfAbsent(sig, k -> new LinkedHashSet<>());

                super.visitCtMethod(m);

                currentCallerSig = null;
                currentCallerClass = null;
            }

            @Override
            public <T> void visitCtConstructor(CtConstructor<T> c) {
                CtType<?> parent = c.getParent(CtType.class);
                if (parent == null) return;

                String classFqn = parent.getQualifiedName();
                if (!acceptPkg(classFqn, pkgPrefix)) return;

                // on nomme le constructeur "Class#Class"
                String sig = classFqn + "#" + parent.getSimpleName();
                currentCallerSig = sig;
                currentCallerClass = classFqn;
                classes.add(classFqn);

                edges.computeIfAbsent(sig, k -> new LinkedHashSet<>());

                super.visitCtConstructor(c);

                currentCallerSig = null;
                currentCallerClass = null;
            }

            @Override
            public <T> void visitCtInvocation(CtInvocation<T> inv) {
                if (currentCallerSig == null) {
                    super.visitCtInvocation(inv);
                    return;
                }

                CtExecutableReference<?> exec = inv.getExecutable();
                String calleeFqn  = safeDeclaringType(exec, inv);
                String calleeName = (exec != null && exec.getSimpleName() != null)
                        ? exec.getSimpleName()
                        : "unknown";

                if (calleeFqn != null && acceptPkg(calleeFqn, pkgPrefix)) {
                    String calleeSig = calleeFqn + "#" + calleeName;
                    edges.computeIfAbsent(currentCallerSig, k -> new LinkedHashSet<>()).add(calleeSig);
                    classes.add(calleeFqn);
                }

                super.visitCtInvocation(inv);
            }

            private boolean acceptPkg(String fqn, String prefix) {
                return (prefix == null || prefix.isBlank() || fqn.startsWith(prefix));
            }

            /**
             * Récupère le FQCN du type déclarant de la méthode appelée.
             * Stratégie :
             *  1) exec.getDeclaringType()
             *  2) exec.getDeclaration() castée en CtMethod/CtConstructor/CtTypeMember -> getDeclaringType()
             *  3) fallback : type de la cible inv.getTarget().getType()
             */
            private String safeDeclaringType(CtExecutableReference<?> exec, CtInvocation<?> inv) {
                if (exec == null) return null;

                // 1) Référence
                CtTypeReference<?> tref = exec.getDeclaringType();
                if (tref != null && tref.getQualifiedName() != null) {
                    return tref.getQualifiedName();
                }

                // 2) Déclaration
                try {
                    CtExecutable<?> decl = exec.getDeclaration();
                    if (decl instanceof CtMethod<?> m && m.getDeclaringType() != null) {
                        return m.getDeclaringType().getQualifiedName();
                    } else if (decl instanceof CtConstructor<?> k && k.getDeclaringType() != null) {
                        return k.getDeclaringType().getQualifiedName();
                    } else if (decl instanceof CtTypeMember tm && tm.getDeclaringType() != null) {
                        return tm.getDeclaringType().getQualifiedName();
                    }
                } catch (Exception ignore) {
                    // résolution absente → on tente le fallback
                }

                // 3) Fallback : type de la cible (obj.m())
                try {
                    if (inv != null && inv.getTarget() != null && inv.getTarget().getType() != null) {
                        String qn = inv.getTarget().getType().getQualifiedName();
                        if (qn != null) return qn;
                    }
                } catch (Exception ignore) {}

                return null;
            }
        };

        // Parcours du modèle (au lieu de model.processWith(...))
        model.getRootPackage().accept(scanner);

        // 3) Remplir ProjectStats et alimenter le CallGraph existant
        ProjectStats stats = new ProjectStats();

        CallGraph cg = obtainCallGraph(stats);
        if (cg == null) {
            // Dernier recours : on tente d'en créer un vide si constructeur no-arg dispo
            cg = tryInstantiateEmptyCallGraph();
            if (cg != null) setCallGraph(stats, cg);
        }

        // Si malgré tout on n'a pas de CallGraph, on retourne stats "vides"
        if (cg == null) {
            return stats;
        }

        // Alimenter le call graph : on tente addEdge / add / asMap
        for (var e : edges.entrySet()) {
            String caller = e.getKey();
            for (String callee : e.getValue()) {
                if (!addEdgeReflective(cg, caller, callee)) {
                    // fallback : si asMap existe, on pousse directement dans la Map
                    Map<String, Set<String>> map = tryAsMap(cg);
                    if (map != null) {
                        map.computeIfAbsent(caller, k -> new LinkedHashSet<>()).add(callee);
                    }
                }
            }
        }

        // (optionnel) renseigner un compteur de classes s'il existe
        try {
            Field f = ProjectStats.class.getDeclaredField("classCount");
            f.setAccessible(true);
            f.set(stats, classes.size());
        } catch (Exception ignore) {}

        return stats;
    }

    /* ===================== Helpers CallGraph via réflexion ===================== */

    private CallGraph obtainCallGraph(ProjectStats stats) {
        try {
            // champ direct
            Field f = ProjectStats.class.getDeclaredField("callGraph");
            f.setAccessible(true);
            Object obj = f.get(stats);
            if (obj instanceof CallGraph) return (CallGraph) obj;
        } catch (Exception ignore) {}

        try {
            // getter éventuel
            Method m = ProjectStats.class.getMethod("getCallGraph");
            Object obj = m.invoke(stats);
            if (obj instanceof CallGraph) return (CallGraph) obj;
        } catch (Exception ignore) {}

        return null;
    }

    private void setCallGraph(ProjectStats stats, CallGraph cg) {
        try {
            Field f = ProjectStats.class.getDeclaredField("callGraph");
            f.setAccessible(true);
            f.set(stats, cg);
        } catch (Exception ignore) {
            // si on ne peut pas setter, on fait au mieux avec obtainCallGraph()
        }
    }

    private CallGraph tryInstantiateEmptyCallGraph() {
        try {
            return CallGraph.class.getDeclaredConstructor().newInstance();
        } catch (Exception ignore) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Set<String>> tryAsMap(CallGraph cg) {
        try {
            Method m = cg.getClass().getMethod("asMap");
            Object res = m.invoke(cg);
            if (res instanceof Map) return (Map<String, Set<String>>) res;
        } catch (Exception ignore) {}
        return null;
    }

    /** Tente d'appeler addEdge(caller, callee) ou add(caller, callee). */
    private boolean addEdgeReflective(CallGraph cg, String caller, String callee) {
        try {
            Method m = findMethod(cg.getClass(), "addEdge", String.class, String.class);
            if (m != null) {
                m.invoke(cg, caller, callee);
                return true;
            }
        } catch (Exception ignore) {}

        try {
            Method m = findMethod(cg.getClass(), "add", String.class, String.class);
            if (m != null) {
                m.invoke(cg, caller, callee);
                return true;
            }
        } catch (Exception ignore) {}

        return false;
    }

    private Method findMethod(Class<?> type, String name, Class<?>... params) {
        try {
            Method m = type.getMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (Exception ignore) {
            return null;
        }
    }
}
