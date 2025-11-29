package com.cohaila.proyectomovil

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Colores personalizados para industria textil
object TexIAColors {
    val Primary = Color(0xFF6366F1) // Índigo
    val PrimaryDark = Color(0xFF4F46E5)
    val Secondary = Color(0xFF10B981) // Verde esmeralda
    val Accent = Color(0xFFF59E0B) // Ámbar
    val Error = Color(0xFFEF4444)
    val Surface = Color(0xFFF8FAFC)
    val Purple = Color(0xFF8B5CF6)
    val Teal = Color(0xFF14B8A6)
}

// Modelos de datos
data class TareaResumen(
    val id: String = "",
    val numeroOrden: String = "",
    val nombre: String = "",
    val tipoTela: String = "",
    val cantidad: String = "",
    val tiempoRestante: String = "",
    val estado: String = "PENDIENTE",
    val prioridad: String = "MEDIA",
    val fechaCreacion: com.google.firebase.Timestamp? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardPrincipal(
    nombreUsuario: String = "Usuario",
    userId: String,
    onNavigateToTareas: () -> Unit = {},
    onNavigateToRegistrarDefecto: () -> Unit = {},
    onNavigateToProgreso: () -> Unit = {},
    onNavigateToComprarTelas: () -> Unit = {},
    onNavigateToInventario: () -> Unit = {},
    onIniciarTarea: (String) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var tareasHoy by remember { mutableStateOf<List<TareaResumen>>(emptyList()) }
    var tareasActivas by remember { mutableStateOf(0) }
    var tareasPendientes by remember { mutableStateOf(0) }
    var metrosProducidos by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val db = FirebaseFirestore.getInstance()

    // Cargar datos desde Firestore
    LaunchedEffect(userId) {
        try {
            isLoading = true
            errorMessage = null

            val tareasSnapshot = db.collection("tareas")
                .whereEqualTo("usuarioAsignado", userId)
                .limit(10)
                .get()
                .await()

            val tareas = tareasSnapshot.documents.mapNotNull { doc ->
                TareaResumen(
                    id = doc.id,
                    numeroOrden = doc.getString("numeroOrden") ?: "",
                    nombre = doc.getString("nombre") ?: "",
                    tipoTela = doc.getString("tipoTela") ?: "Sin especificar",
                    cantidad = doc.getString("cantidad") ?: "0 mts",
                    tiempoRestante = doc.getString("tiempoRestante") ?: "Sin tiempo",
                    estado = doc.getString("estado") ?: "PENDIENTE",
                    prioridad = doc.getString("prioridad") ?: "MEDIA",
                    fechaCreacion = doc.getTimestamp("fechaCreacion")
                )
            }

            tareasHoy = tareas
            tareasActivas = tareas.count { it.estado == "EN_CURSO" }
            tareasPendientes = tareas.count { it.estado == "PENDIENTE" }

            // Calcular metros producidos hoy (ejemplo)
            metrosProducidos = tareas
                .filter { it.estado == "COMPLETADA" }
                .sumOf { it.cantidad.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0 }

        } catch (e: Exception) {
            errorMessage = "Error al cargar datos: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TexIAColors.Surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header mejorado
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            TexIAColors.Primary,
                                            TexIAColors.PrimaryDark
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                nombreUsuario.firstOrNull()?.toString() ?: "U",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "¡Hola, $nombreUsuario!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                "Sistema de Gestión Textil",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showMenu = !showMenu },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                TexIAColors.Surface,
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menú",
                            tint = TexIAColors.Primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Error message
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = TexIAColors.Error.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = TexIAColors.Error
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            errorMessage ?: "",
                            color = TexIAColors.Error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Estadísticas mejoradas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EstadisticaCard(
                    modifier = Modifier.weight(1f),
                    numero = if (isLoading) "-" else tareasActivas.toString(),
                    label = "Órdenes\nActivas",
                    icon = Icons.Default.Build,
                    gradientColors = listOf(TexIAColors.Secondary, TexIAColors.Teal)
                )

                EstadisticaCard(
                    modifier = Modifier.weight(1f),
                    numero = if (isLoading) "-" else tareasPendientes.toString(),
                    label = "Órdenes\nPendientes",
                    icon = Icons.Default.Star,
                    gradientColors = listOf(TexIAColors.Accent, Color(0xFFF97316))
                )
            }

            Spacer(Modifier.height(12.dp))

            // Métrica adicional: Producción del día
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(TexIAColors.Purple.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = TexIAColors.Purple,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "Producción Hoy",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B7280)
                            )
                            Text(
                                "$metrosProducidos metros",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                        }
                    }
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFD1D5DB)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Lista de órdenes de producción
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Órdenes de Producción",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                "Tus tareas asignadas",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                        }
                        TextButton(onClick = onNavigateToTareas) {
                            Text(
                                "Ver todas",
                                color = TexIAColors.Primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TexIAColors.Primary)
                        }
                    } else if (tareasHoy.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    tint = Color(0xFFE5E7EB)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No hay órdenes asignadas",
                                    color = Color(0xFF6B7280),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Disfruta tu día",
                                    color = Color(0xFF9CA3AF),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(tareasHoy) { tarea ->
                                OrdenProduccionItem(
                                    tarea = tarea,
                                    onIniciar = { onIniciarTarea(tarea.id) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Botones de acción rápida mejorados
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primera fila
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AccionRapidaButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ShoppingCart,
                        label = "Comprar\nTelas",
                        gradientColors = listOf(TexIAColors.Primary, TexIAColors.PrimaryDark),
                        onClick = onNavigateToComprarTelas
                    )

                    AccionRapidaButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.List,
                        label = "Ver\nInventario",
                        gradientColors = listOf(TexIAColors.Teal, TexIAColors.Secondary),
                        onClick = onNavigateToInventario
                    )
                }

                // Segunda fila
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AccionRapidaButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Warning,
                        label = "Registrar\nDefecto",
                        gradientColors = listOf(TexIAColors.Error, Color(0xFFDC2626)),
                        onClick = onNavigateToRegistrarDefecto
                    )

                    AccionRapidaButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.DateRange,
                        label = "Ver\nProgreso",
                        gradientColors = listOf(TexIAColors.Purple, Color(0xFF7C3AED)),
                        onClick = onNavigateToProgreso
                    )
                }
            }
        }

        // Menú lateral mejorado
        if (showMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showMenu = false }
            )

            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Header del menú
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "TexIA",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = TexIAColors.Primary
                            )
                            Text(
                                "Gestión Textil",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                        }
                        IconButton(onClick = { showMenu = false }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color(0xFF6B7280)
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // Opciones del menú
                    MenuOption(
                        icon = Icons.Default.Home,
                        label = "Inicio",
                        onClick = { showMenu = false }
                    )
                    MenuOption(
                        icon = Icons.Default.List,
                        label = "Mis Órdenes",
                        onClick = {
                            showMenu = false
                            onNavigateToTareas()
                        }
                    )
                    MenuOption(
                        icon = Icons.Default.ShoppingCart,
                        label = "Comprar Telas",
                        onClick = {
                            showMenu = false
                            onNavigateToComprarTelas()
                        }
                    )
                    MenuOption(
                        icon = Icons.Default.List,
                        label = "Inventario de Telas",
                        onClick = {
                            showMenu = false
                            onNavigateToInventario()
                        }
                    )
                    MenuOption(
                        icon = Icons.Default.Warning,
                        label = "Registrar Defecto",
                        onClick = {
                            showMenu = false
                            onNavigateToRegistrarDefecto()
                        }
                    )
                    MenuOption(
                        icon = Icons.Default.DateRange,
                        label = "Ver Progreso",
                        onClick = {
                            showMenu = false
                            onNavigateToProgreso()
                        }
                    )

                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    MenuOption(
                        icon = Icons.Default.Person,
                        label = "Mi Perfil",
                        onClick = { showMenu = false }
                    )
                    MenuOption(
                        icon = Icons.Default.Settings,
                        label = "Configuración",
                        onClick = { showMenu = false }
                    )

                    Spacer(Modifier.weight(1f))

                    Divider()
                    Spacer(Modifier.height(16.dp))

                    MenuOption(
                        icon = Icons.Default.ExitToApp,
                        label = "Cerrar Sesión",
                        onClick = {
                            showMenu = false
                            onLogout()
                        },
                        color = TexIAColors.Error
                    )
                }
            }
        }
    }
}

