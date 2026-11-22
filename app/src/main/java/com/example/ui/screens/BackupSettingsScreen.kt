package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.CloudSyncStatus
import com.example.model.Currency
import com.example.model.ReportPeriod
import com.example.model.ThemePreference
import com.example.ui.components.GoogleSignInButton
import com.example.ui.components.Icon3DBadge
import com.example.ui.components.signOutFromCloud
import com.example.util.PeriodFilter
import com.example.viewmodel.MainUiState

@Composable
fun BackupSettingsScreen(
    state: MainUiState,
    strings: AppStrings,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectCurrency: (Currency) -> Unit,
    onSelectTheme: (ThemePreference) -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onManualCloudSync: () -> Unit,
    onCloudSignedIn: () -> Unit,
    onCloudSignedOut: () -> Unit,
    onExportJsonBackup: () -> Unit,
    onImportJsonBackup: () -> Unit,
    onGeneratePdf: (ReportPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var authError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Optional Firebase Cloud Backup & Real-Time Sync Card
        item {
            Surface(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_cloud_vault),
                            contentDescription = strings.cloudBackupTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.cloudBackupTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.cloudBackupSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!state.isSignedIn) {
                        GoogleSignInButton(
                            buttonText = strings.signInWithGoogle,
                            onAuthSuccess = {
                                authError = null
                                onCloudSignedIn()
                            },
                            onAuthError = { msg -> authError = msg }
                        )
                        authError?.let { err ->
                            Text(
                                text = err,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.CloudDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = state.cloudUserName ?: state.cloudUserEmail ?: strings.cloudSyncedBadge,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val statusText = when (val st = state.syncStatus) {
                                        CloudSyncStatus.Syncing -> "..."
                                        is CloudSyncStatus.Synced -> "${strings.cloudSyncedBadge} • ${PeriodFilter.formatShortDate(st.timestampMillis, state.preferences.language)}"
                                        is CloudSyncStatus.Error -> st.message
                                        else -> strings.offlineReadyBadge
                                    }
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.autoCloudSync,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Switch(
                                checked = state.preferences.autoCloudSync,
                                onCheckedChange = onToggleAutoSync
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onManualCloudSync,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.syncNow, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = {
                                    signOutFromCloud(credentialManager, scope) {
                                        onCloudSignedOut()
                                    }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = strings.signOut, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.signOut)
                            }
                        }
                    }
                }
            }
        }

        // 2. Language Switcher (Bengali / Arabic RTL / English)
        item {
            SettingsCard(
                title = strings.languageTitle,
                icon = Icons.Rounded.Language,
                topColor = Color(0xFF38BDF8),
                bottomColor = Color(0xFF0369A1)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = lang == state.preferences.language,
                            onClick = { onSelectLanguage(lang) },
                            label = { Text(lang.nativeName) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lang_${lang.code}")
                        )
                    }
                }
            }
        }

        // 3. Active Currency Switcher (ONLY BDT & KWD)
        item {
            SettingsCard(
                title = strings.currencyTitle,
                icon = Icons.Rounded.Payments,
                topColor = Color(0xFF34D399),
                bottomColor = Color(0xFF047857)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Currency.entries.forEach { curr ->
                        FilterChip(
                            selected = curr == state.preferences.activeCurrency,
                            onClick = { onSelectCurrency(curr) },
                            label = { Text(strings.currencyFullName(curr), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("curr_${curr.code}")
                        )
                    }
                }
            }
        }

        // 4. Theme & Eye-Comfort Dark Mode Switcher
        item {
            SettingsCard(
                title = strings.themeTitle,
                icon = Icons.Rounded.DarkMode,
                topColor = Color(0xFFA855F7),
                bottomColor = Color(0xFF6B21A8)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemePreference.entries.forEach { pref ->
                        FilterChip(
                            selected = pref == state.preferences.themePreference,
                            onClick = { onSelectTheme(pref) },
                            label = { Text(strings.themeName(pref), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_${pref.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // 5. Offline JSON File Backup & Restore
        item {
            SettingsCard(
                title = strings.localBackupTitle,
                icon = Icons.Rounded.UploadFile,
                topColor = Color(0xFFFBBF24),
                bottomColor = Color(0xFFB45309)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportJsonBackup,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.exportJsonBackup, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onImportJsonBackup,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.importJsonBackup, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // 6. PDF Report Center
        item {
            SettingsCard(
                title = strings.exportPdfTitle,
                icon = Icons.Rounded.PictureAsPdf,
                topColor = Color(0xFFF43F5E),
                bottomColor = Color(0xFFBE123C)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(ReportPeriod.WEEKLY, ReportPeriod.MONTHLY, ReportPeriod.YEARLY).forEach { p ->
                        OutlinedButton(
                            onClick = { onGeneratePdf(p) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.periodName(p), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    topColor: Color,
    bottomColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon3DBadge(
                    icon = icon,
                    contentDescription = title,
                    topColor = topColor,
                    bottomColor = bottomColor,
                    size = 38.dp,
                    iconSize = 19.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}
