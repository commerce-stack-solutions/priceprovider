import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Validator for AppPermission names with optional selector syntax.
 *
 * Permission format: <prefix>:<DataType>[<selector>]:<Action>
 * Examples:
 * - priceprovider.admin:PriceRow:read
 * - priceprovider.admin:PriceRow[currencyRef=='EUR']:read
 * - priceprovider.public:PriceRow[groupRefs isEmpty]:read
 */
export function permissionSelectorValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;

    if (!value) {
      // Empty values are handled by Validators.required
      return null;
    }

    // Basic format check: <prefix>:<DataType>[<selector>]:<Action>
    // Prefix can be any string with dots
    const permissionPattern = /^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)+:[A-Z][a-zA-Z0-9]*(\[.+\])?:[a-z]+$/;

    if (!permissionPattern.test(value)) {
      return {
        permissionSelector: {
          message: 'Invalid permission format. Expected: prefix:DataType[selector]:action (e.g., priceprovider.admin:PriceRow[currencyRef==\'EUR\']:read)'
        }
      };
    }

    // If there's a selector part, validate its basic structure
    const selectorMatch = value.match(/\[(.+)\]/);
    if (selectorMatch) {
      const selector = selectorMatch[1];

      // Check for common syntax errors
      if (selector.includes('[') || selector.includes(']')) {
        return {
          permissionSelector: {
            message: 'Selector cannot contain nested brackets'
          }
        };
      }

      // Check for balanced parentheses
      let parenDepth = 0;
      for (const char of selector) {
        if (char === '(') parenDepth++;
        if (char === ')') parenDepth--;
        if (parenDepth < 0) {
          return {
            permissionSelector: {
              message: 'Unmatched closing parenthesis in selector'
            }
          };
        }
      }
      if (parenDepth !== 0) {
        return {
          permissionSelector: {
            message: 'Unmatched opening parenthesis in selector'
          }
        };
      }

      // Check for valid operators
      const validOperators = ['==', '!=', 'hasAny', 'hasAll', 'isEmpty', 'AND', 'OR', 'NOT'];
      let hasValidOperator = false;
      for (const op of validOperators) {
        if (selector.includes(op)) {
          hasValidOperator = true;
          break;
        }
      }

      if (!hasValidOperator) {
        return {
          permissionSelector: {
            message: 'Selector must contain at least one valid operator (==, !=, hasAny, hasAll, isEmpty, AND, OR, NOT)'
          }
        };
      }

      // Check for incomplete expressions (operators at the end)
      const trimmed = selector.trim();
      if (trimmed.endsWith('AND') || trimmed.endsWith('OR') || trimmed.endsWith('NOT') ||
          trimmed.endsWith('==') || trimmed.endsWith('!=')) {
        return {
          permissionSelector: {
            message: 'Selector expression is incomplete (operator at the end)'
          }
        };
      }

      // Check for triple equals (common mistake)
      if (selector.includes('===')) {
        return {
          permissionSelector: {
            message: 'Invalid operator ===. Use == for equality checks'
          }
        };
      }
    }

    return null;
  };
}
