

package com.example.attendance_android.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendance_android.ViewModels.OnboardingViewModel
import com.example.attendance_android.ViewModels.UserRole
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarResult
import com.example.attendance_android.data.DataStoreManager
import com.example.attendance_android.ViewModels.OnboardingViewModelFactory
import java.net.URLEncoder

import java.io.BufferedReader
import java.io.InputStreamReader
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import android.util.Log
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    onOnboardingComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val dataStore = remember { DataStoreManager(context) }
    val viewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModelFactory(dataStore)
    )

    val pages = 4
    val pagerState = rememberPagerState { pages }
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isOnboardingComplete) {
        CompletionPage {
            onOnboardingComplete()
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding() // Avoid status bar overlap
            .navigationBarsPadding() // Avoid navigation bar overlap
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // App Logo/Brand
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Attendance",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> InstituteSelectionPage(
                        selectedInstitute = uiState.selectedInstitute,
                        onInstituteSelected = { viewModel.updateInstitute(it) }
                    )
                    1 -> RoleSelectionPage(
                        selectedRole = uiState.selectedRole,
                        onRoleSelected = { viewModel.updateRole(it) }
                    )
                    2 -> CredentialsPage(
                        name = uiState.name,
                        email = uiState.email,
                        onNameChanged = { viewModel.updateName(it) },
                        onEmailChanged = { viewModel.updateEmail(it) }
                    )
                    3 -> ActivationCodePage(
                        code = uiState.activationCode,
                        onCodeChanged = { viewModel.updateActivationCode(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Page Indicators
            HorizontalPagerIndicator(
                pagerState = pagerState,
                pageCount = pages,
                modifier = Modifier.padding(vertical = 8.dp),
                activeColor = MaterialTheme.colorScheme.primary,
                inactiveColor = MaterialTheme.colorScheme.surfaceVariant,
                indicatorWidth = 10.dp,
                indicatorHeight = 10.dp,
                spacing = 8.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            val isPageValid by remember(pagerState.currentPage, uiState) {
                derivedStateOf { viewModel.isPageValid(pagerState.currentPage) }
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < pages - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        scope.launch {
                            val success = try {
                                if (uiState.selectedRole == UserRole.STUDENT) {
                                    checkStudentAndSave(
                                        email = uiState.email,
                                        dataStore = dataStore
                                    )
                                } else {
                                    checkTeacherAndSave(
                                        email = uiState.email,
                                        dataStore = dataStore
                                    )
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                false
                            }

                            if (success) {
                                viewModel.completeOnboarding()
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "No account found with this email",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                },
                enabled = isPageValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Text(
                    text = if (pagerState.currentPage < pages - 1) "Next" else "Get Started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Skip Button
//        if (pagerState.currentPage < pages - 1) {
//            TextButton(
//                onClick = {
//                    viewModel.completeOnboarding()
//                    onOnboardingComplete()
//                },
//                modifier = Modifier
//                    .align(Alignment.TopEnd)
//                    .padding(16.dp)
//            ) {
//                Text(
//                    text = "Skip",
//                    color = MaterialTheme.colorScheme.primary,
//                    fontWeight = FontWeight.Medium
//                )
//            }
//        }
    }
}
// Individual Page Composables

@Composable
private fun ActivationCodePage(
    code: String,
    onCodeChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Almost There!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your activation code to complete setup",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = code,
            onValueChange = onCodeChanged,
            label = { Text("Activation Code") },
            placeholder = { Text("XXXXXX") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Get your activation code from your institute administrator",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun CredentialsPage(
    name: String,
    email: String,
    onNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your Information",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We'll use this to verify your account",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            label = { Text("Full Name") },
            placeholder = { Text("John Doe") },
            leadingIcon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChanged,
            label = { Text("College Email") },
            placeholder = { Text("you@college.edu") },
            leadingIcon = {
                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun RoleSelectionPage(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Choose Your Role",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select how you'll be using the app",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RoleCard(
                role = UserRole.STUDENT,
                selected = selectedRole == UserRole.STUDENT,
                onSelect = { onRoleSelected(UserRole.STUDENT) },
                modifier = Modifier.weight(1f)
            )

            RoleCard(
                role = UserRole.TEACHER,
                selected = selectedRole == UserRole.TEACHER,
                onSelect = { onRoleSelected(UserRole.TEACHER) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ) {
            Text(
                text = "Students scan attendance tokens • Teachers broadcast tokens",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun RoleCard(
    role: UserRole,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Surface(
        onClick = onSelect,
        modifier = modifier.height(160.dp),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor
        ),
        shadowElevation = if (selected) 8.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = if (role == UserRole.STUDENT) "S" else "T",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (role == UserRole.STUDENT) "Student" else "Teacher",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstituteSelectionPage(
    selectedInstitute: String,
    onInstituteSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Select Your Institute",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose your college from the list below",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        var expanded by remember { mutableStateOf(false) }
        val institutes = listOf(
            "Gayatri Vidya Parishad College of Engineering (GVPCE)",
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedInstitute,
                onValueChange = {},
                readOnly = true,
                label = { Text("Institute") },
                placeholder = { Text("Select your institute") },
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                institutes.forEach { institute ->
                    DropdownMenuItem(
                        text = { Text(institute) },
                        onClick = {
                            onInstituteSelected(institute)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ) {
            Text(
                text = "💡 You can change your institute later in settings",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun HorizontalPagerIndicator(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier,
    activeColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    inactiveColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant,
    indicatorWidth: Dp = 8.dp,
    indicatorHeight: Dp = indicatorWidth,
    spacing: Dp = indicatorWidth,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        repeat(pageCount) { iteration ->
            val color = if (pagerState.currentPage == iteration) activeColor else inactiveColor
            val width by animateDpAsState(
                targetValue = if (pagerState.currentPage == iteration) indicatorWidth * 2f else indicatorWidth,
                label = "indicator width"
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(indicatorHeight / 2))
                    .background(color)
                    .height(indicatorHeight)
                    .width(width)
            )
        }
    }
}

@Composable
fun CompletionPage(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "All Set!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your account has been verified successfully",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Text(
                    text = "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// API helper functions remain the same
suspend fun checkTeacher(collegeEmail: String): JSONObject? = withContext(Dispatchers.IO) {
    val base = "https://attendance-app-backend-zr4c.onrender.com"
    val eEmail = try { URLEncoder.encode(collegeEmail.trim().lowercase(), "utf-8") } catch (e: Exception) { collegeEmail.trim().lowercase() }
    val endpoint = "$base/api/teacher/check/$eEmail"
    val url = URL(endpoint)
    val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 10_000
        readTimeout = 10_000
    }

    try {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseText = stream.bufferedReader().use { it.readText() }
        val responseJson = JSONObject(responseText)
        val exists = responseJson.optBoolean("exists", false)
        if (exists && responseJson.has("data") && !responseJson.isNull("data")) {
            return@withContext responseJson.getJSONObject("data")
        }
        return@withContext null
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    } finally {
        conn.disconnect()
    }
}

suspend fun checkStudent(collegeEmail: String): JSONObject? = withContext(Dispatchers.IO) {
    val base = "https://attendance-app-backend-zr4c.onrender.com"
    val eEmail = try { URLEncoder.encode(collegeEmail.trim().lowercase(), "utf-8") } catch (e: Exception) { collegeEmail.trim().lowercase() }
    val endpoint = "$base/api/student/check/$eEmail"
    val url = URL(endpoint)
    val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 10_000
        readTimeout = 10_000
    }

    try {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseText = stream.bufferedReader().use { it.readText() }
        val responseJson = JSONObject(responseText)
        val exists = responseJson.optBoolean("exists", false)
        if (exists && responseJson.has("data") && !responseJson.isNull("data")) {
            return@withContext responseJson.getJSONObject("data")
        }
        return@withContext null
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    } finally {
        conn.disconnect()
    }
}

suspend fun checkTeacherAndSave(
    email: String,
    dataStore: DataStoreManager
): Boolean = withContext(Dispatchers.IO) {
    val teacherJson = checkTeacher(email)
    if (teacherJson == null) return@withContext false

    val name = teacherJson.optString("name", "")
    val collegeEmail = teacherJson.optString("collegeEmail", email)
    val role = teacherJson.optString("role", "TEACHER")

    Log.d("checkTeacherAndSave", "Backend response: $teacherJson")
    Log.d("checkTeacherAndSave", "Extracted name: '$name'")

    try {
        dataStore.setName(name)
        dataStore.setEmail(collegeEmail)
        dataStore.setRole(role)
        Log.d("checkTeacherAndSave", "Successfully saved to DataStore - Name: '$name'")
        dataStore.setStudent(false)
        dataStore.setLoggedIn(true)
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("checkTeacherAndSave", "Failed to save to DataStore: ${e.message}")
        return@withContext false
    }

    return@withContext true
}

suspend fun checkStudentAndSave(
    email: String,
    dataStore: DataStoreManager
): Boolean = withContext(Dispatchers.IO) {
    val studentJson = checkStudent(email)
    if (studentJson == null) return@withContext false

    val name = studentJson.optString("name", "")
    val collegeEmail = studentJson.optString("collegeEmail", email)
    val role = studentJson.optString("role", "STUDENT")
    val rollNumber = studentJson.optString("rollno", "")
    val branch = studentJson.optString("branch", "")
    val section = studentJson.optString("section", "")
    val year = studentJson.optString("year", "")

    try {
        dataStore.setName(name)
        dataStore.setEmail(collegeEmail)
        dataStore.setRole(role)
        dataStore.setrollNumber(rollNumber)
        dataStore.setStudent(true)
        dataStore.setLoggedIn(true)
        dataStore.setBranch(branch)
        dataStore.setSection(section)
        dataStore.setYear(year)
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext false
    }

    return@withContext true
}

suspend fun checkCollegeNetwork(
    collegeEmail: String,
    collegeName: String,
    activationCode: String
): Boolean = withContext(Dispatchers.IO) {
    val base = "https://attendance-app-backend-zr4c.onrender.com"
    val eEmail = URLEncoder.encode(collegeEmail, "utf-8")
    val eName = URLEncoder.encode(collegeName, "utf-8")
    val eCode = URLEncoder.encode(activationCode, "utf-8")

    val endpoint = "$base/api/check/$eEmail/$eName/$eCode"
    val url = URL(endpoint)
    val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 10_000
        readTimeout = 10_000
    }

    try {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseText = stream.bufferedReader().use { it.readText() }
        val responseJson = JSONObject(responseText)

        if (responseJson.has("exists")) {
            return@withContext responseJson.optBoolean("exists", false)
        }
        if (responseJson.has("success")) {
            return@withContext responseJson.optBoolean("success", false)
        }
        return@withContext false
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext false
    } finally {
        conn.disconnect()
    }
}

@Preview
@Composable
fun CompletionPagePreview() {
    CompletionPage { }
}