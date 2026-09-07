import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { Emotion } from '../../shared/models/models';

@Component({
  selector: 'app-checkins',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
<header class="page-head"><div><span class="eyebrow">REGISTRO EMOCIONAL</span><h1>Mis check-ins</h1><p>Selecciona como te sientes para preparar tu proximo registro.</p></div><button class="btn primary" (click)="openForm()" [disabled]="loading()">+ Nuevo check-in</button></header>
@if(error()){<div class="alert error">{{error()}}</div>}
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
@if(showForm()){<div class="modal-layer"><button class="modal-backdrop" (click)="closeForm()"></button><form class="modal" [formGroup]="form" (ngSubmit)="previewPending()"><div class="modal-head"><div><span class="eyebrow">NUEVO CHECK-IN</span><h2>Como te sientes?</h2></div><button type="button" (click)="closeForm()">x</button></div><label>Emocion<select formControlName="emotionId"><option [ngValue]="0">Selecciona una emocion</option>@for(e of emotions();track e.id){<option [ngValue]="e.id">{{e.name}}</option>}</select></label><label>Intensidad <b>{{form.controls.intensity.value}}/10</b><input type="range" min="1" max="10" formControlName="intensity"></label><label>Contexto<input formControlName="context" placeholder="Estudios, trabajo, familia..."></label><label>Nota opcional<textarea formControlName="note" rows="4" placeholder="Que paso? Escribe solo lo que quieras recordar."></textarea></label><div class="alert">Guardar check-ins pertenece a HU05 y todavia esta pendiente. En HU04 solo se consulta el catalogo de emociones.</div>@if(formError()){<div class="alert error">{{formError()}}</div>}<div class="modal-actions"><button type="button" class="btn secondary" (click)="closeForm()">Cancelar</button><button class="btn primary" [disabled]="form.invalid">Validar seleccion</button></div></form></div>}
`
})
export class CheckinsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  readonly emotions = signal<Emotion[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly showForm = signal(false);
  readonly formError = signal('');
  readonly form = this.fb.nonNullable.group({
    emotionId: [0, [Validators.required, Validators.min(1)]],
    intensity: [5, [Validators.required, Validators.min(1), Validators.max(10)]],
    context: ['', [Validators.required, Validators.maxLength(100)]],
    note: ['']
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
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudo cargar el catalogo de emociones.');
      }
    });
  }

  openForm(): void {
    this.form.reset({emotionId: this.emotions()[0]?.id ?? 0, intensity: 5, context: '', note: ''});
    this.formError.set('');
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

  previewPending(): void {
    if (this.form.invalid) {
      this.formError.set('Selecciona una emocion e indica el contexto.');
      return;
    }
    this.formError.set('La seleccion es valida, pero guardar el check-in se implementara en HU05.');
  }
}
