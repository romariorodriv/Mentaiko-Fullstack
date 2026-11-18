import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ApiService } from '../../core/services/api.service';
import { Recommendation } from '../../shared/models/models';

export type ActivityHistoryStatus = 'ALL' | 'COMPLETED' | 'PENDING';

@Component({
  selector: 'app-activity-history',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './activity-history.component.html',
  styleUrl: './activity-history.component.css'
})
export class ActivityHistoryComponent implements OnInit {
  readonly recommendations = signal<Recommendation[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly status = signal<ActivityHistoryStatus>('ALL');
  readonly from = signal('');
  readonly to = signal('');

  readonly completedCount = computed(() =>
    this.recommendations().filter(item => !!item.completedAt).length
  );

  readonly filtered = computed(() => {
    const status = this.status();
    const from = this.from();
    const to = this.to();

    return this.recommendations().filter(item => {
      const completed = !!item.completedAt;
      if (status === 'COMPLETED' && !completed) return false;
      if (status === 'PENDING' && completed) return false;

      // For completed activities use their completion date; otherwise the recommendation date.
      const date = (item.completedAt ?? item.createdAt).slice(0, 10);
      return (!from || date >= from) && (!to || date <= to);
    }).sort((a, b) =>
      (b.completedAt ?? b.createdAt).localeCompare(a.completedAt ?? a.createdAt)
    );
  });

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.recommendations().subscribe({
      next: recommendations => {
        this.recommendations.set(recommendations);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar tu historial de actividades.');
        this.loading.set(false);
      }
    });
  }

  setStatus(value: string): void {
    if (value === 'ALL' || value === 'COMPLETED' || value === 'PENDING') {
      this.status.set(value);
    }
  }

  setFrom(value: string): void {
    this.from.set(value);
  }

  setTo(value: string): void {
    this.to.set(value);
  }

  clearFilters(): void {
    this.status.set('ALL');
    this.from.set('');
    this.to.set('');
  }
}
