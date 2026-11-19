package app.kiwu.android.ui.guia

import androidx.compose.runtime.Composable
import app.kiwu.android.ui.onboarding.OnboardingScreen

/**
 * Guía de inicio / Onboarding de Kiwu.
 * Reemplaza la antigua guía por el nuevo Onboarding interactivo de 4 pasos generado desde Figma.
 * @param alTerminar Callback invocado al presionar "¡Empezar ahora!" o "Saltar".
 */
@Composable
fun GuiaScreen(alTerminar: () -> Unit) {
    OnboardingScreen(onFinish = alTerminar)
}
