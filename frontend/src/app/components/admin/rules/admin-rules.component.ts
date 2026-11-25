import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ActivitiesService } from '../../../core/services/activities.service';
import { AdminRulesService } from '../../../core/services/admin-rules.service';
import { EmotionsService } from '../../../core/services/emotions.service';
import { AdminRule, ApiError, Emotion, MicroActivity } from '../../../shared/models/models';

@Component({
  selector: 'app-admin-rules',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './admin-rules.component.html',
  styleUrl: './admin-rules.component.css'
})
export class AdminRulesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly rulesApi = inject(AdminRulesService);
  private readonly emotionsApi = inject(EmotionsService);
  private readonly activitiesApi = inject(ActivitiesService);

  readonly rules = signal<AdminRule[]>([]);
  readonly emotions = signal<Emotion[]>([]);
  readonly activities = signal<MicroActivity[]>([]);
  readonly editing = signal<AdminRule | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly busyId = signal<number | null>(null);
  readonly error = signal('');
  readonly success = signal('');
  readonly form = this.fb.group({
    emotionId: this.fb.control<number | null>(null, Validators.required),
    activityId: this.fb.control<number | null>(null, Validators.required),
    priority: this.fb.control<number | null>(1, [Validators.required, Validators.min(1)])
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      rules: this.rulesApi.rules(),
      emotions: this.emotionsApi.emotions(true),
      activities: this.activitiesApi.activities(true)
    }).subscribe({
      next: data => {
        this.rules.set(data.rules);
        this.emotions.set(data.emotions);
        this.activities.set(data.activities);
        this.loading.set(false);
      },
      error: error => {
        this.loading.set(false);
        this.error.set(this.errorMessage(error, 'No se pudieron cargar las reglas.'));
      }
    });
  }

  submit(): void {
    const editing = this.editing();
    if (editing) {
      this.saveEdit(editing);
      return;
    }
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.saving.set(true);
    this.clearMessages();
    this.rulesApi.createRule({
      emotionId: Number(value.emotionId),
      activityId: Number(value.activityId),
      priority: Number(value.priority)
    }).subscribe({
      next: rule => {
        this.saving.set(false);
        this.resetForm();
        this.success.set(`Regla "${rule.emotionName} → ${rule.activityTitle}" creada.`);
        this.reloadRules();
      },
      error: error => {
        this.saving.set(false);
        this.error.set(this.errorMessage(error, 'No se pudo crear la regla.'));
      }
    });
  }

  startEdit(rule: AdminRule): void {
    this.clearMessages();
    this.editing.set(rule);
    this.form.reset({ emotionId: rule.emotionId, activityId: rule.activityId, priority: rule.priority });
    this.form.controls.emotionId.disable();
    this.form.controls.activityId.disable();
  }

  cancelEdit(): void {
    this.resetForm();
  }

  toggle(rule: AdminRule): void {
    if (this.busyId() !== null) return;
    this.busyId.set(rule.id);
    this.clearMessages();
    this.rulesApi.updateRule(rule.id, { active: !rule.active }).subscribe({
      next: updated => {
        this.busyId.set(null);
        this.replaceRule(updated);
        this.success.set(`Regla "${updated.emotionName} → ${updated.activityTitle}" ${updated.active ? 'activada' : 'desactivada'}.`);
      },
      error: error => {
        this.busyId.set(null);
        this.error.set(this.errorMessage(error, 'No se pudo cambiar el estado de la regla.'));
      }
    });
  }

  private saveEdit(rule: AdminRule): void {
    const priority = this.form.controls.priority;
    if (priority.invalid || this.saving()) {
      priority.markAsTouched();
      return;
    }
    this.saving.set(true);
    this.clearMessages();
    this.rulesApi.updateRule(rule.id, { priority: Number(priority.value) }).subscribe({
      next: updated => {
        this.saving.set(false);
        this.resetForm();
        this.success.set(`Prioridad de "${updated.emotionName} → ${updated.activityTitle}" actualizada.`);
        this.reloadRules();
      },
      error: error => {
        this.saving.set(false);
        this.error.set(this.errorMessage(error, 'No se pudo actualizar la regla.'));
      }
    });
  }

  private reloadRules(): void {
    this.rulesApi.rules().subscribe({
      next: rules => this.rules.set(rules),
      error: error => this.error.set(this.errorMessage(error, 'No se pudieron cargar las reglas.'))
    });
  }

  private replaceRule(updated: AdminRule): void {
    this.rules.update(rules => rules.map(rule => rule.id === updated.id ? updated : rule));
  }

  private resetForm(): void {
    this.editing.set(null);
    this.form.reset({ emotionId: null, activityId: null, priority: 1 });
    this.form.controls.emotionId.enable();
    this.form.controls.activityId.enable();
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
