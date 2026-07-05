package com.smeet.telesis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smeet.telesis.data.CategoryEntity
import com.smeet.telesis.data.ExpenseWithCategory
import com.smeet.telesis.data.PaymentMode
import com.smeet.telesis.data.RecurringExpenseEntity
import com.smeet.telesis.data.RecurringInterval
import com.smeet.telesis.data.SubscriptionEntity
import com.smeet.telesis.ui.CategorySpendRow
import com.smeet.telesis.ui.DashboardState
import com.smeet.telesis.ui.DonutChart
import com.smeet.telesis.ui.ExpenseRow
import com.smeet.telesis.ui.MainViewModel
import com.smeet.telesis.ui.MiniBarChart
import com.smeet.telesis.ui.PremiumCard
import com.smeet.telesis.ui.SectionTitle
import com.smeet.telesis.ui.StatPill
import com.smeet.telesis.ui.TelesisTheme
import com.smeet.telesis.ui.VaultColors
import com.smeet.telesis.util.DateUtils
import com.smeet.telesis.util.Money
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TelesisTheme {
                TelesisAppRoot()
            }
        }
    }
}

private enum class Tab(val title: String, val icon: ImageVector) {
    Dashboard("Home", Icons.Default.Home),
    Transactions("List", Icons.Default.Category),
    Sms("SMS", Icons.Default.Message),
    Analytics("Stats", Icons.Default.Analytics),
    Settings("More", Icons.Default.Settings)
}

@Composable
private fun TelesisAppRoot(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val app = context.applicationContext as TelesisApp
    var unlocked by remember { mutableStateOf(!app.appLockManager.isPinEnabled()) }
    var biometricMessage by remember { mutableStateOf("") }

    DisposableEffect(activity) {
        val lifecycle = activity?.lifecycle
        if (lifecycle == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP && app.appLockManager.isPinEnabled()) {
                    unlocked = false
                }
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = VaultColors.Ink) {
        if (!unlocked) {
            LockScreen(
                biometricAvailable = app.appLockManager.isBiometricEnabled() && activity != null,
                message = biometricMessage,
                onUnlock = { pin ->
                    val ok = app.appLockManager.verifyPin(pin)
                    if (ok) unlocked = true
                    ok
                },
                onBiometric = {
                    if (activity != null) {
                        launchBiometric(activity, onSuccess = { unlocked = true }, onError = { biometricMessage = it })
                    }
                }
            )
        } else {
            TelesisHome(vm)
        }
    }
}

private fun launchBiometric(activity: FragmentActivity, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError(errString.toString())
            override fun onAuthenticationFailed() = onError("Authentication failed")
        }
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Telesis")
        .setSubtitle("Use biometric or device credential")
        .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        .build()
    prompt.authenticate(info)
}

@Composable
private fun LockScreen(
    biometricAvailable: Boolean,
    message: String,
    onUnlock: (String) -> Boolean,
    onBiometric: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    Box(
        Modifier
            .fillMaxSize()
            .background(VaultColors.HeroBrush)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        PremiumCard {
            Icon(Icons.Default.Lock, contentDescription = null, tint = VaultColors.Gold, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(16.dp))
            Text("Telesis Locked", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Enter your private app PIN to continue.", color = VaultColors.Muted)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            if (message.isNotBlank()) Text(message, color = VaultColors.Muted, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (!onUnlock(pin)) error = "Incorrect PIN or temporarily locked" },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = VaultColors.Gold, contentColor = Color.Black)
            ) { Text("Unlock") }
            if (biometricAvailable) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onBiometric, modifier = Modifier.fillMaxWidth()) { Text("Unlock with biometric") }
            }
        }
    }
}

