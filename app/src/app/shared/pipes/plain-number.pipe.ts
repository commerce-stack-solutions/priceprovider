import { Pipe, PipeTransform } from '@angular/core';

/**
 * Formats small fractions without scientific notation, e.g. 1e-9 -> 0.000000001
 * - Accepts number or numeric string
 * - Returns '-' for null/undefined
 * - Trims trailing zeros after decimal point
 */
@Pipe({
  name: 'plainNumber',
  standalone: true
})
export class PlainNumberPipe implements PipeTransform {
  transform(
    value: number | string | null | undefined,
    opts?: { trimTrailingZeros?: boolean }
  ): string {
    if (value === null || value === undefined) return '-';

    // Keep non-numeric values as-is
    const n = typeof value === 'number' ? value : Number(value);
    if (Number.isNaN(n)) return String(value);

    // Infinity and -Infinity
    if (!Number.isFinite(n)) return String(n);

    const str = this.toPlainString(n);
    return (opts?.trimTrailingZeros ?? true) ? this.trimZeros(str) : str;
  }

  private toPlainString(n: number): string {
    // If already non-exponential, return as-is
    const s = n.toString();
    if (!/e/i.test(s)) return s;

    const negative = n < 0;
    const abs = Math.abs(n);
    const [mantissaRaw, expRaw] = abs.toString().toLowerCase().split('e');
    const exp = parseInt(expRaw, 10);

    const [intPart, fracPart = ''] = mantissaRaw.split('.');
    const digits = intPart + fracPart; // mantissa digits only

    let result: string;
    if (exp < 0) {
      const zerosCount = Math.max(0, -exp - intPart.length);
      const fractional = '0'.repeat(zerosCount) + digits;
      result = '0.' + fractional;
    } else {
      // exp >= 0
      if (exp >= fracPart.length) {
        result = digits + '0'.repeat(exp - fracPart.length);
      } else {
        const pos = intPart.length + exp;
        const head = digits.slice(0, pos);
        const tail = digits.slice(pos);
        result = head + '.' + tail;
      }
    }

    return negative ? '-' + result : result;
  }

  private trimZeros(s: string): string {
    if (!s.includes('.')) return s;
    // Remove trailing zeros
    s = s.replace(/(\d*\.\d*?[1-9])0+$/, '$1');
    // If all fractional digits were zeros, drop the dot and keep single zero
    s = s.replace(/\.0+$/, '.0');
    return s;
  }
}
