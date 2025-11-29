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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Modelo de datos para Inventario de Telas
data class TelaInventario(
    val id: String = "",
    val nombre: String = "",
    val tipo: String = "",
    val color: String = "",
    val cantidadComprada: Double = 0.0,
    val cantidadDisponible: Double = 0.0,
    val cantidadEnProduccion: Double = 0.0,
    val cantidadUsada: Double = 0.0,
    val unidad: String = "metros",
    val precioCompra: Double = 0.0,
    val proveedor: String = "",
    val fechaCompra: com.google.firebase.Timestamp? = null,
    val ubicacionAlmacen: String = "",
    val lote: String = "",
    val estado: EstadoInventario = EstadoInventario.DISPONIBLE
)

enum class EstadoInventario {
    DISPONIBLE,
    EN_PRODUCCION,
    BAJO_STOCK,
    AGOTADO,
    RESERVADO
}

data class FiltroInventario(
    val nombre: String,
    val icono: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioTelasScreen(
    userId: String,
    onNavigateBack: () -> Unit = {}
) {
    var telasInventario by remember { mutableStateOf<List<TelaInventario>>(emptyList()) }
    var telasFiltradas by remember { mutableStateOf<List<TelaInventario>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var filtroSeleccionado by remember { mutableStateOf("Todas") }
    var ordenSeleccionado by remember { mutableStateOf("Recientes") }
    var mostrarFiltros by remember { mutableStateOf(false) }

    // Estadísticas
    var totalTelas by remember { mutableStateOf(0.0) }
    var telasDisponibles by remember { mutableStateOf(0.0) }
    var telasEnProduccion by remember { mutableStateOf(0.0) }
    var valorTotal by remember { mutableStateOf(0.0) }

    val db = FirebaseFirestore.getInstance()

    // Filtros disponibles
    val filtros = listOf(
        FiltroInventario("Todas", Icons.Default.List),
        FiltroInventario("Disponible", Icons.Default.CheckCircle),
        FiltroInventario("En Producción", Icons.Default.Build),
        FiltroInventario("Bajo Stock", Icons.Default.Warning),
        FiltroInventario("Agotado", Icons.Default.Close)
    )

    // Cargar inventario desde Firestore
    LaunchedEffect(userId) {
        try {
            isLoading = true
            errorMessage = null

            // Obtener el inventario del usuario
            val inventarioSnapshot = db.collection("inventario_telas")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val inventarioList = inventarioSnapshot.documents.mapNotNull { doc ->
                val cantidadComprada = doc.getDouble("cantidadComprada") ?: 0.0
                val cantidadUsada = doc.getDouble("cantidadUsada") ?: 0.0
                val cantidadEnProduccion = doc.getDouble("cantidadEnProduccion") ?: 0.0
                val cantidadDisponible = cantidadComprada - cantidadUsada - cantidadEnProduccion

                // Determinar el estado
                val estado = when {
                    cantidadDisponible <= 0.0 -> EstadoInventario.AGOTADO
                    cantidadEnProduccion > 0.0 -> EstadoInventario.EN_PRODUCCION
                    cantidadDisponible < (cantidadComprada * 0.2) -> EstadoInventario.BAJO_STOCK
                    else -> EstadoInventario.DISPONIBLE
                }

                TelaInventario(
                    id = doc.id,
                    nombre = doc.getString("nombre") ?: "",
                    tipo = doc.getString("tipo") ?: "",
                    color = doc.getString("color") ?: "",
                    cantidadComprada = cantidadComprada,
                    cantidadDisponible = cantidadDisponible,
                    cantidadEnProduccion = cantidadEnProduccion,
                    cantidadUsada = cantidadUsada,
                    unidad = doc.getString("unidad") ?: "metros",
                    precioCompra = doc.getDouble("precioCompra") ?: 0.0,
                    proveedor = doc.getString("proveedor") ?: "",
                    fechaCompra = doc.getTimestamp("fechaCompra"),
                    ubicacionAlmacen = doc.getString("ubicacionAlmacen") ?: "",
                    lote = doc.getString("lote") ?: "",
                    estado = estado
                )
            }

            telasInventario = inventarioList
            telasFiltradas = inventarioList

            // Calcular estadísticas
            totalTelas = inventarioList.sumOf { it.cantidadComprada }
            telasDisponibles = inventarioList.sumOf { it.cantidadDisponible }
            telasEnProduccion = inventarioList.sumOf { it.cantidadEnProduccion }
            valorTotal = inventarioList.sumOf { it.cantidadComprada * it.precioCompra }

        } catch (e: Exception) {
            errorMessage = "Error al cargar inventario: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Aplicar filtros
    LaunchedEffect(searchQuery, filtroSeleccionado, ordenSeleccionado) {
        var resultado = telasInventario

        // Filtrar por búsqueda
        if (searchQuery.isNotEmpty()) {
            resultado = resultado.filter {
                it.nombre.contains(searchQuery, ignoreCase = true) ||
                        it.tipo.contains(searchQuery, ignoreCase = true) ||
                        it.color.contains(searchQuery, ignoreCase = true) ||
                        it.lote.contains(searchQuery, ignoreCase = true)
            }
        }

        // Filtrar por estado
        resultado = when (filtroSeleccionado) {
            "Disponible" -> resultado.filter { it.estado == EstadoInventario.DISPONIBLE }
            "En Producción" -> resultado.filter { it.estado == EstadoInventario.EN_PRODUCCION }
            "Bajo Stock" -> resultado.filter { it.estado == EstadoInventario.BAJO_STOCK }
            "Agotado" -> resultado.filter { it.estado == EstadoInventario.AGOTADO }
            else -> resultado
        }

        // Ordenar
        resultado = when (ordenSeleccionado) {
            "Recientes" -> resultado.sortedByDescending { it.fechaCompra }
            "Nombre" -> resultado.sortedBy { it.nombre }
            "Stock Alto" -> resultado.sortedByDescending { it.cantidadDisponible }
            "Stock Bajo" -> resultado.sortedBy { it.cantidadDisponible }
            "Valor" -> resultado.sortedByDescending { it.cantidadComprada * it.precioCompra }
            else -> resultado
        }

        telasFiltradas = resultado
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Mi Inventario de Telas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Text(
                            "${telasFiltradas.size} telas en stock",
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
                actions = {
                    IconButton(onClick = { mostrarFiltros = !mostrarFiltros }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Filtros",
                            tint = TexIAColors.Primary
                        )
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
            // Estadísticas generales
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        "Resumen del Inventario",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EstadisticaInventario(
                            modifier = Modifier.weight(1f),
                            valor = String.format("%.1f", totalTelas),
                            label = "Total\nComprado",
                            color = TexIAColors.Primary
                        )
                        EstadisticaInventario(
                            modifier = Modifier.weight(1f),
                            valor = String.format("%.1f", telasDisponibles),
                            label = "Disponible",
                            color = TexIAColors.Secondary
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EstadisticaInventario(
                            modifier = Modifier.weight(1f),
                            valor = String.format("%.1f", telasEnProduccion),
                            label = "En\nProducción",
                            color = TexIAColors.Accent
                        )
                        EstadisticaInventario(
                            modifier = Modifier.weight(1f),
                            valor = "S/ ${String.format("%.0f", valorTotal)}",
                            label = "Valor\nTotal",
                            color = TexIAColors.Purple
                        )
                    }
                }
            }

            // Barra de búsqueda y filtros
            if (mostrarFiltros) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
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
                            placeholder = { Text("Buscar por nombre, tipo, color o lote...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Limpiar")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TexIAColors.Primary,
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            "Ordenar por:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280)
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                listOf(
                                    "Recientes",
                                    "Nombre",
                                    "Stock Alto",
                                    "Stock Bajo",
                                    "Valor"
                                )
                            ) { orden ->
                                FilterChip(
                                    selected = ordenSeleccionado == orden,
                                    onClick = { ordenSeleccionado = orden },
                                    label = { Text(orden, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Filtros de estado
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtros) { filtro ->
                    FiltroChip(
                        filtro = filtro,
                        isSelected = filtroSeleccionado == filtro.nombre,
                        onClick = { filtroSeleccionado = filtro.nombre }
                    )
                }
            }

            // Mensaje de error
            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = TexIAColors.Error.copy(alpha = 0.1f)
                    )
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
                        Text(errorMessage ?: "", color = TexIAColors.Error)
                    }
                }
            }

            // Lista de telas en inventario
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TexIAColors.Primary)
                }
            } else if (telasFiltradas.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color(0xFFE5E7EB)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No hay telas en inventario",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            "Compra telas para empezar",
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(telasFiltradas) { tela ->
                        TelaInventarioCard(tela = tela)
                    }
                }
            }
        }
    }
}

