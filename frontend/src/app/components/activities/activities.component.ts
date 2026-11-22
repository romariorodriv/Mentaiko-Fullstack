import { Component, OnInit, signal } from '@angular/core';
import { ActivitiesService } from '../../core/services/activities.service';
import { RecommendationsService } from '../../core/services/recommendations.service';
import { MicroActivity, Recommendation } from '../../shared/models/models';

@Component({
  selector: 'app-activities',
  standalone: true,
  templateUrl: './activities.component.html',
  styleUrl: './activities.component.css'
})
export class ActivitiesComponent implements OnInit {
  readonly activities = signal<MicroActivity[]>([]);
  readonly recommendations = signal<Recommendation[]>([]);
  readonly loading = signal(true);
  readonly busyId = signal<number | null>(null);
  readonly error = signal('');
  readonly success = signal('');

  constructor(private readonly activitiesApi: ActivitiesService, private readonly recommendationsApi: RecommendationsService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.success.set('');
    this.activitiesApi.activities(true).subscribe({
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
    this.recommendationsApi.completeRecommendation(recommendation.id).subscribe({
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
