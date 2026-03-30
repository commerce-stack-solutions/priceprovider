import { Component, signal, inject, OnInit, computed, Input, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CountriesService } from '../../../service/country/countries.service';
import { CurrenciesService } from '../../../service/currency/currencies.service';
import { Country } from '../../../model/country/country.model';
import { MetaInfo } from '../../../model/meta-info.model';
import { SessionService } from '../../../service/session.service';
import { PermissionService } from '../../../service/permission.service';
import { LocalizedStringfieldEditComponent } from '../../../components/localized-stringfield-edit/localized-stringfield-edit.component';
import { ReferenceListEditComponent, ReferenceDataSourceResult } from '../../../components/referencelist-edit/referencelist-edit.component';
import { TranslocoModule } from '@jsverse/transloco';
import { MessageTranslationService } from '../../../service/message-translation.service';
import { Message } from '../../../model/message.model';
import { IsMandatoryPipe } from '../../../pipes/is-mandatory.pipe';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Component({
  selector: 'app-country-form',
  templateUrl: './country-form.component.html',
  styleUrls: ['./country-form.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, LocalizedStringfieldEditComponent, ReferenceListEditComponent, RouterModule, TranslocoModule, IsMandatoryPipe],
  host: { '(document:keydown.s)': 'handleSaveKeyPress($event)' }
})
export class CountryFormComponent implements OnInit {
  private countriesService = inject(CountriesService);
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
  @Output() saved = new EventEmitter<Country>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  isEditMode = signal(false);
  isModalMode = signal(false);
  loading = signal(true);
  saving = signal(false);
  error = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  isoKey = signal<string | null>(null);
  fieldErrors = signal<Map<string, string[]>>(new Map());
  originalValues = signal<any>({});
  showSaveKeyHint = signal(false);
  originalCountry = signal<Country | null>(null);
  meta = signal<MetaInfo | null>(null);

  languages = signal<string[]>([]);

  mandatoryLanguages = computed(() => this.sessionService.availableLanguages().filter(l => l.mandatory).map(l => l.isoKey));
  activeLanguages = computed(() => this.sessionService.availableLanguages().filter(l => l.active && !l.mandatory).map(l => l.isoKey));
  inactiveLanguages = computed(() => this.sessionService.availableLanguages().filter(l => !l.active).map(l => l.isoKey));

  currenciesDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
    const query = searchTerm ? `currencyKey:*${searchTerm}*` : undefined;
    return this.currenciesService.getCurrencies(page, 20, undefined, undefined, query).pipe(
      map(response => ({
        options: response.items.map(c => ({ value: c.currencyKey, label: c.currencyKey })),
        hasMore: response.$info.paging.page < response.$info.paging['total-pages'] - 1
      }))
    );
  };

  displayedLanguages = computed(() => {
    const displayed = [...this.mandatoryLanguages(), ...this.activeLanguages()];
    if (this.isEditMode()) {
      this.inactiveLanguages().forEach(lang => {
        const value = this.form?.get(`name_${lang}`)?.value;
        if (value && value.trim() !== '') displayed.push(lang);
      });
    }
    return displayed;
  });

  isMandatory(fieldName: string): boolean {
    const m = this.meta();
    if (m?.mandatoryFields) { return m.mandatoryFields.includes(fieldName); }
    return this.form?.get(fieldName)?.hasValidator(Validators.required) ?? false;
  }

  isLanguageMandatory(lang: string): boolean { return this.mandatoryLanguages().includes(lang); }
  isInactive(lang: string): boolean { return this.inactiveLanguages().includes(lang); }

  ngOnInit(): void {
    if (this.config?.modalMode) {
      this.isModalMode.set(true);
      this.isEditMode.set(false);
      if (this.sessionService.availableLanguages().length > 0) {
        this.initForm();
        this.languages.set([...this.mandatoryLanguages()]);
        this.loading.set(false);
        if (this.config?.initialValue) this.form.patchValue({ isoKey: this.config.initialValue.toUpperCase() });
      } else {
        setTimeout(() => {
          this.initForm();
          this.languages.set([...this.mandatoryLanguages()]);
          this.loading.set(false);
          if (this.config?.initialValue) this.form.patchValue({ isoKey: this.config.initialValue.toUpperCase() });
        }, 100);
      }
    } else {
      const key = this.route.snapshot.paramMap.get('isoKey');
      this.isEditMode.set(!!key);
      this.isoKey.set(key);
      if (!this.permissionService.hasWritePermission('Country')) {
        if (key) {
          this.router.navigate(['/' + this.lang(), 'countries', key]);
        } else {
          this.router.navigate(['/' + this.lang(), 'countries']);
        }
        return;
      }
      if (this.sessionService.availableLanguages().length > 0) {
        this.initFormAndLoadData(key);
      } else {
        setTimeout(() => this.initFormAndLoadData(key), 100);
      }
    }
  }

  private initFormAndLoadData(key: string | null): void {
    this.initForm();
    if (this.isEditMode()) {
      this.loadCountry(key!);
    } else {
      this.languages.set([...this.mandatoryLanguages(), ...this.activeLanguages()]);
      this.countriesService.getMeta().subscribe({
        next: (metaInfo) => { this.meta.set(metaInfo); this.loading.set(false); },
        error: () => this.loading.set(false)
      });
    }
  }

  initForm(): void {
    const formConfig: any = {
      isoKey: [{ value: '', disabled: this.isEditMode() }, [Validators.required, Validators.pattern(/^[A-Z]{2}$/)]],
      allowedCurrencyRefs: [[]],
      primaryCurrencyRef: ['']
    };
    const allLanguages = this.sessionService.availableLanguages().map(l => l.isoKey);
    allLanguages.forEach(lang => {
      const isMandatory = this.mandatoryLanguages().includes(lang);
      formConfig[`name_${lang}`] = ['', isMandatory ? Validators.required : []];
    });
    this.form = this.fb.group(formConfig);
  }

  loadCountry(isoKey: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.countriesService.getCountry(isoKey).subscribe({
      next: (country: Country) => {
        this.originalCountry.set(country);
        if (country.$meta) { this.meta.set(country.$meta); }
        const patchData: any = { isoKey: country.isoKey };
        if (country.name) Object.keys(country.name).forEach(lang => { patchData[`name_${lang}`] = country.name![lang]; });
        patchData['allowedCurrencyRefs'] = country.allowedCurrencyRefs || [];
        patchData['primaryCurrencyRef'] = country.primaryCurrencyRef || '';
        this.form.patchValue(patchData);
        this.originalValues.set({ ...patchData });
        const languagesToDisplay = [...this.mandatoryLanguages(), ...this.activeLanguages()];
        if (country.name) {
          this.inactiveLanguages().forEach(lang => {
            if (country.name![lang] && country.name![lang].trim() !== '' && !languagesToDisplay.includes(lang)) languagesToDisplay.push(lang);
          });
        }
        this.languages.set(languagesToDisplay);
        this.loading.set(false);
      },
      error: (err: any) => { this.error.set('Failed to load country: ' + (err.message || 'Unknown error')); this.loading.set(false); }
    });
  }

  removeLanguage(lang: string): void {
    if (this.isLanguageMandatory(lang) || this.isInactive(lang)) return;
    this.languages.set(this.languages().filter(l => l !== lang));
    const control = this.form.get(`name_${lang}`);
    if (control) control.setValue('', { emitEvent: true });
  }

  addLanguage(lang: string): void {
    if (!this.languages().includes(lang)) this.languages.set([...this.languages(), lang]);
  }

  buildPatchOperations(): any[] {
    const patches: any[] = [];
    const formValue = this.form.getRawValue();
    const original = this.originalValues();
    Object.keys(formValue).forEach(key => {
      if (key.startsWith('name_')) {
        const lang = key.substring(5);
        const currentValue = formValue[key] || '';
        const originalValue = original[key] || '';
        if (currentValue !== originalValue) {
          if (currentValue === '' || currentValue === null) {
            if (originalValue !== '' && originalValue !== null && originalValue !== undefined) patches.push({ op: 'remove', path: `/name/${lang}` });
          } else {
            if (originalValue === '' || originalValue === null || originalValue === undefined) patches.push({ op: 'add', path: `/name/${lang}`, value: currentValue });
            else patches.push({ op: 'replace', path: `/name/${lang}`, value: currentValue });
          }
        }
      }
    });
    const currentAllowed = formValue['allowedCurrencyRefs'] || [];
    const originalAllowed = original['allowedCurrencyRefs'] || [];
    if (JSON.stringify([...currentAllowed].sort()) !== JSON.stringify([...originalAllowed].sort())) {
      patches.push({ op: 'replace', path: '/allowedCurrencyRefs', value: currentAllowed });
    }
    const currentPrimary = formValue['primaryCurrencyRef'] || '';
    const originalPrimary = original['primaryCurrencyRef'] || '';
    if (currentPrimary !== originalPrimary) {
      if (!currentPrimary) {
        patches.push({ op: 'remove', path: '/primaryCurrencyRef' });
      } else if (!originalPrimary) {
        patches.push({ op: 'add', path: '/primaryCurrencyRef', value: currentPrimary });
      } else {
        patches.push({ op: 'replace', path: '/primaryCurrencyRef', value: currentPrimary });
      }
    }
    return patches;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      Object.keys(this.form.controls).forEach(key => this.form.get(key)?.markAsTouched());
      this.cdr.detectChanges();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    const formValue = this.form.getRawValue();

    if (this.isEditMode()) {
      const patches = this.buildPatchOperations();
      if (patches.length === 0) {
        this.saving.set(false);
        if (this.isModalMode()) this.cancelled.emit();
        else this.router.navigate(['/' + this.lang(), 'countries', this.isoKey()]);
        return;
      }
      this.countriesService.patchCountry(this.isoKey()!, patches).subscribe({
        next: (response: any) => {
          if (response.$messages && response.$messages.length > 0) { this.handleValidationErrors(response.$messages); this.saving.set(false); }
          else { if (this.isModalMode()) { this.saving.set(false); this.saved.emit(response); } else { this.saving.set(false); this.router.navigate(['/' + this.lang(), 'countries', this.isoKey()]); } }
        },
        error: (err: any) => {
          if (err.error && err.error.$messages && err.error.$messages.length > 0) this.handleValidationErrors(err.error.$messages);
          else this.error.set(err.error?.message || 'Failed to save country');
          this.saving.set(false);
        }
      });
    } else {
      const name: { [key: string]: string } = {};
      Object.keys(formValue).forEach(key => {
        if (key.startsWith('name_')) { const lang = key.substring(5); const value = formValue[key]; if (value && value.trim()) name[lang] = value; }
      });
      const country: Country = {
        isoKey: formValue.isoKey,
        name: Object.keys(name).length > 0 ? name : undefined,
        allowedCurrencyRefs: formValue.allowedCurrencyRefs || [],
        primaryCurrencyRef: formValue.primaryCurrencyRef || undefined
      };
      this.countriesService.createCountry(country).subscribe({
        next: (response: any) => {
          if (response.$messages && response.$messages.length > 0) { this.handleValidationErrors(response.$messages); this.saving.set(false); }
          else { this.saving.set(false); if (this.isModalMode()) this.saved.emit(response); else this.router.navigate(['/' + this.lang(), 'countries', response.isoKey]); }
        },
        error: (err: any) => {
          if (err.error && err.error.$messages && err.error.$messages.length > 0) this.handleValidationErrors(err.error.$messages);
          else this.error.set(err.error?.message || 'Failed to save country');
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
        const translatedMessage = this.messageTranslationService.translateMessage(msg);
        errorMessages.push(translatedMessage);
        if (msg.fields && msg.fields.length > 0) {
          msg.fields.forEach((field: string) => {
            if (!fieldErrorsMap.has(field)) fieldErrorsMap.set(field, []);
            fieldErrorsMap.get(field)!.push(translatedMessage);
          });
        }
      }
    });
    this.fieldErrors.set(fieldErrorsMap);
    this.error.set(errorMessages.join('; '));
  }

  hasFieldError(fieldName: string): boolean { return this.fieldErrors().has(fieldName); }
  getFieldErrors(fieldName: string): string[] { return this.fieldErrors().get(fieldName) || []; }

  cancel(): void {
    if (this.isModalMode()) this.cancelled.emit();
    else if (this.isEditMode()) this.router.navigate(['/' + this.lang(), 'countries', this.isoKey()]);
    else this.router.navigate(['/' + this.lang(), 'countries']);
  }

  handleSaveKeyPress(event: Event): void {
    if (!(event instanceof KeyboardEvent)) return;
    const target = event.target as HTMLElement | null;
    if (this.isModalMode() || (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA'))) return;
    event.preventDefault();
    this.showSaveKeyHint.set(true);
    this.onSubmit();
    setTimeout(() => this.showSaveKeyHint.set(false), 500);
  }
}
