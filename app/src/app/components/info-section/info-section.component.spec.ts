import { TestBed } from '@angular/core/testing';
import { InfoSectionComponent, InfoField } from './info-section.component';
import { SessionService } from '../../service/session.service';
import { signal, provideZonelessChangeDetection } from '@angular/core';

describe('InfoSectionComponent', () => {
  let component: InfoSectionComponent;
  let sessionServiceMock: any;

  beforeEach(async () => {
    sessionServiceMock = {
      language: signal('en')
    };

    await TestBed.configureTestingModule({
      imports: [InfoSectionComponent],
      providers: [
        provideZonelessChangeDetection(),
        { provide: SessionService, useValue: sessionServiceMock }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(InfoSectionComponent);
    component = fixture.componentInstance;
  });

  describe('formatValue', () => {
    it('should format percentage correctly without multiplying by 100', () => {
      const field: InfoField = {
        label: 'Tax Rate',
        value: 19.0,
        type: 'percentage'
      };
      expect(component.formatValue(field)).toBe('19.00%');
    });

    it('should format percentage with two decimal places', () => {
      const field: InfoField = {
        label: 'Tax Rate',
        value: 7.5,
        type: 'percentage'
      };
      expect(component.formatValue(field)).toBe('7.50%');
    });

    it('should handle null or undefined values', () => {
      expect(component.formatValue({ label: 'Test', value: null })).toBe('-');
      expect(component.formatValue({ label: 'Test', value: undefined })).toBe('-');
    });

    it('should format text values as string', () => {
      const field: InfoField = {
        label: 'ID',
        value: 123,
        type: 'text'
      };
      expect(component.formatValue(field)).toBe('123');
    });

    it('should format currency correctly', () => {
      const field: InfoField = {
        label: 'Price',
        value: 950.0,
        type: 'currency',
        currencyCode: 'EUR'
      };
      // Note: format depends on locale, but we can check if it contains the symbol and the value
      const result = component.formatValue(field);
      expect(result).toContain('950.00');
      expect(result).toContain('€');
    });

    it('should format localized values correctly', () => {
      const field: InfoField = {
        label: 'Name',
        value: { en: 'Piece', de: 'Stück' },
        type: 'localized'
      };

      sessionServiceMock.language.set('en');
      expect(component.formatValue(field)).toBe('Piece');

      sessionServiceMock.language.set('de');
      expect(component.formatValue(field)).toBe('Stück');
    });
  });
});
