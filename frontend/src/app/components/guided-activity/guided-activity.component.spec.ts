import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { GuidedActivityComponent } from './guided-activity.component';

describe('GuidedActivityComponent', () => {
  let fixture: ComponentFixture<GuidedActivityComponent>;
  let http: HttpTestingController;

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval', 'performance'] });
    TestBed.configureTestingModule({
      imports: [GuidedActivityComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(GuidedActivityComponent);
    fixture.componentRef.setInput('activity', {
      id: 7, title: 'Pausa breve', durationMinutes: 1,
      steps: [
        { title: 'Prepárate', instruction: 'Elige un momento cómodo.' },
        { title: 'Realiza la actividad', instruction: 'Respira a tu ritmo.' },
        { title: 'Cierra la pausa', instruction: 'Nota cómo te sientes.' }
      ]
    });
    fixture.componentRef.setInput('recommendationId', 12);
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
    http.verify();
    vi.useRealTimers();
  });

  function findButton(label: string): HTMLButtonElement | undefined {
    return Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'))
      .find(button => button.textContent?.trim() === label);
  }

  function click(label: string): void {
    const button = findButton(label);
    expect(button).toBeDefined();
    button!.click();
    fixture.detectChanges();
  }

  function advance(milliseconds: number): void {
    vi.advanceTimersByTime(milliseconds);
    fixture.detectChanges();
  }

  function displayedTime(): string {
    return fixture.nativeElement.querySelector('[role="timer"]').textContent.trim();
  }

  it('shows the three steps and supports pausing, resuming and resetting', () => {
    expect(displayedTime()).toBe('01:00');
    expect(fixture.nativeElement.querySelectorAll('.guide-steps li')).toHaveLength(3);
    expect(findButton('Confirmar realización')).toBeUndefined();
    click('Iniciar');
    advance(10_000);
    click('Pausar');
    advance(5_000);
    expect(displayedTime()).toBe('00:50');
    click('Reanudar');
    advance(5_000);
    expect(displayedTime()).toBe('00:45');
    click('Reiniciar');
    advance(2_000);
    expect(displayedTime()).toBe('01:00');
    expect(findButton('Iniciar')).toBeDefined();
    http.expectNone('/api/focus-sessions');
  });

  it('waits for explicit confirmation and retries a failed save with the same identifier', () => {
    click('Iniciar');
    advance(60_000);
    expect(displayedTime()).toBe('00:00');
    http.expectNone(() => true);
    click('Confirmar realización');

    const first = http.expectOne('/api/focus-sessions');
    expect(first.request.method).toBe('POST');
    expect(first.request.body).toEqual({
      requestId: expect.any(String), activityId: 7, recommendationId: 12, durationSeconds: 60
    });
    first.flush({ message: 'Intenta de nuevo' }, { status: 500, statusText: 'Server error' });
    fixture.detectChanges();
    click('Confirmar realización');

    const retry = http.expectOne('/api/focus-sessions');
    expect(retry.request.body.requestId).toBe(first.request.body.requestId);
    retry.flush({ id: 34, activityId: 7, recommendationId: 12, durationSeconds: 60, completedAt: '2026-11-23T15:00:00' });
    fixture.detectChanges();
    expect(findButton('Confirmar realización')).toBeUndefined();
    expect(fixture.nativeElement.textContent).toContain('Sesión guiada guardada.');
  });
});
