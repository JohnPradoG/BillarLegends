# Puente Exness (MetaTrader 5) → Mi Portafolio

Exness no ofrece una API pública para que una app externa consulte tu cuenta de
trading directamente. Este puente resuelve eso: es un pequeño servidor que corre
**en la misma PC/VPS con Windows donde ya tienes abierta tu terminal MetaTrader 5**,
lee tus datos localmente y se los expone a la app Android de forma segura.

El puente es **solo lectura**: no envía, modifica ni cancela órdenes. Únicamente
lee balance, equity e historial de depósitos/retiros/operaciones cerradas.

## Requisitos

- Windows (el paquete `MetaTrader5` de Python solo funciona ahí).
- Terminal MetaTrader 5 de Exness instalada, con tu cuenta con sesión iniciada.
- Python 3.9+.

## Instalación

```bash
cd mt5-bridge
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
```

## Configuración

Define un token secreto (largo y aleatorio) que la app Android usará para
autenticarse. En PowerShell:

```powershell
$env:BRIDGE_TOKEN = "genera-aqui-un-token-largo-y-aleatorio"
```

Si prefieres que el puente inicie sesión en MT5 por su cuenta en vez de reusar
la sesión ya abierta en la terminal, define también:

```powershell
$env:MT5_LOGIN = "12345678"
$env:MT5_PASSWORD = "tu-password"
$env:MT5_SERVER = "Exness-MT5Real"
```

## Ejecutar

```bash
uvicorn bridge:app --host 0.0.0.0 --port 8765
```

Prueba local:

```bash
curl -H "Authorization: Bearer TU_TOKEN" http://localhost:8765/account
```

## Exponerlo a tu teléfono con seguridad

**Nunca lo publiques directo en HTTP sobre una IP pública**: el token viajaría
sin cifrar. Opciones recomendadas, de más simple a más robusta:

1. **Misma red Wi-Fi**: usa la IP local de la PC (ej. `http://192.168.1.50:8765`)
   directo desde el celular conectado al mismo router. Suficiente si solo vas
   a sincronizar estando en casa/oficina.
2. **Tailscale** (recomendado): crea una VPN privada gratuita entre tu PC y tu
   celular; en la app usarías la IP de Tailscale de la PC, con tráfico cifrado
   de punta a punta sin abrir puertos en tu router.
3. **Cloudflare Tunnel**: expone el puente con HTTPS y un dominio propio sin
   abrir puertos, si necesitas acceso desde cualquier red.

En cualquier caso, configura en la app Android la URL base (ej.
`http://192.168.1.50:8765/` o la que te dé Tailscale/Cloudflare) y el mismo
`BRIDGE_TOKEN` que definiste arriba.

## Dejarlo corriendo siempre

Para que la sincronización funcione aunque no estés frente a la PC, el puente
(y la terminal MT5) deben quedar corriendo 24/7. Opciones:

- Dejar la PC encendida con el script corriendo en segundo plano
  (`pythonw` o una tarea programada de Windows).
- Un VPS Windows barato con la terminal MT5 y este script instalados.

## Endpoints

| Endpoint | Descripción |
|---|---|
| `GET /account` | `{ login, balance, equity, currency, server }` |
| `GET /history?from=<epoch_ms>` | Lista de depósitos/retiros (`type: "BALANCE"`) y operaciones cerradas (`type: "BUY"/"SELL"`, con `profit` ya realizado) |

Todas las peticiones requieren el header `Authorization: Bearer <BRIDGE_TOKEN>`.
