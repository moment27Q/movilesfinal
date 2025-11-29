package com.cohaila.proyectomovil

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth

object Destinations {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val TAREAS = "tareas"
    const val REGISTRAR_DEFECTO = "registrar_defecto"
    const val PROGRESO = "progreso"
    const val COMPRAR_TELAS = "comprar_telas"
    const val INVENTARIO_TELAS = "inventario_telas"
}

@Composable
fun AuthApp() {
    val navController = rememberNavController()
    AuthNavGraph(navController)
}

@Composable
fun AuthNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Destinations.LOGIN
    ) {
        // Pantalla de Login
        composable(Destinations.LOGIN) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Destinations.REGISTER)
                },
                onLoginSuccess = {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla de Registro
        composable(Destinations.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.LOGIN) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Dashboard Principal (HOME)
        composable(Destinations.HOME) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid ?: ""
            val userName = currentUser?.displayName
                ?: currentUser?.email?.substringBefore("@")
                ?: "Usuario"

            DashboardPrincipal(
                nombreUsuario = userName,
                userId = userId,
                onNavigateToTareas = {
                    navController.navigate(Destinations.TAREAS)
                },
                onNavigateToRegistrarDefecto = {
                    navController.navigate(Destinations.REGISTRAR_DEFECTO)
                },
                onNavigateToProgreso = {
                    navController.navigate(Destinations.PROGRESO)
                },
                onNavigateToComprarTelas = {
                    navController.navigate(Destinations.COMPRAR_TELAS)
                },
                onNavigateToInventario = {
                    navController.navigate(Destinations.INVENTARIO_TELAS)
                },
                onIniciarTarea = { tareaId ->
                    // Aquí puedes navegar a detalle de tarea
                    // navController.navigate("tarea_detalle/$tareaId")
                },
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Destinations.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla de Comprar Telas
        composable(Destinations.COMPRAR_TELAS) {
            ComprarTelasScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Pantalla de Inventario de Telas
        composable(Destinations.INVENTARIO_TELAS) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid ?: ""

            InventarioTelasScreen(
                userId = userId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Pantalla de Tareas (pendiente de implementar)
        composable(Destinations.TAREAS) {
            // TareasScreen(onNavigateBack = { navController.popBackStack() })
        }

        // Pantalla de Registrar Defecto (pendiente de implementar)
        composable(Destinations.REGISTRAR_DEFECTO) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid ?: ""

            RegistrarDefectoScreen(
                userId = userId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Pantalla de Progreso (pendiente de implementar)
        composable(Destinations.PROGRESO) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid ?: ""

            ProgresoScreen(
                userId = userId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}