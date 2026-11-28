package com.example.miniproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.miniproject.ui.theme.MiniProjectTheme
import com.example.miniproject.ui.theme.LocalThemeController
import com.example.miniproject.ui.theme.ThemeController
import com.example.miniproject.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences = remember {
                getSharedPreferences("monee_appearance", MODE_PRIVATE)
            }
            var themeMode by remember {
                mutableStateOf(
                    runCatching {
                            ThemeMode.valueOf(
                                preferences.getString("theme_mode", ThemeMode.SYSTEM.name)!!
                            )
                        }
                        .getOrDefault(ThemeMode.SYSTEM)
                )
            }
            val updateTheme: (ThemeMode) -> Unit = { selected ->
                themeMode = selected
                preferences.edit().putString("theme_mode", selected.name).apply()
            }

            MiniProjectTheme(themeMode = themeMode) {
                CompositionLocalProvider(
                    LocalThemeController provides ThemeController(themeMode, updateTheme)
                ) {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    supabaseClient = SupabaseClient.instance,
                    modifier = Modifier.fillMaxSize()
                )
                }
            }
        }
    }
}
