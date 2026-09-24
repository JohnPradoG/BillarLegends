# Mi Portafolio (módulo `:portfolio`)

App Android (Kotlin + Jetpack Compose) que monitorea tus cuentas de trading y lleva
el registro de tus negocios no digitales:

- **Binance**: conecta una o varias cuentas por API key de **solo lectura**. Sincroniza
  balances, depósitos, retiros y operaciones, y calcula cuánto has invertido, el valor
  actual y la ganancia/pérdida.
- **Exness**: Exness no tiene API pública para cuentas retail, así que se conecta a
  través de un puente local que corre sobre tu terminal MetaTrader 5 (ver
  [`../mt5-bridge/README.md`](../mt5-bridge/README.md)). También solo lectura.
- **Negocios no digitales**: los registras y actualizas tú a mano (capital aportado,
  retiros, ingresos, gastos).

Todo se combina en un dashboard único con el total invertido, el valor total y la
ganancia consolidada, más un historial unificado de movimientos.

## Cómo abrir el proyecto

Este módulo se agregó al proyecto Gradle existente `BillarLegends`. Ábrelo completo en
Android Studio (Iguana o más reciente) y selecciona la configuración de ejecución
`portfolio` para instalarlo como una segunda app independiente del módulo `app`
(NFC del billar), sin que se toquen entre sí.

> **Nota sobre este entorno**: el sandbox donde se generó este código no tiene salida
> de red hacia `dl.google.com`/Maven Central, así que no se pudo ejecutar
> `./gradlew build` aquí para verificar la compilación. El código se escribió y se
> revisó a mano con cuidado, pero la primera compilación real debe hacerse en Android
> Studio (o CI) antes de confiar en él para producción.

## Seguridad

- Las API keys de Binance y el token del puente de Exness se guardan cifrados en el
  dispositivo con `EncryptedSharedPreferences` respaldado por el Android Keystore.
  Nunca se guardan en la base de datos Room ni en logs.
- Al agregar una cuenta de Binance, la app llama a
  `GET /sapi/v1/account/apiRestrictions` y **rechaza la key si tiene el permiso de
  retiro (`enableWithdrawals`) activado**. Crea siempre una key nueva en Binance con
  únicamente `Enable Reading`.
- `android:allowBackup="false"`: los datos no se incluyen en copias de seguridad
  automáticas de Android.

### Cómo crear una API key de solo lectura en Binance

1. Entra a Binance → perfil → **API Management**.
2. Crea una key nueva, dale un nombre (ej. "Mi Portafolio - solo lectura").
3. En los permisos, deja **solo** activado "Enable Reading" (o "Habilitar lectura").
   Asegúrate de que "Enable Withdrawals" y "Enable Spot & Margin Trading" queden
   **desactivados**.
4. Si Binance te pide restringir por IP, puedes dejarlo sin restricción ya que las
   peticiones salen desde tu celular con IP variable; si prefieres más seguridad,
   Binance permite pegar rangos de IP conocidos.
5. Copia el API Key y el API Secret y pégalos en la app al agregar la cuenta.

## Cómo se calcula invertido / valor / ganancia

Para Binance y Exness se usa el mismo criterio (money-weighted, simple y robusto):

- **Invertido** = suma de depósitos − suma de retiros (convertidos a USD).
- **Valor actual** = valor de mercado de todo lo que tienes ahora en la cuenta
  (balances de Binance valuados a precio actual; equity de la cuenta MT5).
- **Ganancia** = Valor actual − Invertido.

Para negocios manuales:

- **Invertido** = capital que aportaste − capital que retiraste.
- **Ganancia operativa** = ingresos registrados − gastos registrados.
- El "valor" mostrado en el resumen combinado = invertido + ganancia operativa
  (equivale a tu capital contable en ese negocio).

## Limitaciones conocidas (MVP)

- **Valuación de depósitos/retiros en cripto**: se usa el precio ACTUAL del activo, no
  el precio histórico del día de la transacción. Para stablecoins (USDT, USDC, etc.)
  es exacto 1:1; para otros activos es una aproximación razonable pero no exacta.
- **Historial de trades de Binance**: solo se descarga para los activos que tienes
  actualmente en balance contra su par en USDT. Una posición que compraste y vendiste
  por completo no aparecerá en el historial de "operaciones" (sí impactó tu balance
  histórico igual, solo que no se reconstruye el detalle de esa operación puntual).
- **Exness/MT5**: depende de que tengas el puente corriendo (ver
  `mt5-bridge/README.md`). Si la PC/VPS está apagada, la sincronización de esa cuenta
  simplemente falla con un error visible en la app; nada se pierde, se reintenta la
  próxima vez que sincronices.
- Sin backend propio: toda la lógica corre en el teléfono. Eso simplifica el
  despliegue, pero implica que los cálculos y la base de datos viven únicamente en tu
  dispositivo (no hay backup en la nube automático).

## Estructura del código

```
portfolio/src/main/java/com/billarlegends/portfolio/
├── data/
│   ├── local/          Room (entidades, DAOs, AppDatabase)
│   ├── remote/binance/  Retrofit + firma HMAC de Binance
│   ├── remote/exness/    Retrofit hacia el puente MT5
│   └── repository/      Lógica de sincronización y cálculo
├── domain/               Modelos de dominio (Source, MovementType, PortfolioSummary)
├── security/             SecureCredentialStore (Android Keystore)
└── ui/                   Pantallas Compose (dashboard, cuentas, negocios) + navegación
```
