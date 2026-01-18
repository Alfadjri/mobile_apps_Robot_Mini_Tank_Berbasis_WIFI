    package com.alfadjri28.e_witank

    import android.os.Bundle
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.activity.enableEdgeToEdge
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.Surface
    import androidx.compose.ui.Modifier
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.lifecycle.viewmodel.compose.viewModel
    import androidx.navigation.compose.NavHost
    import androidx.navigation.compose.composable
    import androidx.navigation.compose.rememberNavController
    import com.alfadjri28.e_witank.dataset.bbox.DatasetScreen
    import com.alfadjri28.e_witank.screen.SplashScreen
    import com.alfadjri28.e_witank.screens.HomeScreen
    import com.alfadjri28.e_witank.ui.theme.EWiTankTheme
    import com.alfadjri28.e_witank.screen.CameraSearchAndStreamScreen
    import com.alfadjri28.e_witank.developer.CnnPlaygroundScreen
    import com.alfadjri28.e_witank.screen.developer.RthExecutionScreen

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

                            composable(
                                route = "rth/{ip}"
                            ) { backStackEntry ->

                                val ip = backStackEntry.arguments?.getString("ip")!!

                                RthExecutionScreen(
                                    navController = navController,
                                    ip = ip,
                                    controlViewModel = viewModel()
                                )
                            }


//                            hapus nnti
                            composable("dataset/{camID}") { backStack ->
                                DatasetScreen(
                                    camIp = backStack.arguments?.getString("camID")!!
                                )
                            }

                        }
                    }
                }
            }
        }
    }
