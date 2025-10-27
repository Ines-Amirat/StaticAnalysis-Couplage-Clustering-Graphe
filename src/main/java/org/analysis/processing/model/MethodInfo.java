package org.analysis.processing.model;

public class MethodInfo {
    public final String className;
    public final String methodName;
    public final int parameterCount;
    public final int loc;

    public MethodInfo(String className, String methodName, int parameterCount, int loc) {
        this.className = className;
        this.methodName = methodName;
        this.parameterCount = parameterCount;
        this.loc = loc;
    }
}
