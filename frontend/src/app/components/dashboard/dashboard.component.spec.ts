import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [DashboardComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
  });

  it('creates the component', () => {
    expect(TestBed.createComponent(DashboardComponent).componentInstance).toBeTruthy();
  });

  it('defaults the comparison to the previous week vs the current week', () => {
    const component = TestBed.createComponent(DashboardComponent).componentInstance;
    expect(component.weekB).toBe(component.week);
    expect(component.weekA).toMatch(/^\d{4}-W\d{2}$/);
    expect(component.weekA).not.toBe(component.weekB);
  });

  it('loads the comparison and exposes the record counts of both weeks', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    const http = TestBed.inject(HttpTestingController);
    const component = fixture.componentInstance;

    component.weekA = '2026-W39';
    component.weekB = '2026-W40';
    component.loadCompare();

    const request = http.expectOne(r => r.url.endsWith('/reports/compare'));
    expect(request.request.params.get('weekA')).toBe('2026-W39');
    expect(request.request.params.get('weekB')).toBe('2026-W40');
    request.flush({
      weekA: { week: '2026-W39', count: 3, averageIntensity: 3, predominantEmotion: 'Calma' },
      weekB: { week: '2026-W40', count: 1, averageIntensity: 4, predominantEmotion: 'Ansiedad' }
    });

    expect(component.compare()?.weekA.count).toBe(3);
    expect(component.compare()?.weekB.count).toBe(1);
    expect(component.isLowData(1)).toBe(true);
    expect(component.isLowData(3)).toBe(false);
  });
});
