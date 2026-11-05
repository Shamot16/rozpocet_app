package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BudgetViewModel
import com.example.ui.MainApp
import com.example.ui.theme.RodinnyRozpocetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RodinnyRozpocetTheme {
                val budgetViewModel: BudgetViewModel = viewModel()
                MainApp(viewModel = budgetViewModel)
            }
        }
    }
}
