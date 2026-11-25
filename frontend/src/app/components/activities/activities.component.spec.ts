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
    http.expectOne('/api/micro-activities/favorites').flush([]);
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => http.verify());

  describe('favoritas (HU-24)', () => {
    it('adds a favorite and updates the accessible star', () => {
      const fixture = createWith([]);
      const component = fixture.componentInstance;
      component.toggleFavorite(activity);
      const add = http.expectOne('/api/micro-activities/1/favorite');
      expect(add.request.method).toBe('POST');
      add.flush({ ...activity, savedAt: '2026-11-23T12:00:00' });
      fixture.detectChanges();
      expect(component.isFavorite(activity.id)).toBe(true);
      const button: HTMLButtonElement = fixture.nativeElement.querySelector('.favorite-toggle');
      expect(button.getAttribute('aria-pressed')).toBe('true');
      expect(button.getAttribute('aria-label')).toContain('Quitar de favoritas');
    });

    it('removes the favorite only after the backend confirms', () => {
      const fixture = createWith([]);
      const component = fixture.componentInstance;
      component.favorites.set([{...activity, activityType: 'GENERAL', savedAt: '2026-11-23T12:00:00'}]);
      component.toggleFavorite(activity);
      const remove = http.expectOne('/api/micro-activities/1/favorite');
      expect(remove.request.method).toBe('DELETE');
      expect(component.isFavorite(activity.id)).toBe(true);
      remove.flush(null);
      expect(component.isFavorite(activity.id)).toBe(false);
    });

    it('preserves the favorite state after a failed remove', () => {
      const fixture = createWith([]);
      const component = fixture.componentInstance;
      component.favorites.set([{...activity, activityType: 'GENERAL', savedAt: '2026-11-23T12:00:00'}]);
      component.toggleFavorite(activity);
      http.expectOne('/api/micro-activities/1/favorite')
        .flush({message: 'Error'}, {status: 500, statusText: 'Server Error'});
      expect(component.isFavorite(activity.id)).toBe(true);
      expect(component.error()).toContain('No se pudo actualizar');
      expect(component.favoriteBusyId()).toBeNull();
    });
  });

  describe('vista Mis Favoritas (HU-24)', () => {
    it('shows an empty state distinct from an API failure', () => {
      const fixture = createWith([]);
      fixture.componentInstance.selectCatalogTab('favorites');
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Todavia no tienes favoritas');
    });

    it('shows saved activities and removes them from the favorites tab', () => {
      const fixture = createWith([]);
      const component = fixture.componentInstance;
      component.favorites.set([{...activity, activityType:'GENERAL', savedAt:'2026-11-23T12:00:00'}]);
      component.selectCatalogTab('favorites');
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('[aria-label="Mis microactividades favoritas"]')).not.toBeNull();
      component.toggleFavorite(activity);
      http.expectOne('/api/micro-activities/1/favorite').flush(null);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Todavia no tienes favoritas');
    });

    it('keeps the empty state hidden on failed favorites load and allows retry', () => {
      const fixture = createWith([]);
      const component = fixture.componentInstance;
      component.selectCatalogTab('favorites');
      component.favoritesError.set('No se pudieron cargar tus favoritas.');
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain('Todavia no tienes favoritas');
      expect(fixture.nativeElement.textContent).toContain('Reintentar');
      component.loadFavorites();
      http.expectOne('/api/micro-activities/favorites').flush([]);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Todavia no tienes favoritas');
    });
  });

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

  describe('comparacion antes y despues (HU-28)', () => {
    it('completes with the selected post intensity and stores the comparison', () => {
      const fixture = createWith([recommendation({ id: 20, completedAt: undefined, preIntensity: 4 })]);
      const component = fixture.componentInstance;
      const item = component.recommendations()[0];

      component.selectPost(item, 2);
      component.complete(item);

      const request = http.expectOne('/api/recommendations/20/complete');
      expect(request.request.method).toBe('POST');
      expect(request.request.body).toEqual({ postIntensity: 2 });
      request.flush(recommendation({ id: 20, preIntensity: 4, postIntensity: 2 }));

      const updated = component.recommendations()[0];
      expect(updated.postIntensity).toBe(2);
      expect(component.currentPost(updated)).toBe(0);
      expect(component.comparisonText(updated)).toContain('de 4 a 2');
    });

    it('lets the user complete without a post intensity', () => {
      const fixture = createWith([recommendation({ id: 21, completedAt: undefined, preIntensity: 3 })]);
      const component = fixture.componentInstance;

      component.complete(component.recommendations()[0]);

      const request = http.expectOne('/api/recommendations/21/complete');
      expect(request.request.body).toEqual({});
      request.flush(recommendation({ id: 21, preIntensity: 3 }));

      expect(component.recommendations()[0].completedAt).toBeTruthy();
      expect(component.recommendations()[0].postIntensity).toBeFalsy();
    });

    it('clears the selected intensity when the same level is chosen twice', () => {
      const fixture = createWith([recommendation({ id: 22, completedAt: undefined })]);
      const component = fixture.componentInstance;
      const item = component.recommendations()[0];

      component.selectPost(item, 5);
      expect(component.currentPost(item)).toBe(5);
      component.selectPost(item, 5);
      expect(component.currentPost(item)).toBe(0);
    });

    it('shows the before and after values on a completed activity', () => {
      const fixture = createWith([recommendation({ preIntensity: 4, postIntensity: 2 })]);
      const compare: HTMLElement = fixture.nativeElement.querySelector('.activity-compare');

      expect(compare).not.toBeNull();
      expect(compare.textContent).toContain('Antes');
      expect(compare.textContent).toContain('4/5');
      expect(compare.textContent).toContain('Despues');
      expect(compare.textContent).toContain('2/5');
      expect(compare.textContent).toContain('no es una evaluacion clinica');
      expect(fixture.nativeElement.querySelector('.activity-post')).toBeNull();
    });

    it('does not show a comparison when no post intensity was registered', () => {
      const fixture = createWith([recommendation({ preIntensity: 4 })]);

      expect(fixture.nativeElement.querySelector('.activity-compare')).toBeNull();
      expect(fixture.nativeElement.querySelector('.activity-post')).not.toBeNull();
    });

    it('adds the post intensity later to an already completed activity', () => {
      const fixture = createWith([recommendation({ id: 23, preIntensity: 5 })]);
      const component = fixture.componentInstance;
      const item = component.recommendations()[0];

      component.savePostIntensity(item);
      http.expectNone('/api/recommendations/23/complete');

      component.selectPost(item, 3);
      component.savePostIntensity(item);
      const request = http.expectOne('/api/recommendations/23/complete');
      expect(request.request.body).toEqual({ postIntensity: 3 });
      request.flush(recommendation({ id: 23, preIntensity: 5, postIntensity: 3 }));

      expect(component.recommendations()[0].postIntensity).toBe(3);
    });

    it('describes lower, equal and higher intensity without clinical language', () => {
      const fixture = createWith([]);
      const component = fixture.componentInstance;

      expect(component.comparisonText(recommendation({ preIntensity: 4, postIntensity: 2 }))).toContain('paso de 4 a 2');
      expect(component.comparisonText(recommendation({ preIntensity: 3, postIntensity: 3 }))).toContain('se mantuvo en 3');
      expect(component.comparisonText(recommendation({ preIntensity: 2, postIntensity: 4 }))).toContain('subio de 2 a 4');
      expect(component.comparisonText(recommendation({ preIntensity: 2 }))).toBe('');
    });
  });
});
