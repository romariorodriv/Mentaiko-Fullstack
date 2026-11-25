import { Component, OnInit, signal } from '@angular/core';
import { ActivitiesService } from '../../core/services/activities.service';
import { RecommendationsService } from '../../core/services/recommendations.service';
import { FavoriteMicroActivity, MicroActivity, Recommendation } from '../../shared/models/models';

@Component({
  selector: 'app-activities',
  standalone: true,
  templateUrl: './activities.component.html',
  styleUrl: './activities.component.css'
})
export class ActivitiesComponent implements OnInit {
  readonly activities = signal<MicroActivity[]>([]);
  readonly availableMinutes = signal<number | null>(null);
  readonly quickMinutes = [2, 5, 10] as const;

  filterByMinutes(minutes: number | null): void {
    this.availableMinutes.set(minutes);
    this.load();
  }
  readonly recommendations = signal<Recommendation[]>([]);
  readonly favorites = signal<FavoriteMicroActivity[]>([]);
  readonly catalogTab = signal<'all' | 'favorites'>('all');

  selectCatalogTab(tab: 'all' | 'favorites'): void {
    this.catalogTab.set(tab);
  }
  readonly favoritesLoading = signal(true);
  readonly favoritesError = signal('');
  readonly favoriteBusyId = signal<number | null>(null);
  readonly loading = signal(true);
  readonly busyId = signal<number | null>(null);
  readonly feedbackBusyId = signal<number | null>(null);
  readonly stars = [1, 2, 3, 4, 5];
  private readonly ratingDrafts = signal<Record<number, number>>({});
  private readonly commentDrafts = signal<Record<number, string>>({});
  private readonly postDrafts = signal<Record<number, number>>({});
  readonly error = signal('');
  readonly success = signal('');

  constructor(private readonly activitiesApi: ActivitiesService, private readonly recommendationsApi: RecommendationsService) {}

  ngOnInit(): void {
    this.load();
    this.loadFavorites();
  }

  loadFavorites(): void {
    this.favoritesLoading.set(true);
    this.favoritesError.set('');
    this.activitiesApi.favorites().subscribe({
      next: items => {
        this.favorites.set(items);
        this.favoritesLoading.set(false);
      },
      error: () => {
        this.favoritesLoading.set(false);
        this.favoritesError.set('No se pudieron cargar tus favoritas.');
      }
    });
  }

  isFavorite(id: number): boolean {
    return this.favorites().some(item => item.id === id);
  }

  toggleFavorite(activity: Pick<MicroActivity, 'id' | 'title'>): void {
    if (this.favoriteBusyId() !== null || this.favoritesLoading() || this.favoritesError()) {
      return;
    }
    const wasFavorite = this.isFavorite(activity.id);
    this.favoriteBusyId.set(activity.id);
    this.error.set('');
    this.success.set('');
    if (wasFavorite) {
      this.activitiesApi.removeFavorite(activity.id).subscribe({
        next: () => {
          this.favoriteBusyId.set(null);
          this.favorites.update(items => items.filter(item => item.id !== activity.id));
          this.success.set('Microactividad retirada de favoritas.');
        },
        error: () => this.handleFavoriteError()
      });
    } else {
      this.activitiesApi.addFavorite(activity.id).subscribe({
        next: favorite => {
          this.favoriteBusyId.set(null);
          this.favorites.update(items => [...items.filter(item => item.id !== activity.id), favorite]);
          this.success.set('Microactividad guardada en favoritas.');
        },
        error: () => this.handleFavoriteError()
      });
    }
  }

  private handleFavoriteError(): void {
    this.favoriteBusyId.set(null);
    this.error.set('No se pudo actualizar la actividad favorita. Intenta nuevamente.');
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.success.set('');
    this.activitiesApi.activities(true, this.availableMinutes() ?? undefined).subscribe({
      next: activities => {
        this.activities.set(activities);
        this.loadRecommendations();
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudo cargar el catalogo de microactividades.');
      }
    });
  }

  private loadRecommendations(): void {
    this.recommendationsApi.recommendations().subscribe({
      next: recommendations => {
        this.recommendations.set(recommendations);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudieron cargar tus recomendaciones.');
      }
    });
  }

