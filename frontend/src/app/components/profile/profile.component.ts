import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
})
export class ProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  readonly deleting = signal(false);
  readonly confirmingDeletion = signal(false);
  readonly deletionPassword = signal('');
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly exporting = signal(false);
  readonly exportFrom = signal('');
  readonly exportTo = signal('');
  readonly includeNotes = signal(false);
  readonly message = signal('');
  readonly error = signal('');
  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    university: ['', [Validators.required, Validators.maxLength(150)]],
    career: ['', [Validators.required, Validators.maxLength(150)]],
  });

  constructor(public readonly auth: AuthService) {}

  ngOnInit(): void {
    const apply = () => {
      const profile = this.auth.profile();
      if (profile) {
        this.form.patchValue({
          name: profile.name,
          university: profile.university,
          career: profile.career,
        });
      }
      this.loading.set(false);
    };
    if (this.auth.profile()) {
      apply();
    } else {
      this.auth.loadProfile().subscribe({
        next: apply,
        error: () => {
          this.loading.set(false);
          this.error.set('No se pudo cargar el perfil.');
        },
      });
    }
  }

  initials(): string {
    return (this.auth.profile()?.name ?? 'M')
      .split(' ').slice(0, 2).map(part => part[0]).join('').toUpperCase();
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.message.set('');
    this.error.set('');
    this.auth.updateProfile(this.form.getRawValue()).subscribe({
      next: () => {
        this.saving.set(false);
        this.message.set('Perfil actualizado correctamente.');
      },
      error: failure => {
        this.saving.set(false);
        this.error.set(failure.error?.message ?? 'No se pudieron guardar los cambios.');
      },
    });
  }

  exportData(): void {
    if (this.exporting()) return;
    if (this.exportFrom() && this.exportTo() && this.exportFrom() > this.exportTo()) {
      this.error.set('La fecha inicial no puede ser posterior a la fecha final.');
      return;
    }
    this.exporting.set(true);
    this.error.set('');
    this.message.set('');
    this.auth.exportPersonalData(this.exportFrom(), this.exportTo(), this.includeNotes()).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'mentaiko-datos-personales.json';
        link.click();
        URL.revokeObjectURL(url);
        this.exporting.set(false);
        this.message.set('Archivo de datos personales generado.');
      },
      error: () => {
        this.exporting.set(false);
        this.error.set('No se pudo exportar tu información.');
      },
    });
  }

  deleteAccount(): void {
    if (this.deleting() || !this.deletionPassword()) return;
    this.deleting.set(true);
    this.error.set('');
    this.auth.deleteAccount(this.deletionPassword()).subscribe({
      next: () => {
        this.deleting.set(false);
        this.deletionPassword.set('');
      },
      error: () => {
        this.deleting.set(false);
        this.error.set('No se pudo eliminar la cuenta. Verifica tu contraseña e inténtalo nuevamente.');
      },
    });
  }
}
