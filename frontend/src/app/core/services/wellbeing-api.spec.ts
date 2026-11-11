import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {WeeklyGoalApi} from './weekly-goal-api.service';
import {ReminderApi} from './reminder-api.service';

describe('Wellbeing settings API contracts',()=>{
  let http:HttpTestingController;
  let weekly:WeeklyGoalApi;
  let reminders:ReminderApi;
  beforeEach(()=>{
    TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});
    http=TestBed.inject(HttpTestingController);
    weekly=TestBed.inject(WeeklyGoalApi);
    reminders=TestBed.inject(ReminderApi);
  });
  afterEach(()=>http.verify());

  it('consults and updates the weekly target with no user-controlled id',()=>{
    const goal={weekStart:'2026-11-09',targetDays:3,completedDays:2,percentage:66,status:'EN_PROGRESO' as const};
    let result:any;
    weekly.get().subscribe(v=>result=v);
    http.expectOne('/api/me/weekly-goal').flush(goal);
    expect(result.completedDays).toBe(2);
    weekly.save(5).subscribe(v=>result=v);
    const put=http.expectOne('/api/me/weekly-goal');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual({targetDays:5});
    put.flush({...goal,targetDays:5,percentage:40});
    expect(result.targetDays).toBe(5);
  });

  it('persists reminder time and selected ISO weekdays',()=>{
    let result:any;
    reminders.update(true,'20:15',[1,3,5]).subscribe(v=>result=v);
    const put=http.expectOne('/api/me/reminders');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual({enabled:true,time:'20:15',days:[1,3,5]});
    put.flush({enabled:true,time:'20:15:00',days:[1,3,5],due:false});
    expect(result.days).toEqual([1,3,5]);
  });

  it('acknowledges a reminder without accepting another user identity',()=>{
    reminders.acknowledge().subscribe();
    const request=http.expectOne('/api/me/reminders/acknowledge');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
    request.flush({enabled:true,time:'20:00:00',days:[1],due:false});
  });
});
