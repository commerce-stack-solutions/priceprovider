
import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ReferenceDataSourceResult, ReferenceEditComponent } from '../../../components/reference-edit/reference-edit.component';
import { ReferenceListEditComponent } from '../../../components/referencelist-edit/referencelist-edit.component';
import { EnumSelectorComponent } from '../../../components/enum-selector/enum-selector.component';
import { PriceRow } from '../../../model/pricerow/price-row.model';
import { CurrenciesService } from '../../../service/currency/currencies.service';
import { GroupsService } from '../../../service/group/groups.service';
import { ChannelsService } from '../../../service/channel/channels.service';
import { ModalService } from '../../../service/modal.service';
import { PricerowsService } from '../../../service/pricerow/pricerows.service';
import { TaxClassesService } from '../../../service/taxclass/taxclasses.service';
import { UnitsService } from '../../../service/unit/units.service';
import { CurrencyFormComponent } from '../../currency/form/currency-form.component';
import { TaxClassFormComponent } from '../../taxclass/form/taxclass-form.component';
import { UnitFormComponent } from '../../unit/form/unit-form.component';
import { SessionService } from '../../../service/session.service';
import { PermissionService } from '../../../service/permission.service';
import { TranslocoModule } from '@jsverse/transloco';
import { MessageTranslationService } from '../../../service/message-translation.service';
import { Message } from '../../../model/message.model';
import { MetaInfo } from '../../../model/meta-info.model';
import { IsMandatoryPipe } from '../../../pipes/is-mandatory.pipe';

const OPTIONS_PAGESIZE = 30;
@Component({
  selector: 'app-pricerow-form',
  templateUrl: './pricerow-form.component.html',
  styleUrls: ['./pricerow-form.component.scss'],
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, RouterModule, ReferenceEditComponent, ReferenceListEditComponent, EnumSelectorComponent, TranslocoModule, IsMandatoryPipe],
  host: {
    '(document:keydown.s)': 'handleSaveKeyPress($event)'
  }
})
export class PricerowFormComponent implements OnInit {
  private pricerowsService = inject(PricerowsService);
  private currenciesService = inject(CurrenciesService);
  private unitsService = inject(UnitsService);
  private taxClassesService = inject(TaxClassesService);
  private groupsService = inject(GroupsService);
  private channelsService = inject(ChannelsService);
  private modalService = inject(ModalService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private sessionService = inject(SessionService);
  private permissionService = inject(PermissionService);
  private messageTranslationService = inject(MessageTranslationService);

  lang = computed(() => this.sessionService.language());

  form!: FormGroup;
  isEditMode = signal(false);
  loading = signal(true);
  saving = signal(false);
  error = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  id = signal<string | null>(null);
  fieldErrors = signal<Map<string, string[]>>(new Map());
  originalValues = signal<any>({});
  showSaveKeyHint = signal(false);
  meta = signal<MetaInfo | null>(null);

  // Data source for units reference-edit
  unitsDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
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

  // Data source for currency reference-edit
  currenciesDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
    // Build query for backend filtering - search in currencyKey OR symbol
    const query = searchTerm ? `currencyKey:*${searchTerm}* OR symbol:*${searchTerm}*` : undefined;

    return this.currenciesService.getCurrencies(page, OPTIONS_PAGESIZE, undefined, undefined, query).pipe(
      map(response => ({
        options: response.items.map(c => ({
          value: c.currencyKey,
          label: `${c.currencyKey}${c.symbol ? ' (' + c.symbol + ')' : ''}`
        })),
        hasMore: response.$info.paging.page < response.$info.paging['total-pages'] - 1
      }))
    );
  };

  // Data source for tax class reference-edit
  taxClassesDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
    // Build query for backend filtering
    const query = searchTerm ? `taxClassId:*${searchTerm}*` : undefined;

