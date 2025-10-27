package org.analysis.processing.model;

import java.util.*;

/**
 * Représente un graphe d'appel (liste d'adjacence).
 * Chaque nœud est une méthode "pkg.Classe#methode".
 */
public class CallGraph {
    // clé = méthode appelante, valeur = ensemble de méthodes appelées
    private final Map<String, Set<String>> adj = new LinkedHashMap<>();

    public void addEdge(String caller, String callee){
        if(caller == null || callee == null || caller.isBlank() || callee.isBlank()) return;
        adj.computeIfAbsent(caller, k -> new LinkedHashSet<>()).add(callee);
        adj.putIfAbsent(callee, new LinkedHashSet<>()); // assure que le callee existe
    }

    public Map<String, Set<String>> asMap(){
        return Collections.unmodifiableMap(adj);
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        adj.forEach((k,v) -> sb.append(k).append(" -> ").append(v).append('\n'));
        return sb.toString();
    }
}
