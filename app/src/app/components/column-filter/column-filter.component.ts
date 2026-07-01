import { Component, input, output, signal, computed, effect, ViewChild, ElementRef } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { FilterDefinition, ColumnFilterConfig, getAllowedOperators, FilterOperator } from '../../model/column-filter.model';

@Component({
  selector: 'app-column-filter',
  templateUrl: './column-filter.component.html',
  styleUrls: ['./column-filter.component.scss'],
  imports: [FormsModule, TranslocoModule]
})
export class ColumnFilterComponent {
  // Inputs
  config = input.required<ColumnFilterConfig>();
  activeFilter = input<FilterDefinition | null>(null);
  
  // Outputs
  filterApplied = output<FilterDefinition>();
  filterRemoved = output<string>();
  
  // Must match the width defined in column-filter.component.scss
  private readonly POPOVER_WIDTH = 280;

  // Template reference for position calculation
  @ViewChild('filterToggleBtn', { static: false }) filterToggleBtn!: ElementRef<HTMLButtonElement>;

  // Component state
  isOpen = signal(false);
  selectedOperator = signal<FilterOperator | null>(null);
  filterValue = signal<any>('');
  filterValueFrom = signal<any>('');
  filterValueTo = signal<any>('');
  
  // Popover position (fixed positioning, set from button's getBoundingClientRect when opening)
  popoverTop = signal<number>(0);
  popoverLeft = signal<number>(0);
  
  // Computed values
  allowedOperators = computed(() => {
    const cfg = this.config();
    return cfg.allowedOperators ?? getAllowedOperators(cfg.type);
  });
  
  isRangeOperator = computed(() => this.selectedOperator() === 'range');
  isExistsOperator = computed(() => this.selectedOperator() === 'exists');
  isMultiValueOperator = computed(() => {
    const op = this.selectedOperator();
    return op === 'hasAny' || op === 'hasAll';
  });
  needsValue = computed(() => {
    const op = this.selectedOperator();
    return op !== null && op !== 'exists';
  });
  
  hasActiveFilter = computed(() => this.activeFilter() !== null);
  
  constructor() {
    // Initialize from active filter if present
    effect(() => {
      const filter = this.activeFilter();
      if (filter) {
        this.selectedOperator.set(filter.operator);
        if (filter.operator === 'range') {
          this.filterValueFrom.set(filter.valueFrom ?? '');
          this.filterValueTo.set(filter.valueTo ?? '');
        } else if (filter.operator === 'exists') {
          this.filterValue.set(filter.value);
        } else {
          this.filterValue.set(filter.value ?? '');
        }
      }
    });
  }
  
  togglePopover(): void {
    if (!this.isOpen()) {
      // Calculate fixed position from the button's viewport coordinates so the
      // popover is never clipped by overflow:auto containers or buried under the sidebar.
      const btn = this.filterToggleBtn?.nativeElement;
      if (btn) {
        const rect = btn.getBoundingClientRect();
        const gap = 6;
        let left = rect.right - this.POPOVER_WIDTH;
        if (left < 8) left = 8; // clamp to viewport left edge
        this.popoverTop.set(rect.bottom + gap);
        this.popoverLeft.set(left);
      }
    }
    this.isOpen.update(open => !open);
    
    // Initialize operator if not set
    if (this.isOpen() && !this.selectedOperator()) {
      const operators = this.allowedOperators();
      if (operators.length > 0) {
        this.selectedOperator.set(operators[0]);
      }
    }
  }
  
  closePopover(): void {
    this.isOpen.set(false);
  }
  
  applyFilter(): void {
    const operator = this.selectedOperator();
    if (!operator) {
      return;
    }
    
    const filter: FilterDefinition = {
      field: this.config().field,
      operator
    };
    
    if (operator === 'range') {
      filter.valueFrom = this.filterValueFrom();
      filter.valueTo = this.filterValueTo();
    } else if (operator === 'exists') {
      filter.value = this.filterValue() === 'true' || this.filterValue() === true;
    } else {
      filter.value = this.filterValue();
    }
    
    this.filterApplied.emit(filter);
    this.closePopover();
  }
  
  removeFilter(): void {
    this.filterRemoved.emit(this.config().field);
    this.resetForm();
    this.closePopover();
  }
  
  resetForm(): void {
    this.selectedOperator.set(null);
    this.filterValue.set('');
    this.filterValueFrom.set('');
    this.filterValueTo.set('');
  }
  
  getOperatorLabel(operator: FilterOperator): string {
    return `common.filterOperators.${operator}`;
  }
}
