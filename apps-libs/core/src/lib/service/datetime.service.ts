import { Injectable, inject } from '@angular/core';
import { SessionService } from './session.service';

/**
 * Reusable date/time formatting utilities.
 * Defaults to current session language and Europe/Berlin timezone unless overridden.
 */
@Injectable({ providedIn: 'root' })
export class DateTimeService {
  private session = inject(SessionService);

  /**
   * Format a date string or Date object to a localized string.
   *
   * @param date Input date (ISO string or Date)
   * @param opts Optional overrides
   *  - locale: BCP 47 language tag (defaults to SessionService.language())
   *  - timeZone: IANA timezone (defaults to 'Europe/Berlin')
   *  - includeTimeZoneName: whether to include short tz name (default true)
   */
  formatDate(
    date: string | Date,
    opts?: { locale?: string; timeZone?: string; includeTimeZoneName?: boolean }
  ): string {
    try {
      const dateObj = date instanceof Date ? date : new Date(date);
      const locale = opts?.locale ?? this.session.language();
      const timeZone = opts?.timeZone ?? 'Europe/Berlin';
      const includeTz = opts?.includeTimeZoneName ?? true;

      return dateObj.toLocaleString(locale, {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        timeZone,
        ...(includeTz ? { timeZoneName: 'short' as const } : {})
      });
    } catch {
      // Fallback to original input if formatting fails
      return typeof date === 'string' ? date : date.toString();
    }
  }
}
