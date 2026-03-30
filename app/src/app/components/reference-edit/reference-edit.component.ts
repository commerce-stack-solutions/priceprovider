import { Component, Input, Output, EventEmitter, signal, effect, ChangeDetectionStrategy, ElementRef, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormControl, AbstractControl } from '@angular/forms';
import { Observable, of, Subject, Subscription } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { TranslocoModule } from '@jsverse/transloco';

export interface ReferenceOption {
  value: string;
  label: string;
}

export interface ReferenceDataSourceResult {
  options: ReferenceOption[];
  hasMore: boolean;
}

export type ReferenceDataSource = (searchTerm: string, page: number) => Observable<ReferenceDataSourceResult>;

@Component({
  selector: 'app-reference-edit',
  templateUrl: './reference-edit.component.html',
  styleUrls: ['./reference-edit.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule,TranslocoModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:keydown)': 'onDocumentKeydown($event)',
    '(document:click)': 'onDocumentClick($event)'
  }
})
export class ReferenceEditComponent implements OnInit, OnDestroy {
  private _control?: FormControl;

  @Input() set inputFormControl(value: AbstractControl | null | undefined) {
    if (value instanceof FormControl) {
      this._control = value;
      this.setupInputValueListener();
    }
  }
  get inputFormControl(): FormControl | undefined {
    return this._control;
  }

  @Input() placeholder?: string;
  @Input() isInvalid: boolean = false;
  @Input() dataSource?: ReferenceDataSource;
  @Input() allowCreate: boolean = false;
  @Input() createLabel?: string;

  @Output() cleared = new EventEmitter<void>();
  @Output() createNew = new EventEmitter<string>();

  availableReferenceOptions = signal<ReferenceOption[]>([]);
  showDropdown = signal(false);
  isLoading = signal(false);
  currentPage = signal(0);
  hasMoreItems = signal(false);
  selectedIndex = signal<number>(-1);
  currentSearchTerm = signal<string>('');
  private preventBlurClose = false;
  private hasLoadedData = false;
  private searchSubject = new Subject<string>();
  private searchSubscription?: Subscription;
  private inputValueSubscription?: Subscription;

  // ElementRef of this component's host to scope document-level events correctly per instance
  private hostEl: ElementRef<HTMLElement> = inject(ElementRef);

  ngOnInit(): void {
    // Set up debounced search
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(searchTerm => {
      this.currentSearchTerm.set(searchTerm);
      this.performSearch(searchTerm);
    });
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
    this.inputValueSubscription?.unsubscribe();
  }

  private setupInputValueListener(): void {
    // Clean up previous subscription
    this.inputValueSubscription?.unsubscribe();
    
    if (this._control) {
      this.inputValueSubscription = this._control.valueChanges.subscribe(value => {
        const searchTerm = (value || '').trim();
        this.searchSubject.next(searchTerm);
      });
    }
  }

  private performSearch(searchTerm: string): void {
    if (!this.dataSource || !this.showDropdown()) {
      return;
    }
    
    // Reset to first page when search term changes
    this.currentPage.set(0);
    this.availableReferenceOptions.set([]);
    this.loadOptions(0, searchTerm);
  }

  onInputFocus(): void {
    if (!this.dataSource) {
      return;
    }
    if (!this.hasLoadedData) {
      // Always show dropdown on click
      this.loadInitialOptions();
    }
    this.showDropdown.set(true);
  }
  
  onInputClick(): void {
    if (!this.dataSource) {
      return;
    }
    if (!this.hasLoadedData) {
      // Always show dropdown on click
      this.loadInitialOptions();
    }
    this.showDropdown.set(true);
  }

  onInputBlur(): void {
    // Close dropdown when focus is lost
    // Use setTimeout to allow click events on dropdown items to fire first
    setTimeout(() => {
      // Don't close if we're preventing blur (e.g., when loading more items)
      if (this.preventBlurClose) {
        this.preventBlurClose = false;
        return;
      }
      this.showDropdown.set(false);
      this.selectedIndex.set(-1);
    }, 200);
  }

