import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AdminAuditComponent } from './admin-audit.component';

describe('AdminAuditComponent', () => {
  it('loads the paginated administrative audit and supports resource filters', () => {
    TestBed.configureTestingModule({
      imports: [AdminAuditComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    const fixture = TestBed.createComponent(AdminAuditComponent);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    const initial = http.expectOne(req => req.url.endsWith('/admin/audit') && req.params.get('limit') === '25' && req.params.get('offset') === '0');
    initial.flush([]);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No hay eventos');

    const filter = (fixture.nativeElement as HTMLElement).querySelector<HTMLSelectElement>('#audit-resource')!;
    filter.value = 'RULE';
    filter.dispatchEvent(new Event('change'));
    const request = http.expectOne(req => req.url.endsWith('/admin/audit') && req.params.get('resourceType') === 'RULE');
    request.flush([]);
    fixture.detectChanges();
    expect(fixture.componentInstance.resourceType()).toBe('RULE');
    http.verify();
  });
});
