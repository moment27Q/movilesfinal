package com.cohaila.proyectomovil

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareasScreen(
    userId: String,
    onNavigateBack: () -> Unit = {}
) {
    var tareas by remember { mutableStateOf<List<TareaResumen>>(emptyList()) }
    var tareasFiltradas by remember { mutableStateOf<List<TareaResumen>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var filtroEstado by remember { mutableStateOf("TODAS") }
    var searchQuery by remember { mutableStateOf("") }

    val db = FirebaseFirestore.getInstance()

    val filtros = listOf("TODAS", "PENDIENTE", "EN_CURSO", "COMPLETADA")

    // Cargar tareas desde Firestore
    LaunchedEffect(userId) {
        try {
            isLoading = true
            errorMessage = null

            val tareasSnapshot = db.collection("tareas")
                .whereEqualTo("usuarioAsignado", userId)
                .get()
                .await()

            val tareasList = tareasSnapshot.documents.mapNotNull { doc ->
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

            tareas = tareasList
            tareasFiltradas = tareasList

        } catch (e: Exception) {
            errorMessage = "Error al cargar tareas: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Filtrar tareas
    LaunchedEffect(filtroEstado, searchQuery, tareas) {
        var resultado = tareas

        if (filtroEstado != "TODAS") {
            resultado = resultado.filter { it.estado == filtroEstado }
        }

        if (searchQuery.isNotEmpty()) {
            resultado = resultado.filter {
                it.nombre.contains(searchQuery, ignoreCase = true) ||
                        it.numeroOrden.contains(searchQuery, ignoreCase = true)
            }
        }

        tareasFiltradas = resultado
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Mis Órdenes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Text(
                            "${tareasFiltradas.size} asignadas",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TexIAColors.Surface)
                .padding(padding)
        ) {
            // Buscador y Filtros
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar orden...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TexIAColors.Primary,
                            unfocusedBorderColor = Color(0xFFE5E7EB)
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtros) { filtro ->
                            FilterChip(
                                selected = filtroEstado == filtro,
                                onClick = { filtroEstado = filtro },
                                label = { Text(filtro) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TexIAColors.Primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Lista de Tareas
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TexIAColors.Primary)
                }
            } else if (tareasFiltradas.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color(0xFFE5E7EB)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No hay órdenes",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tareasFiltradas) { tarea ->
                        OrdenProduccionItem(
                            tarea = tarea,
                            onIniciar = { /* Implementar lógica de inicio */ }
                        )
                    }
                }
            }
        }
    }
}
