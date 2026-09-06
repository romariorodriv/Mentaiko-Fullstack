import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { ShellComponent } from './layouts/app-shell/app-shell.component';

export const routes: Routes = [
  {path: '', pathMatch: 'full', loadComponent: () => import('./features/landing/landing.component').then(m => m.LandingComponent)},
  {path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)},
  {path: 'registro', loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)},
  {path: 'app', component: ShellComponent, canActivate: [authGuard], children: [
    {path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)},
    {path: 'checkins', loadComponent: () => import('./features/checkins/checkins.component').then(m => m.CheckinsComponent)},
    {path: 'actividades', loadComponent: () => import('./features/activities/activities.component').then(m => m.ActivitiesComponent)},
    {path: 'perfil', loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent)},
    {path: 'admin/emociones', canActivate: [adminGuard], loadComponent: () => import('./features/admin/emotions/admin-emotions.component').then(m => m.AdminEmotionsComponent)},
    {path: 'admin/actividades', canActivate: [adminGuard], loadComponent: () => import('./features/admin/activities/admin-activities.component').then(m => m.AdminActivitiesComponent)},
    {path: 'admin/indicadores', canActivate: [adminGuard], loadComponent: () => import('./features/admin/indicators/admin-indicators.component').then(m => m.AdminIndicatorsComponent)},
    {path: '', pathMatch: 'full', redirectTo: 'dashboard'}
  ]},
  {path: '**', redirectTo: 'login'}
];
