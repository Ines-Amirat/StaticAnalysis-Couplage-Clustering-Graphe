package org.analysis.processing.model;

public class ClassInfo {
    public final String packageName;
    public final String className;
    public int fieldCount = 0;
    public int methodCount = 0;

    public ClassInfo(String packageName, String className) {
        this.packageName = packageName;
        this.className = className;
    }
}
