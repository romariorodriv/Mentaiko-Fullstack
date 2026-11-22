import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { SleepComponent } from './sleep.component';

describe('SleepComponent', () => {
  it('creates the component', () => {
    TestBed.configureTestingModule({ imports: [SleepComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    expect(TestBed.createComponent(SleepComponent).componentInstance).toBeTruthy();
  });
});
