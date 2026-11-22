import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ActivitiesComponent } from './activities.component';

describe('ActivitiesComponent', () => {
  it('creates the component', () => {
    TestBed.configureTestingModule({ imports: [ActivitiesComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    expect(TestBed.createComponent(ActivitiesComponent).componentInstance).toBeTruthy();
  });
});
