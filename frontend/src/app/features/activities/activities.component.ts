import { Component, OnInit, signal } from '@angular/core';
import { ApiService } from '../../core/services/api.service';
import { MicroActivity, Recommendation } from '../../shared/models/models';

@Component({
  selector: 'app-activities',
  standalone: true,
  template: `
    <header class="page-head">
      <div>
        <span class="eyebrow">BIENESTAR COTIDIANO</span>
        <h1>Microactividades</h1>
        <p>Acciones breves e informativas que puedes explorar a tu propio ritmo.</p>
      </div>
      <button class="btn secondary" (click)="load()" [disabled]="loading()">Actualizar</button>
    </header>

    @if (error()) {
      <div class="alert error">{{ error() }}</div>
    }
    @if (success()) {
      <div class="alert success">{{ success() }}</div>
    }

    @if (loading()) {
      <div class="loading-panel">Cargando microactividades...</div>
    } @else {
      <section class="dashboard-grid">
        <article class="panel span-2">
          <span class="eyebrow">RECOMENDADAS PARA TI</span>
          <h2>Tu seguimiento</h2>
          <div class="activity-list">
            @for (recommendation of recommendations(); track recommendation.id) {
              <article class="activity-row">
                <span class="activity-time">{{ recommendation.activity.durationMinutes }}<small>min</small></span>
                <div>
                  <h3>{{ recommendation.activity.title }}</h3>
                  <p>{{ recommendation.activity.description }}</p>
                  <small>{{ recommendation.reason }}</small>
                </div>
                @if (recommendation.completedAt) {
                  <span class="status-pill done">Completada</span>
                } @else {
                  <button class="btn secondary" (click)="complete(recommendation)" [disabled]="busyId() === recommendation.id">
                    {{ busyId() === recommendation.id ? 'Guardando...' : 'Marcar completa' }}
                  </button>
                }
                @if (recommendation.completedAt) {
                  <section class="activity-feedback" aria-label="Valorar la utilidad de la actividad">
                    <span class="feedback-label">¿Te resulto util?</span>
                    <div class="star-group" role="radiogroup" aria-label="Valoracion de utilidad de 1 a 5">
                      @for (star of stars; track star) {
                        <button type="button" class="star" role="radio"
                                [class.on]="star <= currentRating(recommendation)"
                                [attr.aria-checked]="star === currentRating(recommendation)"
                                [attr.aria-label]="star + ' de 5'"
                                (click)="setRating(recommendation, star)">★</button>
                      }
                    </div>
                    <textarea class="feedback-comment" rows="2" maxlength="300"
                              placeholder="Comentario opcional (maximo 300 caracteres)"
                              [value]="currentComment(recommendation)"
                              (input)="setComment(recommendation, $any($event.target).value)"></textarea>
                    <button type="button" class="btn secondary" (click)="submitFeedback(recommendation)"
                            [disabled]="!canSubmitFeedback(recommendation)">
                      {{ feedbackBusyId() === recommendation.id ? 'Guardando...' : 'Guardar valoracion' }}
                    </button>
                  </section>
                }
              </article>
            } @empty {
              <div class="empty">
                <b>Aun no tienes recomendaciones</b>
                <p>Genera una recomendacion desde uno de tus check-ins.</p>
              </div>
            }
          </div>
        </article>

        <article class="panel">
          <span class="eyebrow">CATALOGO ACTIVO</span>
          <h2>Explora a tu ritmo</h2>
          <div class="activity-catalog-grid compact" aria-label="Catalogo de microactividades activas">
            @for (activity of activities(); track activity.id) {
              <div class="compact-activity">
                <span>{{ activity.durationMinutes }} min</span>
                <b>{{ activity.title }}</b>
                <p>{{ activity.description }}</p>
              </div>
            } @empty {
              <div class="empty activity-empty">
                <b>Aun no hay microactividades disponibles</b>
                <p>El catalogo se mostrara aqui cuando se publiquen nuevas opciones.</p>
              </div>
            }
          </div>
        </article>
      </section>
    }

    <p class="disclaimer">Estas actividades ofrecen informacion de bienestar general y no reemplazan atencion profesional.</p>
  `
})
export class ActivitiesComponent implements OnInit {
  readonly activities = signal<MicroActivity[]>([]);
  readonly recommendations = signal<Recommendation[]>([]);
  readonly loading = signal(true);
  readonly busyId = signal<number | null>(null);
  readonly feedbackBusyId = signal<number | null>(null);
  readonly stars = [1, 2, 3, 4, 5];
  private readonly ratingDrafts = signal<Record<number, number>>({});
  private readonly commentDrafts = signal<Record<number, string>>({});
  readonly error = signal('');
  readonly success = signal('');

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.success.set('');
    this.api.activities(true).subscribe({
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
    this.api.recommendations().subscribe({
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
    this.api.completeRecommendation(recommendation.id).subscribe({
      next: updated => {
        this.busyId.set(null);
        this.success.set(`Microactividad "${updated.activity.title}" completada.`);
        this.recommendations.update(items => items.map(item => item.id === updated.id ? updated : item));
      },
      error: () => {
        this.busyId.set(null);
        this.error.set('No se pudo completar la recomendacion.');
      }
    });
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
    this.api.rateRecommendation(recommendation.id, {
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
