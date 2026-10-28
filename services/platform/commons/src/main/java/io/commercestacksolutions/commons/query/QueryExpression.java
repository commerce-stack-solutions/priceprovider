package io.commercestacksolutions.commons.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a logical expression in a query that can combine multiple filters
 * using AND, OR, and NOT operators, with support for parenthesized groups.
 */
public class QueryExpression {
    
    private final LogicalOperator logicalOperator;
    private final List<QueryExpression> children;
    private final QueryFilter filter;
    private final boolean negated;
    
    /**
     * Creates a leaf expression with a single filter.
     */
    public QueryExpression(QueryFilter filter) {
        this(filter, false);
    }
    
    /**
     * Creates a leaf expression with a single filter that can be negated.
     */
    public QueryExpression(QueryFilter filter, boolean negated) {
        this.filter = Objects.requireNonNull(filter, "Filter cannot be null");
        this.logicalOperator = null;
        this.children = List.of();
        this.negated = negated;
    }
    
    /**
     * Creates a composite expression combining multiple child expressions.
     */
    public QueryExpression(LogicalOperator logicalOperator, List<QueryExpression> children) {
        this(logicalOperator, children, false);
    }
    
    /**
     * Creates a composite expression that can be negated.
     */
    public QueryExpression(LogicalOperator logicalOperator, List<QueryExpression> children, boolean negated) {
        this.logicalOperator = Objects.requireNonNull(logicalOperator, "Logical operator cannot be null");
        this.children = new ArrayList<>(Objects.requireNonNull(children, "Children cannot be null"));
        if (this.children.isEmpty()) {
            throw new IllegalArgumentException("Children list cannot be empty for composite expression");
        }
        this.filter = null;
        this.negated = negated;
    }
    
    /**
     * Returns true if this is a leaf expression (contains a single filter).
     */
    public boolean isLeaf() {
        return filter != null;
    }
    
    /**
     * Returns true if this expression is negated (NOT).
     */
    public boolean isNegated() {
        return negated;
    }
    
    public QueryFilter getFilter() {
        if (!isLeaf()) {
            throw new IllegalStateException("Cannot get filter from composite expression");
        }
        return filter;
    }
    
    public LogicalOperator getLogicalOperator() {
        if (isLeaf()) {
            throw new IllegalStateException("Leaf expressions don't have logical operators");
        }
        return logicalOperator;
    }
    
    public List<QueryExpression> getChildren() {
        if (isLeaf()) {
            throw new IllegalStateException("Leaf expressions don't have children");
        }
        return children;
    }
    
    @Override
    public String toString() {
        if (isLeaf()) {
            return (negated ? "NOT " : "") + filter.toString();
        }
        return (negated ? "NOT " : "") + logicalOperator + children.toString();
    }
    
    /**
     * Logical operators for combining query expressions.
     */
    public enum LogicalOperator {
        AND,
        OR
    }
}
