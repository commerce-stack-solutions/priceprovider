package io.commercestacksolutions.commons.permissionselector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a logical expression in a permission selector.
 * Supports composite expressions (AND/OR), negation (NOT), and leaf conditions.
 *
 * <p>Examples:
 * <ul>
 *   <li>Leaf: currencyRef == 'EUR'</li>
 *   <li>Composite: channelRefs hasAny('a','b') AND priceType == 'SALES_PRICE'</li>
 *   <li>Negated: NOT priceType == 'PURCHASE_PRICE'</li>
 *   <li>Grouped: (field1 == 'a' OR field2 == 'b') AND field3 == 'c'</li>
 * </ul>
 */
public class SelectorExpression {

    private final ExpressionType type;
    private final SelectorCondition condition;
    private final LogicalOperator logicalOperator;
    private final List<SelectorExpression> children;
    private final boolean negated;

    /**
     * Creates a leaf expression with a single condition.
     */
    public SelectorExpression(SelectorCondition condition) {
        this(condition, false);
    }

    /**
     * Creates a leaf expression with a single condition that can be negated.
     */
    public SelectorExpression(SelectorCondition condition, boolean negated) {
        this.type = ExpressionType.LEAF;
        this.condition = Objects.requireNonNull(condition, "Condition cannot be null");
        this.logicalOperator = null;
        this.children = Collections.emptyList();
        this.negated = negated;
    }

    /**
     * Creates a composite expression combining multiple child expressions.
     */
    public SelectorExpression(LogicalOperator logicalOperator, List<SelectorExpression> children) {
        this(logicalOperator, children, false);
    }

    /**
     * Creates a composite expression that can be negated.
     */
    public SelectorExpression(LogicalOperator logicalOperator, List<SelectorExpression> children, boolean negated) {
        this.type = ExpressionType.COMPOSITE;
        this.logicalOperator = Objects.requireNonNull(logicalOperator, "Logical operator cannot be null");
        this.children = new ArrayList<>(Objects.requireNonNull(children, "Children cannot be null"));
        if (this.children.isEmpty()) {
            throw new IllegalArgumentException("Children list cannot be empty for composite expression");
        }
        this.condition = null;
        this.negated = negated;
    }

    public ExpressionType getType() {
        return type;
    }

    public boolean isLeaf() {
        return type == ExpressionType.LEAF;
    }

    public boolean isNegated() {
        return negated;
    }

    public SelectorCondition getCondition() {
        if (!isLeaf()) {
            throw new IllegalStateException("Cannot get condition from composite expression");
        }
        return condition;
    }

    public LogicalOperator getLogicalOperator() {
        if (isLeaf()) {
            throw new IllegalStateException("Leaf expressions don't have logical operators");
        }
        return logicalOperator;
    }

    public List<SelectorExpression> getChildren() {
        if (isLeaf()) {
            throw new IllegalStateException("Leaf expressions don't have children");
        }
        return Collections.unmodifiableList(children);
    }

    @Override
    public String toString() {
        if (isLeaf()) {
            return (negated ? "NOT " : "") + condition.toString();
        }
        return (negated ? "NOT " : "") + "(" + logicalOperator + " " + children.toString() + ")";
    }

    /**
     * Type of expression node in the AST.
     */
    public enum ExpressionType {
        LEAF,
        COMPOSITE
    }

    /**
     * Logical operators for combining selector expressions.
     */
    public enum LogicalOperator {
        AND,
        OR
    }
}
