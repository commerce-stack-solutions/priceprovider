
import { ChangeDetectorRef, Component, computed, EventEmitter, inject, Input, OnInit, Output, signal } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { LocalizedStringfieldEditComponent } from '../../../components/localized-stringfield-edit/localized-stringfield-edit.component';
import { ReferenceDataSourceResult, ReferenceEditComponent } from '../../../components/reference-edit/reference-edit.component';
import { Unit } from '../../../model/unit/unit.model';
import { ModalService } from '../../../service/modal.service';
import { SessionService } from '../../../service/session.service';
import { PermissionService } from '../../../service/permission.service';
import { UnitsService } from '../../../service/unit/units.service';
import { PlainNumberPipe } from '../../../shared/pipes/plain-number.pipe';
import { MessageTranslationService } from '../../../service/message-translation.service';
import { Message } from '../../../model/message.model';
import { MetaInfo } from '../../../model/meta-info.model';
import { IsMandatoryPipe } from '../../../pipes/is-mandatory.pipe';

const OPTIONS_PAGESIZE = 30;

@Component({
  selector: 'app-unit-form',
  templateUrl: './unit-form.component.html',
  styleUrls: ['./unit-form.component.scss'],
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, LocalizedStringfieldEditComponent, RouterModule, ReferenceEditComponent, TranslocoModule, IsMandatoryPipe],
  host: {
    '(document:keydown.s)': 'handleSaveKeyPress($event)'
  }
})
export class UnitFormComponent implements OnInit {
  private unitsService = inject(UnitsService);
  private modalService = inject(ModalService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  sessionService = inject(SessionService);
  private permissionService = inject(PermissionService);
  private messageTranslationService = inject(MessageTranslationService);
  private plainNumber = new PlainNumberPipe();

  lang = computed(() => this.sessionService.language());

  @Input() config?: { initialValue?: string; modalMode?: boolean };
  @Output() saved = new EventEmitter<Unit>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  isEditMode = signal(false);
  isModalMode = signal(false);
  loading = signal(true);
  saving = signal(false);
  error = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  symbol = signal<string | null>(null);
  fieldErrors = signal<Map<string, string[]>>(new Map());
  originalValues = signal<any>({});
  showSaveKeyHint = signal(false);
  meta = signal<MetaInfo | null>(null);

  // Store the original unit data to preserve invisible languages
  originalUnit = signal<Unit | null>(null);

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
          this.form.patchValue({ symbol: this.config.initialValue });
        }
      } else {
        // Wait a bit for languages to load
        setTimeout(() => {
          this.initForm();
          this.languages.set([...this.mandatoryLanguages()]);
          this.loading.set(false);

          if (this.config?.initialValue) {
            this.form.patchValue({ symbol: this.config.initialValue });
          }
        }, 100);
      }
    } else {
      // Regular page mode
      const symbol = this.route.snapshot.paramMap.get('symbol');
      this.isEditMode.set(!!symbol);
      this.symbol.set(symbol);

      if (!this.permissionService.hasWritePermission('Unit')) {
        if (symbol) {
          this.router.navigate(['/' + this.lang(), 'units', symbol]);
        } else {
          this.router.navigate(['/' + this.lang(), 'units']);
        }
        return;
      }

      // Wait for languages to be loaded before initializing form
      // Check if languages are already loaded
      if (this.sessionService.availableLanguages().length > 0) {
        this.initFormAndLoadData(symbol);
      } else {
        // Wait a bit for languages to load (they load in SessionService constructor)
        setTimeout(() => {
          this.initFormAndLoadData(symbol);
        }, 100);
      }
    }
  }

  private initFormAndLoadData(symbol: string | null): void {
    this.initForm();

    if (this.isEditMode()) {
      this.loadUnit(symbol!);
    } else {
      // When adding new unit, show mandatory + active languages
      this.languages.set([...this.mandatoryLanguages(), ...this.activeLanguages()]);
      this.unitsService.getMeta().subscribe({
        next: (metaInfo) => { this.meta.set(metaInfo); this.loading.set(false); },
        error: () => this.loading.set(false)
      });
    }
  }

  initForm(): void {
    // Dynamically create form controls for all possible languages
    const formConfig: any = {
      symbol: [{ value: '', disabled: this.isEditMode() }, [Validators.required, Validators.pattern(/^[a-zA-Z0-9_-]+$/)]],
      measure: [''],
      baseUnitRef: [''],
      factor: ['', [Validators.pattern(/^\d*\.?\d*$/)]]
    };

    // Add form controls for all languages (active + inactive)
    const allLanguages = this.sessionService.availableLanguages().map(l => l.isoKey);

    allLanguages.forEach(lang => {
      const isMandatory = this.mandatoryLanguages().includes(lang);
      formConfig[`name_${lang}`] = ['', isMandatory ? Validators.required : []];
    });

    this.form = this.fb.group(formConfig);

    // Auto-convert exponential notation in factor to plain decimal when detected
    const factorCtrl = this.form.get('factor');
    factorCtrl?.valueChanges.subscribe((val: unknown) => {
      if (typeof val !== 'string') return;
      const v = val.trim();
      if (!v) return;
      // Matches full exponential forms like 1e-9, -2.5E+3
      const expRe = /^[+-]?\d+(?:\.\d+)?e[+-]?\d+$/i;
      if (expRe.test(v)) {
        const num = Number(v);
        if (!Number.isNaN(num) && Number.isFinite(num)) {
          const formatted = this.plainNumber.transform(num);
          if (formatted !== v) {
            factorCtrl.setValue(formatted, { emitEvent: false });
          }
        }
      }
    });
  }

  loadUnit(symbol: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.unitsService.getUnit(symbol).subscribe({
      next: (unit: Unit) => {
        // Store original unit to preserve invisible languages
        this.originalUnit.set(unit);
        if (unit.$meta) { this.meta.set(unit.$meta); }

        // Patch form with all language values
        const patchData: any = {
          symbol: unit.symbol,
          measure: unit.measure || '',
          baseUnitRef: unit.baseUnitRef || '',
          factor: (unit.factor !== undefined && unit.factor !== null)
            ? this.plainNumber.transform(unit.factor)
            : ''
        };

        // Patch all language values from the unit
        if (unit.name) {
          Object.keys(unit.name).forEach(lang => {
            patchData[`name_${lang}`] = unit.name![lang];
          });
        }

        this.form.patchValue(patchData);

        // Store original values for change tracking
        this.originalValues.set({ ...patchData });

        // In edit mode, show all languages that have values (mandatory, active, and inactive with values)
        this.languages.set(this.displayedLanguages());

        this.loading.set(false);
      },
      error: (err: any) => {
        this.error.set('Failed to load unit: ' + (err.message || 'Unknown error'));
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

    // Check simple fields - only remove if they existed in original
    if (formValue.measure !== original.measure) {
      if (formValue.measure === '' || formValue.measure === null) {
        // Only send remove if field existed in original
        if (original.measure !== '' && original.measure !== null && original.measure !== undefined) {
          patches.push({ op: 'remove', path: '/measure' });
        }
      } else {
        // Use 'add' if field didn't exist in original, 'replace' if it did
        if (original.measure === '' || original.measure === null || original.measure === undefined) {
          patches.push({ op: 'add', path: '/measure', value: formValue.measure });
        } else {
          patches.push({ op: 'replace', path: '/measure', value: formValue.measure });
        }
      }
    }

    if (formValue.baseUnitRef !== original.baseUnitRef) {
      if (formValue.baseUnitRef === '' || formValue.baseUnitRef === null) {
        // Only send remove if field existed in original
        if (original.baseUnitRef !== '' && original.baseUnitRef !== null && original.baseUnitRef !== undefined) {
          patches.push({ op: 'remove', path: '/baseUnitRef' });
        }
      } else {
        // Use 'add' if field didn't exist in original, 'replace' if it did
        if (original.baseUnitRef === '' || original.baseUnitRef === null || original.baseUnitRef === undefined) {
          patches.push({ op: 'add', path: '/baseUnitRef', value: formValue.baseUnitRef });
        } else {
          patches.push({ op: 'replace', path: '/baseUnitRef', value: formValue.baseUnitRef });
        }
      }
    }

    const currentFactor = formValue.factor ? parseFloat(formValue.factor) : null;
    const originalFactor = original.factor ? parseFloat(original.factor) : null;
    if (currentFactor !== originalFactor) {
      if (currentFactor === null) {
        // Only send remove if field existed in original
        if (originalFactor !== null && originalFactor !== undefined) {
          patches.push({ op: 'remove', path: '/factor' });
        }
      } else {
        // Use 'add' if field didn't exist in original, 'replace' if it did
        if (originalFactor === null || originalFactor === undefined) {
          patches.push({ op: 'add', path: '/factor', value: currentFactor });
        } else {
          patches.push({ op: 'replace', path: '/factor', value: currentFactor });
        }
      }
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
        // No changes
        this.saving.set(false);
        if (this.isModalMode()) {
          this.cancelled.emit();
        } else {
          this.router.navigate(['/' + this.lang(), 'units', this.symbol()]);
        }
        return;
      }

      this.unitsService.patchUnit(this.symbol()!, patches).subscribe({
        next: (savedUnit: Unit) => {
          // Check if response contains error messages
          if ((savedUnit as any).$messages && (savedUnit as any).$messages.length > 0) {
            this.handleValidationErrors((savedUnit as any).$messages);
            this.saving.set(false);
          } else {
            if (this.isModalMode()) {
              this.saving.set(false);
              this.saved.emit(savedUnit);
            } else {
              // Navigate to detail view after successful save
              this.saving.set(false);
              this.router.navigate(['/' + this.lang(), 'units', savedUnit.symbol]);
            }
          }
        },
        error: (err: any) => {
          // Check if error response contains validation messages
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            // Show generic error without HTTP status and URL
            const errorMessage = err.error?.message || err.statusText || 'Unknown error';
            this.error.set('Failed to save unit: ' + errorMessage);
          }
          this.saving.set(false);
        }
      });
    } else {
      // Use POST for creates
      // Build name object from form fields
      // Important: Include ALL language values from the form (visible and invisible)
      // to preserve data for languages that aren't currently shown
      const name: { [key: string]: string } = {};

      // Get all language keys from form controls
      Object.keys(formValue).forEach(key => {
        if (key.startsWith('name_')) {
          const lang = key.substring(5);
          const value = formValue[key];
          if (value && value.trim()) {
            name[lang] = value;
          }
        }
      });

      const unit: Unit = {
        symbol: formValue.symbol,
        name: name,
        measure: formValue.measure || undefined,
        baseUnitRef: formValue.baseUnitRef || undefined,
        factor: formValue.factor ? parseFloat(formValue.factor) : undefined
      };

      this.unitsService.createUnit(unit).subscribe({
        next: (savedUnit: Unit) => {
          // Check if response contains error messages
          if ((savedUnit as any).$messages && (savedUnit as any).$messages.length > 0) {
            this.handleValidationErrors((savedUnit as any).$messages);
            this.saving.set(false);
          } else {
            this.saving.set(false);
            if (this.isModalMode()) {
              this.saved.emit(savedUnit);
            } else {
              this.router.navigate(['/' + this.lang(), 'units', savedUnit.symbol]);
            }
          }
        },
        error: (err: any) => {
          // Check if error response contains validation messages
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            // Show generic error without HTTP status and URL
            const errorMessage = err.error?.message || err.statusText || 'Unknown error';
            this.error.set('Failed to save unit: ' + errorMessage);
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

        // If fields are specified, map them to the error message
        if (msg.fields && msg.fields.length > 0) {
          msg.fields.forEach((field: string) => {
            if (!fieldErrorsMap.has(field)) {
              fieldErrorsMap.set(field, []);
            }
            fieldErrorsMap.get(field)!.push(translatedMessage);
          });
        } else {
          // If no specific field, show as general error
          errorMessages.push(translatedMessage);
        }
      }
    });

    this.fieldErrors.set(fieldErrorsMap);

    // Only set general error if there are messages without specific fields
    // or if there are no field-specific errors at all
    if (errorMessages.length > 0) {
      this.error.set(errorMessages.join('; '));
    } else if (fieldErrorsMap.size > 0) {
      // If we have field-specific errors, show a generic message
      this.error.set('Please correct the errors in the form fields below.');
    } else {
      this.error.set(null);
    }
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
    } else {
      if (this.isEditMode()) {
        this.router.navigate(['/' + this.lang(), 'units', this.symbol()]);
      } else {
        this.router.navigate(['/' + this.lang(), 'units']);
      }
    }
  }

  clearBaseUnitRef(): void {
    this.form.get('baseUnitRef')?.setValue('');
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

  // Data source for base unit reference-edit
  baseUnitsDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
    // Build query for backend filtering using Lucene-like syntax
    const query = searchTerm ? `symbol:*${searchTerm}*` : undefined;
    
    return this.unitsService.getUnits(page, OPTIONS_PAGESIZE, undefined, undefined, query).pipe(
      map(response => ({
        options: response.items.map(u => ({
          value: u.symbol,
          label: u.symbol
        })),
        hasMore: response.$info.paging.page < response.$info.paging['total-pages'] - 1
      }))
    );
  };

  async onCreateNewBaseUnit(searchTerm: string): Promise<void> {
    const result = await this.modalService.open(UnitFormComponent, {
      title: 'Create New Unit',
      size: 'lg',
      data: { initialValue: searchTerm, modalMode: true }
    });

    if (result.success && result.data) {
      this.form.get('baseUnitRef')?.setValue(result.data.symbol);
    } else {
      // Clear the field if user cancelled
      this.form.get('baseUnitRef')?.setValue('');
    }
  }
}
