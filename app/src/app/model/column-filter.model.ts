/**
 * Column filter models for table filtering functionality
 */

export type FilterOperator = 
  | 'equals' 
  | 'contains' 
  | 'gt' 
  | 'lt' 
  | 'gte' 
  | 'lte' 
  | 'range' 
  | 'exists'
  | 'hasAny'
  | 'hasAll';

export type FieldType = 
  | 'string' 
  | 'boolean' 
  | 'number' 
  | 'date' 
  | 'datetime' 
  | 'reference' 
  | 'collection';

export interface FilterDefinition {
  field: string;
  operator: FilterOperator;
  value?: any;
  valueFrom?: any;
  valueTo?: any;
}

export interface ColumnFilterConfig {
  field: string;
  type: FieldType;
  label: string;
  allowedOperators?: FilterOperator[];
}

/**
 * Get allowed operators based on field type
 */
export function getAllowedOperators(type: FieldType): FilterOperator[] {
  switch (type) {
    case 'string':
      return ['equals', 'contains', 'exists'];
    case 'boolean':
      return ['equals', 'exists'];
    case 'number':
    case 'date':
    case 'datetime':
      return ['equals', 'gt', 'lt', 'gte', 'lte', 'range', 'exists'];
    case 'reference':
      return ['equals', 'exists'];
    case 'collection':
      return ['hasAny', 'hasAll', 'exists'];
    default:
      return ['equals', 'exists'];
  }
}

/**
 * Build Lucene-like query string from filter definitions
 */
export function buildQueryString(filters: FilterDefinition[]): string {
  if (filters.length === 0) {
    return '';
  }

  const queryParts = filters.map(filter => {
    const field = filter.field;
    
    switch (filter.operator) {
      case 'equals':
        return `${field}:${filter.value}`;
      case 'contains':
        return `${field}:*${filter.value}*`;
      case 'gt':
        return `${field}:>${filter.value}`;
      case 'lt':
        return `${field}:<${filter.value}`;
      case 'gte':
        return `${field}:>=${filter.value}`;
      case 'lte':
        return `${field}:<=${filter.value}`;
      case 'range': {
        const from = filter.valueFrom !== undefined && filter.valueFrom !== '' ? filter.valueFrom : '*';
        const to = filter.valueTo !== undefined && filter.valueTo !== '' ? filter.valueTo : '*';
        return `${field}:[${from} TO ${to}]`;
      }
      case 'exists':
        return `${field}.exists:${filter.value}`;
      case 'hasAny': {
        const ids = normalizeMultiValue(filter.value);
        return ids.length > 0 ? `${field}.hasAny:(${ids.join(',')})` : '';
      }
      case 'hasAll': {
        const ids = normalizeMultiValue(filter.value);
        return ids.length > 0 ? `${field}.hasAll:(${ids.join(',')})` : '';
      }
      default:
        return '';
    }
  }).filter(part => part !== '');

  return queryParts.join(' AND ');
}

/**
 * Normalise a multi-value input (comma-separated string or array) into a trimmed, non-empty string array.
 */
function normalizeMultiValue(value: any): string[] {
  if (!value) return [];
  const raw: string = Array.isArray(value) ? value.join(',') : String(value);
  return raw.split(',').map(v => v.trim()).filter(v => v.length > 0);
}

/**
 * Parse query string back into filter definitions
 */
export function parseQueryString(queryString: string): FilterDefinition[] {
  if (!queryString || queryString.trim() === '') {
    return [];
  }

  const filters: FilterDefinition[] = [];
  
  // Split by AND – but skip splits that are inside parentheses (hasAny/hasAll values)
  const parts = splitByAndOutsideParens(queryString);
  
  for (const part of parts) {
    const trimmed = part.trim();

    // Check for hasAny operator: field.hasAny:(val1,val2)
    const hasAnyMatch = trimmed.match(/^(.+)\.hasAny:\((.+)\)$/);
    if (hasAnyMatch) {
      filters.push({ field: hasAnyMatch[1], operator: 'hasAny', value: hasAnyMatch[2] });
      continue;
    }

    // Check for hasAll operator: field.hasAll:(val1,val2)
    const hasAllMatch = trimmed.match(/^(.+)\.hasAll:\((.+)\)$/);
    if (hasAllMatch) {
      filters.push({ field: hasAllMatch[1], operator: 'hasAll', value: hasAllMatch[2] });
      continue;
    }
    
    // Check for exists operator
    if (trimmed.includes('.exists:')) {
      const [field, value] = trimmed.split('.exists:');
      filters.push({
        field: field.replace('.exists', ''),
        operator: 'exists',
        value: value === 'true'
      });
      continue;
    }
    
    // Check for range operator
    if (trimmed.includes(':[')) {
      const match = trimmed.match(/^(.+):\[(.+) TO (.+)\]$/);
      if (match) {
        const [, field, from, to] = match;
        filters.push({
          field,
          operator: 'range',
          valueFrom: from === '*' ? '' : from,
          valueTo: to === '*' ? '' : to
        });
        continue;
      }
    }
    
    // Check for comparison operators
    if (trimmed.includes(':>=')) {
      const [field, value] = trimmed.split(':>=');
      filters.push({ field, operator: 'gte', value });
      continue;
    }
    if (trimmed.includes(':<=')) {
      const [field, value] = trimmed.split(':<=');
      filters.push({ field, operator: 'lte', value });
      continue;
    }
    if (trimmed.includes(':>')) {
      const [field, value] = trimmed.split(':>');
      filters.push({ field, operator: 'gt', value });
      continue;
    }
    if (trimmed.includes(':<')) {
      const [field, value] = trimmed.split(':<');
      filters.push({ field, operator: 'lt', value });
      continue;
    }
    
    // Check for contains operator (wildcards)
    if (trimmed.includes(':*') || trimmed.includes('*:')) {
      const [field, value] = trimmed.split(':');
      filters.push({
        field,
        operator: 'contains',
        value: value.replace(/\*/g, '')
      });
      continue;
    }
    
    // Default to equals
    if (trimmed.includes(':')) {
      const [field, value] = trimmed.split(':');
      filters.push({ field, operator: 'equals', value });
    }
  }
  
  return filters;
}

/**
 * Split a query string by ' AND ' while ignoring occurrences inside parentheses.
 * Required so that hasAny/hasAll value lists like (A,B AND C) are not split.
 */
function splitByAndOutsideParens(query: string): string[] {
  const parts: string[] = [];
  let depth = 0;
  let start = 0;

  for (let i = 0; i < query.length; i++) {
    if (query[i] === '(') { depth++; continue; }
    if (query[i] === ')') { depth--; continue; }
    if (depth === 0 && query.slice(i, i + 5) === ' AND ') {
      parts.push(query.slice(start, i));
      i += 4; // skip ' AND '
      start = i + 1;
    }
  }
  parts.push(query.slice(start));
  return parts;
}
