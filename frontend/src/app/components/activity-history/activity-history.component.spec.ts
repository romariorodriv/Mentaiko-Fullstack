import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ActivityHistoryComponent } from './activity-history.component';

describe('ActivityHistoryComponent', () => {
  it('creates the component', () => {
    TestBed.configureTestingModule({
      imports: [ActivityHistoryComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    expect(TestBed.createComponent(ActivityHistoryComponent).componentInstance).toBeTruthy();
  });
});
