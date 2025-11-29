package com.cohaila.proyectomovil

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class ProgresoTarea(
    val id: String = "",
    val numeroOrden: String = "",
    val nombre: String = "",
    val estado: String = "",
    val progreso: Int = 0,
    val metrosCompletados: String = "",
    val metrosTotales: String = "",
    val fechaInicio: Timestamp? = null,
    val fechaCompletado: Timestamp? = null,
    val tiempoTrabajado: String = ""
)

data class EstadisticasProgreso(
    val tareasCompletadas: Int = 0,
    val tareasEnCurso: Int = 0,
    val tareasPendientes: Int = 0,
    val metrosTotalesProducidos: Int = 0,
    val promedioCompletado: Int = 0,
    val eficiencia: String = "0%"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgresoScreen(
    userId: String,
    nombreUsuario: String = "Usuario",
    onNavigateBack: () -> Unit = {}
) {
    var estadisticas by remember { mutableStateOf(EstadisticasProgreso()) }
    var tareasProgreso by remember { mutableStateOf<List<ProgresoTarea>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("TODAS") }
    var showFilterMenu by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))

    val filtros = listOf("TODAS", "COMPLETADAS", "EN_CURSO", "PENDIENTE")

    // Cargar datos desde Firestore
    LaunchedEffect(userId, selectedFilter) {
        try {
            isLoading = true

            // Consulta base
            var query = db.collection("tareas")
                .whereEqualTo("usuarioAsignado", userId)

            // Aplicar filtro
            if (selectedFilter != "TODAS") {
                query = query.whereEqualTo("estado", selectedFilter)
            }

            val tareasSnapshot = query.get().await()

            val tareas = tareasSnapshot.documents.mapNotNull { doc ->
                val metrosCompletados = doc.getString("metrosCompletados") ?: "0"
                val metrosTotales = doc.getString("cantidad") ?: "100"

                val completados = metrosCompletados.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
                val totales = metrosTotales.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 100
                val progreso = if (totales > 0) ((completados.toFloat() / totales) * 100).toInt() else 0

                ProgresoTarea(
                    id = doc.id,
                    numeroOrden = doc.getString("numeroOrden") ?: "",
                    nombre = doc.getString("nombre") ?: "",
                    estado = doc.getString("estado") ?: "PENDIENTE",
                    progreso = progreso,
                    metrosCompletados = metrosCompletados,
                    metrosTotales = metrosTotales,
                    fechaInicio = doc.getTimestamp("fechaInicio"),
                    fechaCompletado = doc.getTimestamp("fechaCompletado"),
                    tiempoTrabajado = doc.getString("tiempoTrabajado") ?: "0h"
                )
            }

            tareasProgreso = tareas

            // Calcular estadísticas
            val completadas = tareas.count { it.estado == "COMPLETADA" }
            val enCurso = tareas.count { it.estado == "EN_CURSO" }
            val pendientes = tareas.count { it.estado == "PENDIENTE" }

            val metrosTotales = tareas
                .filter { it.estado == "COMPLETADA" }
                .sumOf {
                    it.metrosCompletados.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
                }

            val promedioProgreso = if (tareas.isNotEmpty()) {
                tareas.map { it.progreso }.average().toInt()
            } else 0

            val eficiencia = if (tareas.isNotEmpty()) {
                val tareasATiempo = tareas.count { it.estado == "COMPLETADA" }
                ((tareasATiempo.toFloat() / tareas.size) * 100).toInt()
            } else 0

            estadisticas = EstadisticasProgreso(
                tareasCompletadas = completadas,
                tareasEnCurso = enCurso,
                tareasPendientes = pendientes,
                metrosTotalesProducidos = metrosTotales,
                promedioCompletado = promedioProgreso,
                eficiencia = "$eficiencia%"
            )

        } catch (e: Exception) {
            // Manejar error
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
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(TexIAColors.Surface, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF1F2937)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Mi Progreso",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            "Seguimiento de Producción",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        TexIAColors.Purple,
                                        Color(0xFF7C3AED)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Resumen de Estadísticas
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                "Resumen General",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )

                            Spacer(Modifier.height(20.dp))

                            // Grid de estadísticas
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                EstadisticaMiniCard(
                                    modifier = Modifier.weight(1f),
                                    valor = if (isLoading) "-" else estadisticas.tareasCompletadas.toString(),
                                    label = "Completadas",
                                    icon = Icons.Default.CheckCircle,
                                    color = TexIAColors.Secondary
                                )
                                EstadisticaMiniCard(
                                    modifier = Modifier.weight(1f),
                                    valor = if (isLoading) "-" else estadisticas.tareasEnCurso.toString(),
                                    label = "En Curso",
                                    icon = Icons.Default.Build,
                                    color = TexIAColors.Accent
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                EstadisticaMiniCard(
                                    modifier = Modifier.weight(1f),
                                    valor = if (isLoading) "-" else estadisticas.tareasPendientes.toString(),
                                    label = "Pendientes",
                                    icon = Icons.Default.Star,
                                    color = TexIAColors.Purple
                                )
                                EstadisticaMiniCard(
                                    modifier = Modifier.weight(1f),
                                    valor = if (isLoading) "-" else "${estadisticas.metrosTotalesProducidos}m",
                                    label = "Producidos",
                                    icon = Icons.Default.Info,
                                    color = TexIAColors.Primary
                                )
                            }
                        }
                    }
                }

                // Métricas de Rendimiento
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(TexIAColors.Teal.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (isLoading) "-" else "${estadisticas.promedioCompletado}%",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TexIAColors.Teal
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Promedio",
                                    fontSize = 13.sp,
                                    color = Color(0xFF6B7280),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Completado",
                                    fontSize = 13.sp,
                                    color = Color(0xFF6B7280),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(TexIAColors.Secondary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (isLoading) "-" else estadisticas.eficiencia,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TexIAColors.Secondary
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Eficiencia",
                                    fontSize = 13.sp,
                                    color = Color(0xFF6B7280),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "General",
                                    fontSize = 13.sp,
                                    color = Color(0xFF6B7280),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Filtros y lista de tareas
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Órdenes de Producción",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )

                        // Filtro
                        ExposedDropdownMenuBox(
                            expanded = showFilterMenu,
                            onExpandedChange = { showFilterMenu = it }
                        ) {
                            Surface(
                                modifier = Modifier.menuAnchor(),
                                shape = RoundedCornerShape(10.dp),
                                color = TexIAColors.Surface,
                                onClick = { showFilterMenu = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = TexIAColors.Primary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        selectedFilter,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF374151)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = Color(0xFF6B7280)
                                    )
                                }
                            }
                            ExposedDropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false }
                            ) {
                                filtros.forEach { filtro ->
                                    DropdownMenuItem(
                                        text = { Text(filtro) },
                                        onClick = {
                                            selectedFilter = filtro
                                            showFilterMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Lista de tareas
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TexIAColors.Primary)
                        }
                    }
                } else if (tareasProgreso.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = Color(0xFFE5E7EB)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "No hay órdenes",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF6B7280)
                                    )
                                    Text(
                                        "con este filtro",
                                        fontSize = 14.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(tareasProgreso) { tarea ->
                        TareaProgresoCard(
                            tarea = tarea,
                            dateFormat = dateFormat
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EstadisticaMiniCard(
    modifier: Modifier = Modifier,
    valor: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    valor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    label,
                    fontSize = 12.sp,
                    color = color.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun TareaProgresoCard(
    tarea: ProgresoTarea,
    dateFormat: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
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

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (tarea.estado) {
                            "COMPLETADA" -> TexIAColors.Secondary
                            "EN_CURSO" -> TexIAColors.Accent
                            "PENDIENTE" -> TexIAColors.Purple
                            else -> Color(0xFF6B7280)
                        }
                    ) {
                        Text(
                            tarea.estado,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Barra de progreso
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Progreso",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "${tarea.progreso}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                tarea.progreso == 100 -> TexIAColors.Secondary
                                tarea.progreso >= 50 -> TexIAColors.Accent
                                else -> TexIAColors.Purple
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = (tarea.progreso / 100f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = when {
                            tarea.progreso == 100 -> TexIAColors.Secondary
                            tarea.progreso >= 50 -> TexIAColors.Accent
                            else -> TexIAColors.Purple
                        },
                        trackColor = Color(0xFFE5E7EB)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Información adicional
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoChipProgreso(
                        icon = Icons.Default.Build,
                        text = "${tarea.metrosCompletados} / ${tarea.metrosTotales}",
                        color = TexIAColors.Teal
                    )

                    if (tarea.fechaInicio != null) {
                        InfoChipProgreso(
                            icon = Icons.Default.DateRange,
                            text = dateFormat.format(tarea.fechaInicio.toDate()),
                            color = TexIAColors.Purple
                        )
                    }
                }

                if (tarea.estado == "COMPLETADA" && tarea.fechaCompletado != null) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TexIAColors.Secondary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = TexIAColors.Secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Completado el ${dateFormat.format(tarea.fechaCompletado.toDate())}",
                                fontSize = 13.sp,
                                color = TexIAColors.Secondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun InfoChipProgreso(
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
}

@Composable
fun InfoChipProgreso(icon: ImageVector, text: String, color: Color) {
    TODO("Not yet implemented")
}