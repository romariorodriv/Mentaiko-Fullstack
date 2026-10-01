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
}
