package com.billarlegends.nfcprototype.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.NfcA

/**
 * Capa NFC — Fase 1: solo diagnóstico. No escribe nada en la tarjeta.
 *
 * Política de seguridad del proyecto:
 * - Solo se prueban claves de fábrica PUBLICADAS por fabricantes de tarjetas en blanco
 *   (no son claves "adivinadas" ni obtenidas por criptoanálisis de Crypto1).
 * - Si ninguna de esas claves abre un sector, se reporta como
 *   "SECTOR NO CONFIGURADO / CLAVE NO DISPONIBLE" y no se intenta nada más.
 * - El sector 0 / bloque 0 nunca se escribe desde esta app, bajo ninguna circunstancia.
 */
object MifareClassicDiagnostics {

    /** Claves de fábrica públicamente documentadas, NO obtenidas por ataque. */
    private val KNOWN_FACTORY_KEYS: List<ByteArray> = listOf(
        MifareClassic.KEY_DEFAULT,                 // FF FF FF FF FF FF
        MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY, // A0 A1 A2 A3 A4 A5 (MAD, publicada por NXP)
        ByteArray(6) { 0x00 }                      // 00 00 00 00 00 00 (usada por algunos fabricantes OEM)
    )

    fun analyze(tag: Tag): DiagnosticResult {
        val uidHex = tag.id.toHex()
        val techList = tag.techList.toList()

        val nfcA = NfcA.get(tag)
        val atqaHex = nfcA?.atqa?.toHex() ?: "N/D"
        val sakHex = nfcA?.sak?.let { "%02X".format(it) } ?: "N/D"

        val mifare = MifareClassic.get(tag)
            ?: return DiagnosticResult(
                uidHex = uidHex,
                atqaHex = atqaHex,
                sakHex = sakHex,
                techList = techList,
                cardType = "No es MIFARE Classic (tecnología no soportada por este prototipo)",
                sizeBytes = null,
                sectorCount = null,
                blockCount = null,
                sectors = emptyList(),
                errorMessage = null
            )

        val cardType = when (mifare.type) {
            MifareClassic.TYPE_CLASSIC -> "MIFARE Classic"
            MifareClassic.TYPE_PLUS -> "MIFARE Plus (compatibilidad limitada)"
            MifareClassic.TYPE_PRO -> "MIFARE Pro (compatibilidad limitada)"
            else -> "MIFARE Classic (variante desconocida)"
        } + " — " + when (mifare.size) {
            MifareClassic.SIZE_1K -> "1K"
            MifareClassic.SIZE_4K -> "4K"
            MifareClassic.SIZE_MINI -> "Mini"
            else -> "${mifare.size} bytes"
        }

        return try {
            mifare.connect()
            val sectorReports = (0 until mifare.sectorCount).map { sector ->
                diagnoseSector(mifare, sector)
            }
            DiagnosticResult(
                uidHex = uidHex,
                atqaHex = atqaHex,
                sakHex = sakHex,
                techList = techList,
                cardType = cardType,
                sizeBytes = mifare.size,
                sectorCount = mifare.sectorCount,
                blockCount = mifare.blockCount,
                sectors = sectorReports,
                errorMessage = null
            )
        } catch (e: Exception) {
            DiagnosticResult(
                uidHex = uidHex,
                atqaHex = atqaHex,
                sakHex = sakHex,
                techList = techList,
                cardType = cardType,
                sizeBytes = mifare.size,
                sectorCount = mifare.sectorCount,
                blockCount = mifare.blockCount,
                sectors = emptyList(),
                errorMessage = "Error al comunicarse con la tarjeta: ${e.message}"
            )
        } finally {
            try {
                mifare.close()
            } catch (_: Exception) {
                // Ignorado: la tarjeta puede haberse retirado durante el cierre.
            }
        }
    }

    private fun diagnoseSector(mifare: MifareClassic, sector: Int): SectorDiagnostic {
        val isManufacturerSector = sector == 0

        var authenticated = false
        var authKeyUsed: String? = null

        for (key in KNOWN_FACTORY_KEYS) {
            try {
                if (mifare.authenticateSectorWithKeyA(sector, key)) {
                    authenticated = true
                    authKeyUsed = key.toHex()
                    break
                }
            } catch (_: Exception) {
                // Autenticación fallida con esta clave; se prueba la siguiente clave conocida.
            }
        }

        if (!authenticated) {
            return SectorDiagnostic(
                sector = sector,
                isManufacturerSector = isManufacturerSector,
                authenticated = false,
                authKeyUsed = null,
                canRead = false,
                canWrite = null,
                statusMessage = "SECTOR NO CONFIGURADO / CLAVE NO DISPONIBLE"
            )
        }

        val firstBlock = mifare.sectorToBlock(sector)
        val rawDataHex = try {
            mifare.readBlock(firstBlock).toHex()
        } catch (_: Exception) {
            null
        }
        val canRead = rawDataHex != null

        return SectorDiagnostic(
            sector = sector,
            isManufacturerSector = isManufacturerSector,
            authenticated = true,
            authKeyUsed = authKeyUsed,
            canRead = canRead,
            canWrite = null, // Nunca se prueba escritura automáticamente. Se evalúa explícitamente y con confirmación en fases posteriores.
            statusMessage = if (canRead) "LECTURA OK" else "AUTENTICADO PERO LECTURA FALLÓ",
            rawDataHex = rawDataHex
        )
    }

    private fun ByteArray.toHex(): String =
        joinToString(separator = " ") { "%02X".format(it) }
}

data class DiagnosticResult(
    val uidHex: String,
    val atqaHex: String,
    val sakHex: String,
    val techList: List<String>,
    val cardType: String,
    val sizeBytes: Int?,
    val sectorCount: Int?,
    val blockCount: Int?,
    val sectors: List<SectorDiagnostic>,
    val errorMessage: String?
)

data class SectorDiagnostic(
    val sector: Int,
    val isManufacturerSector: Boolean,
    val authenticated: Boolean,
    val authKeyUsed: String?,
    val canRead: Boolean,
    val canWrite: Boolean?, // null = no evaluado en esta fase
    val statusMessage: String,
    val rawDataHex: String? = null
)
