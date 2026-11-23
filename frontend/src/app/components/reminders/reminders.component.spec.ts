import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RemindersComponent } from './reminders.component';

describe('RemindersComponent', () => {
  it('creates the component', () => {
    TestBed.configureTestingModule({ imports: [RemindersComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    expect(TestBed.createComponent(RemindersComponent).componentInstance).toBeTruthy();
  });
});