  onKeydown(event: KeyboardEvent): void {
    const key = event.key;

    // Handle Arrow Down - navigate down in list
    if (key === 'ArrowDown') {
      event.preventDefault();
      if (this.showDropdown()) {
        const totalOptions = this.availableReferenceOptions().length +
          (this.hasMoreItems() ? 1 : 0) +
          (this.shouldShowCreateOption() ? 1 : 0);
        const newIndex = this.selectedIndex() < totalOptions - 1 ? this.selectedIndex() + 1 : 0;
        this.selectedIndex.set(newIndex);
      }
      return;
    }

    // Handle Arrow Up - navigate up in list
    if (key === 'ArrowUp') {
      event.preventDefault();
      if (this.showDropdown()) {
        const totalOptions = this.availableReferenceOptions().length +
          (this.hasMoreItems() ? 1 : 0) +
          (this.shouldShowCreateOption() ? 1 : 0);
        const newIndex = this.selectedIndex() > 0 ? this.selectedIndex() - 1 : totalOptions - 1;
        this.selectedIndex.set(newIndex);
      }
      return;
    }

    // Handle Enter - select highlighted option
    if (key === 'Enter' && !event.defaultPrevented) {
      if (this.showDropdown() && this.selectedIndex() >= 0) {
        event.preventDefault();
        const options = this.availableReferenceOptions();
        const loadMoreIndex = options.length;
        const createIndex = loadMoreIndex + (this.hasMoreItems() ? 1 : 0);

        if (this.selectedIndex() < options.length) {
          // Select an option
          this.selectOption(options[this.selectedIndex()]);
        } else if (this.hasMoreItems() && this.selectedIndex() === loadMoreIndex) {
          // Load more
          this.loadMore();
        } else if (this.shouldShowCreateOption() && this.selectedIndex() === createIndex) {
          // Create new
          this.onCreateNew(event);
        }
      }
      return;
    }

    // Handle Delete key - clear the reference
    if (key === 'Delete' && this.inputFormControl?.value) {
      event.preventDefault();
      this.clear(event);
      return;
    }

    // Reset selection on typing
    this.selectedIndex.set(-1);
  }

  // Close dropdown when clicking anywhere outside the input + dropdown wrapper
  onDocumentClick(event: MouseEvent): void {
    if (!this.showDropdown()) return;
    const target = event.target as HTMLElement;
    const root = this.hostEl.nativeElement as HTMLElement;
    const wrapper = root.querySelector('.reference-input-wrapper') as HTMLElement | null;
    const insideThisWrapper = !!(wrapper && wrapper.contains(target));
    if (!insideThisWrapper) {
      this.showDropdown.set(false);
      this.selectedIndex.set(-1);
    }
  }

  // Handle global ESC to close dropdown regardless of focused element
  onDocumentKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Escape') return;
    if (!this.showDropdown()) return;
    event.preventDefault();
    this.showDropdown.set(false);
    this.selectedIndex.set(-1);
    // Blur focused element if it's within the input wrapper to reflect closed state
    const active = document.activeElement as HTMLElement | null;
    const root = this.hostEl.nativeElement as HTMLElement;
    const wrapper = root.querySelector('.reference-input-wrapper') as HTMLElement | null;
    if (active && wrapper && wrapper.contains(active)) {
      active.blur();
    }
  }

  private loadInitialOptions(): void {
    this.currentPage.set(0);
    this.availableReferenceOptions.set([]);
    const searchTerm = this.currentSearchTerm();
    this.loadOptions(0, searchTerm);
  }

  private loadOptions(page: number, searchTerm: string): void {
    if (!this.dataSource) return;
    this.isLoading.set(true);

    this.dataSource(searchTerm, page).pipe(
      catchError(() => of({ options: [], hasMore: false }))
    ).subscribe(result => {
      // Append new options to existing ones if loading more
      if (page === 0) {
        this.availableReferenceOptions.set(result.options);
      } else {
        this.availableReferenceOptions.set([...this.availableReferenceOptions(), ...result.options]);
      }

      this.hasMoreItems.set(result.hasMore);
      this.isLoading.set(false);
      this.hasLoadedData = true;
    });
  }

  loadMore(): void {
    // Prevent blur from closing the dropdown
    this.preventBlurClose = true;
    const nextPage = this.currentPage() + 1;
    this.currentPage.set(nextPage);
    const searchTerm = this.currentSearchTerm();
    this.loadOptions(nextPage, searchTerm);
  }

  selectOption(option: ReferenceOption): void {
    if (this.inputFormControl) {
      this.inputFormControl.setValue(option.value);
      this.showDropdown.set(false);
    }
  }

  clear(event: Event): void {
    event.stopPropagation();
    if (this.inputFormControl) {
      this.inputFormControl.setValue('');
      this.cleared.emit();
      this.showDropdown.set(false);
    }
  }

  onCreateNew(event: Event): void {
    event.stopPropagation();
    const value = this.inputFormControl?.value?.trim();
    if (!value) return;
    this.createNew.emit(value);
    this.showDropdown.set(false);
  }

  shouldShowCreateOption(): boolean {
    const value = this.inputFormControl?.value?.trim();
    return this.allowCreate && !!value && value.length > 0;
  }
}