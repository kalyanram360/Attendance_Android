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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.statusBarsPadding
// ------------------ Header: Left profile initial + College name ------------------
@Composable
fun HeaderWithProfile(
    fullname: String,
    collegeName: String = "GVPCE",
    onProfileClick: () -> Unit = {}
) {
    val initial = remember(fullname) { fullname.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "P" }

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
                .padding(horizontal = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // circular initial
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = collegeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start
                )
            }

            // optional profile icon on the far end (small)
            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ------------------ Header: Centered college title variant ------------------
@Composable
fun HeaderCenteredTitle(
    collegeName: String = "GVPCE",
    showProfileIcon: Boolean = true,
    onProfileClick: () -> Unit = {}
) {
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = collegeName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (showProfileIcon) {
                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = "Profile")
                }
            }
        }
    }
}

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

            FooterItem(
                label = "Settings",
                selected = selected == "SETTINGS",
                icon = Icons.Default.Settings,
                onClick = onSettings
            )
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
@Composable
fun FooterNavCompact(
    onHome: () -> Unit = {},
    onClasses: () -> Unit = {},
    onSettings: () -> Unit = {},
    selectedIndex: Int = 0
) {
    Surface(
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onHome) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = if (selectedIndex == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onClasses) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Classes",
                    tint = if (selectedIndex == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (selectedIndex == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ------------------ Usage preview helpers ------------------
@Preview(showBackground = true)
@Composable
fun HeaderFooterPreview() {
    Column(Modifier.fillMaxSize()) {
        HeaderWithProfile(fullname = "Kalyan", collegeName = "GVPCE")
        Box(Modifier.weight(1f)) { /* content */ }
        FooterNavPrimary()
    }
}
