import { Component, signal, inject, OnInit, Input, Output, EventEmitter, ChangeDetectorRef, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TaxClassesService } from '../../../service/taxclass/taxclasses.service';
import { TaxClass } from '../../../model/taxclass/taxclass.model';
import { SessionService } from '../../../service/session.service';
import { PermissionService } from '../../../service/permission.service';
import { TranslocoModule } from '@jsverse/transloco';
import { MessageTranslationService } from '../../../service/message-translation.service';
import { Message } from '../../../model/message.model';
import { CountriesService } from '../../../service/country/countries.service';
import { ReferenceEditComponent, ReferenceDataSource, ReferenceDataSourceResult } from '../../../components/reference-edit/reference-edit.component';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { MetaInfo } from '../../../model/meta-info.model';
import { IsMandatoryPipe } from '../../../pipes/is-mandatory.pipe';

@Component({
  selector: 'app-taxclass-form',
  templateUrl: './taxclass-form.component.html',
  styleUrls: ['./taxclass-form.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, TranslocoModule, ReferenceEditComponent, IsMandatoryPipe],
  host: {
    '(document:keydown.s)': 'handleSaveKeyPress($event)'
  }
})
export class TaxClassFormComponent implements OnInit {
  private taxClassesService = inject(TaxClassesService);
  private countriesService = inject(CountriesService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private sessionService = inject(SessionService);
  private permissionService = inject(PermissionService);
  private messageTranslationService = inject(MessageTranslationService);

  lang = computed(() => this.sessionService.language());

  @Input() config?: { initialValue?: string; modalMode?: boolean };
  @Output() saved = new EventEmitter<TaxClass>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  isEditMode = signal(false);
  isModalMode = signal(false);
  loading = signal(true);
  saving = signal(false);
  error = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  taxClassId = signal<string | null>(null);
  fieldErrors = signal<Map<string, string[]>>(new Map());
  originalValues = signal<any>({});
  showSaveKeyHint = signal(false);
  meta = signal<MetaInfo | null>(null);

  countriesDataSource: ReferenceDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
    const query = searchTerm ? `isoKey:contains:${searchTerm}` : undefined;
    return this.countriesService.getCountries(page, 20, undefined, undefined, query).pipe(
      map((result: any) => ({
        options: (result.items || []).map((c: any) => ({ value: c.isoKey, label: c.isoKey })),
        hasMore: (result.$info?.paging?.['total-pages'] ?? 0) > page + 1
      })),
      catchError(() => of({ options: [], hasMore: false }))
    );
  };

  ngOnInit(): void {
    // Check if we're in modal mode
    if (this.config?.modalMode) {
      this.isModalMode.set(true);
      this.isEditMode.set(false); // Modal mode is always create mode
      this.loading.set(false);

      this.initForm();

      // Set initial value if provided
      if (this.config.initialValue) {
        this.form.patchValue({ taxClassId: this.config.initialValue });
      }
    } else {
      // Regular page mode
      const id = this.route.snapshot.paramMap.get('taxClassId');
      this.isEditMode.set(!!id);
      this.taxClassId.set(id);

      if (!this.permissionService.hasWritePermission('TaxClass')) {
        if (id) {
          this.router.navigate(['/' + this.lang(), 'taxclasses', id]);
        } else {
          this.router.navigate(['/' + this.lang(), 'taxclasses']);
        }
        return;
      }

      this.initForm();

      if (this.isEditMode()) {
        this.loadTaxClass(id!);
      } else {
        this.taxClassesService.getMeta().subscribe({
          next: (metaInfo) => { this.meta.set(metaInfo); this.loading.set(false); },
          error: () => this.loading.set(false)
        });
      }
    }
  }

  initForm(): void {
    this.form = this.fb.group({
      taxClassId: [{ value: '', disabled: this.isEditMode() }, [Validators.required, Validators.pattern(/^[a-z0-9-]+$/)]],
      taxRate: ['', [Validators.required, Validators.min(0), Validators.max(100)]],
      countryRef: ['']
    });
  }

  loadTaxClass(taxClassId: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.taxClassesService.getTaxClass(taxClassId).subscribe({
      next: (taxClass: TaxClass) => {
        if (taxClass.$meta) { this.meta.set(taxClass.$meta); }
        const patchData = {
          taxClassId: taxClass.taxClassId,
          taxRate: taxClass.taxRate,
          countryRef: taxClass.countryRef || ''
        };
        this.form.patchValue(patchData);
        // Store original values for change tracking
        this.originalValues.set({ ...patchData });
        this.loading.set(false);
      },
      error: (err: any) => {
        this.error.set('Failed to load tax class: ' + (err.message || 'Unknown error'));
        this.loading.set(false);
      }
    });
  }

  isMandatory(fieldName: string): boolean {
    const m = this.meta();
    if (m?.mandatoryFields) { return m.mandatoryFields.includes(fieldName); }
    return this.form?.get(fieldName)?.hasValidator(Validators.required) ?? false;
  }

  buildPatchOperations(): any[] {
    const patches: any[] = [];
    const formValue = this.form.getRawValue();
    const original = this.originalValues();

    if (formValue.taxRate !== original.taxRate) {
      patches.push({ op: 'replace', path: '/taxRate', value: parseFloat(formValue.taxRate) });
    }
    if ((formValue.countryRef || null) !== (original.countryRef || null)) {
      patches.push({ op: 'replace', path: '/countryRef', value: formValue.countryRef || null });
    }

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
          this.router.navigate(['/' + this.lang(), 'taxclasses', this.taxClassId()]);
        }
        return;
      }

      this.taxClassesService.patchTaxClass(this.taxClassId()!, patches).subscribe({
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
              this.router.navigate(['/' + this.lang(), 'taxclasses', this.taxClassId()]);
            }
          }
        },
        error: (err: any) => {
          // Check if error response contains validation messages
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set(err.error?.message || 'Failed to save tax class');
          }
          this.saving.set(false);
        }
      });
    } else {
      // Use POST for creates
      const taxClass: TaxClass = {
        taxClassId: formValue.taxClassId,
        taxRate: parseFloat(formValue.taxRate),
        countryRef: formValue.countryRef || undefined
      };

      this.taxClassesService.createTaxClass(taxClass).subscribe({
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
              this.router.navigate(['/' + this.lang(), 'taxclasses', response.taxClassId]);
            }
          }
        },
        error: (err: any) => {
          // Check if error response contains validation messages
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set(err.error?.message || 'Failed to save tax class');
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
      this.router.navigate(['/' + this.lang(), 'taxclasses', this.taxClassId()]);
    } else {
      // Navigate to list
      this.router.navigate(['/' + this.lang(), 'taxclasses']);
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
