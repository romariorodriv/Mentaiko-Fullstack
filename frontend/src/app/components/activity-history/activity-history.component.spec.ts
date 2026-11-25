import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { Recommendation } from '../../shared/models/models';
import { GuidedActivityComponent } from '../guided-activity/guided-activity.component';
import { ActivityHistoryComponent } from './activity-history.component';

describe('ActivityHistoryComponent', () => {
  const activity = (id: number, title: string, active = true) => ({
    id, title, description: `Descripción de ${title}`, durationMinutes: 5, active, createdAt: '', updatedAt: ''
  });
  const rated: Recommendation = {
    id: 1, checkinId: 10, activity: activity(3, 'Respiración guiada'), reason: 'Por ansiedad', fallbackUsed: false,
    createdAt: '2026-11-20T09:00:00', completedAt: '2026-11-20T09:10:00', helpfulnessRating: 4, feedbackComment: null
  };
  const unrated: Recommendation = {
    id: 2, checkinId: 11, activity: activity(4, 'Caminar'), reason: 'Por estrés', fallbackUsed: false,
    createdAt: '2026-11-19T09:00:00', completedAt: '2026-11-19T09:10:00', helpfulnessRating: null, feedbackComment: null
  };
  const retired: Recommendation = {
    id: 3, checkinId: 12, activity: activity(5, 'Estiramiento retirado', false), reason: 'Por cansancio', fallbackUsed: false,
    createdAt: '2026-11-18T09:00:00', completedAt: '2026-11-18T09:10:00', helpfulnessRating: 5, feedbackComment: null
  };
  const pending: Recommendation = {
    id: 4, checkinId: 13, activity: activity(6, 'Diario de gratitud'), reason: 'Pendiente', fallbackUsed: false,
    createdAt: '2026-11-21T09:00:00'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ActivityHistoryComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
  });

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  function setup(items: Recommendation[] = [rated, unrated, retired, pending]) {
    const fixture = TestBed.createComponent(ActivityHistoryComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne(r => r.method === 'GET' && r.url === '/api/recommendations/me').flush(items);
    fixture.detectChanges();
    return { fixture, http, component: fixture.componentInstance, element: fixture.nativeElement as HTMLElement };
  }

  function item(element: HTMLElement, title: string): HTMLElement {
    return Array.from(element.querySelectorAll<HTMLElement>('.history-item')).find(node => node.textContent?.includes(title))!;
  }

  it('uses GET /me/history with the date filters when the status is "Completadas"', () => {
    const { fixture, http, component, element } = setup();
    component.setFrom('2026-11-01');
    component.setTo('2026-11-30');
    component.setStatus('COMPLETED');

    const request = http.expectOne(r => r.method === 'GET' && r.url === '/api/recommendations/me/history');
    expect(request.request.params.get('from')).toBe('2026-11-01');
    expect(request.request.params.get('to')).toBe('2026-11-30');
    request.flush([rated]);
    fixture.detectChanges();

    expect(element.querySelectorAll('.history-item').length).toBe(1);
    expect(element.querySelector('.history-count')?.textContent).toContain('1 registros');
    // El resumen sigue usando todas las recomendaciones.
    expect(element.querySelector('.history-summary')?.textContent).toContain('4');
  });

  it('does not call the server and shows an error when from is after to', () => {
    const { fixture, http, component, element } = setup();
    component.setFrom('2026-11-30');
    component.setTo('2026-11-01');
    component.setStatus('COMPLETED');
    fixture.detectChanges();

    http.expectNone(r => r.url === '/api/recommendations/me/history');
    expect(element.textContent).toContain('La fecha inicial no puede ser posterior a la fecha final.');
  });

  it('shows the helpfulness rating given by the user', () => {
    const { element } = setup();
    const node = item(element, 'Respiración guiada');
    expect(node.querySelector('.history-rating')?.textContent).toContain('4/5');
    expect(node.querySelectorAll('.history-star.on').length).toBe(4);
  });

  it('shows "Sin valorar" and lets the user rate with the existing feedback endpoint', () => {
    const { fixture, http, element } = setup();
    const node = item(element, 'Caminar');
    expect(node.textContent).toContain('Sin valorar');

    node.querySelectorAll<HTMLButtonElement>('.star-group .star')[2].click();
    const request = http.expectOne(r => r.method === 'POST' && r.url === '/api/recommendations/2/feedback');
    expect(request.request.body).toEqual({ helpfulnessRating: 3 });
    request.flush({ ...unrated, helpfulnessRating: 3 });
    fixture.detectChanges();

    expect(item(element, 'Caminar').querySelector('.history-rating')?.textContent).toContain('3/5');
    expect(item(element, 'Caminar').textContent).not.toContain('Sin valorar');
  });

  it('"Repetir" opens the existing guided mode without linking it to the original recommendation', () => {
    const { fixture, http, element } = setup();
    item(element, 'Respiración guiada').querySelector<HTMLButtonElement>('.history-repeat')!.click();

    http.expectOne(r => r.method === 'GET' && r.url === '/api/micro-activities/3').flush({
      id: 3, title: 'Respiración guiada', durationMinutes: 5,
      steps: [{ title: 'Prepárate', instruction: 'Busca un lugar cómodo' }]
    });
    fixture.detectChanges();

    const guide = fixture.debugElement.query(By.directive(GuidedActivityComponent));
    expect(guide).not.toBeNull();
    expect(guide.componentInstance.activity().id).toBe(3);
    expect(guide.componentInstance.recommendationId()).toBeUndefined();
    http.expectNone(r => r.url.includes('/complete'));
  });

  it('disables "Repetir" with a message when the activity is inactive', () => {
    const { http, element } = setup();
    const node = item(element, 'Estiramiento retirado');
    const button = node.querySelector<HTMLButtonElement>('.history-repeat')!;

    expect(button.disabled).toBe(true);
    expect(node.textContent).toContain('Esta microactividad ya no está disponible.');
    button.click();
    http.expectNone(r => r.url.startsWith('/api/micro-activities/'));
  });
});
