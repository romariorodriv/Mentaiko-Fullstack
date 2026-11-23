import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ActivityPreferencesComponent } from './activity-preferences.component';

describe('ActivityPreferencesComponent', () => {
  it('creates the component', () => {
    TestBed.configureTestingModule({ imports: [ActivityPreferencesComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    expect(TestBed.createComponent(ActivityPreferencesComponent).componentInstance).toBeTruthy();
  });
});
