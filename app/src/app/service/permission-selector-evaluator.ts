/**
 * Simple selector evaluator for permission-based UI visibility.
 * 
 * This evaluates permission selectors like:
 * - PriceRow[currencyRef=='EUR']:write
 * - PriceRow[channelRefs hasAny('global-b2b-sales-channel')]:delete
 * 
 * Against actual object instances to determine if actions are permitted.
 */
export class PermissionSelectorEvaluator {
  
  /**
   * Evaluates if a permission (with optional selector) grants access to an object instance.
   * 
   * @param permissionName Full permission string (e.g., "priceprovider.admin:PriceRow[currencyRef=='EUR']:write")
   * @param obj The object instance to evaluate against
   * @returns true if permission grants access to this object
   */
  evaluate(permissionName: string, obj: any): boolean {
    // Parse permission: prefix:DataType[selector]?:action
    const match = permissionName.match(/^([^:]+):([^\[:]+)(?:\[([^\]]+)\])?:(.+)$/);
    if (!match) {
      console.warn('Invalid permission format:', permissionName);
      return false;
    }

    const [, prefix, dataType, selector, action] = match;
    
    // If no selector, permission grants global access
    if (!selector) {
      return true;
    }

    // Evaluate selector against object
    return this.evaluateSelector(selector, obj);
  }

  private evaluateSelector(selector: string, obj: any): boolean {
    // Handle OR at the top level
    if (selector.includes(' OR ')) {
      const parts = this.splitByTopLevelOperator(selector, ' OR ');
      return parts.some(part => this.evaluateSelector(part.trim(), obj));
    }

    // Handle AND at the top level
    if (selector.includes(' AND ')) {
      const parts = this.splitByTopLevelOperator(selector, ' AND ');
      return parts.every(part => this.evaluateSelector(part.trim(), obj));
    }

    // Handle NOT
    if (selector.trim().startsWith('NOT ')) {
      return !this.evaluateSelector(selector.trim().substring(4), obj);
    }

    // Remove parentheses
    if (selector.trim().startsWith('(') && selector.trim().endsWith(')')) {
      return this.evaluateSelector(selector.trim().slice(1, -1), obj);
    }

    // Evaluate single condition
    return this.evaluateCondition(selector, obj);
  }

  private evaluateCondition(condition: string, obj: any): boolean {
    // Handle hasAny operator
    const hasAnyMatch = condition.match(/^(\w+)\s+hasAny\s*\(([^)]+)\)$/);
    if (hasAnyMatch) {
      const [, field, valuesStr] = hasAnyMatch;
      const values = valuesStr.split(',').map(v => v.trim().replace(/^['"]|['"]$/g, ''));
      const fieldValue = obj[field];
      
      if (Array.isArray(fieldValue)) {
        return values.some(v => fieldValue.includes(v));
      }
      return values.includes(String(fieldValue || ''));
    }

    // Handle hasAll operator
    const hasAllMatch = condition.match(/^(\w+)\s+hasAll\s*\(([^)]+)\)$/);
    if (hasAllMatch) {
      const [, field, valuesStr] = hasAllMatch;
      const values = valuesStr.split(',').map(v => v.trim().replace(/^['"]|['"]$/g, ''));
      const fieldValue = obj[field];
      
      if (Array.isArray(fieldValue)) {
        return values.every(v => fieldValue.includes(v));
      }
      return false;
    }

    // Handle isEmpty operator
    const isEmptyMatch = condition.match(/^(\w+)\s+isEmpty$/);
    if (isEmptyMatch) {
      const [, field] = isEmptyMatch;
      const fieldValue = obj[field];
      return !fieldValue || (Array.isArray(fieldValue) && fieldValue.length === 0);
    }

    // Handle equals operator
    const equalsMatch = condition.match(/^(\w+)\s*==\s*['"]([^'"]+)['"]$/);
    if (equalsMatch) {
      const [, field, value] = equalsMatch;
      const fieldValue = obj[field];
      return String(fieldValue || '') === value;
    }

    // Handle not-equals operator
    const notEqualsMatch = condition.match(/^(\w+)\s*!=\s*['"]([^'"]+)['"]$/);
    if (notEqualsMatch) {
      const [, field, value] = notEqualsMatch;
      const fieldValue = obj[field];
      return String(fieldValue || '') !== value;
    }

    console.warn('Unknown condition format:', condition);
    return false;
  }

  /**
   * Splits a string by a top-level operator (not within parentheses).
   */
  private splitByTopLevelOperator(str: string, operator: string): string[] {
    const parts: string[] = [];
    let current = '';
    let depth = 0;
    let i = 0;

    while (i < str.length) {
      if (str[i] === '(') {
        depth++;
        current += str[i];
        i++;
      } else if (str[i] === ')') {
        depth--;
        current += str[i];
        i++;
      } else if (depth === 0 && str.substring(i, i + operator.length) === operator) {
        parts.push(current);
        current = '';
        i += operator.length;
      } else {
        current += str[i];
        i++;
      }
    }
    parts.push(current);
    return parts;
  }
}
