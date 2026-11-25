/**
 * Etiquetas visibles de los tipos de microactividad (HU-22).
 * Equivalencia con Jira: "reflexión escrita" = ESCRITURA, "estiramiento" se agrupa en MOVIMIENTO
 * y "descanso ocular" = DESCANSO_OCULAR (migración database/012).
 */
export const ACTIVITY_TYPE_LABELS: Readonly<Record<string, string>> = {
  GENERAL: 'General',
  RESPIRACION: 'Respiración',
  MOVIMIENTO: 'Movimiento y estiramiento',
  RELAJACION: 'Relajación',
  ESCRITURA: 'Reflexión escrita',
  ATENCION_PLENA: 'Atención plena',
  DESCANSO_OCULAR: 'Descanso ocular'
};

export function activityTypeLabel(type: string): string {
  return ACTIVITY_TYPE_LABELS[type] ?? type;
}
