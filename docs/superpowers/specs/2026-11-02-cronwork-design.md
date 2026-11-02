# CronWork — Diseño (v1)

Fecha: 2026-11-02

## Objetivo

App Android personal para controlar las horas trabajadas. Cada día laborable
muestra una cuenta atrás desde la jornada (7,5 h) y lleva una **bolsa de horas**
para saber, de un vistazo, si se cumplen las 37,5 h semanales.

Criterio de éxito: iniciar/parar en un toque (app, notificación o widget), poder
corregir olvidos, y que el saldo semanal y acumulado sea siempre correcto aunque
Android cierre la app o se reinicie el móvil.

Usuario único, datos solo en el móvil, sin servidor ni cuenta.

## Pantallas

### Hoy
- Cuenta atrás grande: `objetivo del día − trabajado hoy` (formato `H:MM:SS`).
- Al pasar de 0 muestra el exceso: `+0:12 extra`.
- Botón grande **Iniciar / Parar**.
- Ajustes rápidos: `−30`, `−15`, `−5`, `+5`, `+15`, `+30` min y "Otro…" (cantidad
  libre + nota opcional).
- Lista de tramos del día (inicio–fin) y ajustes; cada uno se puede editar o borrar.
- Resumen: saldo de la semana y bolsa acumulada.

### Histórico
- Semana actual (lunes–domingo): por día, trabajado vs objetivo y diferencia.
- Totales: trabajado en la semana vs objetivo semanal, saldo de la semana y bolsa
  acumulada total.
- Navegación a semanas anteriores; tocar un día abre su detalle editable.

### Ajustes
- Horas por día (por defecto 7:30) y días laborables (por defecto L–V).
- Pausa computable: activada/desactivada (por defecto **desactivada**), minutos
  (por defecto 30) y a partir de cuántas horas trabajadas se aplica (por defecto 6 h).
  Si se cumple, se suma ese tiempo como trabajado una vez al día.
- Aviso de olvido: horas seguidas antes de avisar (por defecto 4; 0 = desactivado).
- Días especiales: festivo / vacaciones / otro, con objetivo 0 h o personalizado.

## Reglas de cálculo

- Se guardan **marcas de tiempo** (inicio y fin) de cada tramo; nada se basa en
  un contador en memoria. El tramo en curso tiene `fin = null`.
- `trabajado(día) = Σ duración de tramos que empiezan ese día + Σ ajustes del día
  + pausa computable (si aplica)`.
- `objetivo(día)` = día especial si existe; si no, horas por día en días
  laborables y 0 el resto.
- `saldo(día) = trabajado − objetivo`. Lo trabajado en días con objetivo 0 suma
  íntegro a la bolsa.
- Solo cuentan días **cerrados** (anteriores a hoy) más el día de hoy en curso; los
  días futuros no restan.
- `saldo semana = Σ saldo(día)` lunes–domingo. `bolsa = Σ saldo(día)` desde el
  primer día con registros.
- Tramo que cruza medianoche: pertenece entero al día en que empezó. Al detectarlo
  se muestra un aviso para revisarlo.
- Un ajuste negativo nunca puede dejar el trabajado de un día por debajo de 0
  (se limita y se avisa).

## Avisos y segundo plano

- **Notificación fija** mientras el contador está en marcha, con cuenta atrás nativa
  (`Chronometer` en modo cuenta atrás) y acción **Parar**. No requiere servicio en
  primer plano porque el tiempo lo cuenta el sistema.
- **Fin de jornada**: alarma programada con `AlarmManager` para el momento en que
  el contador llega a 0. Se reprograma al iniciar/parar/ajustar.
- **Aviso de olvido**: alarma a las N horas seguidas. Acciones: "Sigo trabajando"
  (reprograma) y "Paré hace…" (abre la app para fijar la hora de fin).
- Tras reiniciar el móvil (`BOOT_COMPLETED`) se restauran notificación y alarmas.
- Si no hay permiso de alarmas exactas, se usan alarmas inexactas y se indica en
  Ajustes. Se pide permiso de notificaciones (Android 13+).

## Widget (pantalla de inicio)

- Tamaño 2×1 (redimensionable). Muestra el tiempo restante del día o el exceso, y un
  botón Iniciar/Parar.
- En marcha usa `Chronometer` de `RemoteViews` en cuenta atrás, así se actualiza
  cada segundo sin gastar batería. Parado muestra el valor fijo.
- Se actualiza al iniciar/parar/ajustar, al cambiar de día y tras reiniciar.
- Tocar fuera del botón abre la app.

## Arquitectura

- Kotlin, Jetpack Compose (Material 3), minSdk 26, targetSdk actual.
- Un solo módulo `app` con paquetes:
  - `domain`: lógica pura de cálculo (sin Android) → fácil de probar.
  - `data`: Room (`WorkSession`, `Adjustment`, `SpecialDay`) y DataStore (ajustes).
  - `timer`: casos de uso iniciar/parar/ajustar que actualizan datos, notificación,
    alarmas y widget en un único sitio (`TimerController`).
  - `notify`: notificación, alarmas, receptores (acciones, boot).
  - `widget`: `AppWidgetProvider` con `RemoteViews`.
  - `ui`: pantallas Hoy, Histórico, Ajustes con ViewModels.
- Inyección de dependencias manual (contenedor simple), sin Hilt.

### Modelo de datos
- `WorkSession(id, start: Instant, end: Instant?)`
- `Adjustment(id, date: LocalDate, minutes: Int, note: String?, createdAt)`
- `SpecialDay(date: LocalDate, type, targetMinutes: Int)`
- Ajustes (DataStore): minutos por día, días laborables, pausa (on, minutos,
  umbral), horas aviso olvido.

## Compilación y distribución

- GitHub Actions: en cada push a la rama se ejecutan pruebas y se compila el APK
  de release.
- El APK se publica en **GitHub Releases** (etiqueta con número de build) para
  descargarlo desde el móvil e instalarlo.
- Firma siempre con la **misma clave** para que las actualizaciones se instalen
  encima sin perder datos. La clave va en el repositorio (excepción en
  `.gitignore`) por simplicidad; es una app personal. Alternativa más segura si se
  prefiere: guardarla como *secret* de GitHub.

## Pruebas

- Pruebas unitarias del paquete `domain`: saldo diario, semanal y bolsa; días
  especiales; fines de semana; pausa computable; tramo en curso; tramo que cruza
  medianoche; límites de ajustes negativos.
- Pruebas de los DAO de Room (si el entorno lo permite) y compilación del APK en CI.

## Fuera de la v1
- Exportar a CSV.
- Botón rápido en el panel de notificaciones (Quick Settings tile).
