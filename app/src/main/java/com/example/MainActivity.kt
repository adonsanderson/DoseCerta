package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.dosecerta.alarm.AlarmConstants
import com.example.dosecerta.presentation.navigation.DoseCertaNavHost
import com.example.dosecerta.presentation.theme.DoseCertaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialTreatmentId = intent
            ?.getLongExtra(AlarmConstants.EXTRA_TREATMENT_ID, -1L)
            ?.takeIf { it > 0L }

        setContent {
            DoseCertaTheme {
                DoseCertaNavHost(initialTreatmentId = initialTreatmentId)
            }
        }
    }
}
