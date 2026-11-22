package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.FinanceRepository
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.remote.CloudSyncRepository
import com.example.localization.stringsFor
import com.example.model.Currency
import com.example.model.ReportPeriod
import com.example.model.TransactionItem
import com.example.ui.components.AddEditTransactionSheet
import com.example.ui.components.Icon3DBadge
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BackupSettingsScreen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.HisabProTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.UiEvent
import kotlinx.coroutines.flow.collectLatest

private enum class AppRoute(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", Icons.Rounded.Home, Icons.Outlined.Home),
    ANALYTICS("analytics", Icons.Rounded.BarChart, Icons.Outlined.BarChart),
    BUDGET("budget", Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    BACKUP("backup", Icons.Rounded.CloudSync, Icons.Outlined.CloudSync)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val db = AppDatabase.getInstance(app)
                        val prefs = UserPreferencesRepository(app)
                        val cloud = CloudSyncRepository(app)
                        val repo = FinanceRepository(db.appDao(), prefs, cloud)
                        MainViewModel(repo)
                    }
                }
            )
            HisabProApp(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HisabProApp(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.preferences
    val strings = remember(prefs.language) { stringsFor(prefs.language) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppRoute.HOME.route

    var showEntrySheet by remember { mutableStateOf(false) }
    var entryAutoMode by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionItem?>(null) }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportJsonBackup(context, it) }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importJsonBackup(context, it) }
    }

    LaunchedEffect(viewModel, strings) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
                is UiEvent.PdfGenerated -> {
                    val res = snackbarHostState.showSnackbar(
                        message = "${strings.pdfReadyMsg} (${event.result.fileName})",
                        actionLabel = strings.sharePdf,
                        duration = SnackbarDuration.Long
                    )
                    if (res == SnackbarResult.ActionPerformed) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, event.result.shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, strings.sharePdf))
                    }
                }
            }
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    HisabProTheme(
        themePreference = prefs.themePreference,
        language = prefs.language
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 680.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon3DBadge(
                                    icon = Icons.Rounded.AccountBalanceWallet,
                                    contentDescription = strings.appTitle,
                                    topColor = Color(0xFF34D399),
                                    bottomColor = Color(0xFF047857),
                                    size = 34.dp,
                                    iconSize = 18.dp,
                                    cornerRadius = 10.dp
                                )
                                Text(
                                    text = strings.appTitle,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        actions = {
                            // 1-Tap BDT <-> KWD Switcher Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val next = if (prefs.activeCurrency == Currency.BDT) Currency.KWD else Currency.BDT
                                        viewModel.setActiveCurrency(next)
                                    }
                                    .testTag("top_currency_toggle")
                            ) {
                                Text(
                                    text = "${prefs.activeCurrency.symbol} ${prefs.activeCurrency.code}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            // Offline / Cloud Sync Status Chip
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { navigateTo(AppRoute.BACKUP.route) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (state.isSignedIn) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                                        contentDescription = if (state.isSignedIn) strings.cloudSyncedBadge else strings.offlineReadyBadge,
                                        tint = if (state.isSignedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (state.isSignedIn) strings.cloudSyncedBadge else strings.offlineReadyBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar {
                            AppRoute.entries.forEach { dest ->
                                val selected = currentRoute == dest.route
                                val label = when (dest) {
                                    AppRoute.HOME -> strings.navHome
                                    AppRoute.ANALYTICS -> strings.navAnalytics
                                    AppRoute.BUDGET -> strings.navBudget
                                    AppRoute.BACKUP -> strings.navBackup
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_${dest.route}")
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            editingTransaction = null
                            entryAutoMode = false
                            showEntrySheet = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_transaction")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = strings.addTransaction
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail {
                            AppRoute.entries.forEach { dest ->
                                val selected = currentRoute == dest.route
                                val label = when (dest) {
                                    AppRoute.HOME -> strings.navHome
                                    AppRoute.ANALYTICS -> strings.navAnalytics
                                    AppRoute.BUDGET -> strings.navBudget
                                    AppRoute.BACKUP -> strings.navBackup
                                }
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = AppRoute.HOME.route,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(AppRoute.HOME.route) {
                            DashboardScreen(
                                state = state,
                                strings = strings,
                                onOpenManualEntry = {
                                    editingTransaction = null
                                    entryAutoMode = false
                                    showEntrySheet = true
                                },
                                onOpenAutoEntry = {
                                    editingTransaction = null
                                    entryAutoMode = true
                                    showEntrySheet = true
                                },
                                onExportMonthlyPdf = {
                                    viewModel.generatePdfReport(context, ReportPeriod.MONTHLY)
                                },
                                onNavigateToBudgets = { navigateTo(AppRoute.BUDGET.route) },
                                onNavigateToAdvice = { navigateTo(AppRoute.ANALYTICS.route) },
                                onEditTransaction = { tx ->
                                    editingTransaction = tx
                                    entryAutoMode = false
                                    showEntrySheet = true
                                }
                            )
                        }

                        composable(AppRoute.ANALYTICS.route) {
                            AnalyticsScreen(
                                state = state,
                                strings = strings,
                                onSelectPeriod = viewModel::setPeriod,
                                onSearchChange = viewModel::setSearchQuery,
                                onGeneratePdf = { period -> viewModel.generatePdfReport(context, period) },
                                onEditTransaction = { tx ->
                                    editingTransaction = tx
                                    entryAutoMode = false
                                    showEntrySheet = true
                                }
                            )
                        }

                        composable(AppRoute.BUDGET.route) {
                            BudgetScreen(
                                state = state,
                                strings = strings,
                                onSaveBudget = viewModel::saveBudget,
                                onSaveRecurringTemplate = viewModel::saveRecurringTemplate,
                                onLogRecurringNow = viewModel::logRecurringTemplateNow,
                                onDeleteRecurringTemplate = viewModel::deleteRecurringTemplate
                            )
                        }

                        composable(AppRoute.BACKUP.route) {
                            BackupSettingsScreen(
                                state = state,
                                strings = strings,
                                onSelectLanguage = viewModel::setLanguage,
                                onSelectCurrency = viewModel::setActiveCurrency,
                                onSelectTheme = viewModel::setThemePreference,
                                onToggleAutoSync = viewModel::setAutoCloudSync,
                                onManualCloudSync = viewModel::triggerManualCloudSync,
                                onCloudSignedIn = viewModel::onCloudSignedIn,
                                onCloudSignedOut = viewModel::onCloudSignedOut,
                                onExportJsonBackup = {
                                    exportJsonLauncher.launch("hisabpro_backup_${System.currentTimeMillis()}.json")
                                },
                                onImportJsonBackup = {
                                    importJsonLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                onGeneratePdf = { period -> viewModel.generatePdfReport(context, period) }
                            )
                        }
                    }
                }
            }

            if (showEntrySheet) {
                AddEditTransactionSheet(
                    initialTransaction = editingTransaction,
                    initialAutoMode = entryAutoMode,
                    defaultCurrency = prefs.activeCurrency,
                    language = prefs.language,
                    strings = strings,
                    onDismiss = {
                        showEntrySheet = false
                        editingTransaction = null
                    },
                    onSave = { existingId, type, amountMinor, currency, category, paymentMethod, title, note, occurredAtMillis ->
                        viewModel.saveTransaction(
                            existingId = existingId,
                            type = type,
                            amountMinor = amountMinor,
                            currency = currency,
                            category = category,
                            paymentMethod = paymentMethod,
                            title = title,
                            note = note,
                            occurredAtMillis = occurredAtMillis
                        )
                        showEntrySheet = false
                        editingTransaction = null
                    },
                    onDelete = { id ->
                        viewModel.deleteTransaction(id)
                        showEntrySheet = false
                        editingTransaction = null
                    }
                )
            }
        }
    }
}
