package org.analysis.processing.model;

import java.util.*;

public class ProjectStats {
    public final Map<String, ClassInfo> classes = new LinkedHashMap<>();
    public final List<MethodInfo> methods = new ArrayList<>();
    public final Set<String> packages = new LinkedHashSet<>();
    public int totalLOC = 0;

    // Graphe d’appel
    public final CallGraph callGraph = new CallGraph();

    public ClassInfo getOrCreate(String pkg, String cls){
        packages.add(pkg == null ? "" : pkg);
        return classes.computeIfAbsent(fullName(pkg, cls), k -> new ClassInfo(pkg, cls));
    }

    public static String fullName(String pkg, String cls){
        return (pkg == null || pkg.isBlank()) ? cls : pkg + "." + cls;
    }
}
