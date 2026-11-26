package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.JhfCard
import com.example.ui.components.JhfDropdown
import com.example.ui.components.JhfTextField
import com.example.ui.components.NeonButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ErpUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScreen(
    uiState: ErpUiState,
    onSaveUser: (editingId: Int?, name: String, accessId: String, password: String, role: String, onDone: () -> Unit) -> Unit,
    onDeleteUser: (Int) -> Unit,
    onNotify: (String, Boolean) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserProfile?>(null) }
    var deleteCandidate by remember { mutableStateOf<UserProfile?>(null) }

    var name by remember { mutableStateOf("") }
    var accessId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Staff") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Staff Management", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text("${uiState.staffList.size} accounts configured", color = TextSecondary, fontSize = 12.sp)
            }
            NeonButton(
                text = "Add Staff",
                icon = Icons.Default.Add,
                onClick = {
                    editingUser = null
                    name = ""
                    accessId = ""
                    password = ""
                    role = "Staff"
                    showSheet = true
                },
                testTag = "staff_add_button"
            )
        }

        if (uiState.staffLoading && uiState.staffList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.staffList, key = { it.id }) { member ->
                    val isSelf = member.id == uiState.currentUser?.id
                    val roleColor = when {
                        member.isAdmin -> NeonGreen
                        member.isManagerOrAdmin -> InfoBlue
                        else -> TextSecondary
                    }
                    JhfCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(member.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    StatusBadge(label = member.role, color = roleColor)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Access ID: ${member.accessId}${if (isSelf) " (You)" else ""}",
                                    color = NeonGreen,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 12.sp
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        editingUser = member
                                        name = member.name
                                        accessId = member.accessId
                                        password = ""
                                        role = member.role
                                        showSheet = true
                                    },
                                    modifier = Modifier.testTag("staff_edit_${member.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit staff", tint = NeonGreen)
                                }
                                if (!isSelf) {
                                    IconButton(
                                        onClick = { deleteCandidate = member },
                                        modifier = Modifier.testTag("staff_delete_${member.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete staff", tint = DangerRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }, containerColor = DarkCard) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (editingUser == null) "Create Staff Account" else "Edit Staff Account",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonGreen
                )
                JhfTextField(value = name, onValueChange = { name = it }, label = "Full Name *", testTag = "staff_name_input")
                JhfTextField(value = accessId, onValueChange = { accessId = it }, label = "Access ID *", testTag = "staff_access_id_input")
                JhfTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = if (editingUser == null) "Password *" else "New Password (blank to keep)",
                    keyboardType = KeyboardType.Password,
                    visualTransformation = PasswordVisualTransformation(),
                    testTag = "staff_password_input"
                )
                JhfDropdown(
                    label = "Role",
                    items = listOf("Admin", "Manager", "Staff"),
                    selectedText = role,
                    itemLabel = { it },
                    onSelect = { role = it },
                    testTag = "staff_role_dropdown"
                )
                HorizontalDivider(color = DarkBorder)
                NeonButton(
                    text = if (uiState.globalBusy) "Saving..." else "Save Account & Sync to Web",
                    enabled = !uiState.globalBusy,
                    onClick = {
                        if (name.isBlank() || accessId.isBlank() || (editingUser == null && password.isBlank())) {
                            onNotify("Name, Access ID, and Password are required", true)
                            return@NeonButton
                        }
                        onSaveUser(editingUser?.id, name, accessId, password, role) {
                            showSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "staff_save_button"
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    deleteCandidate?.let { member ->
        ConfirmActionDialog(
            title = "Delete Staff Account",
            message = "Remove ${member.name} (${member.accessId}) from system access?",
            confirmLabel = "Delete",
            onConfirm = { onDeleteUser(member.id) },
            onDismiss = { deleteCandidate = null }
        )
    }
}
