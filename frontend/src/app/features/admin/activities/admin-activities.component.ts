import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { ApiError, Emotion } from '../../../shared/models/models';

@Component({
  selector: 'app-admin-emotions',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
<header class="page-head"><div><span class="eyebrow">ADMINISTRACION</span><h1>Catalogo de emociones</h1><p>Gestiona opciones consistentes sin eliminar informacion historica.</p></div></header>
@if(error()){<div class="alert error">{{error()}}</div>} @if(success()){<div class="alert success">{{success()}}</div>}
<section class="admin-grid"><form class="panel" [formGroup]="form" (ngSubmit)="create()"><span class="eyebrow">NUEVA EMOCION</span><h2>Agregar al catalogo</h2><label>Nombre<input formControlName="name" placeholder="Ej. Esperanza" maxlength="80"></label>@if(form.controls.name.touched&&form.controls.name.invalid){<div class="field-error">Escribe un nombre de 2 a 80 caracteres.</div>}<button class="btn primary full" [disabled]="form.invalid||saving()">{{saving()?'Guardando...':'Agregar emocion'}}</button></form><article class="panel span-2"><div class="panel-head"><div><span class="eyebrow">CATALOGO COMPLETO</span><h2>{{items().length}} emociones</h2></div><button class="btn secondary" (click)="load()" [disabled]="loading()">Actualizar</button></div>@if(loading()){<div class="loading-panel">Cargando catalogo...</div>} @else {<div class="data-list">@for(e of items();track e.id){<form [formGroup]="editForms[e.id]" (ngSubmit)="save(e)"><span class="status-dot" [class.off]="!e.active"></span><input formControlName="name" [attr.aria-label]="'Nombre de '+e.name" maxlength="80"><span>{{e.active?'Activa':'Inactiva'}}</span><button type="button" class="switch" [class.on]="e.active" (click)="toggle(e)" [disabled]="busyId()===e.id" [attr.aria-label]="'Cambiar estado de '+e.name"><i></i></button><button class="btn secondary" [disabled]="editForms[e.id].invalid||busyId()===e.id||!hasNameChanged(e)">Guardar</button></form>} @empty {<div class="empty"><b>No hay emociones registradas</b><p>Agrega la primera opcion del catalogo.</p></div>}</div>}</article></section>
`
})
export class AdminEmotionsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  readonly items = signal<Emotion[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly busyId = signal<number | null>(null);
  readonly error = signal('');
  readonly success = signal('');
  readonly editForms: Record<number, FormGroup> = {};
  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]]
  });

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.emotions(false).subscribe({
      next: emotions => {
        this.items.set(emotions);
        this.rebuildForms(emotions);
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
      return;
    }
    this.saving.set(true);
    this.clearMessages();
    this.api.createEmotion(this.form.getRawValue()).subscribe({
      next: emotion => {
        this.saving.set(false);
        this.form.reset();
        this.success.set(`Emocion "${emotion.name}" creada.`);
        this.load();
      },
      error: error => {
        this.saving.set(false);
        this.error.set(this.errorMessage(error, 'No se pudo crear.'));
      }
    });
  }

  save(emotion: Emotion): void {
    const form = this.editForms[emotion.id];
    if (!form || form.invalid || !this.hasNameChanged(emotion) || this.busyId()) {
      return;
    }
    this.busyId.set(emotion.id);
    this.clearMessages();
    this.api.updateEmotion(emotion.id, {name: String(form.value.name)}).subscribe({
      next: updated => {
        this.busyId.set(null);
        this.success.set(`Emocion "${updated.name}" actualizada.`);
        this.load();
      },
      error: error => {
        this.busyId.set(null);
        this.error.set(this.errorMessage(error, 'No se pudo actualizar.'));
      }
    });
  }

  toggle(emotion: Emotion): void {
    const action = emotion.active ? 'Desactivar' : 'Activar';
    if (!confirm(`${action} "${emotion.name}"?`)) {
      return;
    }
    this.busyId.set(emotion.id);
    this.clearMessages();
    this.api.updateEmotion(emotion.id, {active: !emotion.active}).subscribe({
      next: updated => {
        this.busyId.set(null);
        this.success.set(`Emocion "${updated.name}" ${updated.active ? 'activada' : 'desactivada'}.`);
        this.load();
      },
      error: error => {
        this.busyId.set(null);
        this.error.set(this.errorMessage(error, 'No se pudo cambiar el estado.'));
      }
    });
  }

  hasNameChanged(emotion: Emotion): boolean {
    return String(this.editForms[emotion.id]?.value.name ?? '').trim() !== emotion.name;
  }

  private rebuildForms(items: Emotion[]): void {
    for (const emotion of items) {
      this.editForms[emotion.id] = this.fb.nonNullable.group({
        name: [emotion.name, [Validators.required, Validators.minLength(2), Validators.maxLength(80)]]
      });
    }
  }

  private clearMessages(): void {
    this.error.set('');
    this.success.set('');
  }

  private errorMessage(error: ApiError, fallback: string): string {
    return error?.message ?? error?.errors?.['name'] ?? fallback;
  }
}