@Composable
fun EstadisticaCard(
    modifier: Modifier = Modifier,
    numero: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>
) {
    Card(
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(colors = gradientColors)
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        numero,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        label,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.95f),
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun OrdenProduccionItem(
    tarea: TareaResumen,
    onIniciar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = TexIAColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tarea.numeroOrden,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TexIAColors.Primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tarea.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }

                // Badge de prioridad
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (tarea.prioridad) {
                        "URGENTE" -> TexIAColors.Error
                        "ALTA" -> TexIAColors.Accent
                        "MEDIA" -> TexIAColors.Primary
                        "BAJA" -> TexIAColors.Secondary
                        else -> Color(0xFF6B7280)
                    }
                ) {
                    Text(
                        tarea.prioridad,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Información de la tela
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(
                    icon = Icons.Default.Info,
                    text = tarea.tipoTela,
                    color = TexIAColors.Teal
                )
                InfoChip(
                    icon = Icons.Default.Build,
                    text = tarea.cantidad,
                    color = TexIAColors.Purple
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = TexIAColors.Accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    tarea.tiempoRestante,
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onIniciar,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (tarea.estado == "EN_CURSO")
                        TexIAColors.Secondary
                    else
                        TexIAColors.Primary
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(
                    if (tarea.estado == "EN_CURSO") Icons.Default.PlayArrow else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (tarea.estado == "EN_CURSO") "Continuar Producción" else "Iniciar Producción",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text,
                fontSize = 12.sp,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun AccionRapidaButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(colors = gradientColors))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun MenuOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color = Color(0xFF1F2937)
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}