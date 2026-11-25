package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.dosecerta.alarm.AlarmConstants
import com.example.dosecerta.presentation.navigation.DoseCertaNavHost
import com.example.dosecerta.presentation.theme.DoseCertaTheme

class MainActivity : ComponentActivity() {

    private var deepLinkTreatmentId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkTreatmentId = extractTreatmentId(intent)

        setContent {
            DoseCertaTheme {
                DoseCertaNavHost(initialTreatmentId = deepLinkTreatmentId)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkTreatmentId = extractTreatmentId(intent)
    }

    private fun extractTreatmentId(intent: Intent?): Long? =
        intent?.getLongExtra(AlarmConstants.EXTRA_TREATMENT_ID, -1L)
            ?.takeIf { it > 0L }
}
