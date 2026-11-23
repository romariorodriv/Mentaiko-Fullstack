import { Component, OnInit, DestroyRef, inject, signal } from '@angular/core';
import { interval, startWith, switchMap, catchError, of } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ReminderApi } from '../../core/services/reminder-api.service';

@Component({
  selector: 'app-shell', standalone: true, imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.css'
})
export class ShellComponent implements OnInit {
  readonly menuOpen = signal(false);
  readonly reminderDue = signal(false);
  private readonly destroyRef = inject(DestroyRef);
  constructor(public readonly auth: AuthService, private readonly reminders: ReminderApi) {}
  ngOnInit(): void {
    if (!this.auth.profile()) this.auth.loadProfile().subscribe({error: () => undefined});
    interval(60_000).pipe(
      startWith(0),
      switchMap(() => this.reminders.get().pipe(catchError(() => of(null)))),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(p => { if (p) this.reminderDue.set(p.due); });
  }
  markReminderSeen(): void {
    this.reminders.acknowledge().subscribe({
      next: p => this.reminderDue.set(p.due),
      error: () => undefined
    });
  }
  initials(): string { return (this.auth.profile()?.name ?? 'M').split(' ').slice(0,2).map(x => x[0]).join('').toUpperCase(); }
}
