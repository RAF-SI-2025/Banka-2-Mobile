package com.example.banka_2_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import com.example.banka_2_mobile.data.api.RetrofitClient
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.navigation.NavGraph
import com.example.banka_2_mobile.ui.theme.Banka2MobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize RetrofitClient with AuthRepository for token management
        val authRepository = AuthRepository(applicationContext)
        RetrofitClient.init(authRepository)

        // Dark system bars matching app theme
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.parseColor("#070B24")
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.parseColor("#070B24")

        // Light icons on dark background (white battery, wifi icons)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                0, // clear LIGHT flags = use light (white) icons
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility and
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() and
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        }
        setContent {
            Banka2MobileTheme(darkTheme = true) {
                NavGraph()
            }
        }
    }
}
