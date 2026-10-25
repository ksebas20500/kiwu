package app.kiwu.android.ui.ajustes

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kiwu.android.Idioma
import app.kiwu.android.R
import app.kiwu.android.data.ResumenImportacion

private fun nombreDe(etiqueta: String): Int = when (etiqueta) {
    "es" -> R.string.idioma_es
    "en" -> R.string.idioma_en
    "pt" -> R.string.idioma_pt
    else -> R.string.idioma_sistema
}

/** El idioma que se ve ahora, para la línea bajo «Idioma» en Ajustes. */
internal fun nombreIdioma(c: Context): String {
    val elegido = Idioma.elegido(c)
    if (elegido.isNotEmpty()) return c.getString(nombreDe(elegido))
    val actual = Idioma.actual(c).language
    return c.getString(nombreDe(if (actual in Idioma.etiquetas) actual else "es"))
}

/** Selector de idioma propio de Kiwu (Android 8–12). Al elegir, guarda y vuelve a crear la pantalla con el idioma nuevo. */
@Composable
internal fun DialogoIdioma(alCerrar: () -> Unit) {
    val contexto = LocalContext.current
    val elegido = Idioma.elegido(contexto)
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(R.string.idioma_titulo)) },
        text = {
            Column {
                for (etiqueta in Idioma.etiquetas) {
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            alCerrar()
                            if (etiqueta != elegido) {
                                Idioma.guardar(contexto, etiqueta)
                                contexto.actividad()?.recreate()
                            }
                        }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(etiqueta == elegido, onClick = null, modifier = Modifier.padding(12.dp))
                        Text(stringResource(nombreDe(etiqueta)))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text(stringResource(R.string.cancelar)) } },
    )
}

private fun Context.actividad(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.actividad()
    else -> null
}

/** «Importado: 3 listas, 12 tareas, 2 faltas»: solo lo que llegó. */
internal fun textoImportado(r: Resources, i: ResumenImportacion): String {
    val partes = listOf(
        R.plurals.importado_listas to i.materias, R.plurals.importado_tareas to i.deberes, R.plurals.importado_cuadernos to i.cuadernos,
        R.plurals.importado_notas to i.paginas, R.plurals.importado_horarios to i.horarios, R.plurals.importado_clases to i.clases,
        R.plurals.importado_faltas to i.faltas,
    ).filter { it.second > 0 }.map { (id, n) -> r.getQuantityString(id, n, n) }
    val lista = partes.ifEmpty { listOf(r.getQuantityString(R.plurals.importado_tareas, 0, 0)) }.joinToString(", ")
    return r.getString(R.string.importado, lista)
}
