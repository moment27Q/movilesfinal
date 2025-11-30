package com.cohaila.proyectomovil

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    val userId = currentUser?.uid ?: ""

    // Estados para los datos del usuario
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    
    // Estado de carga y edición
    var isLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Cargar datos
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            try {
                val document = db.collection("users").document(userId).get().await()
                if (document.exists()) {
                    nombre = document.getString("nombre") ?: ""
                    telefono = document.getString("telefono") ?: ""
                    direccion = document.getString("direccion") ?: ""
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al cargar datos: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    // Función para guardar datos
    fun guardarDatos() {
        if (nombre.isBlank()) {
            Toast.makeText(context, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        isSaving = true
        val userMap = hashMapOf(
            "nombre" to nombre,
            "telefono" to telefono,
            "direccion" to direccion,
            "email" to email
        )

        db.collection("users").document(userId)
            .set(userMap, SetOptions.merge())
            .addOnSuccessListener {
                isSaving = false
                isEditing = false
                Toast.makeText(context, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                isSaving = false
                Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TexIAColors.Primary
                ),
                actions = {
                    if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(TexIAColors.Surface)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(TexIAColors.Primary, TexIAColors.PrimaryDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = nombre.firstOrNull()?.toString()?.uppercase() ?: "U",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Formulario
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Email (No editable)
                            OutlinedTextField(
                                value = email,
                                onValueChange = {},
                                label = { Text("Correo Electrónico") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color.Black,
                                    disabledBorderColor = Color.Gray,
                                    disabledLabelColor = Color.Gray
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Nombre
                            OutlinedTextField(
                                value = nombre,
                                onValueChange = { if (isEditing) nombre = it },
                                label = { Text("Nombre Completo") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isEditing
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Teléfono
                            OutlinedTextField(
                                value = telefono,
                                onValueChange = { if (isEditing) telefono = it },
                                label = { Text("Teléfono") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isEditing
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Dirección
                            OutlinedTextField(
                                value = direccion,
                                onValueChange = { if (isEditing) direccion = it },
                                label = { Text("Dirección") },
                                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isEditing
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botones de Acción
                    if (isEditing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { isEditing = false }, // Cancelar
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                            ) {
                                Text("Cancelar")
                            }

                            Button(
                                onClick = { guardarDatos() },
                                modifier = Modifier.weight(1f),
                                enabled = !isSaving,
                                colors = ButtonDefaults.buttonColors(containerColor = TexIAColors.Primary)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Guardar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
