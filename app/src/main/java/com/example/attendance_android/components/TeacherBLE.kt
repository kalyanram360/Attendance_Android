package com.example.attendance_android.components


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.attendance_android.NavRoutes

/**
 * Teacher Home screen: select Year, Branch, Section and Start Class
 *
 * @param availableYears list of years to choose from (e.g. "I", "II", "III", "IV")
 * @param availableBranches list of branches (e.g. "CSE", "ECE")
 * @param availableSections list of sections (e.g. "A", "B", "C")
 * @param onStartClass callback with selected values to actually start the class
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherBLE(
    modifier: Modifier = Modifier,
    availableYears: List<String> = listOf("I", "II", "III", "IV"),
    availableBranches: List<String> = listOf("CSE", "ECE", "ME", "CE"),
    availableSections: List<String> = listOf("A", "B", "C"),
    initialYear: String? = null,
    initialBranch: String? = null,
    initialSection: String? = null,
    fullname: String = "Professor",
    collegeName: String = "GVPCE",
    onStartClass: (year: String, branch: String, section: String) -> Unit = { _, _, _ -> },
    navController: NavController
) {
    // state for selected values
    var selectedYear by remember { mutableStateOf(initialYear ?: "") }
    var selectedBranch by remember { mutableStateOf(initialBranch ?: "") }
    var selectedSection by remember { mutableStateOf(initialSection ?: "") }

    // dropdown expanded flags
    var yearExpanded by remember { mutableStateOf(false) }
    var branchExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }

    // simple derived validation to enable Start button
    val canStart = selectedYear.isNotBlank() && selectedBranch.isNotBlank() && selectedSection.isNotBlank()

    Scaffold(
        topBar = {
            HeaderWithProfile(fullname = fullname, collegeName = collegeName)
        },
        bottomBar = {
            FooterNavPrimary(
                onHome = { navController.navigate(NavRoutes.TeacherHome.route) { launchSingleTop = true } },
                onClasses = { /* optional nav */ },
                onSettings = { /* optional nav */ },
                selected = "CLASSES"
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
        Text(text = "Start a Class", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(18.dp))

        // Year dropdown
        ExposedDropdownMenuBox(
            expanded = yearExpanded,
            onExpandedChange = { yearExpanded = !yearExpanded }
        ) {
            TextField(
                readOnly = true,
                value = selectedYear,
                onValueChange = { },
                label = { Text("Year") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor() // anchor for dropdown
            )
            ExposedDropdownMenu(
                expanded = yearExpanded,
                onDismissRequest = { yearExpanded = false }
            ) {
                availableYears.forEach { year ->
                    DropdownMenuItem(
                        text = { Text(year) },
                        onClick = {
                            selectedYear = year
                            yearExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Branch dropdown
        ExposedDropdownMenuBox(
            expanded = branchExpanded,
            onExpandedChange = { branchExpanded = !branchExpanded }
        ) {
            TextField(
                readOnly = true,
                value = selectedBranch,
                onValueChange = { },
                label = { Text("Branch") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = branchExpanded,
                onDismissRequest = { branchExpanded = false }
            ) {
                availableBranches.forEach { branch ->
                    DropdownMenuItem(
                        text = { Text(branch) },
                        onClick = {
                            selectedBranch = branch
                            branchExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section dropdown
        ExposedDropdownMenuBox(
            expanded = sectionExpanded,
            onExpandedChange = { sectionExpanded = !sectionExpanded }
        ) {
            TextField(
                readOnly = true,
                value = selectedSection,
                onValueChange = { },
                label = { Text("Section") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = sectionExpanded,
                onDismissRequest = { sectionExpanded = false }
            ) {
                availableSections.forEach { section ->
                    DropdownMenuItem(
                        text = { Text(section) },
                        onClick = {
                            selectedSection = section
                            sectionExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Optional quick input: number of students or other details (example)
        OutlinedTextField(
            value = "",
            onValueChange = { /* optional - keep for future */ },
            label = { Text("Optional: Class Title / Notes") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Start Class Button aligned to center horizontally
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(
                onClick = {
                    if (canStart) {
                        onStartClass(selectedYear, selectedBranch, selectedSection)
                    }
                },
                enabled = canStart,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(52.dp)
            ) {
                Text(text = "Start Class")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Friendly hint / feedback
        if (!canStart) {
            Text(
                text = "Select Year, Branch and Section to start the class.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Text(
                text = "Starting for: $selectedYear / $selectedBranch / $selectedSection",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        }
    }
}

//
//
//@Preview(showBackground = true)
//@Composable
//fun TeacherHomePreview() {
//    TeacherBLE(
//        availableYears = listOf("I", "II", "III", "IV"),
//        availableBranches = listOf("CSE", "ECE", "ME"),
//        availableSections = listOf("A", "B"),
//        onStartClass = { year, branch, section ->
//            // preview callback - no-op
//        },
//        navController = N
//    )
//}
