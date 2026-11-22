import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';

@Component({selector: 'app-forgot-password', standalone: true, imports: [ReactiveFormsModule, RouterLink], template: `
<main class="auth-page">
  <section class="auth-story"><a class="brand brand-light" routerLink="/"><img class="brand-logo" src="/branding/mentaiko-logo.svg" alt="Mentaiko"></a><div><span class="eyebrow light">RECUPERA TU ACCESO</span><h1>Vuelve a tu espacio de bienestar.</h1><p>Te enviaremos un enlace para elegir una contraseña nueva.</p></div></section>
  <section class="auth-form-wrap"><form class="auth-form" [formGroup]="form" (ngSubmit)="submit()" [attr.aria-busy]="loading()">
    <span class="eyebrow">ESTAMOS PARA AYUDARTE</span><h2>¿Olvidaste tu contraseña?</h2>
    <p>Ingresa el correo de tu cuenta. El enlace será válido durante 15 minutos.</p>
    @if(message()) { <div class="alert success" role="status">{{message()}} Revisa también tu carpeta de spam.</div><button type="button" class="btn secondary full" (click)="message.set('')">Solicitar otro enlace</button><p>Espera dos minutos antes de volver a solicitarlo.</p> }
    @else {
      <label>Correo electrónico<input type="email" formControlName="email" autocomplete="email" maxlength="150" [readOnly]="loading()"></label>
      @if(form.controls.email.touched && form.controls.email.invalid) { <small role="alert">Ingresa un correo válido de hasta 150 caracteres.</small> }
      @if(error()) { <div class="alert error" role="alert">{{error()}}</div> }
      <button class="btn primary full" [disabled]="form.invalid || loading()">{{loading() ? 'Enviando...' : 'Enviar enlace'}}</button>
    }
    <p class="auth-switch"><a routerLink="/login">Volver al inicio de sesión</a></p>
  </form></section>
</main>`})
export class ForgotPasswordComponent {
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  readonly loading = signal(false);
  readonly message = signal('');
  readonly error = signal('');
  readonly form = this.fb.nonNullable.group({email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]]});

  submit(): void {
    if (this.loading() || this.message()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.error.set('');
    this.auth.forgotPassword(this.form.getRawValue().email).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: response => this.message.set(response.message),
      error: error => this.error.set(error.status === 429
        ? 'Demasiadas solicitudes. Intenta de nuevo en 15 minutos.'
        : 'No pudimos procesar la solicitud. Revisa tu conexión e intenta de nuevo.')
    });
  }
}