@Composable
fun EstadisticaInventario(
    modifier: Modifier = Modifier,
    valor: String,
    label: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                valor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                fontSize = 11.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun FiltroChip(
    filtro: FiltroInventario,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) TexIAColors.Primary else Color.White,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFFE5E7EB)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                filtro.icono,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFF6B7280),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                filtro.nombre,
                color = if (isSelected) Color.White else Color(0xFF1F2937),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun TelaInventarioCard(tela: TelaInventario) {
    var showDetails by remember { mutableStateOf(false) }

    val estadoColor = when (tela.estado) {
        EstadoInventario.DISPONIBLE -> TexIAColors.Secondary
        EstadoInventario.EN_PRODUCCION -> TexIAColors.Accent
        EstadoInventario.BAJO_STOCK -> Color(0xFFF97316)
        EstadoInventario.AGOTADO -> TexIAColors.Error
        EstadoInventario.RESERVADO -> TexIAColors.Purple
    }

    val estadoTexto = when (tela.estado) {
        EstadoInventario.DISPONIBLE -> "Disponible"
        EstadoInventario.EN_PRODUCCION -> "En Producción"
        EstadoInventario.BAJO_STOCK -> "Bajo Stock"
        EstadoInventario.AGOTADO -> "Agotado"
        EstadoInventario.RESERVADO -> "Reservado"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDetails = !showDetails }
        ) {
            // Header con gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                estadoColor.copy(alpha = 0.8f),
                                estadoColor
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            tela.nombre,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "${tela.tipo} - ${tela.color}",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.3f)
                    ) {
                        Text(
                            estadoTexto,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Información principal
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Cantidades
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CantidadBox(
                        modifier = Modifier.weight(1f),
                        label = "Disponible",
                        cantidad = tela.cantidadDisponible,
                        unidad = tela.unidad,
                        color = TexIAColors.Secondary
                    )
                    CantidadBox(
                        modifier = Modifier.weight(1f),
                        label = "En Producción",
                        cantidad = tela.cantidadEnProduccion,
                        unidad = tela.unidad,
                        color = TexIAColors.Accent
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CantidadBox(
                        modifier = Modifier.weight(1f),
                        label = "Usada",
                        cantidad = tela.cantidadUsada,
                        unidad = tela.unidad,
                        color = TexIAColors.Purple
                    )
                    CantidadBox(
                        modifier = Modifier.weight(1f),
                        label = "Total Comprado",
                        cantidad = tela.cantidadComprada,
                        unidad = tela.unidad,
                        color = TexIAColors.Primary
                    )
                }

                // Detalles expandibles
                if (showDetails) {
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))

                    DetailRow(
                        icon = Icons.Default.ShoppingCart,
                        label = "Precio de compra",
                        value = "S/ ${String.format("%.2f", tela.precioCompra)} por ${tela.unidad}"
                    )

                    Spacer(Modifier.height(8.dp))

                    DetailRow(
                        icon = Icons.Default.Person,
                        label = "Proveedor",
                        value = tela.proveedor
                    )

                    Spacer(Modifier.height(8.dp))

                    DetailRow(
                        icon = Icons.Default.LocationOn,
                        label = "Ubicación",
                        value = tela.ubicacionAlmacen.ifEmpty { "No especificada" }
                    )

                    Spacer(Modifier.height(8.dp))

                    DetailRow(
                        icon = Icons.Default.Info,
                        label = "Lote",
                        value = tela.lote.ifEmpty { "Sin lote" }
                    )

                    Spacer(Modifier.height(8.dp))

                    val valorTotal = tela.cantidadComprada * tela.precioCompra
                    DetailRow(
                        icon = Icons.Default.ShoppingCart,
                        label = "Valor total invertido",
                        value = "S/ ${String.format("%.2f", valorTotal)}"
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Botón ver más
                OutlinedButton(
                    onClick = { showDetails = !showDetails },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        if (showDetails) "Ver menos" else "Ver detalles",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        if (showDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CantidadBox(
    modifier: Modifier = Modifier,
    label: String,
    cantidad: Double,
    unidad: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                fontSize = 11.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                String.format("%.1f", cantidad),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                unidad,
                fontSize = 10.sp,
                color = Color(0xFF9CA3AF)
            )
        }
    }
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                label,
                fontSize = 11.sp,
                color = Color(0xFF9CA3AF)
            )
            Text(
                value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937)
            )
        }
    }
}