  complete(recommendation: Recommendation): void {
    if (this.busyId() !== null) {
      return;
    }

    this.busyId.set(recommendation.id);
    this.error.set('');
    this.success.set('');
    const postIntensity = this.currentPost(recommendation) || undefined;
    this.recommendationsApi.completeRecommendation(recommendation.id, postIntensity).subscribe({
      next: updated => {
        this.busyId.set(null);
        this.success.set(`Microactividad "${updated.activity.title}" completada.`);
        this.recommendations.update(items => items.map(item => item.id === updated.id ? updated : item));
        this.clearPostDraft(updated.id);
      },
      error: () => {
        this.busyId.set(null);
        this.error.set('No se pudo completar la recomendacion.');
      }
    });
  }

  currentPost(recommendation: Recommendation): number {
    return this.postDrafts()[recommendation.id] ?? 0;
  }

  /** Elegir de nuevo la misma intensidad la quita, porque indicarla es opcional. */
  selectPost(recommendation: Recommendation, level: number): void {
    if (this.currentPost(recommendation) === level) {
      this.clearPostDraft(recommendation.id);
      return;
    }
    this.postDrafts.update(drafts => ({ ...drafts, [recommendation.id]: level }));
  }

  savePostIntensity(recommendation: Recommendation): void {
    const level = this.currentPost(recommendation);
    if (level < 1 || this.busyId() !== null) {
      return;
    }

    this.busyId.set(recommendation.id);
    this.error.set('');
    this.success.set('');
    this.recommendationsApi.completeRecommendation(recommendation.id, level).subscribe({
      next: updated => {
        this.busyId.set(null);
        this.success.set(`Guardamos como te sentias despues de "${updated.activity.title}".`);
        this.recommendations.update(items => items.map(item => item.id === updated.id ? updated : item));
        this.clearPostDraft(updated.id);
      },
      error: () => {
        this.busyId.set(null);
        this.error.set('No se pudo guardar como te sientes.');
      }
    });
  }

  comparisonText(recommendation: Recommendation): string {
    const before = recommendation.preIntensity;
    const after = recommendation.postIntensity;
    if (!before || !after) {
      return '';
    }
    if (after < before) {
      return `Segun lo que registraste, la intensidad paso de ${before} a ${after}.`;
    }
    if (after > before) {
      return `Segun lo que registraste, la intensidad subio de ${before} a ${after}.`;
    }
    return `Segun lo que registraste, la intensidad se mantuvo en ${before}.`;
  }

  private clearPostDraft(id: number): void {
    this.postDrafts.update(({ [id]: _removed, ...rest }) => rest);
  }

  currentRating(recommendation: Recommendation): number {
    return this.ratingDrafts()[recommendation.id] ?? recommendation.helpfulnessRating ?? 0;
  }

  currentComment(recommendation: Recommendation): string {
    return this.commentDrafts()[recommendation.id] ?? recommendation.feedbackComment ?? '';
  }

  setRating(recommendation: Recommendation, rating: number): void {
    this.ratingDrafts.update(drafts => ({ ...drafts, [recommendation.id]: rating }));
  }

  setComment(recommendation: Recommendation, comment: string): void {
    this.commentDrafts.update(drafts => ({ ...drafts, [recommendation.id]: comment.slice(0, 300) }));
  }

  canSubmitFeedback(recommendation: Recommendation): boolean {
    const rating = this.currentRating(recommendation);
    if (rating < 1 || this.feedbackBusyId() !== null) {
      return false;
    }
    const changedRating = rating !== (recommendation.helpfulnessRating ?? 0);
    const changedComment = this.currentComment(recommendation).trim() !== (recommendation.feedbackComment ?? '');
    return changedRating || changedComment;
  }

  submitFeedback(recommendation: Recommendation): void {
    if (!this.canSubmitFeedback(recommendation)) {
      return;
    }

    const comment = this.currentComment(recommendation).trim();
    this.feedbackBusyId.set(recommendation.id);
    this.error.set('');
    this.success.set('');
    this.recommendationsApi.rateRecommendation(recommendation.id, {
      helpfulnessRating: this.currentRating(recommendation),
      feedbackComment: comment || undefined
    }).subscribe({
      next: updated => {
        this.feedbackBusyId.set(null);
        this.success.set(`Gracias por valorar "${updated.activity.title}".`);
        this.recommendations.update(items => items.map(item => item.id === updated.id ? updated : item));
        this.ratingDrafts.update(({ [updated.id]: _removed, ...rest }) => rest);
        this.commentDrafts.update(({ [updated.id]: _removed, ...rest }) => rest);
      },
      error: () => {
        this.feedbackBusyId.set(null);
        this.error.set('No se pudo guardar tu valoracion.');
      }
    });
  }
}
