package com.billarlegends.nfcprototype

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import com.billarlegends.nfcprototype.nfc.DiagnosticResult
import com.billarlegends.nfcprototype.nfc.MifareClassicDiagnostics
import com.billarlegends.nfcprototype.ui.DiagnosticScreen
import com.billarlegends.nfcprototype.ui.NfcAvailability

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null

    private var availabilityState by mutableStateOf(NfcAvailability.NOT_SUPPORTED)
    private var resultState by mutableStateOf<DiagnosticResult?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            MaterialTheme {
                DiagnosticScreen(
                    availability = availabilityState,
                    result = resultState
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val adapter = nfcAdapter

        availabilityState = when {
            adapter == null -> NfcAvailability.NOT_SUPPORTED
            !adapter.isEnabled -> NfcAvailability.DISABLED
            else -> NfcAvailability.READY
        }

        if (adapter != null && adapter.isEnabled) {
            // FLAG_READER_SKIP_NDEF_CHECK evita que el sistema intente leer NDEF antes de
            // entregarnos la tag; nosotros manejamos MIFARE Classic directamente.
            val flags = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
            adapter.enableReaderMode(this, this, flags, null)
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    /**
     * Se ejecuta en un hilo secundario proporcionado por el sistema NFC.
     * Se resuelve el diagnóstico completo aquí, y solo el resultado final
     * se publica al estado de Compose (que sí es seguro actualizar desde
     * cualquier hilo con mutableStateOf).
     */
    override fun onTagDiscovered(tag: Tag) {
        resultState = MifareClassicDiagnostics.analyze(tag)
    }
}
