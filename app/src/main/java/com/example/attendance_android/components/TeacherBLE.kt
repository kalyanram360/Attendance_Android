//package com.example.attendance_android.components
//
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavController
//import com.example.attendance_android.NavRoutes
//import com.example.attendance_android.ViewModels.TeacherClassViewModel
//import java.net.URLEncoder
//import androidx.compose.runtime.rememberCoroutineScope
//import kotlinx.coroutines.launch
//import androidx.datastore.dataStore
//import androidx.compose.ui.platform.LocalContext
//import com.example.attendance_android.data.DataStoreManager
//import androidx.compose.runtime.collectAsState
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.runtime.collectAsState
///**
// * Teacher Home screen: select Year, Branch, Section and Start Class
// *
// * @param availableYears list of years to choose from (e.g. "I", "II", "III", "IV")
// * @param availableBranches list of branches (e.g. "CSE", "ECE")
// * @param availableSections list of sections (e.g. "A", "B", "C")
// * @param onStartClass callback with selected values to actually start the class
// */
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
//
//    // Collect email from DataStore
//    val teacherEmail by dataStore.email.collectAsState(initial = "")
//
//    // Collect state from ViewModel
//    val yearValue by viewModel.year.collectAsState()
//    val branchValue by viewModel.branch.collectAsState()
//    val sectionValue by viewModel.section.collectAsState()
//    val subjectValue by viewModel.subject.collectAsState()
//
//
//    // Convert year Int to String for display
//    val selectedYear = if (yearValue > 0) availableYears.getOrNull(yearValue - 1) ?: "" else ""
//    val selectedBranch = branchValue
//    val selectedSection = sectionValue
//    val selectedSubject = subjectValue
//
//    val scope = rememberCoroutineScope()
//
//    // dropdown expanded flags
//    var yearExpanded by remember { mutableStateOf(false) }
//    var branchExpanded by remember { mutableStateOf(false) }
//    var sectionExpanded by remember { mutableStateOf(false) }
//    var subjectExpanded by remember { mutableStateOf(false) }
//
//
//    // simple derived validation to enable Start button
//    val canStart = selectedYear.isNotBlank() && selectedBranch.isNotBlank() && selectedSection.isNotBlank()
//
//    Scaffold(
//        topBar = {
//            HeaderWithProfile(fullname = fullname, collegeName = collegeName, navController = navController)
//        },
//        bottomBar = {
//            FooterNavPrimary(
//                onHome = { navController.navigate(NavRoutes.TeacherHome.route) { launchSingleTop = true } },
//                onClasses = { /* optional nav */ },
//                onSettings = { /* optional nav */ },
//                selected = "CLASSES"
//            )
//        },
//        containerColor = MaterialTheme.colorScheme.background
//    ) { innerPadding ->
//        Column(
//            modifier = modifier
//                .fillMaxSize()
//                .padding(innerPadding)
//                .padding(20.dp),
//            verticalArrangement = Arrangement.Top,
//            horizontalAlignment = Alignment.Start
//        ) {
//            Text(text = "Start a Class", style = MaterialTheme.typography.headlineSmall)
//            Spacer(modifier = Modifier.height(18.dp))
//
//
//            // Year dropdown
//
//            ExposedDropdownMenuBox(
//                expanded = yearExpanded,
//                onExpandedChange = { yearExpanded = !yearExpanded }
//            ) {
//                TextField(
//                    readOnly = true,
//                    value = selectedYear,
//                    onValueChange = { },
//                    label = { Text("Year") },
//                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .menuAnchor() // anchor for dropdown
//                )
//                ExposedDropdownMenu(
//                    expanded = yearExpanded,
//                    onDismissRequest = { yearExpanded = false }
//                ) {
//                    availableYears.forEachIndexed { index, year ->
//                        DropdownMenuItem(
//                            text = { Text(year) },
//                            onClick = {
//                                viewModel.updateYear(index + 1)
//                                yearExpanded = false
//                            }
//                        )
//                    }
//                }
//            }
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            // Branch dropdown
//            ExposedDropdownMenuBox(
//                expanded = branchExpanded,
//                onExpandedChange = { branchExpanded = !branchExpanded }
//            ) {
//                TextField(
//                    readOnly = true,
//                    value = selectedBranch,
//                    onValueChange = { },
//                    label = { Text("Branch") },
//                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchExpanded) },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .menuAnchor()
//                )
//                ExposedDropdownMenu(
//                    expanded = branchExpanded,
//                    onDismissRequest = { branchExpanded = false }
//                ) {
//                    availableBranches.forEach { branch ->
//                        DropdownMenuItem(
//                            text = { Text(branch) },
//                            onClick = {
//                                viewModel.updateBranch(branch)
//                                branchExpanded = false
//                            }
//                        )
//                    }
//                }
//            }
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            // Section dropdown
//            ExposedDropdownMenuBox(
//                expanded = sectionExpanded,
//                onExpandedChange = { sectionExpanded = !sectionExpanded }
//            ) {
//                TextField(
//                    readOnly = true,
//                    value = selectedSection,
//                    onValueChange = { },
//                    label = { Text("Section") },
//                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionExpanded) },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .menuAnchor()
//                )
//                ExposedDropdownMenu(
//                    expanded = sectionExpanded,
//                    onDismissRequest = { sectionExpanded = false }
//                ) {
//                    availableSections.forEach { section ->
//                        DropdownMenuItem(
//                            text = { Text(section) },
//                            onClick = {
//                                viewModel.updateSection(section)
//                                sectionExpanded = false
//                            }
//                        )
//                    }
//                }
//            }
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            //subject dropdown
//            ExposedDropdownMenuBox(
//                expanded = subjectExpanded,
//                onExpandedChange = { subjectExpanded = !subjectExpanded }
//            ) {
//                TextField(
//                    readOnly = true,
//                    value = selectedSubject,
//                    onValueChange = { },
//                    label = { Text("Subject") },
//                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .menuAnchor()
//                )
//                ExposedDropdownMenu(
//                    expanded = subjectExpanded,
//                    onDismissRequest = { subjectExpanded = false }
//                ) {
//                    availableSubjects.forEach { subject ->
//                        DropdownMenuItem(
//                            text = { Text(subject) },
//                            onClick = {
//                                viewModel.updateSubject(subject)
//                                subjectExpanded = false
//                            }
//                        )
//                    }
//                }
//            }
//
//            Spacer(modifier = Modifier.height(24.dp))
//
//            // Optional quick input: number of students or other details (example)
//            OutlinedTextField(
//                value = "",
//                onValueChange = { /* optional - keep for future */ },
//                label = { Text("Optional: Class Title / Notes") },
//                modifier = Modifier.fillMaxWidth(),
//                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
//                singleLine = true
//            )
//
//            Spacer(modifier = Modifier.height(24.dp))
//            // Start Class Button aligned to center horizontally
//            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
//                Button(
//                    onClick = {
//                        if (canStart) {
//                            // encode path segments to be safe for navigation
//                            val eYear = try { URLEncoder.encode(selectedYear, "utf-8") } catch (_: Exception) { selectedYear }
//                            val eBranch = try { URLEncoder.encode(selectedBranch, "utf-8") } catch (_: Exception) { selectedBranch }
//                            val eSection = try { URLEncoder.encode(selectedSection, "utf-8") } catch (_: Exception) { selectedSection }
//                            val eSubject = try { URLEncoder.encode(selectedSubject, "utf-8") } catch (_: Exception) { selectedSubject }
//                            val eEmail = try { URLEncoder.encode(teacherEmail, "utf-8") } catch (_: Exception) { teacherEmail }
//                            // navigate to Advertising route with parameters
//                            // route example: "advertising/{year}/{branch}/{section}"
//                            // Make sure you registered this route in NavHost (see previous message)
//                            scope.launch {
//                                navController.navigate("advertising/$eYear/$eBranch/$eSection/$eSubject/$eEmail") {
//                                    launchSingleTop = true
//                                    restoreState = true
//                                }
//                            }
//                        }
//                    },
//                    enabled = canStart,
//                    modifier = Modifier
//                        .fillMaxWidth(0.6f)
//                        .height(52.dp)
//                ) {
//                    Text(text = "Start Class")
//                }
//            }
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            // Friendly hint / feedback
//            if (!canStart) {
//                Text(
//                    text = "Select Year, Branch and Section to start the class.",
//                    style = MaterialTheme.typography.bodySmall,
//                    modifier = Modifier.padding(top = 8.dp)
//                )
//            } else {
//                Text(
//                    text = "Starting for: $selectedYear / $selectedBranch / $selectedSection",
//                    style = MaterialTheme.typography.bodySmall,
//                    modifier = Modifier.padding(top = 8.dp)
//                )
//            }
//        }
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun TeacherBLEPreview() {
//    val navController = androidx.navigation.compose.rememberNavController()
//    TeacherBLE(navController = navController)
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
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.attendance_android.NavRoutes
import com.example.attendance_android.ViewModels.TeacherClassViewModel
import com.example.attendance_android.data.DataStoreManager
import kotlinx.coroutines.launch
import java.net.URLEncoder

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

    // Collect state from ViewModel
    val yearValue by viewModel.year.collectAsState()
    val branchValue by viewModel.branch.collectAsState()
    val sectionValue by viewModel.section.collectAsState()
    val subjectValue by viewModel.subject.collectAsState()

    val selectedYear = if (yearValue > 0) availableYears.getOrNull(yearValue - 1) ?: "" else ""
    val selectedBranch = branchValue
    val selectedSection = sectionValue
    val selectedSubject = subjectValue

    val scope = rememberCoroutineScope()
    var classNotes by remember { mutableStateOf("") }

    // Dropdown expanded flags
    var yearExpanded by remember { mutableStateOf(false) }
    var branchExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }

    val canStart = selectedYear.isNotBlank() &&
            selectedBranch.isNotBlank() &&
            selectedSection.isNotBlank() &&
            selectedSubject.isNotBlank()

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
                selected = "CLASSES"
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
                    Text(
                        text = "Class Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Year Dropdown
                    EnhancedDropdownField(
                        value = selectedYear,
                        label = "Year",
                        icon = Icons.Outlined.CalendarToday,
                        expanded = yearExpanded,
                        onExpandedChange = { yearExpanded = it },
                        items = availableYears,
                        onItemSelected = { index ->
                            viewModel.updateYear(index + 1)
                            yearExpanded = false
                        }
                    )

                    // Branch Dropdown
                    EnhancedDropdownField(
                        value = selectedBranch,
                        label = "Branch",
                        icon = Icons.Outlined.School,
                        expanded = branchExpanded,
                        onExpandedChange = { branchExpanded = it },
                        items = availableBranches,
                        onItemSelected = { index ->
                            viewModel.updateBranch(availableBranches[index])
                            branchExpanded = false
                        }
                    )

                    // Section Dropdown
                    EnhancedDropdownField(
                        value = selectedSection,
                        label = "Section",
                        icon = Icons.Outlined.Group,
                        expanded = sectionExpanded,
                        onExpandedChange = { sectionExpanded = it },
                        items = availableSections,
                        onItemSelected = { index ->
                            viewModel.updateSection(availableSections[index])
                            sectionExpanded = false
                        }
                    )

                    // Subject Dropdown
                    EnhancedDropdownField(
                        value = selectedSubject,
                        label = "Subject",
                        icon = Icons.Outlined.Class,
                        expanded = subjectExpanded,
                        onExpandedChange = { subjectExpanded = it },
                        items = availableSubjects,
                        onItemSelected = { index ->
                            viewModel.updateSubject(availableSubjects[index])
                            subjectExpanded = false
                        }
                    )

                    // Optional Notes Field
                    OutlinedTextField(
                        value = classNotes,
                        onValueChange = { classNotes = it },
                        label = { Text("Class Notes (Optional)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        singleLine = true
                    )
                }
            }

            // Selection Summary Card
            AnimatedVisibility(
                visible = canStart,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SelectionSummaryCard(
                    year = selectedYear,
                    branch = selectedBranch,
                    section = selectedSection,
                    subject = selectedSubject
                )
            }

            // Start Button
            Button(
                onClick = {
                    if (canStart) {
                        val eYear = URLEncoder.encode(selectedYear, "utf-8")
                        val eBranch = URLEncoder.encode(selectedBranch, "utf-8")
                        val eSection = URLEncoder.encode(selectedSection, "utf-8")
                        val eSubject = URLEncoder.encode(selectedSubject, "utf-8")
                        val eEmail = URLEncoder.encode(teacherEmail, "utf-8")

                        scope.launch {
                            navController.navigate("advertising/$eYear/$eBranch/$eSection/$eSubject/$eEmail") {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                enabled = canStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = if (canStart) "Start Class Session" else "Fill All Required Fields",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Helper Text
            AnimatedVisibility(visible = !canStart) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "⚠️ Please select Year, Branch, Section, and Subject to continue",
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
    onItemSelected: (Int) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { onExpandedChange(!expanded) }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = value,
            onValueChange = { },
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (value.isNotBlank())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
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
                focusedLabelColor = MaterialTheme.colorScheme.primary
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

@Composable
fun SelectionSummaryCard(
    year: String,
    branch: String,
    section: String,
    subject: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    text = "✓ Ready to Start",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Divider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(label = "Year", value = year)
                SummaryItem(label = "Branch", value = branch)
                SummaryItem(label = "Section", value = section)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Subject",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = subject,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TeacherBLEPreview() {
    val navController = androidx.navigation.compose.rememberNavController()
    MaterialTheme {
        TeacherBLE(navController = navController)
    }
}