import { AfterViewInit, Component, DestroyRef, ElementRef, OnDestroy, OnInit, computed, inject, input, output, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ActivitiesService } from '../../core/services/activities.service';
import { FocusSession, GuidedMicroActivity } from '../../shared/models/models';

type TimerPhase = 'ready' | 'running' | 'paused' | 'finished' | 'saved';

@Component({
  selector: 'app-guided-activity',
  templateUrl: './guided-activity.component.html',
  styleUrl: './guided-activity.component.css'
})
export class GuidedActivityComponent implements OnInit, AfterViewInit, OnDestroy {
  readonly activity = input.required<GuidedMicroActivity>();
  readonly recommendationId = input<number>();
  readonly closed = output<void>();
  readonly recorded = output<FocusSession>();

  readonly phase = signal<TimerPhase>('ready');
  readonly saving = signal(false);
  readonly error = signal('');
  readonly durationSeconds = computed(() => this.activity().durationMinutes * 60);
  readonly currentStep = computed(() => {
    switch (this.phase()) {
      case 'ready': return 0;
      case 'running':
      case 'paused': return 1;
      case 'finished':
      case 'saved': return 2;
    }
  });
  readonly remainingSeconds = signal(0);
  readonly time = computed(() => {
    const seconds = this.remainingSeconds();
    return `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
  });

  private readonly api = inject(ActivitiesService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly heading = viewChild.required<ElementRef<HTMLHeadingElement>>('heading');
  private interval: ReturnType<typeof setInterval> | undefined;
  private remainingMs = 0;
  private deadline = 0;
  private requestId: string | undefined;

  ngOnInit(): void {
    this.restart();
  }

  ngAfterViewInit(): void {
    this.heading().nativeElement.focus();
  }

  ngOnDestroy(): void {
    this.stopInterval();
  }

  start(): void {
    if (this.phase() !== 'ready' && this.phase() !== 'paused') return;

    this.deadline = performance.now() + this.remainingMs;
    this.phase.set('running');
    this.interval = setInterval(() => this.tick(), 200);
  }

  pause(): void {
    if (this.phase() !== 'running') return;

    this.tick();
    if (this.phase() === 'running') {
      this.stopInterval();
      this.phase.set('paused');
    }
  }

  restart(): void {
    if (this.saving() || this.phase() === 'saved') return;

    this.stopInterval();
    this.remainingMs = this.durationSeconds() * 1000;
    this.remainingSeconds.set(this.durationSeconds());
    this.requestId = undefined;
    this.error.set('');
    this.phase.set('ready');
  }

  confirm(): void {
    if (this.phase() !== 'finished' || this.saving()) return;

    this.requestId ??= crypto.randomUUID();
    this.error.set('');
    this.saving.set(true);
    this.api.confirmFocusSession({
      requestId: this.requestId,
      activityId: this.activity().id,
      recommendationId: this.recommendationId(),
      durationSeconds: this.durationSeconds()
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: session => {
        this.saving.set(false);
        this.phase.set('saved');
        this.recorded.emit(session);
      },
      error: failure => {
        this.saving.set(false);
        this.error.set(failure.error?.message ?? 'No se pudo guardar la sesión. Intenta de nuevo.');
      }
    });
  }

  private tick(): void {
    this.remainingMs = Math.max(0, this.deadline - performance.now());
    this.remainingSeconds.set(Math.ceil(this.remainingMs / 1000));
    if (this.remainingMs === 0) {
      this.stopInterval();
      this.phase.set('finished');
    }
  }

  private stopInterval(): void {
    clearInterval(this.interval);
    this.interval = undefined;
  }
}
