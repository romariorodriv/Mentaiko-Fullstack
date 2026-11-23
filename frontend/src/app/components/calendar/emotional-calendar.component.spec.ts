import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { EmotionalCalendarComponent } from './emotional-calendar.component';

describe('EmotionalCalendarComponent', () => {
  it('creates the component', () => {
    TestBed.configureTestingModule({ imports: [EmotionalCalendarComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    expect(TestBed.createComponent(EmotionalCalendarComponent).componentInstance).toBeTruthy();
  });
});
