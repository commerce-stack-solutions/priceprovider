import { Component, signal, inject, OnInit, computed, Input, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router , RouterModule} from '@angular/router';
import { CurrenciesService } from '../../../service/currency/currencies.service';
import { Currency } from '../../../model/currency/currency.model';
import { MetaInfo } from '../../../model/meta-info.model';
import { SessionService } from '../../../service/session.service';
import { PermissionService } from '../../../service/permission.service';
import { LocalizedStringfieldEditComponent } from '../../../components/localized-stringfield-edit/localized-stringfield-edit.component';
import { TranslocoModule } from '@jsverse/transloco';
import { MessageTranslationService } from '../../../service/message-translation.service';
import { Message } from '../../../model/message.model';
import { IsMandatoryPipe } from '../../../pipes/is-mandatory.pipe';

@Component({
  selector: 'app-currency-form',
  templateUrl: './currency-form.component.html',
  styleUrls: ['./currency-form.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, LocalizedStringfieldEditComponent, RouterModule, TranslocoModule, IsMandatoryPipe],
  host: {
    '(document:keydown.s)': 'handleSaveKeyPress($event)'
  }
})
export class CurrencyFormComponent implements OnInit {
  private currenciesService = inject(CurrenciesService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  sessionService = inject(SessionService);
  private permissionService = inject(PermissionService);
  private messageTranslationService = inject(MessageTranslationService);

  lang = computed(() => this.sessionService.language());

  @Input() config?: { initialValue?: string; modalMode?: boolean };
  @Output() saved = new EventEmitter<Currency>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  isEditMode = signal(false);
  isModalMode = signal(false);
  loading = signal(true);
  saving = signal(false);
  error = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  currencyKey = signal<string | null>(null);
  fieldErrors = signal<Map<string, string[]>>(new Map());
  originalValues = signal<any>({});
  showSaveKeyHint = signal(false);
  
  // Store the original currency data to preserve invisible languages
  originalCurrency = signal<Currency | null>(null);
  meta = signal<MetaInfo | null>(null);

  // Language management for localized values
  languages = signal<string[]>([]);
  
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
  
  // All languages to display in form (mandatory + active + inactive with values)
  displayedLanguages = computed(() => {
    const displayed = [...this.mandatoryLanguages(), ...this.activeLanguages()];
    
    // Add inactive languages that have values in the form
    if (this.isEditMode()) {
      this.inactiveLanguages().forEach(lang => {
        const value = this.form?.get(`name_${lang}`)?.value;
        if (value && value.trim() !== '') {
          displayed.push(lang);
        }
      });
    }
    
    return displayed;
  });

  isMandatory(fieldName: string): boolean {
    const m = this.meta();
    if (m?.mandatoryFields) { return m.mandatoryFields.includes(fieldName); }
    return this.form?.get(fieldName)?.hasValidator(Validators.required) ?? false;
  }

  isLanguageMandatory(lang: string): boolean {
    return this.mandatoryLanguages().includes(lang);
  }
  
  isInactive(lang: string): boolean {
    return this.inactiveLanguages().includes(lang);
  }

  ngOnInit(): void {
    // Check if we're in modal mode
    if (this.config?.modalMode) {
      this.isModalMode.set(true);
      this.isEditMode.set(false); // Modal mode is always create mode
      
      // Wait for languages to be loaded before initializing form
      if (this.sessionService.availableLanguages().length > 0) {
        this.initForm();
        this.languages.set([...this.mandatoryLanguages()]);
        this.loading.set(false);
        
        // Set initial value if provided
        if (this.config?.initialValue) {
          this.form.patchValue({ currencyKey: this.config.initialValue.toUpperCase() });
        }
      } else {
        // Wait a bit for languages to load
        setTimeout(() => {
          this.initForm();
          this.languages.set([...this.mandatoryLanguages()]);
          this.loading.set(false);
          
          if (this.config?.initialValue) {
            this.form.patchValue({ currencyKey: this.config.initialValue.toUpperCase() });
          }
        }, 100);
      }
    } else {
      // Regular page mode
      const key = this.route.snapshot.paramMap.get('currencyKey');
      this.isEditMode.set(!!key);
      this.currencyKey.set(key);

      if (!this.permissionService.hasWritePermission('Currency')) {
        if (key) {
          this.router.navigate(['/' + this.lang(), 'currencies', key]);
        } else {
          this.router.navigate(['/' + this.lang(), 'currencies']);
        }
        return;
      }

      // Wait for languages to be loaded before initializing form
      if (this.sessionService.availableLanguages().length > 0) {
        this.initFormAndLoadData(key);
      } else {
        // Wait a bit for languages to load
        setTimeout(() => {
          this.initFormAndLoadData(key);
        }, 100);
      }
    }
  }

  private initFormAndLoadData(key: string | null): void {
    this.initForm();

    if (this.isEditMode()) {
      this.loadCurrency(key!);
    } else {
      // When adding new currency, show mandatory + active languages
      this.languages.set([...this.mandatoryLanguages(), ...this.activeLanguages()]);
      this.currenciesService.getMeta().subscribe({
        next: (metaInfo) => { this.meta.set(metaInfo); this.loading.set(false); },
        error: () => this.loading.set(false)
      });
    }
  }

  initForm(): void {
    // Create a FormGroup for the main form controls
    const formConfig: any = {
      currencyKey: [{ value: '', disabled: this.isEditMode() }, [Validators.required, Validators.pattern(/^[A-Z]{3}$/)]],
      symbol: ['', Validators.required]
    };

    // Add form controls for all languages (active + inactive)
    const allLanguages = this.sessionService.availableLanguages().map(l => l.isoKey);
    
    allLanguages.forEach(lang => {
      const isMandatory = this.mandatoryLanguages().includes(lang);
      formConfig[`name_${lang}`] = ['', isMandatory ? Validators.required : []];
    });
    this.form = this.fb.group(formConfig);
  }

  loadCurrency(currencyKey: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.currenciesService.getCurrency(currencyKey).subscribe({
      next: (currency: Currency) => {
        // Store original currency to preserve invisible languages
        this.originalCurrency.set(currency);
        if (currency.$meta) { this.meta.set(currency.$meta); }
        
        // Build patch data with all fields including language values
        const patchData: any = {
          currencyKey: currency.currencyKey,
          symbol: currency.symbol
        };
        
        // Patch all language values from the currency
        if (currency.name) {
          Object.keys(currency.name).forEach(lang => {
            patchData[`name_${lang}`] = currency.name![lang];
          });
        }
        
        this.form.patchValue(patchData);
        
        // Store original values for change tracking
        this.originalValues.set({ ...patchData });
        
        // Build list of languages to display: mandatory + active + inactive with values
        const languagesToDisplay = [...this.mandatoryLanguages(), ...this.activeLanguages()];
        
        // Add inactive languages that have values
        if (currency.name) {
          this.inactiveLanguages().forEach(lang => {
            if (currency.name![lang] && currency.name![lang].trim() !== '' && !languagesToDisplay.includes(lang)) {
              languagesToDisplay.push(lang);
            }
          });
        }
        
        this.languages.set(languagesToDisplay);
        
        this.loading.set(false);
      },
      error: (err: any) => {
        this.error.set('Failed to load currency: ' + (err.message || 'Unknown error'));
        this.loading.set(false);
      }
    });
  }

  removeLanguage(lang: string): void {
    // Cannot remove mandatory languages or inactive languages
    if (this.isLanguageMandatory(lang) || this.isInactive(lang)) return;
    
    const current = this.languages();
    this.languages.set(current.filter(l => l !== lang));
    // Clear the form field value when user explicitly removes a language
    const controlName = `name_${lang}`;
    const control = this.form.get(controlName);
    if (control) {
      control.setValue('', { emitEvent: true });
    }
  }

  addLanguage(lang: string): void {
    const current = this.languages();
    if (!current.includes(lang)) {
      this.languages.set([...current, lang]);
    }
  }

  buildPatchOperations(): any[] {
    const patches: any[] = [];
    const formValue = this.form.getRawValue();
    const original = this.originalValues();
    
    // Check simple fields
    if (formValue.symbol !== original.symbol) {
      patches.push({ op: 'replace', path: '/symbol', value: formValue.symbol });
    }
    
    // Check name fields (localized strings)
    Object.keys(formValue).forEach(key => {
      if (key.startsWith('name_')) {
        const lang = key.substring(5);
        const currentValue = formValue[key] || '';
        const originalValue = original[key] || '';
        
        if (currentValue !== originalValue) {
          if (currentValue === '' || currentValue === null) {
            // Only send remove if field existed in original
            if (originalValue !== '' && originalValue !== null && originalValue !== undefined) {
              patches.push({ op: 'remove', path: `/name/${lang}` });
            }
          } else {
            // Use 'add' if field didn't exist in original, 'replace' if it did
            if (originalValue === '' || originalValue === null || originalValue === undefined) {
              patches.push({ op: 'add', path: `/name/${lang}`, value: currentValue });
            } else {
              patches.push({ op: 'replace', path: `/name/${lang}`, value: currentValue });
            }
          }
        }
      }
    });
    
    return patches;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      Object.keys(this.form.controls).forEach(key => {
        this.form.get(key)?.markAsTouched();
      });
      // Trigger change detection to show validation errors
      this.cdr.detectChanges();
      return;
    }

    this.saving.set(true);
    this.error.set(null);

    const formValue = this.form.getRawValue();
    
    if (this.isEditMode()) {
      // Use PATCH for updates
      const patches = this.buildPatchOperations();
      
      if (patches.length === 0) {
        // No changes, navigate to detail view
        this.saving.set(false);
        if (this.isModalMode()) {
          this.cancelled.emit();
        } else {
          this.router.navigate(['/' + this.lang(), 'currencies', this.currencyKey()]);
        }
        return;
      }
      
      this.currenciesService.patchCurrency(this.currencyKey()!, patches).subscribe({
        next: (response: any) => {
          // Check if response contains error messages
          if (response.$messages && response.$messages.length > 0) {
            this.handleValidationErrors(response.$messages);
            this.saving.set(false);
          } else {
            if (this.isModalMode()) {
              this.saving.set(false);
              this.saved.emit(response);
            } else {
              // Navigate to detail view after successful save
              this.saving.set(false);
              this.router.navigate(['/' + this.lang(), 'currencies', this.currencyKey()]);
            }
          }
        },
        error: (err: any) => {
          // Check if error response contains validation messages
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set(err.error?.message || 'Failed to save currency');
          }
          this.saving.set(false);
        }
      });
    } else {
      // Use POST for creates
      // Build name object from form fields
      // Important: Include all language values (visible and invisible) to preserve data
      const name: { [key: string]: string } = {};
      
      Object.keys(formValue).forEach(key => {
        if (key.startsWith('name_')) {
          const lang = key.substring(5); // Remove "name_" prefix
          const value = formValue[key];
          if (value && value.trim()) {
            name[lang] = value;
          }
        }
      });

      const currency: Currency = {
        currencyKey: formValue.currencyKey,
        symbol: formValue.symbol,
        name: Object.keys(name).length > 0 ? name : undefined
      };

      this.currenciesService.createCurrency(currency).subscribe({
        next: (response: any) => {
          // Check if response contains error messages
          if (response.$messages && response.$messages.length > 0) {
            this.handleValidationErrors(response.$messages);
            this.saving.set(false);
          } else {
            this.saving.set(false);
            if (this.isModalMode()) {
              this.saved.emit(response);
            } else {
              this.router.navigate(['/' + this.lang(), 'currencies', response.currencyKey]);
            }
          }
        },
        error: (err: any) => {
          // Check if error response contains validation messages
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set(err.error?.message || 'Failed to save currency');
          }
          this.saving.set(false);
        }
      });
    }
  }

  handleValidationErrors(messages: any[]): void {
    const fieldErrorsMap = new Map<string, string[]>();
    const errorMessages: string[] = [];

    messages.forEach((msg: Message) => {
      if (msg.type === 'ERROR') {
        // Translate the message using the message translation service
        const translatedMessage = this.messageTranslationService.translateMessage(msg);
        errorMessages.push(translatedMessage);
        
        // If fields are specified, map them to the error message
        if (msg.fields && msg.fields.length > 0) {
          msg.fields.forEach((field: string) => {
            if (!fieldErrorsMap.has(field)) {
              fieldErrorsMap.set(field, []);
            }
            fieldErrorsMap.get(field)!.push(translatedMessage);
          });
        }
      }
    });

    this.fieldErrors.set(fieldErrorsMap);
    this.error.set(errorMessages.join('; '));
  }

  hasFieldError(fieldName: string): boolean {
    return this.fieldErrors().has(fieldName);
  }

  getFieldErrors(fieldName: string): string[] {
    return this.fieldErrors().get(fieldName) || [];
  }

  cancel(): void {
    if (this.isModalMode()) {
      this.cancelled.emit();
    } else if (this.isEditMode()) {
      // Navigate to detail view
      this.router.navigate(['/' + this.lang(), 'currencies', this.currencyKey()]);
    } else {
      // Navigate to list
      this.router.navigate(['/' + this.lang(), 'currencies']);
    }
  }

  handleSaveKeyPress(event: Event): void {
    // Ensure keyboard event and guard against input/textarea targets
    if (!(event instanceof KeyboardEvent)) return;
    const target = event.target as HTMLElement | null;
    if (
      this.isModalMode() ||
      (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA'))
    ) {
      return;
    }

    // Prevent default 's' key behavior (e.g., browser save)
    event.preventDefault();

    // Show visual feedback
    this.showSaveKeyHint.set(true);

    // Submit the form
    this.onSubmit();

    // Hide hint after a short delay
    setTimeout(() => {
      this.showSaveKeyHint.set(false);
    }, 500);
  }
}
