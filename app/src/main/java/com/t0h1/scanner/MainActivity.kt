package com.t0h1.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.t0h1.scanner.ui.T0H1ScannerApp
import com.t0h1.scanner.ui.theme.T0H1ScannerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ScanViewModel by lazy {
        ViewModelProvider(this, ScanViewModelFactory(application))[ScanViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            T0H1ScannerTheme {
                T0H1ScannerApp(viewModel = viewModel)
            }
        }
    }
}
