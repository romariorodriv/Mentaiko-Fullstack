import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AdminActivitiesComponent } from './admin-activities.component';

describe('AdminActivitiesComponent', () => {
  it('creates the component', () => {
    TestBed.configureTestingModule({ imports: [AdminActivitiesComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    expect(TestBed.createComponent(AdminActivitiesComponent).componentInstance).toBeTruthy();
  });
});
