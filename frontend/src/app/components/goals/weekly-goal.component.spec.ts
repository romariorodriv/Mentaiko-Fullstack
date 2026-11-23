import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { WeeklyGoalComponent } from './weekly-goal.component';

describe('WeeklyGoalComponent', () => {
  it('creates the component', () => {
    TestBed.configureTestingModule({ imports: [WeeklyGoalComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    expect(TestBed.createComponent(WeeklyGoalComponent).componentInstance).toBeTruthy();
  });
});
