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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

data class Defecto(
    val id: String = "",
    val numeroOrden: String = "",
    val tipoDefecto: String = "",
    val descripcion: String = "",
    val gravedad: String = "MEDIA",
    val metrosAfectados: String = "",
    val fecha: Timestamp? = null,
    val usuarioReporte: String = "",
    val estado: String = "REPORTADO"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarDefectoScreen(
    userId: String,
    nombreUsuario: String = "Usuario",
    onNavigateBack: () -> Unit = {}
) {
    var numeroOrden by remember { mutableStateOf("") }
    var tipoDefecto by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var gravedad by remember { mutableStateOf("MEDIA") }
    var metrosAfectados by remember { mutableStateOf("") }
    var showGravedadMenu by remember { mutableStateOf(false) }
    var showTipoDefectoMenu by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var defectosRecientes by remember { mutableStateOf<List<Defecto>>(emptyList()) }
    var isLoadingDefectos by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()

    val tiposDefecto = listOf(
        "Mancha en tela",
        "Rotura de hilos",
        "Defecto de tejido",
        "Color irregular",
        "Ancho incorrecto",
        "Encogimiento",
        "Defecto de tinte",
        "Agujeros",
        "Otro"
    )

    val nivelesGravedad = listOf(
        "BAJA" to TexIAColors.Secondary,
        "MEDIA" to TexIAColors.Accent,
        "ALTA" to Color(0xFFFF6B6B),
        "CRÍTICA" to TexIAColors.Error
    )

    // Cargar defectos recientes
    LaunchedEffect(userId) {
        isLoadingDefectos = true
        try {
            val snapshot = db.collection("defectos")
                .whereEqualTo("usuarioReporte", userId)
                .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .await()

            defectosRecientes = snapshot.documents.mapNotNull { doc ->
                Defecto(
                    id = doc.id,
                    numeroOrden = doc.getString("numeroOrden") ?: "",
                    tipoDefecto = doc.getString("tipoDefecto") ?: "",
                    descripcion = doc.getString("descripcion") ?: "",
                    gravedad = doc.getString("gravedad") ?: "MEDIA",
                    metrosAfectados = doc.getString("metrosAfectados") ?: "",
                    fecha = doc.getTimestamp("fecha"),
                    usuarioReporte = doc.getString("usuarioReporte") ?: "",
                    estado = doc.getString("estado") ?: "REPORTADO"
                )
            }
        } catch (e: Exception) {
            // Manejar error silenciosamente
        } finally {
            isLoadingDefectos = false
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
                            "Registrar Defecto",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            "Control de Calidad",
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
                                        TexIAColors.Error,
                                        Color(0xFFDC2626)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
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
                // Formulario de registro
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
                                "Información del Defecto",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                            Spacer(Modifier.height(20.dp))

                            // Número de Orden
                            Text(
                                "Número de Orden *",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF374151)
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = numeroOrden,
                                onValueChange = { numeroOrden = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Ej: ORD-2024-001") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = TexIAColors.Primary
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TexIAColors.Primary,
                                    unfocusedBorderColor = Color(0xFFE5E7EB)
                                )
                            )

                            Spacer(Modifier.height(16.dp))

                            // Tipo de Defecto
                            Text(
                                "Tipo de Defecto *",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF374151)
                            )
                            Spacer(Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = showTipoDefectoMenu,
                                onExpandedChange = { showTipoDefectoMenu = it }
                            ) {
                                OutlinedTextField(
                                    value = tipoDefecto,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    placeholder = { Text("Seleccionar tipo") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = TexIAColors.Error
                                        )
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = showTipoDefectoMenu
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TexIAColors.Primary,
                                        unfocusedBorderColor = Color(0xFFE5E7EB)
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = showTipoDefectoMenu,
                                    onDismissRequest = { showTipoDefectoMenu = false }
                                ) {
                                    tiposDefecto.forEach { tipo ->
                                        DropdownMenuItem(
                                            text = { Text(tipo) },
                                            onClick = {
                                                tipoDefecto = tipo
                                                showTipoDefectoMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Nivel de Gravedad
                            Text(
                                "Nivel de Gravedad *",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF374151)
                            )
                            Spacer(Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = showGravedadMenu,
                                onExpandedChange = { showGravedadMenu = it }
                            ) {
                                OutlinedTextField(
                                    value = gravedad,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = nivelesGravedad.find { it.first == gravedad }?.second ?: TexIAColors.Accent
                                        )
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = showGravedadMenu
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TexIAColors.Primary,
                                        unfocusedBorderColor = Color(0xFFE5E7EB)
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = showGravedadMenu,
                                    onDismissRequest = { showGravedadMenu = false }
                                ) {
                                    nivelesGravedad.forEach { (nivel, color) ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(color)
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(nivel)
                                                }
                                            },
                                            onClick = {
                                                gravedad = nivel
                                                showGravedadMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Metros Afectados
                            Text(
                                "Metros Afectados *",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF374151)
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = metrosAfectados,
                                onValueChange = { metrosAfectados = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Ej: 5.5") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Build,
                                        contentDescription = null,
                                        tint = TexIAColors.Purple
                                    )
                                },
                                suffix = { Text("mts") },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TexIAColors.Primary,
                                    unfocusedBorderColor = Color(0xFFE5E7EB)
                                )
                            )

                            Spacer(Modifier.height(16.dp))

                            // Descripción
                            Text(
                                "Descripción Detallada *",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF374151)
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = descripcion,
                                onValueChange = { descripcion = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                placeholder = { Text("Describe el defecto encontrado...") },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TexIAColors.Primary,
                                    unfocusedBorderColor = Color(0xFFE5E7EB)
                                )
                            )

                            if (errorMessage != null) {
                                Spacer(Modifier.height(16.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = TexIAColors.Error.copy(alpha = 0.1f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = TexIAColors.Error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            errorMessage ?: "",
                                            fontSize = 13.sp,
                                            color = TexIAColors.Error
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            // Botón de registro
                            Button(
                                onClick = {
                                    when {
                                        numeroOrden.isBlank() -> errorMessage = "Ingrese el número de orden"
                                        tipoDefecto.isBlank() -> errorMessage = "Seleccione el tipo de defecto"
                                        metrosAfectados.isBlank() -> errorMessage = "Ingrese los metros afectados"
                                        descripcion.isBlank() -> errorMessage = "Ingrese una descripción"
                                        else -> {
                                            isLoading = true
                                            errorMessage = null

                                            val defectoData = hashMapOf(
                                                "numeroOrden" to numeroOrden,
                                                "tipoDefecto" to tipoDefecto,
                                                "descripcion" to descripcion,
                                                "gravedad" to gravedad,
                                                "metrosAfectados" to metrosAfectados,
                                                "fecha" to Timestamp.now(),
                                                "usuarioReporte" to userId,
                                                "nombreUsuario" to nombreUsuario,
                                                "estado" to "REPORTADO"
                                            )

                                            db.collection("defectos")
                                                .add(defectoData)
                                                .addOnSuccessListener {
                                                    isLoading = false
                                                    showSuccessDialog = true
                                                    // Limpiar campos
                                                    numeroOrden = ""
                                                    tipoDefecto = ""
                                                    descripcion = ""
                                                    gravedad = "MEDIA"
                                                    metrosAfectados = ""
                                                }
                                                .addOnFailureListener { e ->
                                                    isLoading = false
                                                    errorMessage = "Error: ${e.message}"
                                                }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                enabled = !isLoading,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TexIAColors.Error,
                                    disabledContainerColor = TexIAColors.Error.copy(alpha = 0.5f)
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Registrar Defecto",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Defectos recientes
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Defectos Reportados Recientemente",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        if (isLoadingDefectos) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = TexIAColors.Primary
                            )
                        }
                    }
                }

                if (defectosRecientes.isEmpty() && !isLoadingDefectos) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
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
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color(0xFFE5E7EB)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "No hay defectos reportados",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(defectosRecientes) { defecto ->
                        DefectoCard(defecto)
                    }
                }
            }
        }

        // Diálogo de éxito
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(TexIAColors.Secondary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = TexIAColors.Secondary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                },
                title = {
                    Text(
                        "¡Defecto Registrado!",
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                text = {
                    Text(
                        "El defecto ha sido registrado exitosamente. El equipo de calidad ha sido notificado.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = Color(0xFF6B7280)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showSuccessDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TexIAColors.Secondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Entendido")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun DefectoCard(defecto: Defecto) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        defecto.numeroOrden,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TexIAColors.Primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        defecto.tipoDefecto,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (defecto.gravedad) {
                        "CRÍTICA" -> TexIAColors.Error
                        "ALTA" -> Color(0xFFFF6B6B)
                        "MEDIA" -> TexIAColors.Accent
                        "BAJA" -> TexIAColors.Secondary
                        else -> Color(0xFF6B7280)
                    }
                ) {
                    Text(
                        defecto.gravedad,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                defecto.descripcion,
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                lineHeight = 20.sp,
                maxLines = 2
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TexIAColors.Purple.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            tint = TexIAColors.Purple,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${defecto.metrosAfectados} mts",
                            fontSize = 12.sp,
                            color = TexIAColors.Purple,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (defecto.estado) {
                        "RESUELTO" -> TexIAColors.Secondary.copy(alpha = 0.1f)
                        "EN_REVISION" -> TexIAColors.Accent.copy(alpha = 0.1f)
                        else -> TexIAColors.Error.copy(alpha = 0.1f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when (defecto.estado) {
                                "RESUELTO" -> Icons.Default.CheckCircle
                                "EN_REVISION" -> Icons.Default.Refresh
                                else -> Icons.Default.Warning
                            },
                            contentDescription = null,
                            tint = when (defecto.estado) {
                                "RESUELTO" -> TexIAColors.Secondary
                                "EN_REVISION" -> TexIAColors.Accent
                                else -> TexIAColors.Error
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            defecto.estado,
                            fontSize = 12.sp,
                            color = when (defecto.estado) {
                                "RESUELTO" -> TexIAColors.Secondary
                                "EN_REVISION" -> TexIAColors.Accent
                                else -> TexIAColors.Error
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}