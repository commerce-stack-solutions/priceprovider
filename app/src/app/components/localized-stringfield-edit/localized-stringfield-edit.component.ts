import { Component, Input, Output, EventEmitter, signal, inject, computed, OnInit, OnChanges, SimpleChanges, effect } from '@angular/core';
import { UpperCasePipe } from '@angular/common';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { SessionService } from '../../service/session.service';
import { TranslocoModule } from '@jsverse/transloco';

@Component({
  selector: 'app-localized-stringfield-edit',
  templateUrl: './localized-stringfield-edit.component.html',
  styleUrls: ['./localized-stringfield-edit.component.scss'],
  standalone: true,
  imports: [UpperCasePipe, ReactiveFormsModule, TranslocoModule]
})
export class LocalizedStringfieldEditComponent implements OnInit, OnChanges {
  @Input() form!: FormGroup;
  @Input() fieldPrefix: string = 'field_'; // Default prefix for form control names
  @Input() fieldLabel: string = 'Field'; // Label for the field section
  @Input() configuredLanguages = signal<string[]>([]);
  @Input() fieldErrors = signal<Map<string, string[]>>(new Map());
  @Input() isEditMode: boolean = false;
  @Input() isModalMode: boolean = false;
  @Output() removeLanguageEvent = new EventEmitter<string>();
  @Output() addLanguageEvent = new EventEmitter<string>();
  
  sessionService = inject(SessionService);
  
  // State to track whether to show all localized values or just mandatory
  showAllLocalizedValues = signal<boolean>(false);
  
  // Track recently added languages (to keep them visible even if empty)
  recentlyAddedLanguages = signal<string[]>([]);
  
  // Computed values for mandatory, active, and inactive languages
  mandatoryLanguages = computed(() => {
    return this.sessionService.availableLanguages()
      .filter(l => l.mandatory)
      .map(l => l.isoKey);
  });
  
  activeLanguages = computed(() => {
    return this.sessionService.availableLanguages()
      .filter(l => l.active && !l.mandatory)
      .map(l => l.isoKey);
  });
  
  inactiveLanguages = computed(() => {
    return this.sessionService.availableLanguages()
      .filter(l => !l.active)
      .map(l => l.isoKey);
  });

  // Languages currently visible in the UI based on showAllLocalizedValues state
  visibleLanguages = computed(() => {
    const configured = this.configuredLanguages();
    const recentlyAdded = this.recentlyAddedLanguages();
    
    if (this.showAllLocalizedValues()) {
      // Show only languages that have values (filter out empty ones)
      // BUT also show recently added languages even if empty
      return configured.filter(lang => {
        const controlName = this.getControlName(lang);
        const control = this.form?.get(controlName);
        const value = control?.value;
        // Show mandatory languages, languages with non-empty values, or recently added languages
        return this.isMandatory(lang) || (value && value.trim() !== '') || recentlyAdded.includes(lang);
      });
    } else {
      // Show mandatory languages AND recently added languages (even if empty)
      return configured.filter(lang => this.isMandatory(lang) || recentlyAdded.includes(lang));
    }
  });

  // Languages available to add (active languages not yet configured)
  availableToAdd = computed(() => {
      // In "View All" mode, show languages that are not currently visible
      // (either not in configuredLanguages OR in configuredLanguages but filtered out because they're empty)
      const shown = this.visibleLanguages();
      return this.activeLanguages().filter(lang => !shown.includes(lang));
  });
  
  // Check if there are active languages with values that are hidden
  hasHiddenLocalizedValues = computed(() => {
    if (this.showAllLocalizedValues()) {
      return false; // Already showing all
    }
    
    const configured = this.configuredLanguages();
    const mandatory = this.mandatoryLanguages();
    
    // Check if there are any configured languages that are not mandatory AND have values
    return configured.some(lang => {
      if (mandatory.includes(lang)) {
        return false; // Skip mandatory languages
      }
      
      // Check if this non-mandatory language has a value
      const controlName = this.getControlName(lang);
      const control = this.form?.get(controlName);
      const value = control?.value;
      return value && value.trim() !== '';
    });
  });

  constructor() {
    // Use effect to update form control states when languages change
    effect(() => {
      const configured = this.configuredLanguages();
      const inactive = this.inactiveLanguages();
      
      if (this.form) {
        configured.forEach(lang => {
          const controlName = this.getControlName(lang);
          const control = this.form.get(controlName);
          
          if (control) {
            if (inactive.includes(lang)) {
              control.disable({ emitEvent: false });
            } else {
              control.enable({ emitEvent: false });
            }
          }
        });
      }
    });
    
    // Effect to clean up recently added languages once they have values
    effect(() => {
      const recentlyAdded = this.recentlyAddedLanguages();
      
      if (this.form && recentlyAdded.length > 0) {
        const toRemove: string[] = [];
        
        recentlyAdded.forEach(lang => {
          const controlName = this.getControlName(lang);
          const control = this.form.get(controlName);
          const value = control?.value;
          
          // If the language now has a value, remove it from recently added
          if (value && value.trim() !== '') {
            toRemove.push(lang);
          }
        });
        
        if (toRemove.length > 0) {
          this.recentlyAddedLanguages.update(current => 
            current.filter(lang => !toRemove.includes(lang))
          );
        }
      }
    });
  }

  ngOnInit(): void {
    // Update form control states on initialization
    this.updateFormControlStates();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Update form control states when inputs change
    if (changes['form'] || changes['configuredLanguages']) {
      this.updateFormControlStates();
    }
  }

  private updateFormControlStates(): void {
    if (!this.form) return;

    const configured = this.configuredLanguages();
    const inactive = this.inactiveLanguages();

    configured.forEach(lang => {
      const controlName = this.getControlName(lang);
      const control = this.form.get(controlName);

      if (control) {
        if (inactive.includes(lang)) {
          control.disable({ emitEvent: false });
        } else {
          control.enable({ emitEvent: false });
        }
      }
    });
  }

  isMandatory(lang: string): boolean {
    return this.mandatoryLanguages().includes(lang);
  }
  
  isInactive(lang: string): boolean {
    return this.inactiveLanguages().includes(lang);
  }

  removeLanguage(lang: string): void {
    this.removeLanguageEvent.emit(lang);
    // Remove from recently added if it was tracked
    this.recentlyAddedLanguages.update(current => 
      current.filter(l => l !== lang)
    );
  }

  addLanguage(lang: string): void {
    this.addLanguageEvent.emit(lang);
    // Track this language as recently added so it stays visible even if empty
    this.recentlyAddedLanguages.update(current => {
      if (!current.includes(lang)) {
        return [...current, lang];
      }
      return current;
    });
  }
  
  toggleShowAllLocalizedValues(): void {
    this.showAllLocalizedValues.update(current => !current);
  }

  hasFieldError(fieldName: string): boolean {
    return this.fieldErrors().has(fieldName);
  }

  getFieldErrors(fieldName: string): string[] {
    return this.fieldErrors().get(fieldName) || [];
  }

  getControlName(lang: string): string {
    return this.fieldPrefix + lang;
  }
}
