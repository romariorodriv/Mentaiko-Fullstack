import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { matchingPasswords, utf8PasswordValidator } from '../password.validators';

@Component({selector: 'app-reset-password', standalone: true, imports: [ReactiveFormsModule, RouterLink], template: `
<main class="auth-page">
  <section class="auth-story"><a class="brand brand-light" routerLink="/"><img class="brand-logo" src="/branding/mentaiko-logo.svg" alt="Mentaiko"></a><div><span class="eyebrow light">UN NUEVO COMIENZO</span><h1>Elige una contraseña nueva.</h1><p>Usa una contraseña que no compartas con otros servicios.</p></div></section>
  <section class="auth-form-wrap"><form class="auth-form" [formGroup]="form" (ngSubmit)="submit()" [attr.aria-busy]="loading()">
    <h2>Restablecer contraseña</h2>
    @if(message()) { <div class="alert success" role="status">{{message()}}</div> }
    @else if(invalidLink()) {
      <div class="alert error" role="alert">El enlace está ausente, es inválido, ya fue usado o ha vencido.</div>
      <p><a routerLink="/olvide-contrasena">Solicitar un nuevo enlace</a></p>
    } @else {
      <p>Usa al menos 8 caracteres y un máximo de 72 bytes UTF-8. Las letras acentuadas y los emojis pueden ocupar varios bytes.</p>
      <label>Nueva contraseña<input type="password" formControlName="newPassword" autocomplete="new-password" maxlength="72" [readOnly]="loading()"></label>
      @if(form.controls.newPassword.touched && form.controls.newPassword.invalid) { <small role="alert">La contraseña debe tener al menos 8 caracteres, no estar en blanco y no superar 72 bytes UTF-8.</small> }
      <label>Confirmar contraseña<input type="password" formControlName="confirmPassword" autocomplete="new-password" maxlength="72" [readOnly]="loading()"></label>
      @if(form.controls.confirmPassword.touched && form.hasError('passwordMismatch')) { <small role="alert">Las contraseñas no coinciden.</small> }
      @if(error()) { <div class="alert error" role="alert">{{error()}}</div> }
      <button class="btn primary full" [disabled]="form.invalid || loading()">{{loading() ? 'Guardando...' : 'Guardar contraseña'}}</button>
    }
    <p class="auth-switch"><a routerLink="/login">Volver al inicio de sesión</a></p>
  </form></section>
</main>`})
export class ResetPasswordComponent {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly token = this.route.snapshot.queryParamMap.get('token') ?? '';
  readonly invalidLink = signal(!/^[A-Za-z0-9_-]{43}$/.test(this.token));
  readonly loading = signal(false);
  readonly message = signal('');
  readonly error = signal('');
  readonly form = this.fb.nonNullable.group({
    newPassword: ['', [Validators.required, Validators.pattern(/\S/), Validators.minLength(8), Validators.maxLength(72), utf8PasswordValidator]],
    confirmPassword: ['', Validators.required]
  }, {validators: matchingPasswords});

  submit(): void {
    if (this.loading() || this.message() || this.invalidLink()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.error.set('');
    this.auth.resetPassword(this.token, this.form.getRawValue().newPassword).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: response => { this.message.set(response.message); this.form.reset(); },
      error: error => {
        if (error.status === 400 && !error.error?.errors) this.invalidLink.set(true);
        else this.error.set(error.status === 429 ? 'Demasiadas solicitudes. Intenta de nuevo en 15 minutos.'
          : error.status === 400 ? 'Revisa la contraseña. Debe tener al menos 8 caracteres y como máximo 72 bytes UTF-8.'
          : 'No pudimos guardar la contraseña. Revisa tu conexión e intenta de nuevo.');
      }
    });
  }
}
