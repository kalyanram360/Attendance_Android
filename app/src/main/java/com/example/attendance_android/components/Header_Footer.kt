package com.example.attendance_android.components

/*
  HeaderFooter_Composables.kt
  Contains four Jetpack Compose composables (two header variants, two footer variants)
  - HeaderWithProfile: left circular initial (first letter of name) + college name on right
  - HeaderCenteredTitle: centered college name with small profile icon on the right
  - FooterNavPrimary: bottom nav with icons + labels (Home, Classes, Settings)
  - FooterNavCompact: bottom nav with icons only

  NOTE: Your uploaded reference image is available at:
  /mnt/data/WhatsApp Image 2025-11-22 at 9.15.04 PM.jpeg
  (use this path if you want to display the sketch in debug or preview)
*/


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.attendance_android.NavRoutes
import androidx.compose.ui.platform.LocalContext

// ------------------ Header: Left profile initial + College name ------------------

@Composable
fun HeaderWithProfile(
    navController:  NavController? = null,
    fullname: String,
    collegeName: String = "GVPCE"
) {
    val initial = remember(fullname) {
        if (fullname.isBlank()) {
            collegeName.firstOrNull()?.uppercaseChar()?.toString() ?: "G"
        } else {
            fullname.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "P"
        }
    }

    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {

            // Left — College Name
            Text(
                text = collegeName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Right — Profile Circle (navigates automatically)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {
                        navController?.navigate(NavRoutes.profile.route) {
                            launchSingleTop = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}




// ------------------ Header: Centered college title variant ------------------

// ------------------ Footer: Primary with labels ------------------
@Composable
fun FooterNavPrimary(
    onHome: () -> Unit = {},
    onClasses: () -> Unit = {},
    onSettings: () -> Unit = {},
    selected: String = "HOME"
) {
    Surface(
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(72.dp)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FooterItem(
                label = "Home",
                selected = selected == "HOME",
                icon = Icons.Default.Home,
                onClick = onHome
            )

            FooterItem(
                label = "Classes",
                selected = selected == "CLASSES",
                icon = Icons.Default.Person, // replace with a classes icon if available
                onClick = onClasses
            )

            // FooterItem(
            //     label = "Settings",
            //     selected = selected == "SETTINGS",
            //     icon = Icons.Default.Settings,
            //     onClick = onSettings
            // )
        }
    }
}

@Composable
private fun FooterItem(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(26.dp),
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ------------------ Footer: Compact (icons only) ------------------

// ------------------ Usage preview helpers ------------------
@Preview(showBackground = true)
@Composable
fun HeaderFooterPreview() {
    Column(Modifier.fillMaxSize()) {
        HeaderWithProfile(navController = rememberNavController(), fullname = "Kalyan", collegeName = "GVPCE")
        Box(Modifier.weight(1f)) { /* content */ }
        FooterNavPrimary()
    }
}
