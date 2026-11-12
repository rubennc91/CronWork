# CronWork

App Android para llevar las horas trabajadas: cuenta atrás diaria desde tu jornada
(7:30 por defecto, de lunes a viernes) y **bolsa de horas** para saber si cumples
las 37,5 h semanales.

## Qué hace

- **Hoy**: cuenta atrás grande, botón Iniciar/Parar, ajustes rápidos (±5, ±15, ±30 min
  u otra cantidad con nota) y lista de tramos editable.
- **Histórico**: semana a semana, trabajado frente a objetivo, saldo de la semana y
  bolsa acumulada. Toca un día para corregirlo. "Exportar CSV" comparte todo el histórico
  en un archivo que Excel abre directamente.
- **Ajustes**: horas por día, días laborables, pausa computable (desactivada por
  defecto), aviso de "¿sigues trabajando?" y festivos/vacaciones.
- **Notificación** con la cuenta atrás y botón Parar mientras el contador está en marcha.
- **Avisos** al terminar la jornada y si el contador lleva demasiadas horas seguidas.
- **Widget** 2×1 para la pantalla de inicio con el tiempo restante y botón Iniciar/Parar.
- **Botón rápido** en el panel de ajustes rápidos para iniciar/parar sin abrir la app.

Los datos se guardan solo en el móvil.

## Instalar

1. En el móvil, abre la página de
   [Releases](https://github.com/rubennc91/CronWork/releases/latest) del repositorio.
2. Descarga el archivo `CronWork-N.apk`.
3. Ábrelo. La primera vez Android pedirá permitir instalar apps de este origen
   (el navegador o la app de Archivos): acéptalo.
4. Al abrir CronWork, permite las notificaciones.
5. Para el widget: mantén pulsado el fondo de la pantalla de inicio → Widgets → CronWork.
6. Para el botón rápido: despliega el panel de ajustes rápidos → lápiz (editar) → arrastra
   "CronWork" a los botones activos.

Para actualizar, descarga el APK más reciente e instálalo encima: se conservan los datos.

## Desarrollo

- `core/`: cálculo de horas en Kotlin puro, con pruebas (`./gradlew :core:test`).
  Sin SDK de Android: `./gradlew :core:test -PcoreOnly`.
- `app/`: app Android (Jetpack Compose, Room, DataStore).
- Cada push compila y publica el APK en Releases (`.github/workflows/build.yml`).
- La clave de firma (`app/release.keystore`) está en el repositorio a propósito para
  que cada versión se instale encima de la anterior. Si el repositorio es público,
  cualquiera podría firmar un APK como si fuera CronWork; para una app personal es
  un riesgo asumible, pero puede moverse a un *secret* de GitHub.
