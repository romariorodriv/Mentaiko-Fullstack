import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { ApiError, Checkin, Emotion } from '../../shared/models/models';

@Component({
  selector: 'app-checkins',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe],
  template: `
<header class="page-head"><div><span class="eyebrow">REGISTRO EMOCIONAL</span><h1>Mis check-ins</h1><p>Registra como te sientes en este momento.</p></div><button class="btn primary" (click)="openForm()" [disabled]="loading()||!emotions().length">+ Nuevo check-in</button></header>
@if(error()){<div class="alert error">{{error()}}</div>} @if(success()){<div class="alert success">{{success()}}</div>}
@if(loading()){<div class="loading-panel">Cargando catalogo de emociones...</div>} @else {
  <section class="panel">
    <div class="panel-head"><div><span class="eyebrow">EMOCIONES ACTIVAS</span><h2>{{emotions().length}} disponibles</h2></div></div>
    @if(emotions().length){
      <div class="emotion-picker">
        @for(e of emotions();track e.id){<button type="button" [class.selected]="form.controls.emotionId.value===e.id" (click)="selectEmotion(e)">{{e.name}}</button>}
      </div>
    } @else {
      <div class="empty"><b>No hay emociones activas</b><p>Pide a un administrador que habilite opciones para poder registrar check-ins.</p></div>
    }
  </section>
}
@if(created()){<section class="timeline recent-checkin"><article class="checkin-card"><div class="emotion-dot" [style.--intensity]="created()!.intensity"></div><div class="checkin-main"><div><span class="emotion-name">{{created()!.emotion.name}}</span><span class="context-tag">{{created()!.context}}</span></div><p>{{created()!.note||'Sin nota personal'}}</p><small>{{created()!.createdAt|date:'d MMM y, h:mm a'}}</small></div><div class="intensity"><strong>{{created()!.intensity}}</strong><small>de 5</small></div></article></section>}
@if(showForm()){<div class="modal-layer"><button class="modal-backdrop" type="button" (click)="closeForm()" aria-label="Cerrar formulario"></button><form class="modal" [formGroup]="form" (ngSubmit)="save()"><div class="modal-head"><div><span class="eyebrow">NUEVO CHECK-IN</span><h2>Como te sientes?</h2></div><button type="button" (click)="closeForm()" aria-label="Cerrar">x</button></div><label>Emocion<select formControlName="emotionId"><option [ngValue]="0">Selecciona una emocion</option>@for(e of emotions();track e.id){<option [ngValue]="e.id">{{e.name}}</option>}</select></label>@if(form.controls.emotionId.touched&&form.controls.emotionId.invalid){<div class="field-error">Selecciona una emocion activa.</div>}<label>Intensidad <b>{{form.controls.intensity.value}}/5</b><input type="range" min="1" max="5" formControlName="intensity"></label><label>Contexto<input formControlName="context" placeholder="Estudios, trabajo, familia..." maxlength="100"></label>@if(form.controls.context.touched&&form.controls.context.invalid){<div class="field-error">El contexto es obligatorio y debe tener maximo 100 caracteres.</div>}<label>Nota opcional<textarea formControlName="note" rows="4" maxlength="500" placeholder="Que paso? Escribe solo lo que quieras recordar."></textarea><small>Maximo 500 caracteres.</small></label>@if(formError()){<div class="alert error">{{formError()}}</div>}<div class="modal-actions"><button type="button" class="btn secondary" (click)="closeForm()" [disabled]="saving()">Cancelar</button><button class="btn primary" [disabled]="form.invalid||saving()">{{saving()?'Guardando...':'Guardar check-in'}}</button></div></form></div>}
`
})
export class CheckinsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  readonly emotions = signal<Emotion[]>([]);
  readonly created = signal<Checkin | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly showForm = signal(false);
  readonly formError = signal('');
  readonly form = this.fb.nonNullable.group({
    emotionId: [0, [Validators.required, Validators.min(1)]],
    intensity: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
    context: ['', [Validators.required, Validators.maxLength(100)]],
    note: ['', [Validators.maxLength(500)]]
  });

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.loadEmotions();
  }

  loadEmotions(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.emotions(true).subscribe({
      next: emotions => {
        this.emotions.set(emotions);
        this.loading.set(false);
      },
      error: error => {
        this.loading.set(false);
        this.error.set(this.errorMessage(error, 'No se pudo cargar el catalogo de emociones.'));
      }
    });
  }

  openForm(): void {
    this.form.reset({emotionId: this.emotions()[0]?.id ?? 0, intensity: 3, context: '', note: ''});
    this.clearMessages();
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
    this.formError.set('');
  }

  selectEmotion(emotion: Emotion): void {
    this.openForm();
    this.form.patchValue({emotionId: emotion.id});
  }

  save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.clearMessages();
    this.api.createCheckin(this.form.getRawValue()).subscribe({
      next: checkin => {
        this.created.set(checkin);
        this.saving.set(false);
        this.success.set('Check-in emocional creado correctamente.');
        this.closeForm();
      },
      error: error => {
        this.saving.set(false);
        this.formError.set(this.errorMessage(error, 'No se pudo crear el check-in. Revisa los datos e intenta nuevamente.'));
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
}
