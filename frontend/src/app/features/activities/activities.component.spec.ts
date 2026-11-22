import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivitiesComponent } from './activities.component';
import { MicroActivity, Recommendation } from '../../shared/models/models';

const activity: MicroActivity = {
  id: 1,
  title: 'Respiracion guiada',
  description: 'Respira lento',
  durationMinutes: 5,
  active: true,
  createdAt: '2026-11-01T10:00:00',
  updatedAt: '2026-11-01T10:00:00'
};

function recommendation(overrides: Partial<Recommendation> = {}): Recommendation {
  return {
    id: 10,
    checkinId: 3,
    activity,
    reason: 'Recomendacion de prueba',
    fallbackUsed: false,
    createdAt: '2026-11-22T10:00:00',
    completedAt: '2026-11-22T10:05:00',
    ...overrides
  };
}

describe('ActivitiesComponent', () => {
  let http: HttpTestingController;

  function createWith(recommendations: Recommendation[]) {
    TestBed.configureTestingModule({
      imports: [ActivitiesComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(ActivitiesComponent);
    fixture.detectChanges();
    http.expectOne(request => request.url === '/api/micro-activities').flush([activity]);
    http.expectOne('/api/recommendations/me').flush(recommendations);
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => http.verify());

  describe('valoracion de utilidad (HU-27)', () => {
    it('shows the rating controls only for completed activities', () => {
      const fixture = createWith([
        recommendation({ id: 10 }),
        recommendation({ id: 11, completedAt: undefined })
      ]);

      const sections = fixture.nativeElement.querySelectorAll('.activity-feedback');
      expect(sections.length).toBe(1);
    });

    it('keeps the save button disabled until a rating is chosen', () => {
      const fixture = createWith([recommendation()]);
      const component = fixture.componentInstance;
      const item = component.recommendations()[0];

      expect(component.canSubmitFeedback(item)).toBe(false);
      component.setRating(item, 4);
      expect(component.canSubmitFeedback(item)).toBe(true);
    });

    it('sends the rating and an optional trimmed comment to the feedback endpoint', () => {
      const fixture = createWith([recommendation()]);
      const component = fixture.componentInstance;
      const item = component.recommendations()[0];

      component.setRating(item, 5);
      component.setComment(item, '  Me ayudo a calmarme  ');
      component.submitFeedback(item);

      const request = http.expectOne('/api/recommendations/10/feedback');
      expect(request.request.method).toBe('POST');
      expect(request.request.body).toEqual({ helpfulnessRating: 5, feedbackComment: 'Me ayudo a calmarme' });
      request.flush(recommendation({ helpfulnessRating: 5, feedbackComment: 'Me ayudo a calmarme' }));

      expect(component.recommendations()[0].helpfulnessRating).toBe(5);
      expect(component.success()).toContain('Gracias por valorar');
      expect(component.canSubmitFeedback(component.recommendations()[0])).toBe(false);
    });

    it('omits the comment when it is left empty and limits it to 300 characters', () => {
      const fixture = createWith([recommendation()]);
      const component = fixture.componentInstance;
      const item = component.recommendations()[0];

      component.setRating(item, 3);
      component.setComment(item, 'a'.repeat(350));
      expect(component.currentComment(item).length).toBe(300);

      component.setComment(item, '   ');
      component.submitFeedback(item);

      const request = http.expectOne('/api/recommendations/10/feedback');
      expect(request.request.body).toEqual({ helpfulnessRating: 3 });
      request.flush(recommendation({ helpfulnessRating: 3 }));
    });

    it('shows an error and keeps the draft when the backend rejects the rating', () => {
      const fixture = createWith([recommendation()]);
      const component = fixture.componentInstance;
      const item = component.recommendations()[0];

      component.setRating(item, 2);
      component.submitFeedback(item);
      http.expectOne('/api/recommendations/10/feedback')
        .flush({ message: 'Primero debes realizar la actividad' }, { status: 400, statusText: 'Bad Request' });

      expect(component.error()).toBe('No se pudo guardar tu valoracion.');
      expect(component.currentRating(item)).toBe(2);
      expect(component.feedbackBusyId()).toBeNull();
    });
  });
});
