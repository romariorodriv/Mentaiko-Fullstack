import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ActivitiesService } from '../../core/services/activities.service';
import { RecommendationsService } from '../../core/services/recommendations.service';
import { GuidedMicroActivity, Recommendation } from '../../shared/models/models';
import { GuidedActivityComponent } from '../guided-activity/guided-activity.component';

export type ActivityHistoryStatus = 'ALL' | 'COMPLETED' | 'PENDING' | 'DISCARDED';

@Component({
  selector: 'app-activity-history',
  standalone: true,
  imports: [DatePipe, RouterLink, GuidedActivityComponent],
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
  readonly stars = [1, 2, 3, 4, 5];

  /** HU-29: historial de realizadas desde GET /api/recommendations/me/history (filtrado y ordenado en el servidor). */
  readonly completedHistory = signal<Recommendation[] | null>(null);
  readonly historyLoading = signal(false);
  readonly ratingBusyId = signal<number | null>(null);
  readonly guide = signal<GuidedMicroActivity | null>(null);
  readonly guideLoading = signal(false);
  readonly success = signal('');

  readonly dateError = computed(() => {
    const from = this.from();
    const to = this.to();
    return from && to && from > to ? 'La fecha inicial no puede ser posterior a la fecha final.' : '';
  });

  readonly completedCount = computed(() =>
    this.recommendations().filter(item => !!item.completedAt).length
  );

  readonly filtered = computed(() => {
    const history = this.completedHistory();
    if (this.status() === 'COMPLETED' && history !== null) {
      return history;
    }

    const status = this.status();
    const from = this.from();
    const to = this.to();

    return this.recommendations().filter(item => {
      const itemStatus = item.completedAt ? 'COMPLETED' : item.discardedAt ? 'DISCARDED' : 'PENDING';
      if (status !== 'ALL' && status !== itemStatus) return false;

      const date = (item.completedAt ?? item.discardedAt ?? item.createdAt).slice(0, 10);
      return (!from || date >= from) && (!to || date <= to);
    }).sort((a, b) =>
      (b.completedAt ?? b.discardedAt ?? b.createdAt).localeCompare(a.completedAt ?? a.discardedAt ?? a.createdAt)
    );
  });

  constructor(
    private readonly recommendationsApi: RecommendationsService,
    private readonly activitiesApi: ActivitiesService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.recommendationsApi.recommendations().subscribe({
      next: recommendations => {
        this.recommendations.set(recommendations);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar tu historial de actividades.');
        this.loading.set(false);
      }
    });
    if (this.status() === 'COMPLETED') {
      this.loadCompletedHistory();
    }
  }

  setStatus(value: string): void {
    if (value === 'ALL' || value === 'COMPLETED' || value === 'PENDING' || value === 'DISCARDED') {
      this.status.set(value);
      if (value === 'COMPLETED') {
        this.loadCompletedHistory();
      } else {
        this.completedHistory.set(null);
      }
    }
  }

  setFrom(value: string): void {
    this.from.set(value);
    this.reloadCompletedHistoryIfNeeded();
  }

  setTo(value: string): void {
    this.to.set(value);
    this.reloadCompletedHistoryIfNeeded();
  }

  clearFilters(): void {
    this.status.set('ALL');
    this.from.set('');
    this.to.set('');
    this.completedHistory.set(null);
  }

  loadCompletedHistory(): void {
    if (this.dateError()) {
      this.completedHistory.set([]);
      return;
    }
    this.historyLoading.set(true);
    this.error.set('');
    this.recommendationsApi.history(this.from() || undefined, this.to() || undefined).subscribe({
      next: history => {
        this.completedHistory.set(history);
        this.historyLoading.set(false);
      },
      error: failure => {
        this.completedHistory.set([]);
        this.historyLoading.set(false);
        this.error.set(failure.error?.message ?? 'No se pudo cargar tu historial de actividades realizadas.');
      }
    });
  }

  /** Reutiliza el flujo de valoracion de la HU-27; solo se ofrece si aun no hay valoracion. */
  rate(item: Recommendation, rating: number): void {
    if (this.ratingBusyId() !== null || item.helpfulnessRating) return;

    this.ratingBusyId.set(item.id);
    this.error.set('');
    this.success.set('');
    this.recommendationsApi.rateRecommendation(item.id, { helpfulnessRating: rating }).subscribe({
      next: updated => {
        this.ratingBusyId.set(null);
        this.replaceRecommendation(updated);
        this.success.set(`Valoraste "${updated.activity.title}" con ${updated.helpfulnessRating}/5.`);
      },
      error: failure => {
        this.ratingBusyId.set(null);
        this.error.set(failure.error?.message ?? 'No se pudo guardar tu valoración.');
      }
    });
  }

  /**
   * Abre el modo guiado existente SIN recommendationId: la repeticion se guarda como una sesion nueva
   * y no vuelve a completar la recomendacion original ni cambia su valoracion.
   */
  repeat(item: Recommendation): void {
    if (!item.activity.active || this.guideLoading() || this.guide()) return;

    this.error.set('');
    this.success.set('');
    this.guideLoading.set(true);
    this.activitiesApi.activityGuide(item.activity.id).subscribe({
      next: activity => {
        this.guideLoading.set(false);
        this.guide.set(activity);
      },
      error: failure => {
        this.guideLoading.set(false);
        this.error.set(failure.error?.message ?? 'No se pudo abrir el modo guiado.');
      }
    });
  }

  onRepeatRecorded(): void {
    this.success.set('Repetición registrada como una nueva sesión. Tu registro y valoración originales no cambian.');
  }

  private reloadCompletedHistoryIfNeeded(): void {
    if (this.status() === 'COMPLETED') {
      this.loadCompletedHistory();
    }
  }

  private replaceRecommendation(updated: Recommendation): void {
    const replace = (items: Recommendation[]) => items.map(item => item.id === updated.id ? updated : item);
    this.recommendations.update(replace);
    this.completedHistory.update(items => items === null ? null : replace(items));
  }
}
