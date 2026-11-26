import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

interface AuditEvent {
  id: number;
  actor_user_id: number;
  actor_name: string | null;
  action: string;
  resource_type: string;
  resource_id: number;
  before_summary: string | null;
  after_summary: string;
  occurred_at: string;
}

@Component({
  selector: 'app-admin-audit',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './admin-audit.component.html',
  styleUrl: './admin-audit.component.css'
})
export class AdminAuditComponent implements OnInit {
  private readonly http = inject(HttpClient);
  readonly events = signal<AuditEvent[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly resourceType = signal('');
  readonly page = signal(0);
  readonly pageSize = 25;

  ngOnInit(): void { this.load(); }
  changeFilter(value: string): void { this.resourceType.set(value); this.page.set(0); this.load(); }
  next(): void { if (this.events().length === this.pageSize) { this.page.update(p => p + 1); this.load(); } }
  previous(): void { if (this.page() > 0) { this.page.update(p => p - 1); this.load(); } }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    let params = new HttpParams().set('limit', this.pageSize).set('offset', this.page() * this.pageSize);
    if (this.resourceType()) params = params.set('resourceType', this.resourceType());
    this.http.get<AuditEvent[]>(`${environment.apiUrl}/admin/audit`, { params }).subscribe({
      next: data => { this.events.set(data); this.loading.set(false); },
      error: () => { this.events.set([]); this.error.set('No se pudo consultar el historial de auditoría.'); this.loading.set(false); }
    });
  }
}
