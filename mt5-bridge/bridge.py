"""
Puente HTTP local entre MetaTrader 5 (cuenta Exness) y la app "Mi Portafolio".

Expone dos endpoints de solo lectura protegidos con un token fijo:
  GET /account  -> balance, equity, moneda de la cuenta
  GET /history  -> depósitos/retiros y operaciones cerradas (deals)

No coloca, modifica ni cancela ninguna orden: solo lee datos de la terminal MT5
que ya tienes abierta y con sesión iniciada en tu PC/VPS con Windows.

Requisitos (solo funciona en Windows, donde corre la terminal MT5):
    pip install MetaTrader5 fastapi "uvicorn[standard]"

Configuración por variables de entorno:
    BRIDGE_TOKEN   -> token secreto que la app Android debe enviar como
                       "Authorization: Bearer <token>". Genera uno largo y random.
    MT5_LOGIN      -> (opcional) número de cuenta, si no quieres usar la sesión
                       ya abierta en el terminal.
    MT5_PASSWORD   -> (opcional) contraseña, solo si defines MT5_LOGIN.
    MT5_SERVER     -> (opcional) servidor de Exness, solo si defines MT5_LOGIN.

Ejecutar:
    set BRIDGE_TOKEN=un-token-largo-y-aleatorio
    uvicorn bridge:app --host 0.0.0.0 --port 8765

IMPORTANTE - seguridad:
  - Corre esto en una red de confianza o detrás de HTTPS (por ejemplo con un
    túnel como Cloudflare Tunnel o Tailscale) antes de exponerlo a Internet.
    Nunca lo publiques en HTTP plano en una IP pública: el token viajaría
    en claro.
  - El token controla acceso de LECTURA a los datos de tu cuenta. Trátalo
    como una contraseña.
"""

import os
from datetime import datetime, timezone
from typing import List, Optional

import MetaTrader5 as mt5
from fastapi import FastAPI, Header, HTTPException, Query

BRIDGE_TOKEN = os.environ.get("BRIDGE_TOKEN")
MT5_LOGIN = os.environ.get("MT5_LOGIN")
MT5_PASSWORD = os.environ.get("MT5_PASSWORD")
MT5_SERVER = os.environ.get("MT5_SERVER")

if not BRIDGE_TOKEN:
    raise SystemExit("Define la variable de entorno BRIDGE_TOKEN antes de arrancar el puente.")

app = FastAPI(title="Exness MT5 Bridge", description="Puente de solo lectura para Mi Portafolio")

_DEAL_TYPE_NAMES = {
    mt5.DEAL_TYPE_BUY: "BUY",
    mt5.DEAL_TYPE_SELL: "SELL",
    mt5.DEAL_TYPE_BALANCE: "BALANCE",
}


def _check_auth(authorization: Optional[str]) -> None:
    expected = f"Bearer {BRIDGE_TOKEN}"
    if authorization != expected:
        raise HTTPException(status_code=401, detail="Token inválido o ausente")


def _ensure_connected() -> None:
    if mt5.terminal_info() is not None:
        return
    initialized = (
        mt5.initialize(login=int(MT5_LOGIN), password=MT5_PASSWORD, server=MT5_SERVER)
        if MT5_LOGIN
        else mt5.initialize()
    )
    if not initialized:
        raise HTTPException(status_code=503, detail=f"No se pudo conectar a MT5: {mt5.last_error()}")


@app.on_event("shutdown")
def _shutdown() -> None:
    mt5.shutdown()


@app.get("/account")
def get_account(authorization: Optional[str] = Header(default=None)):
    _check_auth(authorization)
    _ensure_connected()

    info = mt5.account_info()
    if info is None:
        raise HTTPException(status_code=503, detail=f"No se pudo leer la cuenta: {mt5.last_error()}")

    return {
        "login": info.login,
        "balance": info.balance,
        "equity": info.equity,
        "currency": info.currency,
        "server": info.server,
    }


@app.get("/history")
def get_history(
    from_: int = Query(default=0, alias="from"),
    authorization: Optional[str] = Header(default=None),
) -> List[dict]:
    _check_auth(authorization)
    _ensure_connected()

    date_from = datetime.fromtimestamp(from_ / 1000, tz=timezone.utc) if from_ else datetime(2000, 1, 1, tzinfo=timezone.utc)
    date_to = datetime.now(timezone.utc)

    deals = mt5.history_deals_get(date_from, date_to)
    if deals is None:
        return []

    result = []
    for deal in deals:
        deal_type = _DEAL_TYPE_NAMES.get(deal.type)
        # Solo nos interesan depósitos/retiros (BALANCE) y operaciones cerradas (BUY/SELL).
        if deal_type is None:
            continue
        result.append(
            {
                "ticket": deal.ticket,
                "timeMillis": int(deal.time * 1000),
                "type": deal_type,
                "volume": deal.volume,
                "symbol": deal.symbol or None,
                "profit": deal.profit,
                "comment": deal.comment or None,
            }
        )
    return result
