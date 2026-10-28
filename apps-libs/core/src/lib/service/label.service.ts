import { Injectable } from '@angular/core';

/**
 * Utilities for transforming technical keys into human-readable labels.
 */
@Injectable({ providedIn: 'root' })
export class LabelService {
  /**
   * Convert a camelCase or PascalCase key to Title Case with spaces.
   * Examples: "createdAt" -> "Created At", "LastModifiedAt" -> "Last Modified At"
   */
  formatLabel(key: string): string {
    if (!key) return '';
    const withSpaces = key.replace(/([A-Z])/g, ' $1').trim();
    return withSpaces.charAt(0).toUpperCase() + withSpaces.slice(1);
  }
}