@Composable
private fun TelesisHome(vm: MainViewModel) {
    var selected by remember { mutableStateOf(Tab.Dashboard) }
    var showAdd by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val categories by vm.categories.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = VaultColors.Ink,
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Telesis", fontWeight = FontWeight.Black, fontSize = 24.sp)
                    Text("Local-only money intelligence", color = VaultColors.Muted, fontSize = 12.sp)
                }
                AssistChip(onClick = { selected = Tab.Sms }, label = { Text("No cloud") })
            }
        },
        bottomBar = {
            NavigationBar(containerColor = VaultColors.Panel, modifier = Modifier.navigationBarsPadding()) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab,
                        onClick = { selected = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        alwaysShowLabel = selected == tab
                    )
                }
            }
        },
        floatingActionButton = {
            if (selected != Tab.Sms && selected != Tab.Settings) {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    containerColor = VaultColors.Gold,
                    contentColor = Color.Black,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (selected) {
                Tab.Dashboard -> DashboardScreen(vm)
                Tab.Transactions -> TransactionsScreen(vm)
                Tab.Sms -> SmsImportScreen(vm, snackbar)
                Tab.Analytics -> AnalyticsScreen(vm, snackbar)
                Tab.Settings -> SettingsScreen(vm, categories, snackbar)
            }
        }
    }

    if (showAdd) {
        AddExpenseDialog(
            categories = categories,
            onDismiss = { showAdd = false },
            onSave = { amount, merchant, category, mode, note, dateMillis ->
                vm.addExpense(amount, merchant, category, mode, note, dateMillis) { ok, msg ->
                    scope.launch { snackbar.showSnackbar(msg) }
                    if (ok) showAdd = false
                }
            }
        )
    }
}

