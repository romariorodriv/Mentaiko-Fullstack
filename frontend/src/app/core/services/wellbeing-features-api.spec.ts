import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting,HttpTestingController} from '@angular/common/http/testing';
import {DailyWellbeingApi} from './daily-wellbeing.service';
import {EmotionalFactorsApi} from './emotional-factors-api.service';
import {ActivityPreferencesApi} from './activity-preferences-api.service';

describe('Wellbeing feature HTTP contracts',()=>{
  let http:HttpTestingController;
  let daily:DailyWellbeingApi;
  let factors:EmotionalFactorsApi;
  let preferences:ActivityPreferencesApi;
  beforeEach(()=>{
    TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});
    http=TestBed.inject(HttpTestingController);
    daily=TestBed.inject(DailyWellbeingApi);
    factors=TestBed.inject(EmotionalFactorsApi);
    preferences=TestBed.inject(ActivityPreferencesApi);
  });
  afterEach(()=>http.verify());

  it('saves sleep and energy records by date',()=>{
    daily.saveSleep('2026-11-11',7.5,4).subscribe();
    const sleep=http.expectOne('/api/me/sleep/2026-11-11');
    expect(sleep.request.method).toBe('PUT');
    expect(sleep.request.body).toEqual({hours:7.5,quality:4});
    sleep.flush({date:'2026-11-11',hours:7.5,quality:4});

    daily.saveEnergy('2026-11-11',3).subscribe();
    const energy=http.expectOne('/api/me/energy/2026-11-11');
    expect(energy.request.method).toBe('PUT');
    expect(energy.request.body).toEqual({level:3});
    energy.flush({date:'2026-11-11',level:3});
  });

  it('uses the authenticated checkin endpoint for factor association',()=>{
    factors.save(24,[1,3]).subscribe();
    const saved=http.expectOne('/api/checkins/24/factors');
    expect(saved.request.method).toBe('PUT');
    expect(saved.request.body).toEqual({factorIds:[1,3]});
    saved.flush([1,3]);
    factors.analysis('2026-11-01','2026-11-11').subscribe();
    const analysis=http.expectOne(r=>r.url==='/api/me/emotional-factors/analysis');
    expect(analysis.request.params.get('from')).toBe('2026-11-01');
    expect(analysis.request.params.get('to')).toBe('2026-11-11');
    analysis.flush({from:'2026-11-01',to:'2026-11-11',factors:[]});
  });

  it('persists activity types without an editable user id',()=>{
    preferences.save(['RESPIRACION','MOVIMIENTO']).subscribe();
    const request=http.expectOne('/api/me/activity-preferences');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({selected:['RESPIRACION','MOVIMIENTO']});
    request.flush({selected:['RESPIRACION','MOVIMIENTO'],available:[]});
  });
});
