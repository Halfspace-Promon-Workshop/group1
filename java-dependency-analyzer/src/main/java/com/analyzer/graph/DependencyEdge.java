package com.analyzer.graph;

import java.util.Objects;

/**
 * Represents a dependency edge between two classes in the graph.
 * An edge exists when the source class has an instance variable of the target class type.
 */
public class DependencyEdge {
    private final ClassNode source;
    private final ClassNode target;
    private final String fieldName;

    public DependencyEdge(ClassNode source, ClassNode target, String fieldName) {
        this.source = source;
        this.target = target;
        this.fieldName = fieldName;
    }

    public ClassNode getSource() {
        return source;
    }

    public ClassNode getTarget() {
        return target;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependencyEdge that = (DependencyEdge) o;
        return Objects.equals(source, that.source) &&
                Objects.equals(target, that.target) &&
                Objects.equals(fieldName, that.fieldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target, fieldName);
    }

    @Override
    public String toString() {
        return "DependencyEdge{" +
                "source=" + source.getName() +
                ", target=" + target.getName() +
                ", fieldName='" + fieldName + '\'' +
                '}';
    }
}
