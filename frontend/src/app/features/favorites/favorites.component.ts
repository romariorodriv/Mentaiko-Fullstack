import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  FavoriteMicroactivitiesApiService,
  FavoriteMicroActivity
} from '../../core/services/favorite-microactivities-api.service';

@Component({
  selector: 'app-favorites',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './favorites.component.html',
  styleUrl: './favorites.component.css'
})
export class FavoritesComponent implements OnInit {
  private readonly api = inject(FavoriteMicroactivitiesApiService);

  readonly favorites = signal<FavoriteMicroActivity[]>([]);
  readonly loading = signal(true);
  readonly busyId = signal<number | null>(null);
  readonly error = signal('');
  readonly message = signal('');

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.list().subscribe({
      next: data => {
        this.favorites.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar tus microactividades favoritas.');
        this.loading.set(false);
      }
    });
  }

  remove(activityId: number): void {
    if (this.busyId() !== null) return;
    this.busyId.set(activityId);
    this.error.set('');
    this.message.set('');

    this.api.remove(activityId).subscribe({
      next: () => {
        this.favorites.update(items => items.filter(item => item.id !== activityId));
        this.message.set('Microactividad retirada de favoritas.');
        this.busyId.set(null);
      },
      error: () => {
        this.error.set('No se pudo retirar la microactividad de favoritas.');
        this.busyId.set(null);
      }
    });
  }
}
