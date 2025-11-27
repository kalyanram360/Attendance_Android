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
import androidx.navigation.NavController
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
//    viewModel: OnboardingViewModel = viewModel(),
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
            // call caller callback so host can navigate away if needed
            onOnboardingComplete()
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(150.dp))

            // top logo placeholder (keeps same position for all pages)
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Logo",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // pager
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

            Spacer(modifier = Modifier.height(12.dp))

            // indicators
            HorizontalPagerIndicator(
                pagerState = pagerState,
                pageCount = pages,
                modifier = Modifier.padding(8.dp),
                activeColor = MaterialTheme.colorScheme.primary,
                inactiveColor = MaterialTheme.colorScheme.surfaceVariant,
                indicatorWidth = 12.dp,
                indicatorHeight = 12.dp,
                spacing = 8.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Next / Get Started button
            val isPageValid by remember(pagerState.currentPage, uiState) {
                derivedStateOf { viewModel.isPageValid(pagerState.currentPage) }
            }
            val context = LocalContext.current

            Button(
                onClick = {
                    if (pagerState.currentPage < pages - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        // Last page → call correct API based on role
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
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage < pages - 1) "Next" else "Get Started",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }



            Spacer(modifier = Modifier.height(8.dp))
        }

        // Optional Skip at top-end
        if (pagerState.currentPage < pages - 1) {
            TextButton(
                onClick = {
                    viewModel.completeOnboarding()
                    onOnboardingComplete()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Text(text = "Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ActivationCodePage(
    code: String,
    onCodeChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Activation Code",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Enter activation code",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = code,
            onValueChange = onCodeChanged,
            label = { Text("Activation code") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // explanatory / optional space
        Text(
            text = "You can get the activation code from your institute.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
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
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter Credentials",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            label = { Text("Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChanged,
            label = { Text("College-mail-id") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // small note
        Text(
            text = "We'll use this email to verify your account.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select your Role",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RoleCard(
                role = UserRole.STUDENT,
                selected = selectedRole == UserRole.STUDENT,
                onSelect = { onRoleSelected(UserRole.STUDENT) }
            )

            RoleCard(
                role = UserRole.TEACHER,
                selected = selectedRole == UserRole.TEACHER,
                onSelect = { onRoleSelected(UserRole.TEACHER) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Choose Student if you will scan tokens. Choose Teacher to broadcast tokens.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun RoleCard(role: UserRole, selected: Boolean, onSelect: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant
    val borderRadius = if (selected) 18.dp else 12.dp
    Card(
        modifier = Modifier
            .size(width = 140.dp, height = 120.dp)
            .clickable { onSelect() },
        shape = RoundedCornerShape(borderRadius),
        colors = CardDefaults.cardColors(containerColor = background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (role == UserRole.STUDENT) "S" else "T",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (role == UserRole.STUDENT) "Student" else "Teacher",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun InstituteSelectionPage(
    selectedInstitute: String,
    onInstituteSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Select Institute",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(24.dp))

        var expanded by remember { mutableStateOf(false) }
        val institutes = listOf("Gayatri Vidya Parishad", "Institute B", "Institute C", "MainCollege")

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { expanded = !expanded },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selectedInstitute.ifEmpty { "Select Institute" },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                institutes.forEach { inst ->
                    DropdownMenuItem(text = { Text(inst) }, onClick = {
                        onInstituteSelected(inst)
                        expanded = false
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "Pick your college/institute from the list. You can change it later.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/*
 Custom pager indicator (kept from your original code; slightly refactored)
*/
@Composable
fun HorizontalPagerIndicator(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier,
    activeColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    inactiveColor: androidx.compose.ui.graphics.Color = activeColor.copy(alpha = 0.5f),
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
                targetValue = if (pagerState.currentPage == iteration) indicatorWidth * 1.5f else indicatorWidth,
                label = "indicator width"
            )
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(color)
                    .height(indicatorHeight)
                    .width(width)
            )
        }
    }
}

// --- add this helper function near the bottom of the file (or in a suitable utils/VM) ---

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

// ---------- Save-to-DataStore helpers ----------

/**
 * Teacher: save ROLE, NAME, EMAIL. Do NOT set COLLEGE or RollNumber.
 * Returns true if teacher found and saved; false otherwise.
 */
suspend fun checkTeacherAndSave(
    email: String,
    dataStore: DataStoreManager
): Boolean = withContext(Dispatchers.IO) {
    val teacherJson = checkTeacher(email)
    if (teacherJson == null) return@withContext false

    val name = teacherJson.optString("fullname", "")
    val collegeEmail = teacherJson.optString("collegeEmail", email)
    val role = teacherJson.optString("role", "TEACHER")

    // save values
    try {
        dataStore.setName(name)
        dataStore.setEmail(collegeEmail)
        dataStore.setRole(role)
        // teacher: do NOT set COLLEGE or RollNumber (per your requirement)
        dataStore.setStudent(false)
        dataStore.setLoggedIn(true)
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext false
    }

    return@withContext true
}

/**
 * Student: save ROLE, NAME, EMAIL, RollNumber. Do NOT set COLLEGE.
 * Returns true if student found and saved; false otherwise.
 */
suspend fun checkStudentAndSave(
    email: String,
    dataStore: DataStoreManager
): Boolean = withContext(Dispatchers.IO) {
    val studentJson = checkStudent(email)
    if (studentJson == null) return@withContext false

    val name = studentJson.optString("fullname", "")
    val collegeEmail = studentJson.optString("collegeEmail", email)
    val role = studentJson.optString("role", "STUDENT")
    val rollNumber = studentJson.optString("roll_number", "")
    val branch = studentJson.optString("branch", "")
    val section = studentJson.optString("section", "")
    val year = studentJson.optString("year", "")


    // save values
    try {
        dataStore.setName(name)
        dataStore.setEmail(collegeEmail)
        dataStore.setRole(role)
        // Save roll number (your DataStore method is named `RollNumber`)
        dataStore.rollNumber(rollNumber)
        // do NOT set COLLEGE (per your requirement)
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
    // encode path segments
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

        // Primary: check "exists" boolean. Fallback: "success"
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



// --- Minimal CompletionPage UI (add to file if you want to display a simple success screen) ---
@Composable
fun CompletionPage(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Setup complete", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Your college was validated successfully.", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onClose) {
            Text("Continue")
        }
    }
}