    return this.taxClassesService.getTaxClasses(page, OPTIONS_PAGESIZE, undefined, undefined, query).pipe(
      map(response => ({
        options: response.items.map(tc => ({
          value: tc.taxClassId,
          label: `${tc.taxClassId} (${tc.taxRate.toFixed(2)}%)`
        })),
        hasMore: response.$info.paging.page < response.$info.paging['total-pages'] - 1
      }))
    );
  };

  // Data source for group reference-edit
  groupsDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
    // Build query for backend filtering - search in path OR name
    const query = searchTerm ? `path:*${searchTerm}* OR name:*${searchTerm}*` : undefined;

    return this.groupsService.getGroups(page, OPTIONS_PAGESIZE, undefined, undefined, undefined, query).pipe(
      map(response => ({
        options: response.items.filter(g => g.path).map(g => ({
          value: g.path as string,
          label: `${g.path}${g.name ? ' - ' + g.name : ''}`
        })),
        hasMore: response.$info?.paging ? response.$info.paging.page < response.$info.paging['total-pages'] - 1 : false
      }))
    );
  };

  // Data source for channel reference-edit
  channelsDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
    const query = searchTerm ? `id:*${searchTerm}*` : undefined;

    return this.channelsService.getChannels(page, OPTIONS_PAGESIZE, undefined, undefined, query).pipe(
      map(response => ({
        options: response.items.map(c => ({
          value: c.id,
          label: c.id
        })),
        hasMore: response.$info?.paging ? response.$info.paging.page < response.$info.paging['total-pages'] - 1 : false
      }))
    );
  };

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.isEditMode.set(!!id);
    this.id.set(id ?? null);

    if (!this.permissionService.hasWritePermission('PriceRow')) {
      if (id) {
        this.router.navigate(['/' + this.lang(), 'pricerows', id]);
      } else {
        this.router.navigate(['/' + this.lang(), 'pricerows']);
      }
      return;
    }

    this.initForm();

    if (this.isEditMode()) {
      this.loadPriceRow(this.id()!);
    } else {
      this.pricerowsService.getMeta().subscribe({
        next: (metaInfo) => { this.meta.set(metaInfo); this.loading.set(false); },
        error: () => this.loading.set(false)
      });
    }
  }

  initForm(): void {
    this.form = this.fb.group({
      priceType: ['', Validators.required],
      pricedResourceId: ['', Validators.required],
      channelRefs: [[]],
      priceValue: ['', [Validators.required, Validators.pattern(/^\d*\.?\d*$/)]],
      minQuantity: ['', [Validators.required, Validators.pattern(/^\d*\.?\d*$/)]],
      unitRef: ['', Validators.required],
      currency: ['', Validators.required],
      taxClassRef: ['', Validators.required],
      taxIncluded: [false, Validators.required],
      validFrom: [''],
      validTo: [''],
      groupRefs: [[]]
    });
  }

  loadPriceRow(id: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.pricerowsService.getPriceRow(id).subscribe({
      next: (priceRow: PriceRow) => {
        if ((priceRow as any).$meta) { this.meta.set((priceRow as any).$meta); }
        const formData = {
          priceType: priceRow.priceType || '',
          pricedResourceId: priceRow.pricedResourceId,
          channelRefs: priceRow.channelRefs || [],
          priceValue: priceRow.priceValue?.toString() || '',
          minQuantity: priceRow.minQuantity?.toString() || '',
          unitRef: priceRow.unitRef || '',
          currency: priceRow.currencyRef || '',
          taxClassRef: priceRow.taxClassRef || '',
          taxIncluded: priceRow.taxIncluded,
          validFrom: priceRow.validFrom ? this.formatDateForInput(priceRow.validFrom) : '',
          validTo: priceRow.validTo ? this.formatDateForInput(priceRow.validTo) : '',
          groupRefs: priceRow.groupRefs || []
        };
        this.form.patchValue(formData);
        // Store original values for change tracking
        this.originalValues.set({ ...formData });
        this.loading.set(false);
      },
      error: (err: any) => {
        this.error.set('Failed to load price row: ' + (err.message || 'Unknown error'));
        this.loading.set(false);
      }
    });
  }

  isMandatory(fieldName: string): boolean {
    const m = this.meta();
    if (m?.mandatoryFields) { return m.mandatoryFields.includes(fieldName); }
    return this.form?.get(fieldName)?.hasValidator(Validators.required) ?? false;
  }

  formatDateForInput(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toISOString().slice(0, 16); // Format for datetime-local input
  }

  buildPatchOperations(): any[] {
    const patches: any[] = [];
    const formValue = this.form.value;
    const original = this.originalValues();

    // Compare each field and create patch operations for changes
    Object.keys(formValue).forEach(key => {
      const currentValue = formValue[key];
      const originalValue = original[key];

      // Convert values for comparison
      let compareCurrentValue = currentValue;
      let compareOriginalValue = originalValue;

      // Handle numeric fields
      if (key === 'priceValue' || key === 'minQuantity') {
        compareCurrentValue = currentValue ? parseFloat(currentValue) : null;
        compareOriginalValue = originalValue ? parseFloat(originalValue) : null;
      }

      // Handle date fields
      if (key === 'validFrom' || key === 'validTo') {
        compareCurrentValue = currentValue ? new Date(currentValue).toISOString() : null;
        compareOriginalValue = originalValue ? new Date(originalValue).toISOString() : null;
      }

      // Handle currency field mapping (form uses 'currency', API uses 'currencyRef')
      const apiFieldName = key === 'currency' ? 'currencyRef' : key;

      if (compareCurrentValue !== compareOriginalValue) {
        if (currentValue === '' || currentValue === null || currentValue === undefined) {
          // Only send remove if field existed in original (not empty/null/undefined)
          if (compareOriginalValue !== '' && compareOriginalValue !== null && compareOriginalValue !== undefined) {
            patches.push({ op: 'remove', path: `/${apiFieldName}` });
          }
        } else {
          // Use 'add' if field didn't exist in original, 'replace' if it did
          let patchValue = compareCurrentValue;
          if (compareOriginalValue === '' || compareOriginalValue === null || compareOriginalValue === undefined) {
            patches.push({ op: 'add', path: `/${apiFieldName}`, value: patchValue });
          } else {
            patches.push({ op: 'replace', path: `/${apiFieldName}`, value: patchValue });
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
      return;
    }

    this.saving.set(true);
    this.error.set(null);

    const formValue = this.form.value;

    if (this.isEditMode()) {
      // Use PATCH for updates
      const patches = this.buildPatchOperations();

      if (patches.length === 0) {
        // No changes, just navigate back
        this.saving.set(false);
        this.router.navigate(['/' + this.lang(), 'pricerows', this.id()]);
        return;
      }

      this.pricerowsService.patchPriceRow(this.id()!, patches).subscribe({
        next: (savedPriceRow: PriceRow) => {
          // Check if response contains error messages
          if ((savedPriceRow as any).$messages && (savedPriceRow as any).$messages.length > 0) {
            this.handleValidationErrors((savedPriceRow as any).$messages);
            this.saving.set(false);
          } else {
            // Navigate to detail view after successful save
            this.saving.set(false);
            this.router.navigate(['/' + this.lang(), 'pricerows', savedPriceRow.id]);
          }
        },
        error: (err: any) => {
          // Check if error response contains $messages array
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set('Failed to save price row: ' + (err.error?.message || err.message || 'Unknown error'));
          }
          this.saving.set(false);
        }
      });
    } else {
      // Use POST for creates
      const basePriceRow = {
        priceType: formValue.priceType,
        pricedResourceId: formValue.pricedResourceId,
        channelRefs: formValue.channelRefs || [],
        priceValue: parseFloat(formValue.priceValue),
        minQuantity: parseFloat(formValue.minQuantity),
        unitRef: formValue.unitRef,
        currencyRef: formValue.currency,
        taxClassRef: formValue.taxClassRef || undefined,
        taxIncluded: formValue.taxIncluded,
        validFrom: formValue.validFrom ? new Date(formValue.validFrom).toISOString() : undefined,
        validTo: formValue.validTo ? new Date(formValue.validTo).toISOString() : undefined,
        groupRefs: formValue.groupRefs || []
      };

      this.pricerowsService.createPriceRow(basePriceRow as PriceRow).subscribe({
        next: (savedPriceRow: PriceRow) => {
          // Check if response contains error messages
          if ((savedPriceRow as any).$messages && (savedPriceRow as any).$messages.length > 0) {
            this.handleValidationErrors((savedPriceRow as any).$messages);
            this.saving.set(false);
          } else {
            this.saving.set(false);
            this.router.navigate(['/' + this.lang(), 'pricerows', savedPriceRow.id]);
          }
        },
        error: (err: any) => {
          // Check if error response contains $messages array
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set('Failed to save price row: ' + (err.error?.message || err.message || 'Unknown error'));
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
    if (this.isEditMode()) {
      this.router.navigate(['/' + this.lang(), 'pricerows', this.id()]);
    } else {
      this.router.navigate(['/' + this.lang(), 'pricerows']);
    }
  }

  handleSaveKeyPress(event: Event): void {
    // Ensure keyboard event and guard against input/textarea targets
    if (!(event instanceof KeyboardEvent)) return;
    const target = event.target as HTMLElement | null;
    if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA')) {
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

  clearUnitRef(): void {
    this.form.get('unitRef')?.setValue('');
  }

  clearCurrency(): void {
    this.form.get('currency')?.setValue('');
  }

  clearTaxClassRef(): void {
    this.form.get('taxClassRef')?.setValue('');
  }

  clearGroupRefs(): void {
    this.form.get('groupRefs')?.setValue([]);
  }

  clearChannelRefs(): void {
    this.form.get('channelRefs')?.setValue([]);
  }

  onGroupRefsChanged(groupRefs: string[]): void {
    // Handle group refs change if needed
    // The referencelist-edit component already updates the form control
  }

  onChannelRefsChanged(channelRefs: string[]): void {
    // Handle channel refs change if needed
    // The referencelist-edit component already updates the form control
  }

  async onCreateNewUnit(searchTerm: string): Promise<void> {
    const result = await this.modalService.open(UnitFormComponent, {
      title: 'Create New Unit',
      size: 'lg',
      data: { initialValue: searchTerm, modalMode: true }
    });

    if (result.success && result.data) {
      this.form.get('unitRef')?.setValue(result.data.symbol);
    } else {
      // Clear the field if user cancelled
      this.form.get('unitRef')?.setValue('');
    }
  }

  async onCreateNewCurrency(searchTerm: string): Promise<void> {
    const result = await this.modalService.open(CurrencyFormComponent, {
      title: 'Create New Currency',
      data: { initialValue: searchTerm, modalMode: true }
    });

    if (result.success && result.data) {
      this.form.get('currency')?.setValue(result.data.currencyKey);
    } else {
      // Clear the field if user cancelled
      this.form.get('currency')?.setValue('');
    }
  }

  async onCreateNewTaxClass(searchTerm: string): Promise<void> {
    const result = await this.modalService.open(TaxClassFormComponent, {
      title: 'Create New Tax Class',
      data: { initialValue: searchTerm, modalMode: true }
    });

    if (result.success && result.data) {
      this.form.get('taxClassRef')?.setValue(result.data.taxClassId);
    } else {
      // Clear the field if user cancelled
      this.form.get('taxClassRef')?.setValue('');
    }
  }
}