@Composable
private fun DashboardScreen(vm: MainViewModel) {
    val state by vm.dashboard.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HeroCard(state) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatPill("Today", Money.format(state.todaySpentPaise), Modifier.weight(1f))
                StatPill("Review", state.review.size.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatPill("Subscriptions", state.subscriptions.size.toString(), Modifier.weight(1f))
                StatPill("Fixed", Money.format(state.fixedCommitmentsPaise, compact = true), Modifier.weight(1f))
            }
        }
        item {
            PremiumCard {
                SectionTitle("Monthly trend", "daily")
                if (state.dailySpend.isEmpty()) Text("Import SMS or add expenses to see daily spend movement.", color = VaultColors.Muted, modifier = Modifier.padding(vertical = 18.dp))
                else MiniBarChart(state.dailySpend.map { it.dayKey to it.spentPaise })
            }
        }
        item {
            PremiumCard {
                SectionTitle("Category budgets", "This month")
                Spacer(Modifier.height(8.dp))
                state.categories.take(8).forEach { CategorySpendRow(it) }
            }
        }
        item {
            PremiumCard {
                SectionTitle("Recent activity")
                if (state.recent.isEmpty()) {
                    Text("No expenses yet. Add one manually or import SMS.", color = VaultColors.Muted, modifier = Modifier.padding(vertical = 18.dp))
                } else {
                    state.recent.take(6).forEach { ExpenseRow(it) }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(state: DashboardState) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(34.dp))
            .background(VaultColors.HeroBrush)
            .padding(22.dp)
    ) {
        Column {
            Text("Spent this month", color = VaultColors.Muted)
            Spacer(Modifier.height(8.dp))
            Text(Money.format(state.monthSpentPaise), fontSize = 42.sp, fontWeight = FontWeight.Black, color = VaultColors.Gold)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatPill("Budget", if (state.budgetPaise > 0) Money.format(state.budgetPaise) else "Set budget", Modifier.weight(1f))
                StatPill("Remaining", Money.format(state.remainingPaise), Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Text("Top category: ${state.topCategory?.name ?: "Not enough data"}", color = VaultColors.Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun TransactionsScreen(vm: MainViewModel) {
    val items by vm.expenses.collectAsState()
    val categories by vm.categories.collectAsState()
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<ExpenseWithCategory?>(null) }
    var pendingDelete by remember { mutableStateOf<ExpenseWithCategory?>(null) }
    val filtered = remember(items, query) {
        if (query.isBlank()) items else items.filter {
            it.merchant.contains(query, true) || it.categoryName.contains(query, true) || it.paymentMode.name.contains(query, true)
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search merchant, category, mode") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item { SectionTitle("Transactions", "${filtered.size} rows") }
        if (filtered.isEmpty()) {
            item { EmptyCard("No transactions found", "Import SMS or add an expense manually.") }
        } else {
            items(filtered, key = { it.id }) { expense ->
                PremiumCard {
                    ExpenseRow(
                        expense,
                        trailing = {
                            Row {
                                IconButton(onClick = { editing = expense }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                                IconButton(onClick = { pendingDelete = expense }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                            }
                        }
                    )
                }
            }
        }
    }
    editing?.let { current ->
        AddExpenseDialog(
            categories = categories,
            initial = current,
            onDismiss = { editing = null },
            onSave = { amount, merchant, category, mode, note, dateMillis ->
                vm.updateExpense(current.id, amount, merchant, category, mode, note, dateMillis) { ok, _ ->
                    if (ok) editing = null
                }
            }
        )
    }
    pendingDelete?.let { expense ->
        ConfirmDeleteDialog(
            title = "Delete transaction?",
            body = "This will permanently remove ${expense.merchant} from your local ledger.",
            confirmText = "Delete",
            onDismiss = { pendingDelete = null },
            onConfirm = {
                vm.deleteExpense(expense.id)
                pendingDelete = null
            }
        )
    }
}

@Composable
private fun SmsImportScreen(vm: MainViewModel, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val review by vm.reviewQueue.collectAsState()
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isImporting by remember { mutableStateOf(false) }
    var pendingIgnore by remember { mutableStateOf<ExpenseWithCategory?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        hasPermission = results[Manifest.permission.READ_SMS] == true && results[Manifest.permission.RECEIVE_SMS] == true
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            PremiumCard {
                Icon(Icons.Default.Message, contentDescription = null, tint = VaultColors.Gold, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(12.dp))
                Text("Private SMS Import", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Telesis reads bank, UPI, card, and wallet SMS on this phone only for your personal sideloaded use. Raw SMS is not uploaded, this app declares no internet permission, and Play Store distribution is not targeted.", color = VaultColors.Muted)
                Spacer(Modifier.height(18.dp))
                if (!hasPermission) {
                    Button(
                        onClick = { launcher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = VaultColors.Gold, contentColor = Color.Black)
                    ) { Text("Allow SMS access") }
                } else {
                    Button(
                        enabled = !isImporting,
                        onClick = {
                            isImporting = true
                            vm.importSms(
                                onDone = { report ->
                                    isImporting = false
                                    scope.launch { snackbar.showSnackbar("Imported ${report.imported}, review ${report.review}, ignored ${report.ignored}, duplicates ${report.duplicate}") }
                                },
                                onError = { err ->
                                    isImporting = false
                                    scope.launch { snackbar.showSnackbar(err) }
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = VaultColors.Gold, contentColor = Color.Black)
                    ) { Text(if (isImporting) "Scanning SMS…" else "Scan SMS now") }
                }
            }
        }
        item { SectionTitle("Review queue", "${review.size} items") }
        if (review.isEmpty()) {
            item { EmptyCard("Review queue is clean", "Low-confidence SMS expenses will appear here before they affect final insights.") }
        } else {
            items(review, key = { it.id }) { item ->
                PremiumCard {
                    ExpenseRow(item)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { pendingIgnore = item }) { Text("Ignore") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { vm.approveExpense(item.id) }) { Icon(Icons.Default.Check, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Approve") }
                    }
                }
            }
        }
    }
    pendingIgnore?.let { item ->
        ConfirmDeleteDialog(
            title = "Ignore SMS import?",
            body = "This removes ${item.merchant} from the review queue and local ledger.",
            confirmText = "Ignore",
            onDismiss = { pendingIgnore = null },
            onConfirm = {
                vm.deleteExpense(item.id)
                pendingIgnore = null
            }
        )
    }
}

@Composable
private fun AnalyticsScreen(vm: MainViewModel, snackbar: SnackbarHostState) {
    val state by vm.dashboard.collectAsState()
    val scope = rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            PremiumCard {
                SectionTitle("Payment split")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DonutChart(values = state.paymentModes.map { it.paymentMode.name to it.spentPaise })
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        state.paymentModes.take(6).forEach {
                            Text("${it.paymentMode}: ${Money.format(it.spentPaise)}", color = VaultColors.Muted, fontSize = 13.sp)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
        item {
            PremiumCard {
                SectionTitle("Subscription detector", "${state.subscriptions.size} active")
                Text("Detected from repeated merchant + amount patterns. Run detection after SMS import or manual edits.", color = VaultColors.Muted, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Button(onClick = { vm.detectSubscriptions { msg -> scope.launch { snackbar.showSnackbar(msg) } } }) { Text("Detect subscriptions") }
                Spacer(Modifier.height(12.dp))
                if (state.subscriptions.isEmpty()) Text("No recurring subscription pattern found yet.", color = VaultColors.Muted)
                state.subscriptions.take(8).forEach { SubscriptionRow(it) }
            }
        }
        item {
            PremiumCard {
                SectionTitle("Top merchants")
                if (state.merchants.isEmpty()) Text("No merchant insights yet.", color = VaultColors.Muted, modifier = Modifier.padding(vertical = 18.dp))
                state.merchants.forEach { merchant ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(merchant.merchant, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${merchant.count} transactions", color = VaultColors.Muted, fontSize = 12.sp)
                        }
                        Text(Money.format(merchant.spentPaise), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            PremiumCard {
                SectionTitle("Category intelligence")
                state.categories.filter { it.spentPaise > 0 }.take(12).forEach { CategorySpendRow(it) }
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: MainViewModel, categories: List<CategoryEntity>, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var budgetCategory by remember { mutableStateOf(categories.firstOrNull()?.name.orEmpty()) }
    var budgetAmount by remember { mutableStateOf("") }
    var ruleText by remember { mutableStateOf("") }
    var ruleCategory by remember { mutableStateOf(categories.firstOrNull()?.name.orEmpty()) }
    var ruleMerchant by remember { mutableStateOf("") }
    var recurringAmount by remember { mutableStateOf("") }
    var recurringMerchant by remember { mutableStateOf("") }
    var recurringCategory by remember { mutableStateOf(categories.firstOrNull()?.name.orEmpty()) }
    var recurringInterval by remember { mutableStateOf(RecurringInterval.MONTHLY) }
    var recurringNote by remember { mutableStateOf("") }
    val rules by vm.rules.collectAsState()
    val recurring by vm.recurring.collectAsState()
    val subscriptions by vm.subscriptions.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportBackup(uri, context) { _, msg -> scope.launch { snackbar.showSnackbar(msg) } }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.exportCsv(uri, context) { _, msg -> scope.launch { snackbar.showSnackbar(msg) } }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importBackup(uri, context) { _, msg -> scope.launch { snackbar.showSnackbar(msg) } }
    }

    LaunchedEffect(categories) {
        if (budgetCategory.isBlank()) budgetCategory = categories.firstOrNull()?.name.orEmpty()
        if (ruleCategory.isBlank()) ruleCategory = categories.firstOrNull()?.name.orEmpty()
        if (recurringCategory.isBlank()) recurringCategory = categories.firstOrNull()?.name.orEmpty()
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            PremiumCard {
                SectionTitle("Privacy")
                Text("No login. No backend. No Firebase. No analytics SDK. No INTERNET permission. SMS is parsed locally and saved to your private Room database. This APK is intended for private sideloaded use because SMS permissions are sensitive on Android.", color = VaultColors.Muted)
            }
        }
        item {
            PremiumCard {
                SectionTitle("Backup and export")
                Text("Export JSON backup, restore JSON backup, or export CSV for spreadsheet review. Keep all files private.", color = VaultColors.Muted)
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { exportLauncher.launch("telesis-backup-v1.0.0.json") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Download, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Export JSON backup") }
                    OutlinedButton(onClick = { csvLauncher.launch("telesis-expenses-v1.0.0.csv") }, modifier = Modifier.fillMaxWidth()) { Text("Export CSV") }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/json", "*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Restore JSON backup") }
                }
            }
        }
        item {
            PremiumCard {
                SectionTitle("PIN and biometric lock")
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                    label = { Text("4–8 digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.setPin(pin) { _, msg -> scope.launch { snackbar.showSnackbar(msg) } } }) { Text("Enable PIN") }
                    OutlinedButton(onClick = { vm.disablePin { msg -> scope.launch { snackbar.showSnackbar(msg) } } }) { Text("Disable") }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.setBiometric(true) { _, msg -> scope.launch { snackbar.showSnackbar(msg) } } }) { Text("Enable biometric") }
                    OutlinedButton(onClick = { vm.setBiometric(false) { _, msg -> scope.launch { snackbar.showSnackbar(msg) } } }) { Text("Disable biometric") }
                }
            }
        }
        item {
            PremiumCard {
                SectionTitle("Budgets")
                CategorySelector("Category", budgetCategory, categories.map { it.name }) { budgetCategory = it }
                OutlinedTextField(
                    value = budgetAmount,
                    onValueChange = { budgetAmount = it },
                    label = { Text("Monthly budget amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = {
                    val category = categories.firstOrNull { it.name == budgetCategory }
                    if (category != null) vm.setBudget(category, budgetAmount) { _, msg -> scope.launch { snackbar.showSnackbar(msg) } }
                }) { Text("Save budget") }
            }
        }
        item {
            PremiumCard {
                SectionTitle("Recurring expenses")
                Text("Use this for rent, subscriptions, dues, or any fixed expense that may not always arrive by SMS.", color = VaultColors.Muted, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(recurringAmount, { recurringAmount = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(recurringMerchant, { recurringMerchant = it }, label = { Text("Merchant / title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                CategorySelector("Category", recurringCategory, categories.map { it.name }) { recurringCategory = it }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecurringInterval.entries.forEach { item -> FilterChip(selected = recurringInterval == item, onClick = { recurringInterval = item }, label = { Text(item.name) }) }
                }
                OutlinedTextField(recurringNote, { recurringNote = it }, label = { Text("Note optional") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.addRecurring(recurringAmount, recurringMerchant, recurringCategory, PaymentMode.BANK, recurringInterval, recurringNote) { ok, msg -> if (ok) { recurringAmount = ""; recurringMerchant = ""; recurringNote = "" }; scope.launch { snackbar.showSnackbar(msg) } } }) { Text("Save recurring") }
                    OutlinedButton(onClick = { vm.generateDueRecurring { msg -> scope.launch { snackbar.showSnackbar(msg) } } }) { Text("Add due") }
                }
                Spacer(Modifier.height(12.dp))
                if (recurring.isEmpty()) Text("No recurring expenses saved yet.", color = VaultColors.Muted)
                recurring.take(8).forEach { item -> RecurringRow(item, onDelete = { vm.deleteRecurring(item.id) }) }
            }
        }
        item {
            PremiumCard {
                SectionTitle("SMS rules")
                Text("Create local matching rules. Example: ZOMATO → Food, JIO → Bills.", color = VaultColors.Muted, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = ruleText, onValueChange = { ruleText = it }, label = { Text("Match text") }, modifier = Modifier.fillMaxWidth())
                CategorySelector("Category", ruleCategory, categories.map { it.name }) { ruleCategory = it }
                OutlinedTextField(value = ruleMerchant, onValueChange = { ruleMerchant = it }, label = { Text("Merchant name override optional") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Button(onClick = { vm.addRule(ruleText, ruleCategory, ruleMerchant) { ok, msg -> if (ok) ruleText = ""; scope.launch { snackbar.showSnackbar(msg) } } }) {
                    Icon(Icons.Default.Rule, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save rule")
                }
                Spacer(Modifier.height(12.dp))
                rules.forEach { rule ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(rule.matchText, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { vm.deleteRule(rule.id) }) { Icon(Icons.Default.Delete, contentDescription = "Delete rule") }
                    }
                }
            }
        }
        item {
            PremiumCard {
                SectionTitle("Detected subscriptions", "${subscriptions.size}")
                if (subscriptions.isEmpty()) Text("No subscription candidates yet.", color = VaultColors.Muted)
                subscriptions.take(6).forEach { SubscriptionRow(it) }
            }
        }
        item {
            PremiumCard {
                SectionTitle("Version")
                Text("Telesis v1.0.0", fontWeight = FontWeight.Bold)
                Text("Bundle identifier: com.smeet.telesis", color = VaultColors.Gold, fontWeight = FontWeight.SemiBold)
                Text("Stable personal-use release with manual expenses, SMS import, review queue, auto categories, budgets, insights, rules, recurring expenses, subscription detector, JSON backup/restore, CSV export, PIN, and biometric unlock.", color = VaultColors.Muted)
            }
        }
    }
}

@Composable
private fun RecurringRow(item: RecurringExpenseEntity, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.merchant, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.intervalType} • Next ${DateUtils.formatShort(item.nextDueDate)}", color = VaultColors.Muted, fontSize = 12.sp)
        }
        Text(Money.format(item.amountPaise), fontWeight = FontWeight.Bold)
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete recurring expense") }
    }
}

@Composable
private fun SubscriptionRow(item: SubscriptionEntity) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.merchant, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.frequency} • ${item.transactionCount} hits • ${item.confidence}% confidence • next ${DateUtils.formatShort(item.nextExpectedDate)}", color = VaultColors.Muted, fontSize = 12.sp)
        }
        Text(Money.format(item.amountPaise), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AddExpenseDialog(
    categories: List<CategoryEntity>,
    initial: ExpenseWithCategory? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, PaymentMode, String, Long?) -> Unit
) {
    var amount by remember(initial?.id) { mutableStateOf(initial?.let { (it.amountPaise / 100.0).toString() } ?: "") }
    var merchant by remember(initial?.id) { mutableStateOf(initial?.merchant ?: "") }
    var category by remember(initial?.id, categories) { mutableStateOf(initial?.categoryName ?: categories.firstOrNull()?.name ?: "Other") }
    var mode by remember(initial?.id) { mutableStateOf(initial?.paymentMode ?: PaymentMode.UPI) }
    var note by remember(initial?.id) { mutableStateOf(initial?.note ?: "") }
    var dateText by remember(initial?.id) { mutableStateOf(initial?.let { DateUtils.formatInputDate(it.dateTime) } ?: DateUtils.formatInputDate(DateUtils.now())) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = { onSave(amount, merchant, category, mode, note, DateUtils.parseInputDate(dateText)) }) { Text(if (initial == null) "Save" else "Update") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(if (initial == null) "Add expense" else "Edit expense") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(amount, { amount = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(merchant, { merchant = it }, label = { Text("Merchant") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(dateText, { dateText = it.take(10) }, label = { Text("Date (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                CategorySelector("Category", category, categories.map { it.name }.ifEmpty { listOf("Other") }) { category = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    listOf(PaymentMode.UPI, PaymentMode.CARD, PaymentMode.CASH, PaymentMode.BANK, PaymentMode.WALLET).forEach { item ->
                        FilterChip(selected = mode == item, onClick = { mode = item }, label = { Text(item.name) })
                    }
                }
                OutlinedTextField(note, { note = it }, label = { Text("Note optional") }, modifier = Modifier.fillMaxWidth())
            }
        }
    )
}

@Composable
private fun CategorySelector(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    Column {
        Text(label, color = VaultColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { name ->
                FilterChip(selected = selected == name, onClick = { onSelect(name) }, label = { Text(name) })
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    body: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body, color = VaultColors.Muted) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EmptyCard(title: String, body: String) {
    PremiumCard {
        Text(title, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(body, color = VaultColors.Muted)
    }
}
