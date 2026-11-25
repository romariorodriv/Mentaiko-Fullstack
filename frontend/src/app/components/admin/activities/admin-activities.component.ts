import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivitiesService } from '../../../core/services/activities.service';
import { ApiError, MicroActivity, UpdateMicroActivityRequest } from '../../../shared/models/models';
import { ACTIVITY_TYPE_LABELS } from '../../../shared/models/activity-types';

@Component({
  selector: 'app-admin-activities',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './admin-activities.component.html',
  styleUrl: './admin-activities.component.css'
})
export class AdminActivitiesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);

  readonly activityTypes = Object.entries(ACTIVITY_TYPE_LABELS).map(([value,label]) => ({value,label}));
  readonly items = signal<MicroActivity[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly busyId = signal<number | null>(null);
  readonly error = signal('');
  readonly success = signal('');
  readonly editForms: Record<number, FormGroup> = {};
  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(120), Validators.pattern(/\S/)]],
    description: ['', [Validators.required, Validators.maxLength(500), Validators.pattern(/\S/)]],
    durationMinutes: [5, [Validators.required, Validators.min(1), Validators.max(120)]]
  });

  constructor(private readonly activitiesApi: ActivitiesService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.activitiesApi.activities(false).subscribe({
      next: activities => {
        this.items.set(activities);
        this.rebuildForms(activities);
        this.loading.set(false);
      },
      error: error => {
        this.loading.set(false);
        this.error.set(this.errorMessage(error, 'No se pudo cargar el catalogo.'));
      }
    });
  }

  create(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.clearMessages();
    this.activitiesApi.createActivity(this.form.getRawValue()).subscribe({
      next: activity => {
        this.saving.set(false);
        this.form.reset({title: '', description: '', durationMinutes: 5});
        this.success.set(`Microactividad "${activity.title}" creada.`);
        this.load();
      },
      error: error => {
        this.saving.set(false);
        this.error.set(this.errorMessage(error, 'No se pudo crear la microactividad.'));
      }
    });
  }

  save(activity: MicroActivity): void {
    const form = this.editForms[activity.id];
    if (!form || form.invalid || !this.hasChanges(activity) || this.busyId() !== null) {
      form?.markAllAsTouched();
      return;
    }

    const value = form.getRawValue();
    const changes: UpdateMicroActivityRequest = {};
    if (value.title.trim() !== activity.title) changes.title = value.title;
    if (value.description.trim() !== activity.description) changes.description = value.description;
    if (value.durationMinutes !== activity.durationMinutes) changes.durationMinutes = value.durationMinutes;

    this.busyId.set(activity.id);
    this.clearMessages();
    this.activitiesApi.updateActivity(activity.id, changes).subscribe({
      next: updated => {
        this.busyId.set(null);
        this.success.set(`Microactividad "${updated.title}" actualizada.`);
        this.load();
      },
      error: error => {
        this.busyId.set(null);
        this.error.set(this.errorMessage(error, 'No se pudo actualizar la microactividad.'));
      }
    });
  }

  changeType(activity: MicroActivity, activityType: string): void {
    if (this.busyId() !== null || activityType === (activity.activityType ?? 'GENERAL')) return;
    this.busyId.set(activity.id);
    this.clearMessages();
    this.activitiesApi.setActivityType(activity.id,activityType).subscribe({
      next: result => {
        this.busyId.set(null);
        this.items.update(items => items.map(item => item.id === activity.id
          ? {...item,activityType:result.activityType} : item));
        this.success.set('Tipo de actividad actualizado.');
      },
      error: () => {
        this.busyId.set(null);
        this.error.set('No se pudo cambiar el tipo de actividad.');
      }
    });
  }

  toggle(activity: MicroActivity): void {
    const action = activity.active ? 'Desactivar' : 'Activar';
    if (this.busyId() !== null || !confirm(`${action} "${activity.title}"?`)) {
      return;
    }

    this.busyId.set(activity.id);
    this.clearMessages();
    this.activitiesApi.updateActivity(activity.id, {active: !activity.active}).subscribe({
      next: updated => {
        this.busyId.set(null);
        this.success.set(`Microactividad "${updated.title}" ${updated.active ? 'activada' : 'desactivada'}.`);
        this.load();
      },
      error: error => {
        this.busyId.set(null);
        this.error.set(this.errorMessage(error, 'No se pudo cambiar el estado.'));
      }
    });
  }

  hasChanges(activity: MicroActivity): boolean {
    const value = this.editForms[activity.id]?.getRawValue();
    return !!value && (
      value.title.trim() !== activity.title
      || value.description.trim() !== activity.description
      || value.durationMinutes !== activity.durationMinutes
    );
  }

  private rebuildForms(items: MicroActivity[]): void {
    for (const id of Object.keys(this.editForms)) {
      delete this.editForms[Number(id)];
    }
    for (const activity of items) {
      this.editForms[activity.id] = this.fb.nonNullable.group({
        title: [activity.title, [Validators.required, Validators.maxLength(120), Validators.pattern(/\S/)]],
        description: [activity.description, [Validators.required, Validators.maxLength(500), Validators.pattern(/\S/)]],
        durationMinutes: [activity.durationMinutes, [Validators.required, Validators.min(1), Validators.max(120)]]
      });
    }
  }

  private clearMessages(): void {
    this.error.set('');
    this.success.set('');
  }

  private errorMessage(error: unknown, fallback: string): string {
    if (!(error instanceof HttpErrorResponse)) {
      return fallback;
    }
    const body = error.error as ApiError | undefined;
    return body?.message ?? Object.values(body?.errors ?? {})[0] ?? fallback;
  }
}
