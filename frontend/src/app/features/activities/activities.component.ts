import { Component, OnInit, signal } from '@angular/core';
import { ApiService } from '../../core/services/api.service';
import { MicroActivity } from '../../shared/models/models';

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

    @if (loading()) {
      <div class="loading-panel">Cargando microactividades...</div>
    } @else {
      <section class="activity-catalog-grid" aria-label="Catalogo de microactividades activas">
        @for (activity of activities(); track activity.id) {
          <article class="panel activity-catalog-item">
            <span class="activity-time">{{ activity.durationMinutes }}<small>min</small></span>
            <div>
              <h2>{{ activity.title }}</h2>
              <p>{{ activity.description }}</p>
            </div>
          </article>
        } @empty {
          <div class="empty activity-empty">
            <b>Aun no hay microactividades disponibles</b>
            <p>El catalogo se mostrara aqui cuando se publiquen nuevas opciones.</p>
          </div>
        }
      </section>
    }

    <p class="disclaimer">Estas actividades ofrecen informacion de bienestar general y no reemplazan atencion profesional.</p>
  `
})
export class ActivitiesComponent implements OnInit {
  readonly activities = signal<MicroActivity[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.activities(true).subscribe({
      next: activities => {
        this.activities.set(activities);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudo cargar el catalogo de microactividades.');
      }
    });
  }
}
