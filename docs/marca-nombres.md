# Nuevo nombre de la marca: propuestas con K

Complementa el apartado 5 de [plan-desarrollo.md](plan-desarrollo.md). **Todavía no se ha cambiado nada**: «Kiwu» sigue siendo el nombre en el código y en la interfaz hasta que se elija uno.

## 1. Por qué cambiarlo

«Kiwu» se lee y se pronuncia casi igual que «kiwi». En las tiendas y en los buscadores eso trae dos problemas: las búsquedas de «kiwi» (la fruta, Kiwi Browser, Kiwi.com…) tapan a la app, y quien busca «kiwu» corrige a «kiwi» sin darse cuenta. Cuanto antes se cambie, menos cuesta: todavía no hay cuentas, ni ficha en las tiendas, ni dominio.

## 2. Criterios

- Empieza por **K**.
- Se pronuncia igual en español e inglés y se escribe sin dudas.
- Corto (máximo 3 sílabas) y fácil de recordar.
- **No existe una app con ese nombre exacto en Google Play** y no tiene una marca conocida en la web.
- Dominios `.com`, `.app`, `.io` y `.es` libres, para no depender de uno solo.
- Que no se confunda con otra palabra o marca de apps de estudio.

## 3. Cómo se comprobó (13 oct 2026)

| Comprobación | Cómo |
| --- | --- |
| Google Play | Búsqueda del nombre en la tienda (región Colombia) y lectura de los resultados: se descarta cualquier nombre que aparezca exacto, y se anotan los parecidos |
| Web | Búsqueda del nombre entre comillas |
| Dominios | Consulta DNS de `.com`, `.app`, `.io` y `.es`: «libre» significa que **no responde ningún registro**, que es un indicio fuerte pero no una garantía (un dominio puede estar registrado sin configurar) |

**Limitaciones:** no se consultó el registro de marcas (ver sección 6), ni la App Store de Apple, ni las redes sociales. Los resultados de la búsqueda de Google Play son aproximados y pueden cambiar.

## 4. Candidatos

| Nombre | Significado | Google Play | Web | Dominios libres | Observaciones |
| --- | --- | --- | --- | --- | --- |
| **Kapunte** | de «apunte» (notas de clase) | Sin resultados | Sin coincidencias | `.com` `.app` `.io` `.es` | Corto, claro y con sentido para estudiantes. Lo más limpio de todos |
| **Kuaderia** | cuaderno + -ería | Sin resultados | Sin coincidencias | `.com` `.app` `.io` `.es` | Limpio, pero se pronuncia con más esfuerzo |
| **Kapuntes** | «apuntes» | Sin nombre exacto | Sin coincidencias | `.com` `.app` `.io` `.es` | Igual que Kapunte pero en plural; más largo |
| **Kuaderno** | «cuaderno» con K | Sin nombre exacto | Sin app con ese nombre; existe **Quaderno** (empresa de facturación) | `.io` `.es` (`.com` y `.app` ya registrados) | Es el que mejor explica el producto, pero pierde el `.com` y se parece a Quaderno |
| **Kemeo** | inventado | Sin nombre exacto (hay Keoto, Kismo…) | Sin coincidencias | `.app` `.io` `.es` (`.com` registrado) | Corto y neutro, sin sentido en español |
| **Kronota** | crono + nota | Sin nombre exacto, pero hay Krono, Kronos y Kronio | Sin coincidencias | `.com` `.app` `.io` `.es` | Se parece a otras apps de tiempo y turnos |

**Descartados**

| Nombre | Motivo |
| --- | --- |
| Kuadernia | Sin app ni dominio ocupado, pero suena igual que **Cuadernia**, un programa educativo gratuito de uso escolar en España |
| Kurso | Ya existe «Kurso: Conversor de divisas» en Google Play |
| Klase | Existe «E-klase» (sistema escolar) y todos los dominios están ocupados |
| Koda | Existe «KODA — Agenda Universitaria» |
| Kodari | Existe la app «Kodari» |
| Kampus | Existen «Kampus Connect» y «Kampus: School Management» |
| Kalmo / Kalma / Kalm | Existen «Kalmos» y «Kalm» (bienestar) y otras con el mismo nombre |
| Kaleo, Kivo, Kuvo | Ya hay apps o marcas con ese nombre (KIVO bienestar, KUVO de Pioneer DJ) |
| Kalen | Todos los dominios importantes ocupados; parecido a Kalena y Kalan |

## 5. Recomendación

1. **Kapunte**: es el único con todo limpio (tienda, web y los cuatro dominios), corto, pronunciable y con sentido directo para quien toma apuntes. No dice «tareas» ni «horario», pero «Kiwu» tampoco lo decía.
2. **Kuaderno** como alternativa si pesa más el significado que el dominio `.com`.
3. **Kuaderia** como tercera opción.

Antes de decidir conviene probar el nombre en voz alta y escrito con la frase «Descarga ___ para tus tareas, notas y horario».

## 6. Pasos antes de cambiar el nombre

1. **Consultar el registro de marcas** del nombre elegido, en las clases 9 (software) y 42 (servicios de software): la Superintendencia de Industria y Comercio de Colombia (SIC), la oficina de EE. UU. (USPTO), la de la UE (EUIPO) y el registro de WIPO. Un abogado de propiedad intelectual puede confirmar en una hora si hay conflicto.
2. **Reservar** los dominios y los nombres de usuario en las redes más usadas, y buscar el nombre en la App Store de Apple.
3. **Elegir el identificador de la app** (por ejemplo `app.kapunte.android`): una vez publicada en Google Play no se puede cambiar.
4. **Cambiar el nombre en una sola pasada**, con el código estable:
   - texto de la interfaz y `app_name` en Android;
   - `applicationId` y paquete de Kotlin (`app.kiwu.android`);
   - carpetas y módulo de Python en Linux, y el nombre del producto en macOS;
   - extensión de los archivos de copia (`.kiwu.json`): aceptar el formato antiguo al importar;
   - README, documentación, iconos y la clave «app: kiwu» del formato de copia, que debe seguir leyéndose;
   - flujos de CI y nombres de los paquetes de descarga.
5. Conservar la compatibilidad de lectura con las copias `.kiwu.json` ya existentes.

Mientras tanto, el nombre en clave del proyecto sigue siendo Kiwu.
