import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { ApiError, MicroActivity, UpdateMicroActivityRequest } from '../../../shared/models/models';

@Component({
  selector: 'app-admin-activities',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <header class="page-head">
      <div>
        <span class="eyebrow">ADMINISTRACION</span>
        <h1>Catalogo de microactividades</h1>
        <p>Crea y actualiza acciones de bienestar sin eliminar su historial.</p>
      </div>
    </header>

    @if (error()) { <div class="alert error">{{ error() }}</div> }
    @if (success()) { <div class="alert success">{{ success() }}</div> }

    <section class="admin-grid activity-admin-grid">
      <form class="panel" [formGroup]="form" (ngSubmit)="create()">
        <span class="eyebrow">NUEVA MICROACTIVIDAD</span>
        <h2>Agregar al catalogo</h2>

        <label>Titulo
          <input formControlName="title" maxlength="120" placeholder="Ej. Pausa de respiracion">
        </label>
        @if (form.controls.title.touched && form.controls.title.invalid) {
          <div class="field-error">Escribe un titulo de hasta 120 caracteres.</div>
        }

        <label>Descripcion
          <textarea formControlName="description" maxlength="500" rows="4" placeholder="Describe una accion breve y clara"></textarea>
        </label>
        @if (form.controls.description.touched && form.controls.description.invalid) {
          <div class="field-error">Escribe una descripcion de hasta 500 caracteres.</div>
        }

        <label>Duracion en minutos
          <input type="number" formControlName="durationMinutes" min="1" max="120">
        </label>
        @if (form.controls.durationMinutes.touched && form.controls.durationMinutes.invalid) {
          <div class="field-error">La duracion debe estar entre 1 y 120 minutos.</div>
        }

        <button class="btn primary full" [disabled]="form.invalid || saving()">
          {{ saving() ? 'Guardando...' : 'Agregar microactividad' }}
        </button>
      </form>

      <article class="panel span-2">
        <div class="panel-head">
          <div>
            <span class="eyebrow">CATALOGO COMPLETO</span>
            <h2>{{ items().length }} microactividades</h2>
          </div>
          <button class="btn secondary" (click)="load()" [disabled]="loading()">Actualizar</button>
        </div>

        @if (loading()) {
          <div class="loading-panel">Cargando catalogo...</div>
        } @else {
          <div class="activity-admin-list">
            @for (activity of items(); track activity.id) {
              <form class="activity-admin-item" [formGroup]="editForms[activity.id]" (ngSubmit)="save(activity)">
                <div class="activity-admin-status">
                  <span class="status-dot" [class.off]="!activity.active"></span>
                  <b>{{ activity.active ? 'Activa' : 'Inactiva' }}</b>
                  <button type="button" class="switch" [class.on]="activity.active" (click)="toggle(activity)" [disabled]="busyId() === activity.id" [attr.aria-label]="'Cambiar estado de ' + activity.title"><i></i></button>
                </div>
                <label>Titulo
                  <input formControlName="title" maxlength="120">
                </label>
                <label>Descripcion
                  <textarea formControlName="description" maxlength="500" rows="3"></textarea>
                </label>
                <label>Duracion en minutos
                  <input type="number" formControlName="durationMinutes" min="1" max="120">
                </label>
                <button class="btn secondary" [disabled]="editForms[activity.id].invalid || busyId() === activity.id || !hasChanges(activity)">
                  {{ busyId() === activity.id ? 'Guardando...' : 'Guardar cambios' }}
                </button>
              </form>
            } @empty {
              <div class="empty">
                <b>No hay microactividades registradas</b>
                <p>Agrega la primera opcion del catalogo.</p>
              </div>
            }
          </div>
        }
      </article>
    </section>
  `
})
export class AdminActivitiesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);

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

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.activities(false).subscribe({
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
    this.api.createActivity(this.form.getRawValue()).subscribe({
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
    this.api.updateActivity(activity.id, changes).subscribe({
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

  toggle(activity: MicroActivity): void {
    const action = activity.active ? 'Desactivar' : 'Activar';
    if (this.busyId() !== null || !confirm(`${action} "${activity.title}"?`)) {
      return;
    }

    this.busyId.set(activity.id);
    this.clearMessages();
    this.api.updateActivity(activity.id, {active: !activity.active}).subscribe({
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
