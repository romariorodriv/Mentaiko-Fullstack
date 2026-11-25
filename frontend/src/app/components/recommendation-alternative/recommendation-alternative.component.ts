import { Component, DestroyRef, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';

import { RecommendationsService } from '../../core/services/recommendations.service';
import { Recommendation, RecommendationDiscardReason } from '../../shared/models/models';

@Component({
  selector: 'app-recommendation-alternative',
  templateUrl: './recommendation-alternative.component.html',
  styleUrl: './recommendation-alternative.component.css'
})
export class RecommendationAlternativeComponent {
  readonly recommendationId = input.required<number>();
  readonly disabled = input(false);
  readonly replacement = output<Recommendation>();
  readonly busyChange = output<boolean>();

  readonly reasons: ReadonlyArray<{ value: RecommendationDiscardReason; label: string }> = [
    { value: 'TRANSPORTE', label: 'Estoy en el bus o en otro transporte' },
    { value: 'BIBLIOTECA', label: 'Estoy en una biblioteca o necesito silencio' },
    { value: 'POCO_ESPACIO', label: 'Tengo poco espacio' },
    { value: 'POCO_TIEMPO', label: 'Tengo poco tiempo' },
    { value: 'OTRO', label: 'Otro motivo' }
  ];
  readonly busy = signal(false);
  readonly error = signal('');

  private readonly api = inject(RecommendationsService);
  private readonly destroyRef = inject(DestroyRef);

  submit(reasonValue: string, minutesValue: string): void {
    if (this.busy() || this.disabled()) return;

    const reason = this.reasons.find(option => option.value === reasonValue)?.value;
    const availableMinutes = minutesValue ? Number(minutesValue) : undefined;
    this.error.set('');
    this.busy.set(true);
    this.busyChange.emit(true);
    this.api.requestAlternative(this.recommendationId(), { reason, availableMinutes }).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => {
        this.busy.set(false);
        this.busyChange.emit(false);
      })
    ).subscribe({
      next: alternative => this.replacement.emit(alternative),
      error: failure => this.error.set(failure.error?.message ?? 'No se pudo pedir otra sugerencia. Intenta nuevamente.')
    });
  }
}
