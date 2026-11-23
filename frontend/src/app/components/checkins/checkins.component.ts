import { DatePipe } from '@angular/common';
import { Component, ElementRef, inject, OnInit, signal, ViewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { EmotionsService } from '../../core/services/emotions.service';
import { CheckinsService } from '../../core/services/checkins.service';
import { RecommendationsService } from '../../core/services/recommendations.service';
import { ApiError, Checkin, Emotion, Recommendation } from '../../shared/models/models';
import { RecommendationAlternativeComponent } from '../recommendation-alternative/recommendation-alternative.component';

@Component({
  selector: 'app-checkins',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, RecommendationAlternativeComponent],
  templateUrl: './checkins.component.html',
  styleUrl: './checkins.component.css'
})
export class CheckinsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  @ViewChild('emotionSelect') private emotionSelect?: ElementRef<HTMLSelectElement>;
  @ViewChild('deleteConfirmButton') private deleteConfirmButton?: ElementRef<HTMLButtonElement>;

  readonly emotions = signal<Emotion[]>([]);
  readonly items = signal<Checkin[]>([]);
  readonly emotionsLoading = signal(true);
  readonly historyLoading = signal(true);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly showForm = signal(false);
  readonly formError = signal('');
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly editingId = signal<number | null>(null);
  readonly recommendationBusyId = signal<number | null>(null);
  readonly recommendationPreview = signal<Recommendation | null>(null);
  readonly alternativeBusy = signal(false);
  readonly deleteTarget = signal<Checkin | null>(null);
  readonly deleting = signal(false);
  readonly deleteError = signal('');

  readonly filtersForm = this.fb.nonNullable.group({
    from: [''],
    to: [''],
    context: ['', [Validators.maxLength(100)]]
  });

  readonly form = this.fb.nonNullable.group({
    emotionId: [0, [Validators.required, Validators.min(1)]],
    intensity: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
    context: ['', [Validators.required, Validators.maxLength(100)]],
    note: ['', [Validators.maxLength(500)]]
  });

  constructor(private readonly emotionsApi: EmotionsService, private readonly checkinsApi: CheckinsService, private readonly recommendationsApi: RecommendationsService) {}

  ngOnInit(): void {
    this.loadEmotions();
    this.loadHistory();
  }

  loadEmotions(): void {
    this.emotionsLoading.set(true);
    this.emotionsApi.emotions(true).subscribe({
      next: emotions => {
        this.emotions.set(emotions);
        this.emotionsLoading.set(false);
      },
      error: error => {
        this.emotionsLoading.set(false);
        this.error.set(this.errorMessage(error, 'No se pudo cargar el catalogo de emociones.'));
      }
    });
  }

  loadHistory(): void {
    this.historyLoading.set(true);
    this.error.set('');
    const filters = this.filtersForm.getRawValue();
    this.checkinsApi.checkins({
      from: filters.from,
      to: filters.to,
      context: filters.context,
      page: this.page(),
      size: this.size()
    }).subscribe({
      next: response => {
        this.items.set(response.content);
        this.totalElements.set(response.totalElements);
        this.totalPages.set(response.totalPages);
        this.page.set(response.number);
        this.size.set(response.size);
        this.historyLoading.set(false);
      },
      error: error => {
        this.historyLoading.set(false);
        this.error.set(this.errorMessage(error, 'No se pudo cargar el historial de check-ins.'));
      }
    });
  }

  retry(): void {
    this.loadEmotions();
    this.loadHistory();
  }

  applyFilters(): void {
    if (this.filtersForm.invalid) {
      this.filtersForm.markAllAsTouched();
      return;
    }
    this.page.set(0);
    this.loadHistory();
  }

  clearFilters(): void {
    this.filtersForm.reset({from: '', to: '', context: ''});
    this.page.set(0);
    this.loadHistory();
  }

  hasFilters(): boolean {
    const filters = this.filtersForm.getRawValue();
    return !!filters.from || !!filters.to || !!filters.context.trim();
  }

  previousPage(): void {
    if (this.page() === 0) {
      return;
    }
    this.page.update(value => value - 1);
    this.loadHistory();
  }

  nextPage(): void {
    if (this.page() + 1 >= this.totalPages()) {
      return;
    }
    this.page.update(value => value + 1);
    this.loadHistory();
  }

  openForm(): void {
    this.form.reset({emotionId: this.emotions()[0]?.id ?? 0, intensity: 3, context: '', note: ''});
    this.editingId.set(null);
    this.clearMessages();
    this.showForm.set(true);
    this.focusEmotionSelect();
  }

  openEdit(item: Checkin): void {
    this.form.reset({
      emotionId: item.emotion.id,
      intensity: item.intensity,
      context: item.context,
      note: item.note ?? ''
    });
    this.editingId.set(item.id);
    this.clearMessages();
    this.showForm.set(true);
    this.focusEmotionSelect();
  }

  closeForm(): void {
    this.showForm.set(false);
    this.formError.set('');
    this.editingId.set(null);
  }

  isEditing(): boolean {
    return this.editingId() !== null;
  }

  save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.clearMessages();
    const request = this.form.getRawValue();
    const editingId = this.editingId();
    const operation = editingId === null
      ? this.checkinsApi.createCheckin(request)
      : this.checkinsApi.updateCheckin(editingId, request);

    operation.subscribe({
      next: () => {
        this.saving.set(false);
        this.success.set(editingId === null ? 'Check-in emocional creado correctamente.' : 'Check-in emocional actualizado correctamente.');
        this.closeForm();
        if (editingId === null) {
          this.page.set(0);
          this.loadHistory();
          return;
        }
        this.loadHistory();
      },
      error: error => {
        this.saving.set(false);
        this.formError.set(this.errorMessage(error, editingId === null ? 'No se pudo crear el check-in. Revisa los datos e intenta nuevamente.' : 'No se pudo actualizar el check-in. Revisa los datos e intenta nuevamente.'));
      }
    });
  }

  recommend(item: Checkin): void {
    if (this.recommendationBusyId() !== null || this.alternativeBusy()) {
      return;
    }

    this.recommendationBusyId.set(item.id);
    this.clearMessages();
    this.recommendationsApi.recommend(item.id).subscribe({
      next: recommendation => {
        this.recommendationBusyId.set(null);
        this.recommendationPreview.set(recommendation);
      },
      error: error => {
        this.recommendationBusyId.set(null);
        this.error.set(this.errorMessage(error, 'No se pudo generar la recomendacion.'));
      }
    });
  }

  showAlternative(recommendation: Recommendation): void {
    this.recommendationPreview.set(recommendation);
    this.success.set(`Nueva sugerencia: ${recommendation.activity.title}`);
  }

  openDeleteConfirmation(item: Checkin): void {
    if (this.deleting() || this.alternativeBusy()) {
      return;
    }
    this.clearMessages();
    this.deleteError.set('');
    this.deleteTarget.set(item);
    setTimeout(() => this.deleteConfirmButton?.nativeElement.focus());
  }

  closeDeleteConfirmation(): void {
    if (this.deleting()) {
      return;
    }
    this.deleteTarget.set(null);
    this.deleteError.set('');
  }

  confirmDelete(): void {
    const target = this.deleteTarget();
    if (!target || this.deleting()) {
      return;
    }

    this.deleting.set(true);
    this.deleteError.set('');
    this.checkinsApi.deleteCheckin(target.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.deleteTarget.set(null);
        if (this.recommendationPreview()?.checkinId === target.id) this.recommendationPreview.set(null);
        this.success.set('Check-in eliminado correctamente');
        if (this.items().length === 1 && this.page() > 0) {
          this.page.update(value => value - 1);
        }
        this.loadHistory();
      },
      error: error => {
        this.deleting.set(false);
        this.deleteError.set(this.errorMessage(error, 'No se pudo eliminar el check-in. Intenta nuevamente.'));
      }
    });
  }

  private clearMessages(): void {
    this.error.set('');
    this.success.set('');
    this.formError.set('');
  }

  private errorMessage(error: ApiError, fallback: string): string {
    return error?.message ?? fallback;
  }

  private focusEmotionSelect(): void {
    setTimeout(() => this.emotionSelect?.nativeElement.focus());
  }
}
