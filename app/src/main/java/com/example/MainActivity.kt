package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.TokTikAppNavigation
import com.example.ui.theme.TokTikTheme
import com.example.ui.viewmodel.TokTikViewModel
import com.example.ui.viewmodel.TokTikViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: TokTikViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Instantiate the centralized TokTik ViewModel
        val factory = TokTikViewModelFactory(this)
        viewModel = ViewModelProvider(this, factory)[TokTikViewModel::class.java]

        setContent {
            TokTikTheme(darkTheme = viewModel.isDarkMode.value) {
                // Renders the secure Navigation host containing all 14 screens of TokTik App
                TokTikAppNavigation(viewModel = viewModel)
            }
        }
    }
}
