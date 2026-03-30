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
  | 'exists';

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
      return ['exists'];
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
      case 'range':
        const from = filter.valueFrom !== undefined && filter.valueFrom !== '' ? filter.valueFrom : '*';
        const to = filter.valueTo !== undefined && filter.valueTo !== '' ? filter.valueTo : '*';
        return `${field}:[${from} TO ${to}]`;
      case 'exists':
        return `${field}.exists:${filter.value}`;
      default:
        return '';
    }
  }).filter(part => part !== '');

  return queryParts.join(' AND ');
}

/**
 * Parse query string back into filter definitions
 */
export function parseQueryString(queryString: string): FilterDefinition[] {
  if (!queryString || queryString.trim() === '') {
    return [];
  }

  const filters: FilterDefinition[] = [];
  
  // Split by AND (simple implementation, doesn't handle parentheses)
  const parts = queryString.split(' AND ');
  
  for (const part of parts) {
    const trimmed = part.trim();
    
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
