import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({selector:'app-login', standalone:true, imports:[ReactiveFormsModule,RouterLink], templateUrl: './login.component.html',
  styleUrl: './login.component.css'})
export class LoginComponent {
  private readonly fb=inject(FormBuilder);
  readonly loading=signal(false); readonly error=signal('');
  readonly form=this.fb.nonNullable.group({email:['',[Validators.required,Validators.email]],password:['',Validators.required]});
  constructor(private readonly auth:AuthService,private readonly router:Router){}
  submit():void{if(this.form.invalid)return;this.loading.set(true);this.error.set('');this.auth.login(this.form.getRawValue().email,this.form.getRawValue().password).subscribe({next:()=>void this.router.navigate([this.auth.role()==='ADMIN'?'/app/admin/dashboard':'/app/dashboard']),error:e=>{this.loading.set(false);this.error.set(e.status===401?'El correo o la contrasena no son correctos.':'No pudimos conectar con el servidor. Intenta nuevamente.');}});}
}
