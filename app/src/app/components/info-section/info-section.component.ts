import { Component, input } from '@angular/core';

import { SessionService } from '../../service/session.service';
import { inject } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';

export interface InfoSection {
  title: string;
  fields: InfoField[];
}

export interface InfoField {
  label: string;
  value: any;
  type?: 'text' | 'percentage' | 'localized' | 'currency';
  currencyCode?: string;
}

@Component({
  selector: 'app-info-section',
  templateUrl: './info-section.component.html',
  styleUrls: ['./info-section.component.scss'],
  imports: [TranslocoModule],
  standalone: true
})
export class InfoSectionComponent {
  sections = input<InfoSection[]>([]);
  sessionService = inject(SessionService);

  hasContent(): boolean {
    return this.sections().length > 0;
  }

  formatValue(field: InfoField): string {
    if (field.value === null || field.value === undefined) {
      return '-';
    }

    switch (field.type) {
      case 'percentage':
        return `${field.value.toFixed(2)}%`;
      
      case 'localized':
        return this.getLocalizedValue(field.value);
      
      case 'currency':
        return this.formatCurrency(field.value, field.currencyCode || 'EUR');
      
      case 'text':
      default:
        return String(field.value);
    }
  }

  private getLocalizedValue(names: { [key: string]: string } | string): string {
    if (typeof names === 'string') {
      return names;
    }
    const locale = this.sessionService.language();
    return names[locale] || names['en'] || Object.values(names)[0] || '-';
  }

  private formatCurrency(value: number, currencyCode: string): string {
    const locale = this.sessionService.language();
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: currencyCode
    }).format(value);
  }
}
