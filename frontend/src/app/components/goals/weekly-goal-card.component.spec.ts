import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { WeeklyGoalCardComponent } from './weekly-goal-card.component';

describe('WeeklyGoalCardComponent', () => {
  it('creates the component', () => {
    TestBed.configureTestingModule({ imports: [WeeklyGoalCardComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    expect(TestBed.createComponent(WeeklyGoalCardComponent).componentInstance).toBeTruthy();
  });
});
