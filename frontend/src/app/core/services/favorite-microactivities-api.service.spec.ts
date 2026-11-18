import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { FavoriteMicroactivitiesApiService } from './favorite-microactivities-api.service';

describe('FavoriteMicroactivitiesApiService', () => {
  let api: FavoriteMicroactivitiesApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    api = TestBed.inject(FavoriteMicroactivitiesApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists current user favorites without a user id', () => {
    let count = -1;
    api.list().subscribe(items => count = items.length);
    const request = http.expectOne('/api/me/favorite-micro-activities');
    expect(request.request.method).toBe('GET');
    request.flush([]);
    expect(count).toBe(0);
  });

  it('saves a specific activity with PUT', () => {
    api.save(7).subscribe();
    const request = http.expectOne('/api/me/favorite-micro-activities/7');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({});
    request.flush({id: 7, title: 'Respirar', savedAt: '2026-11-18T10:00:00'});
  });

  it('removes a favorite with DELETE', () => {
    api.remove(7).subscribe();
    const request = http.expectOne('/api/me/favorite-micro-activities/7');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, {status: 204, statusText: 'No Content'});
  });
});
