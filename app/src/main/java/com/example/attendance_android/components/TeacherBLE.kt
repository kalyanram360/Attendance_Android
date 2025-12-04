//package com.example.attendance_android.components
//
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.expandVertically
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.animation.shrinkVertically
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.outlined.CalendarToday
//import androidx.compose.material.icons.outlined.Class
//import androidx.compose.material.icons.outlined.Edit
//import androidx.compose.material.icons.outlined.Group
//import androidx.compose.material.icons.outlined.School
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavController
//import com.example.attendance_android.NavRoutes
//import com.example.attendance_android.ViewModels.TeacherClassViewModel
//import com.example.attendance_android.data.DataStoreManager
//import kotlinx.coroutines.launch
//import java.net.URLEncoder
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun TeacherBLE(
//    modifier: Modifier = Modifier,
//    availableYears: List<String> = listOf("I", "II", "III", "IV"),
//    availableBranches: List<String> = listOf("CSE", "ECE", "ME", "CE"),
//    availableSections: List<String> = listOf("A", "B", "C"),
//    availableSubjects: List<String> = listOf("DS", "OS", "DBMS"),
//    fullname: String = "Professor",
//    collegeName: String = "GVPCE",
//    onStartClass: (year: String, branch: String, section: String) -> Unit = { _, _, _ -> },
//    navController: NavController,
//    viewModel: TeacherClassViewModel = viewModel()
//) {
//    val context = LocalContext.current
//    val dataStore = remember { DataStoreManager(context) }
//    val teacherEmail by dataStore.email.collectAsState(initial = "")
//
//    // Collect state from ViewModel
//    val yearValue by viewModel.year.collectAsState()
//    val branchValue by viewModel.branch.collectAsState()
//    val sectionValue by viewModel.section.collectAsState()
//    val subjectValue by viewModel.subject.collectAsState()
//
//    val selectedYear = if (yearValue > 0) availableYears.getOrNull(yearValue - 1) ?: "" else ""
//    val selectedBranch = branchValue
//    val selectedSection = sectionValue
//    val selectedSubject = subjectValue
//
//    val scope = rememberCoroutineScope()
//    var classNotes by remember { mutableStateOf("") }
//
//    // Dropdown expanded flags
//    var yearExpanded by remember { mutableStateOf(false) }
//    var branchExpanded by remember { mutableStateOf(false) }
//    var sectionExpanded by remember { mutableStateOf(false) }
//    var subjectExpanded by remember { mutableStateOf(false) }
//
//    val canStart = selectedYear.isNotBlank() &&
//            selectedBranch.isNotBlank() &&
//            selectedSection.isNotBlank() &&
//            selectedSubject.isNotBlank()
//
//    Scaffold(
//        topBar = {
//            HeaderWithProfile(
//                fullname = fullname,
//                collegeName = collegeName,
//                navController = navController
//            )
//        },
//        bottomBar = {
//            FooterNavPrimary(
//                onHome = { navController.navigate(NavRoutes.TeacherHome.route) { launchSingleTop = true } },
//                onClasses = { /* optional nav */ },
//                onSettings = { /* optional nav */ },
//                selected = "HOME"
//            )
//        },
//        containerColor = MaterialTheme.colorScheme.surface
//    ) { innerPadding ->
//        Column(
//            modifier = modifier
//                .fillMaxSize()
//                .padding(innerPadding)
//                .verticalScroll(rememberScrollState())
//                .padding(20.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            // Header Section
//            ClassSetupHeader()
//
//            // Form Card
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                shape = MaterialTheme.shapes.extraLarge,
//                colors = CardDefaults.cardColors(
//                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
//                ),
//                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
//            ) {
//                Column(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(20.dp),
//                    verticalArrangement = Arrangement.spacedBy(16.dp)
//                ) {
//                    Text(
//                        text = "Class Details",
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.SemiBold,
//                        color = MaterialTheme.colorScheme.onSurface
//                    )
//
//                    // Year Dropdown
//                    EnhancedDropdownField(
//                        value = selectedYear,
//                        label = "Year",
//                        icon = Icons.Outlined.CalendarToday,
//                        expanded = yearExpanded,
//                        onExpandedChange = { yearExpanded = it },
//                        items = availableYears,
//                        onItemSelected = { index ->
//                            viewModel.updateYear(index + 1)
//                            yearExpanded = false
//                        }
//                    )
//
//                    // Branch Dropdown
//                    EnhancedDropdownField(
//                        value = selectedBranch,
//                        label = "Branch",
//                        icon = Icons.Outlined.School,
//                        expanded = branchExpanded,
//                        onExpandedChange = { branchExpanded = it },
//                        items = availableBranches,
//                        onItemSelected = { index ->
//                            viewModel.updateBranch(availableBranches[index])
//                            branchExpanded = false
//                        }
//                    )
//
//                    // Section Dropdown
//                    EnhancedDropdownField(
//                        value = selectedSection,
//                        label = "Section",
//                        icon = Icons.Outlined.Group,
//                        expanded = sectionExpanded,
//                        onExpandedChange = { sectionExpanded = it },
//                        items = availableSections,
//                        onItemSelected = { index ->
//                            viewModel.updateSection(availableSections[index])
//                            sectionExpanded = false
//                        }
//                    )
//
//                    // Subject Dropdown
//                    EnhancedDropdownField(
//                        value = selectedSubject,
//                        label = "Subject",
//                        icon = Icons.Outlined.Class,
//                        expanded = subjectExpanded,
//                        onExpandedChange = { subjectExpanded = it },
//                        items = availableSubjects,
//                        onItemSelected = { index ->
//                            viewModel.updateSubject(availableSubjects[index])
//                            subjectExpanded = false
//                        }
//                    )
//
//                    // Optional Notes Field
//                    OutlinedTextField(
//                        value = classNotes,
//                        onValueChange = { classNotes = it },
//                        label = { Text("Class Notes (Optional)") },
//                        leadingIcon = {
//                            Icon(
//                                imageVector = Icons.Outlined.Edit,
//                                contentDescription = null,
//                                tint = MaterialTheme.colorScheme.primary
//                            )
//                        },
//                        modifier = Modifier.fillMaxWidth(),
//                        shape = MaterialTheme.shapes.large,
//                        colors = OutlinedTextFieldDefaults.colors(
//                            focusedBorderColor = MaterialTheme.colorScheme.primary,
//                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
//                        ),
//                        singleLine = true
//                    )
//                }
//            }
//
//            // Selection Summary Card
//            AnimatedVisibility(
//                visible = canStart,
//                enter = fadeIn() + expandVertically(),
//                exit = fadeOut() + shrinkVertically()
//            ) {
//                SelectionSummaryCard(
//                    year = selectedYear,
//                    branch = selectedBranch,
//                    section = selectedSection,
//                    subject = selectedSubject
//                )
//            }
//
//            // Start Button
//            Button(
//                onClick = {
//                    if (canStart) {
//                        val eYear = URLEncoder.encode(selectedYear, "utf-8")
//                        val eBranch = URLEncoder.encode(selectedBranch, "utf-8")
//                        val eSection = URLEncoder.encode(selectedSection, "utf-8")
//                        val eSubject = URLEncoder.encode(selectedSubject, "utf-8")
//                        val eEmail = URLEncoder.encode(teacherEmail, "utf-8")
//
//                        scope.launch {
//                            navController.navigate("advertising/$eYear/$eBranch/$eSection/$eSubject/$eEmail") {
//                                launchSingleTop = true
//                                restoreState = true
//                            }
//                        }
//                    }
//                },
//                enabled = canStart,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(56.dp),
//                shape = MaterialTheme.shapes.extraLarge,
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = MaterialTheme.colorScheme.primary,
//                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
//                )
//            ) {
//                Text(
//                    text = if (canStart) "Start Class Session" else "Fill All Required Fields",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.SemiBold
//                )
//            }
//
//            // Helper Text
//            AnimatedVisibility(visible = !canStart) {
//                Card(
//                    modifier = Modifier.fillMaxWidth(),
//                    colors = CardDefaults.cardColors(
//                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
//                    ),
//                    shape = MaterialTheme.shapes.medium
//                ) {
//                    Text(
//                        text = "⚠️ Please select Year, Branch, Section, and Subject to continue",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onErrorContainer,
//                        modifier = Modifier.padding(16.dp),
//                        textAlign = TextAlign.Center
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun ClassSetupHeader() {
//    Column(
//        modifier = Modifier.fillMaxWidth(),
//        verticalArrangement = Arrangement.spacedBy(8.dp)
//    ) {
//        Text(
//            text = "Start New Class",
//            style = MaterialTheme.typography.headlineMedium,
//            fontWeight = FontWeight.Bold,
//            color = MaterialTheme.colorScheme.onSurface
//        )
//        Text(
//            text = "Configure your class session details",
//            style = MaterialTheme.typography.bodyLarge,
//            color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun EnhancedDropdownField(
//    value: String,
//    label: String,
//    icon: ImageVector,
//    expanded: Boolean,
//    onExpandedChange: (Boolean) -> Unit,
//    items: List<String>,
//    onItemSelected: (Int) -> Unit
//) {
//    ExposedDropdownMenuBox(
//        expanded = expanded,
//        onExpandedChange = { onExpandedChange(!expanded) }
//    ) {
//        OutlinedTextField(
//            readOnly = true,
//            value = value,
//            onValueChange = { },
//            label = { Text(label) },
//            leadingIcon = {
//                Icon(
//                    imageVector = icon,
//                    contentDescription = null,
//                    tint = if (value.isNotBlank())
//                        MaterialTheme.colorScheme.primary
//                    else
//                        MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            },
//            trailingIcon = {
//                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
//            },
//            modifier = Modifier
//                .fillMaxWidth()
//                .menuAnchor(),
//            shape = MaterialTheme.shapes.large,
//            colors = OutlinedTextFieldDefaults.colors(
//                focusedBorderColor = MaterialTheme.colorScheme.primary,
//                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
//                focusedLabelColor = MaterialTheme.colorScheme.primary
//            )
//        )
//        ExposedDropdownMenu(
//            expanded = expanded,
//            onDismissRequest = { onExpandedChange(false) },
//            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
//        ) {
//            items.forEachIndexed { index, item ->
//                DropdownMenuItem(
//                    text = {
//                        Text(
//                            text = item,
//                            style = MaterialTheme.typography.bodyLarge,
//                            fontWeight = if (item == value) FontWeight.SemiBold else FontWeight.Normal
//                        )
//                    },
//                    onClick = { onItemSelected(index) },
//                    colors = MenuDefaults.itemColors(
//                        textColor = if (item == value)
//                            MaterialTheme.colorScheme.primary
//                        else
//                            MaterialTheme.colorScheme.onSurface
//                    )
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun SelectionSummaryCard(
//    year: String,
//    branch: String,
//    section: String,
//    subject: String
//) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = MaterialTheme.shapes.large,
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.primaryContainer
//        ),
//        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(20.dp),
//            verticalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = "✓ Ready to Start",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold,
//                    color = MaterialTheme.colorScheme.onPrimaryContainer
//                )
//            }
//
//            Divider(
//                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
//                thickness = 1.dp
//            )
//
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                SummaryItem(label = "Year", value = year)
//                SummaryItem(label = "Branch", value = branch)
//                SummaryItem(label = "Section", value = section)
//            }
//
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clip(RoundedCornerShape(8.dp))
//                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
//                    .padding(12.dp)
//            ) {
//                Column {
//                    Text(
//                        text = "Subject",
//                        style = MaterialTheme.typography.labelSmall,
//                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
//                    )
//                    Text(
//                        text = subject,
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.Bold,
//                        color = MaterialTheme.colorScheme.onPrimaryContainer
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun SummaryItem(label: String, value: String) {
//    Column(horizontalAlignment = Alignment.Start) {
//        Text(
//            text = label,
//            style = MaterialTheme.typography.labelSmall,
//            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
//        )
//        Text(
//            text = value,
//            style = MaterialTheme.typography.titleMedium,
//            fontWeight = FontWeight.Bold,
//            color = MaterialTheme.colorScheme.onPrimaryContainer
//        )
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun TeacherBLEPreview() {
//    val navController = androidx.navigation.compose.rememberNavController()
//    MaterialTheme {
//        TeacherBLE(navController = navController)
//    }
//}
package com.example.attendance_android.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.attendance_android.NavRoutes
import com.example.attendance_android.ViewModels.TeacherClassViewModel
import com.example.attendance_android.data.DataStoreManager
import kotlinx.coroutines.launch
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

// Data class for selected section
data class SelectedSection(
    val year: String,
    val branch: String,
    val section: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherBLE(
    modifier: Modifier = Modifier,
    availableYears: List<String> = listOf("I", "II", "III", "IV"),
    availableBranches: List<String> = listOf("CSE", "ECE", "ME", "CE"),
    availableSections: List<String> = listOf("A", "B", "C"),
    availableSubjects: List<String> = listOf("DS", "OS", "DBMS"),
    fullname: String = "Professor",
    collegeName: String = "GVPCE",
    onStartClass: (year: String, branch: String, section: String) -> Unit = { _, _, _ -> },
    navController: NavController,
    viewModel: TeacherClassViewModel = viewModel()
) {
    val context = LocalContext.current
    val dataStore = remember { DataStoreManager(context) }
    val teacherEmail by dataStore.email.collectAsState(initial = "")

    val scope = rememberCoroutineScope()

    // Subject - selected once for all sections
    var globalSubject by remember { mutableStateOf("") }
    var subjectLocked by remember { mutableStateOf(false) }

    // List of selected sections
    val selectedSectionsList = remember { mutableStateListOf<SelectedSection>() }

    // Current form state
    var currentYear by remember { mutableStateOf("") }
    var currentBranch by remember { mutableStateOf("") }
    var currentSection by remember { mutableStateOf("") }

    // Dropdown expanded flags
    var yearExpanded by remember { mutableStateOf(false) }
    var branchExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }

    // Check if current form is filled
    val currentFormFilled = currentYear.isNotBlank() &&
            currentBranch.isNotBlank() &&
            currentSection.isNotBlank() &&
            globalSubject.isNotBlank()

    // Check if can start class (at least one section added)
    val canStartClass = selectedSectionsList.isNotEmpty() && globalSubject.isNotBlank()

    Scaffold(
        topBar = {
            HeaderWithProfile(
                fullname = fullname,
                collegeName = collegeName,
                navController = navController
            )
        },
        bottomBar = {
            FooterNavPrimary(
                onHome = { navController.navigate(NavRoutes.TeacherHome.route) { launchSingleTop = true } },
                onClasses = { /* optional nav */ },
                onSettings = { /* optional nav */ },
                selected = "HOME"
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            ClassSetupHeader()

            // Subject Selection Card (Always visible at top, locked after first section)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer

                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Subject for All Sections",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        if (subjectLocked) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Locked",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    EnhancedDropdownField(
                        value = globalSubject,
                        label = "Subject",
                        icon = Icons.Outlined.Class,
                        expanded = subjectExpanded,
                        onExpandedChange = { if (!subjectLocked) subjectExpanded = it },
                        items = availableSubjects,
                        onItemSelected = { index ->
                            if (!subjectLocked) {
                                globalSubject = availableSubjects[index]
                                subjectExpanded = false
                            }
                        },
                        enabled = !subjectLocked
                    )

                    if (globalSubject.isNotBlank() && !subjectLocked) {
                        Text(
                            text = "ℹ️ This subject will be used for all sections. It will be locked after adding the first section.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (subjectLocked) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✓ Subject locked for all sections",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )

                            TextButton(
                                onClick = {
                                    // Reset everything to change subject
                                    selectedSectionsList.clear()
                                    subjectLocked = false
                                    globalSubject = ""
                                    currentYear = ""
                                    currentBranch = ""
                                    currentSection = ""
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Change Subject")
                            }
                        }
                    }
                }
            }

            // Display selected sections as cards
            if (selectedSectionsList.isNotEmpty()) {
                Text(
                    text = "Selected Sections (${selectedSectionsList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                selectedSectionsList.forEachIndexed { index, selectedSection ->
                    SelectedSectionCard(
                        section = selectedSection,
                        subject = globalSubject,
                        onRemove = {
                            selectedSectionsList.removeAt(index)
                            // Unlock subject if all sections removed
                            if (selectedSectionsList.isEmpty()) {
                                subjectLocked = false
                            }
                        }
                    )
                }

                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            // Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedSectionsList.isEmpty()) "Add First Section" else "Add Another Section",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (selectedSectionsList.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Add section",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (globalSubject.isBlank()) {
                        // Show message to select subject first
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = "⚠️ Please select a subject first",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Year Dropdown
                    EnhancedDropdownField(
                        value = currentYear,
                        label = "Year",
                        icon = Icons.Outlined.CalendarToday,
                        expanded = yearExpanded,
                        onExpandedChange = { yearExpanded = it },
                        items = availableYears,
                        onItemSelected = { index ->
                            currentYear = availableYears[index]
                            yearExpanded = false
                        },
                        enabled = globalSubject.isNotBlank()
                    )

                    // Branch Dropdown
                    EnhancedDropdownField(
                        value = currentBranch,
                        label = "Branch",
                        icon = Icons.Outlined.School,
                        expanded = branchExpanded,
                        onExpandedChange = { branchExpanded = it },
                        items = availableBranches,
                        onItemSelected = { index ->
                            currentBranch = availableBranches[index]
                            branchExpanded = false
                        },
                        enabled = globalSubject.isNotBlank()
                    )

                    // Section Dropdown
                    EnhancedDropdownField(
                        value = currentSection,
                        label = "Section",
                        icon = Icons.Outlined.Group,
                        expanded = sectionExpanded,
                        onExpandedChange = { sectionExpanded = it },
                        items = availableSections,
                        onItemSelected = { index ->
                            currentSection = availableSections[index]
                            sectionExpanded = false
                        },
                        enabled = globalSubject.isNotBlank()
                    )

                    // Subject Dropdown


                    // Add Section Button
                    FilledTonalButton(
                        onClick = {
                            if (currentFormFilled) {
                                // Check for duplicates
                                val isDuplicate = selectedSectionsList.any {
                                    it.year == currentYear &&
                                            it.branch == currentBranch &&
                                            it.section == currentSection
                                }

                                if (!isDuplicate) {
                                    selectedSectionsList.add(
                                        SelectedSection(
                                            year = currentYear,
                                            branch = currentBranch,
                                            section = currentSection
                                        )
                                    )

                                    // Lock subject after first section
                                    if (!subjectLocked) {
                                        subjectLocked = true
                                    }

                                    // Clear form (except subject)
                                    currentYear = ""
                                    currentBranch = ""
                                    currentSection = ""
                                }
                            }
                        },
                        enabled = currentFormFilled && globalSubject.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (selectedSectionsList.isEmpty()) "Add Section" else "Add Another Section",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Start Class Button
            Button(
                onClick = {
                    if (canStartClass) {
                        // Create JSON array of sections
                        val sectionsJson = JSONArray()
                        selectedSectionsList.forEach { section ->
                            val sectionObj = JSONObject().apply {
                                put("year", section.year)
                                put("branch", section.branch)
                                put("section", section.section)
                            }
                            sectionsJson.put(sectionObj)
                        }

                        // Encode the JSON array and subject as strings
                        val encodedSections = URLEncoder.encode(sectionsJson.toString(), "utf-8")
                        val encodedSubject = URLEncoder.encode(globalSubject, "utf-8")
                        val eEmail = URLEncoder.encode(teacherEmail, "utf-8")

                        scope.launch {
                            navController.navigate("advertising/$encodedSections/$encodedSubject/$eEmail") {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                enabled = canStartClass,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (canStartClass) "Start Class Session" else "Add at least one section",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Helper Text
            AnimatedVisibility(visible = !canStartClass) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "⚠️ Please add at least one section to continue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun SelectedSectionCard(
    section: SelectedSection,
    subject: String,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Subject as header
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Class,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = subject,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Year, Branch, Section details
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DetailChip(label = "Year", value = section.year)
                    DetailChip(label = "Branch", value = section.branch)
                    DetailChip(label = "Section", value = section.section)
                }
            }

            // Remove button
            IconButton(
                onClick = onRemove,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Remove section"
                )
            }
        }
    }
}

@Composable
fun DetailChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun ClassSetupHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Start New Class",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Configure your class session details",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedDropdownField(
    value: String,
    label: String,
    icon: ImageVector,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<String>,
    onItemSelected: (Int) -> Unit,
    enabled: Boolean = true
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) onExpandedChange(!expanded) }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = value,
            onValueChange = { },
            label = { Text(label) },
            enabled = enabled,
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (value.isNotBlank() && enabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.38f)
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (item == value) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    onClick = { onItemSelected(index) },
                    colors = MenuDefaults.itemColors(
                        textColor = if (item == value)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}