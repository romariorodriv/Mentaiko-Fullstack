import { DatePipe } from '@angular/common';
import { Component, ElementRef, inject, OnInit, signal, ViewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { ApiError, Checkin, Emotion } from '../../shared/models/models';

@Component({
  selector: 'app-checkins',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe],
  template: `
<header class="page-head"><div><span class="eyebrow">HISTORIAL EMOCIONAL</span><h1>Mis check-ins</h1><p>Revisa tus registros del mas reciente al mas antiguo.</p></div><button class="btn primary" (click)="openForm()" [disabled]="emotionsLoading()||!emotions().length">+ Nuevo check-in</button></header>
@if(error()){<div class="alert error">{{error()}} <button type="button" (click)="retry()">Reintentar</button></div>} @if(success()){<div class="alert success">{{success()}}</div>}
<section class="panel filters" [formGroup]="filtersForm"><label>Desde<input type="date" formControlName="from"></label><label>Hasta<input type="date" formControlName="to"></label><label>Contexto<input formControlName="context" placeholder="Ej. Estudios" maxlength="100"></label><button class="btn secondary" type="button" (click)="applyFilters()" [disabled]="historyLoading()">Filtrar</button><button class="btn secondary" type="button" (click)="clearFilters()" [disabled]="historyLoading()||!hasFilters()">Limpiar</button></section>
@if(historyLoading()){<div class="loading-panel">Cargando historial...</div>} @else {
  <section class="timeline">
    @for(item of items();track item.id){<article class="checkin-card"><div class="emotion-dot" [style.--intensity]="item.intensity"></div><div class="checkin-main"><div><span class="emotion-name">{{item.emotion.name}}</span><span class="context-tag">{{item.context}}</span></div><p>{{item.note||'Sin nota personal'}}</p><small>{{item.createdAt|date:'d MMM y, h:mm a'}}</small></div><div class="intensity"><strong>{{item.intensity}}</strong><small>de 5</small></div><div class="row-actions"><button type="button" (click)="openEdit(item)" [disabled]="emotionsLoading()" [attr.aria-label]="'Editar check-in de '+item.emotion.name">Editar</button></div></article>}
    @empty {<div class="empty"><b>{{hasFilters()?'No hay resultados':'Aun no tienes check-ins'}}</b><p>{{hasFilters()?'Prueba con otro rango de fechas o contexto.':'Crea tu primer registro emocional cuando quieras.'}}</p></div>}
  </section>
  @if(totalElements()>0){<div class="pagination"><button class="btn secondary" type="button" (click)="previousPage()" [disabled]="page()===0||historyLoading()">Anterior</button><span>Pagina {{page()+1}} de {{totalPages()}}</span><button class="btn secondary" type="button" (click)="nextPage()" [disabled]="page()+1>=totalPages()||historyLoading()">Siguiente</button></div>}
}
@if(showForm()){<div class="modal-layer"><button class="modal-backdrop" type="button" (click)="closeForm()" aria-label="Cerrar formulario"></button><form class="modal" [formGroup]="form" (ngSubmit)="save()" role="dialog" aria-modal="true" aria-labelledby="checkinFormTitle"><div class="modal-head"><div><span class="eyebrow">{{isEditing()?'EDITAR CHECK-IN':'NUEVO CHECK-IN'}}</span><h2 id="checkinFormTitle">{{isEditing()?'Actualizar registro':'Como te sientes?'}}</h2></div><button type="button" (click)="closeForm()" aria-label="Cerrar">x</button></div>@if(emotionsLoading()){<div class="loading-panel">Cargando emociones...</div>} @else {<label>Emocion<select #emotionSelect formControlName="emotionId" aria-describedby="emotionError"><option [ngValue]="0">Selecciona una emocion</option>@for(e of emotions();track e.id){<option [ngValue]="e.id">{{e.name}}</option>}</select></label>@if(form.controls.emotionId.touched&&form.controls.emotionId.invalid){<div class="field-error" id="emotionError">Selecciona una emocion activa.</div>}}<label>Intensidad <b>{{form.controls.intensity.value}}/5</b><input type="range" min="1" max="5" formControlName="intensity"></label><label>Contexto<input formControlName="context" placeholder="Estudios, trabajo, familia..." maxlength="100" aria-describedby="contextError"></label>@if(form.controls.context.touched&&form.controls.context.invalid){<div class="field-error" id="contextError">El contexto es obligatorio y debe tener maximo 100 caracteres.</div>}<label>Nota opcional<textarea formControlName="note" rows="4" maxlength="500" placeholder="Que paso? Escribe solo lo que quieras recordar."></textarea><small>Maximo 500 caracteres.</small></label>@if(formError()){<div class="alert error" role="alert">{{formError()}}</div>}<div class="modal-actions"><button type="button" class="btn secondary" (click)="closeForm()" [disabled]="saving()">Cancelar</button><button class="btn primary" [disabled]="form.invalid||saving()">{{saving()?(isEditing()?'Actualizando...':'Guardando...'):(isEditing()?'Guardar cambios':'Guardar check-in')}}</button></div></form></div>}
`
})
export class CheckinsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  @ViewChild('emotionSelect') private emotionSelect?: ElementRef<HTMLSelectElement>;

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

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.loadEmotions();
    this.loadHistory();
  }

  loadEmotions(): void {
    this.emotionsLoading.set(true);
    this.api.emotions(true).subscribe({
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
    this.api.checkins({
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
      ? this.api.createCheckin(request)
      : this.api.updateCheckin(editingId, request);

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
