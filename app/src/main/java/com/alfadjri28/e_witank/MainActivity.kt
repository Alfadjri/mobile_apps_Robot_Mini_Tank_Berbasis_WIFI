    package com.alfadjri28.e_witank

    import android.os.Bundle
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.activity.enableEdgeToEdge
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.Surface
    import androidx.compose.ui.Modifier
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.navigation.compose.NavHost
    import androidx.navigation.compose.composable
    import androidx.navigation.compose.rememberNavController
    import com.alfadjri28.e_witank.screen.SplashScreen
    import com.alfadjri28.e_witank.screens.HomeScreen
    import com.alfadjri28.e_witank.ui.theme.EWiTankTheme
    import com.alfadjri28.e_witank.screen.CameraSearchAndStreamScreen

    class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            enableEdgeToEdge()
            setContent {
                EWiTankTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()

                        NavHost(navController = navController, startDestination = "splash_guide") {
                            composable("splash_guide") {
                                SplashScreen(
                                    onGuideFinished = {
                                        navController.navigate("home") {
                                            popUpTo("splash_guide") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("home") {
                                HomeScreen(navController = navController)
                            }

                            composable("device_list") {
                                DeviceListScreen(navController = navController)
                            }

                            composable("camera_search/{ip}/{camID}") { backStackEntry ->
                                val ip = backStackEntry.arguments?.getString("ip") ?: ""
                                val camID = backStackEntry.arguments?.getString("camID") ?: ""
                                CameraSearchAndStreamScreen(navController = navController, ip = ip,
                                    camID = camID
                                )
                            }
                        }
                    }
                }
            }
        }
    }
