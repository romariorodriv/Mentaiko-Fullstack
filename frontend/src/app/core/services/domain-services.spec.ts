import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { EmotionsService } from './emotions.service';
import { CheckinsService } from './checkins.service';
import { ActivitiesService } from './activities.service';
import { RecommendationsService } from './recommendations.service';
import { ReportsService } from './reports.service';
import { AdminService } from './admin.service';

describe('Domain API services', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests the emotions catalog', () => {
    TestBed.inject(EmotionsService).emotions(true).subscribe();
    const req = http.expectOne(r => r.url === '/api/emotions');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('activeOnly')).toBe('true');
    req.flush([]);
  });

  it('sends check-in filters and pagination', () => {
    TestBed.inject(CheckinsService).checkins({ page: 1, size: 10, context: 'Estudios' }).subscribe();
    const req = http.expectOne(r => r.url === '/api/checkins');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('context')).toBe('Estudios');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 1, size: 10 });
  });

  it('retrieves the active activities catalog', () => {
    TestBed.inject(ActivitiesService).activities().subscribe();
    const req = http.expectOne(r => r.url === '/api/micro-activities');
    expect(req.request.params.get('activeOnly')).toBe('true');
    req.flush([]);
  });

  it('generates a recommendation for a check-in', () => {
    TestBed.inject(RecommendationsService).recommend(12).subscribe();
    const req = http.expectOne('/api/recommendations');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ checkinId: 12 });
    req.flush({ id: 8, checkinId: 12 });
  });

  it('requests weekly reports with the requested week', () => {
    TestBed.inject(ReportsService).weekly('2026-W47').subscribe();
    const req = http.expectOne(r => r.url === '/api/reports/weekly');
    expect(req.request.params.get('week')).toBe('2026-W47');
    req.flush([]);
  });

  it('requests paginated admin users', () => {
    TestBed.inject(AdminService).adminUsers(2, 5).subscribe();
    const req = http.expectOne(r => r.url === '/api/admin/users');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('5');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 2, size: 5 });
  });
});
