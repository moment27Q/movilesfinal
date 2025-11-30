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

// Modelo de datos para Tela
data class Tela(
    val id: String = "",
    val nombre: String = "",
    val tipo: String = "",
    val color: String = "",
    val precio: Double = 0.0,
    val stockDisponible: Int = 0,
    val unidad: String = "metros",
    val proveedor: String = "",
    val ubicacion: String = "",
    val imagen: String = "",
    val descripcion: String = "",
    val ordenesNacionales: Int = 0
)

// Categorías de filtro
data class CategoriaFiltro(
    val nombre: String,
    val icono: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprarTelasScreen(
    onNavigateBack: () -> Unit = {}
) {
    var telas by remember { mutableStateOf<List<Tela>>(emptyList()) }
    var telasFiltradas by remember { mutableStateOf<List<Tela>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("Todas") }
    var ordenSeleccionado by remember { mutableStateOf("Nombre") }
    var mostrarFiltros by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()

    // Categorías disponibles
    val categorias = listOf(
        CategoriaFiltro("Todas", Icons.Filled.List),
        CategoriaFiltro("Algodón", Icons.Filled.CheckCircle),
        CategoriaFiltro("Poliéster", Icons.Filled.Star),
        CategoriaFiltro("Seda", Icons.Filled.Info),
        CategoriaFiltro("Lino", Icons.Filled.Check)
    )

    // Cargar telas desde Firestore
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            errorMessage = null

            val telasSnapshot = db.collection("telas")
                .get()
                .await()

            val telasList = telasSnapshot.documents.mapNotNull { doc ->
                Tela(
                    id = doc.id,
                    nombre = doc.getString("nombre") ?: "",
                    tipo = doc.getString("tipo") ?: "",
                    color = doc.getString("color") ?: "",
                    precio = doc.getDouble("precio") ?: 0.0,
                    stockDisponible = doc.getLong("stockDisponible")?.toInt() ?: 0,
                    unidad = doc.getString("unidad") ?: "metros",
                    proveedor = doc.getString("proveedor") ?: "",
                    ubicacion = doc.getString("ubicacion") ?: "",
                    imagen = doc.getString("imagen") ?: "",
                    descripcion = doc.getString("descripcion") ?: "",
                    ordenesNacionales = doc.getLong("ordenesNacionales")?.toInt() ?: 0
                )
            }

            telas = telasList
            telasFiltradas = telasList

        } catch (e: Exception) {
            errorMessage = "Error al cargar telas: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Aplicar filtros
    LaunchedEffect(searchQuery, categoriaSeleccionada, ordenSeleccionado) {
        var resultado = telas

        // Filtrar por búsqueda
        if (searchQuery.isNotEmpty()) {
            resultado = resultado.filter {
                it.nombre.contains(searchQuery, ignoreCase = true) ||
                        it.tipo.contains(searchQuery, ignoreCase = true) ||
                        it.color.contains(searchQuery, ignoreCase = true)
            }
        }

        // Filtrar por categoría
        if (categoriaSeleccionada != "Todas") {
            resultado = resultado.filter {
                it.tipo.equals(categoriaSeleccionada, ignoreCase = true)
            }
        }

        // Ordenar
        resultado = when (ordenSeleccionado) {
            "Precio: Menor a Mayor" -> resultado.sortedBy { it.precio }
            "Precio: Mayor a Menor" -> resultado.sortedByDescending { it.precio }
            "Stock Disponible" -> resultado.sortedByDescending { it.stockDisponible }
            "Más Ordenados" -> resultado.sortedByDescending { it.ordenesNacionales }
            else -> resultado.sortedBy { it.nombre }
        }

        telasFiltradas = resultado
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Comprar Telas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Text(
                            "${telasFiltradas.size} telas disponibles",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarFiltros = !mostrarFiltros }) {
                        Icon(
                            Icons.Filled.Search,
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
            // Barra de búsqueda
            if (mostrarFiltros) {
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
                        // Campo de búsqueda
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Buscar telas...") },
                            leadingIcon = {
                                Icon(Icons.Filled.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Limpiar")
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

                        // Selector de ordenamiento
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
                                    "Nombre",
                                    "Precio: Menor a Mayor",
                                    "Precio: Mayor a Menor",
                                    "Stock Disponible",
                                    "Más Ordenados"
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
            }

            // Categorías
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categorias) { categoria ->
                    CategoriaChip(
                        categoria = categoria,
                        isSelected = categoriaSeleccionada == categoria.nombre,
                        onClick = { categoriaSeleccionada = categoria.nombre }
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
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = TexIAColors.Error
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(errorMessage ?: "", color = TexIAColors.Error)
                    }
                }
            }

            // Lista de telas
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
                            Icons.Filled.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color(0xFFE5E7EB)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No se encontraron telas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            "Intenta con otros filtros",
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
                        TelaCard(tela = tela)
                    }
                }
            }
        }
    }
}

@Composable
fun CategoriaChip(
    categoria: CategoriaFiltro,
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                categoria.icono,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFF6B7280),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                categoria.nombre,
                color = if (isSelected) Color.White else Color(0xFF1F2937),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun TelaCard(tela: Tela) {
    var showDetails by remember { mutableStateOf(false) }

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
                    .height(120.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                TexIAColors.Primary.copy(alpha = 0.8f),
                                TexIAColors.PrimaryDark
                            )
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(
                        tela.nombre,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "${tela.tipo} - ${tela.color}",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Información principal
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Precio
                    Column {
                        Text(
                            "Precio",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            "S/ ${String.format("%.2f", tela.precio)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TexIAColors.Primary
                        )
                        Text(
                            "por ${tela.unidad}",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }

                    // Stock
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (tela.stockDisponible > 100)
                            TexIAColors.Secondary.copy(alpha = 0.1f)
                        else if (tela.stockDisponible > 50)
                            TexIAColors.Accent.copy(alpha = 0.1f)
                        else
                            TexIAColors.Error.copy(alpha = 0.1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.List,
                                contentDescription = null,
                                tint = if (tela.stockDisponible > 100)
                                    TexIAColors.Secondary
                                else if (tela.stockDisponible > 50)
                                    TexIAColors.Accent
                                else
                                    TexIAColors.Error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${tela.stockDisponible}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (tela.stockDisponible > 100)
                                    TexIAColors.Secondary
                                else if (tela.stockDisponible > 50)
                                    TexIAColors.Accent
                                else
                                    TexIAColors.Error
                            )
                            Text(
                                "en stock",
                                fontSize = 10.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Estadísticas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoBadge(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.LocationOn,
                        label = "Ubicación",
                        value = tela.ubicacion,
                        color = TexIAColors.Teal
                    )
                    InfoBadge(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.ShoppingCart,
                        label = "Órdenes",
                        value = "${tela.ordenesNacionales}",
                        color = TexIAColors.Purple
                    )
                }

                // Detalles expandibles
                if (showDetails) {
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Descripción:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tela.descripcion.ifEmpty { "Sin descripción disponible" },
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        lineHeight = 18.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Proveedor: ${tela.proveedor}",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDetails = !showDetails },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            if (showDetails) "Ver menos" else "Ver más",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = { /* Agregar al carrito */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TexIAColors.Primary
                        )
                    ) {
                        Icon(
                            Icons.Filled.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Ordenar",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoBadge(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    label,
                    fontSize = 10.sp,
                    color = Color(0xFF6B7280)
                )
                Text(
                    value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}