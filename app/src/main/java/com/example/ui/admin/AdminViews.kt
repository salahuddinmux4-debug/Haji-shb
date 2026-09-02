package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppNotification
import com.example.model.BalanceType
import com.example.model.Customer
import com.example.model.MarketItem
import com.example.model.MarketRates
import com.example.model.RateHistoryEntry
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.ui.common.BalanceBadge
import com.example.ui.common.MarketStatusBanner
import com.example.ui.theme.*
import com.example.util.FormatUtils

enum class AdminNavTab {
    DASHBOARD,
    CUSTOMERS,
    TRANSACTIONS,
    DAILY_RATES,
    NOTIFICATIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    adminViewModel: AdminViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customers by adminViewModel.customersFlow.collectAsState()
    val marketItems by adminViewModel.marketItemsFlow.collectAsState()
    val marketRates by adminViewModel.currentRatesFlow.collectAsState()
    val rateHistory by adminViewModel.rateHistoryFlow.collectAsState()
    val notifications by adminViewModel.notificationsFlow.collectAsState()
    val transactions by adminViewModel.transactionsFlow.collectAsState()
    val stats by adminViewModel.statsFlow.collectAsState()
    val statusMessage by adminViewModel.statusMessage.collectAsState()

    var currentTab by remember { mutableStateOf(AdminNavTab.DASHBOARD) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Dialog States
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var customerForBalanceUpdate by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }

    // Item Management Dialog States
    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToRename by remember { mutableStateOf<MarketItem?>(null) }
    var itemToRemove by remember { mutableStateOf<MarketItem?>(null) }

    // Transaction Dialog States (Bills & Payments)
    var showAddBillDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var preselectedCustomerForTx by remember { mutableStateOf<Customer?>(null) }
    var transactionForDetail by remember { mutableStateOf<TransactionRecord?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            adminViewModel.clearStatusMessage()
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out from Admin Panel?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to end your administrator session?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ReceivableRed)
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            items = marketItems,
            onDismiss = { showAddCustomerDialog = false },
            onAddCustomer = { name, username, password, phone, balance, balanceType, hasCustom, c1, c2, c3, c4, customMap ->
                adminViewModel.addCustomer(
                    name = name,
                    username = username,
                    password = password,
                    phone = phone,
                    balance = balance,
                    balanceType = balanceType,
                    hasCustomRates = hasCustom,
                    cItem1 = c1,
                    cItem2 = c2,
                    cItem3 = c3,
                    cItem4 = c4,
                    customRatesMap = customMap
                ) {
                    showAddCustomerDialog = false
                }
            }
        )
    }

    customerToEdit?.let { cust ->
        EditCustomerDialog(
            customer = cust,
            items = marketItems,
            onDismiss = { customerToEdit = null },
            onSave = { updated, newPass ->
                adminViewModel.updateCustomer(updated, newPass) {
                    customerToEdit = null
                }
            }
        )
    }

    customerForBalanceUpdate?.let { cust ->
        UpdateBalanceDialog(
            customer = cust,
            onDismiss = { customerForBalanceUpdate = null },
            onUpdateBalance = { newBal, type, note ->
                adminViewModel.updateCustomerBalance(cust.id, newBal, type, note)
                customerForBalanceUpdate = null
            }
        )
    }

    customerToDelete?.let { cust ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("Delete Customer?", fontWeight = FontWeight.Bold, color = ReceivableRed) },
            text = { Text("Are you sure you want to delete '${cust.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.deleteCustomer(cust.id) {
                            customerToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ReceivableRed)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddItemDialog) {
        AddMarketItemDialog(
            onDismiss = { showAddItemDialog = false },
            onAddItem = { name, rate ->
                adminViewModel.addItem(name, rate) {
                    showAddItemDialog = false
                }
            }
        )
    }

    itemToRename?.let { item ->
        EditMarketItemNameDialog(
            item = item,
            onDismiss = { itemToRename = null },
            onRename = { newName ->
                adminViewModel.editItemName(item.id, newName) {
                    itemToRename = null
                }
            }
        )
    }

    itemToRemove?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToRemove = null },
            title = { Text("Remove Item?", fontWeight = FontWeight.Bold, color = ReceivableRed) },
            text = { Text("Are you sure you want to remove '${item.name}' from daily market rates?") },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.removeItem(item.id) {
                            itemToRemove = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ReceivableRed)
                ) {
                    Text("Remove Item")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Bill Dialog (Purchasing goods from customer)
    if (showAddBillDialog) {
        AddBillDialog(
            customers = customers,
            marketItems = marketItems,
            preSelectedCustomer = preselectedCustomerForTx,
            onDismiss = {
                showAddBillDialog = false
                preselectedCustomerForTx = null
            },
            onAddBill = { customerId, itemId, itemName, qty, unit, rate, total, billNo, notes, date ->
                adminViewModel.addBill(customerId, itemId, itemName, qty, unit, rate, total, billNo, notes, date) {
                    showAddBillDialog = false
                    preselectedCustomerForTx = null
                }
            }
        )
    }

    // Add Payment Dialog (Paying cash/bank to customer)
    if (showAddPaymentDialog) {
        AddPaymentDialog(
            customers = customers,
            preSelectedCustomer = preselectedCustomerForTx,
            onDismiss = {
                showAddPaymentDialog = false
                preselectedCustomerForTx = null
            },
            onAddPayment = { customerId, amount, method, ref, notes, date ->
                adminViewModel.addPayment(customerId, amount, method, ref, notes, date) {
                    showAddPaymentDialog = false
                    preselectedCustomerForTx = null
                }
            }
        )
    }

    // Transaction Detail & Delete Dialog
    transactionForDetail?.let { tx ->
        TransactionDetailDialog(
            transaction = tx,
            onDismiss = { transactionForDetail = null },
            onDelete = {
                adminViewModel.deleteTransaction(tx.id) {
                    transactionForDetail = null
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = NavyDark,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "ADMIN",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Mujahid Accounts",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                    }
                },
                actions = {
                    // Quick Market Status Pill
                    val isOpen = marketRates?.isMarketOpen ?: true
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isOpen) MarketOpenGreenBg else MarketClosedRedBg,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clickable {
                                adminViewModel.toggleMarketStatus(!isOpen)
                            }
                            .testTag("admin_quick_market_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOpen) MarketOpenGreen else MarketClosedRed)
                            )
                            Text(
                                text = if (isOpen) "OPEN" else "CLOSED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isOpen) MarketOpenGreen else MarketClosedRed
                            )
                        }
                    }

                    // Logout Icon
                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.testTag("button_admin_logout")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = SlateMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = NavyDark
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == AdminNavTab.DASHBOARD,
                    onClick = { currentTab = AdminNavTab.DASHBOARD },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Overview") },
                    modifier = Modifier.testTag("nav_admin_dashboard")
                )
                NavigationBarItem(
                    selected = currentTab == AdminNavTab.CUSTOMERS,
                    onClick = { currentTab = AdminNavTab.CUSTOMERS },
                    icon = { Icon(Icons.Default.People, contentDescription = "Customers") },
                    label = { Text("Customers") },
                    modifier = Modifier.testTag("nav_admin_customers")
                )
                NavigationBarItem(
                    selected = currentTab == AdminNavTab.TRANSACTIONS,
                    onClick = { currentTab = AdminNavTab.TRANSACTIONS },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Bills & Payments") },
                    label = { Text("Bills/Ledger") },
                    modifier = Modifier.testTag("nav_admin_transactions")
                )
                NavigationBarItem(
                    selected = currentTab == AdminNavTab.DAILY_RATES,
                    onClick = { currentTab = AdminNavTab.DAILY_RATES },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Rates") },
                    label = { Text("Daily Rates") },
                    modifier = Modifier.testTag("nav_admin_rates")
                )
                NavigationBarItem(
                    selected = currentTab == AdminNavTab.NOTIFICATIONS,
                    onClick = { currentTab = AdminNavTab.NOTIFICATIONS },
                    icon = { Icon(Icons.Default.Campaign, contentDescription = "Broadcast") },
                    label = { Text("Broadcast") },
                    modifier = Modifier.testTag("nav_admin_notifications")
                )
            }
        },
        floatingActionButton = {
            when (currentTab) {
                AdminNavTab.DASHBOARD, AdminNavTab.CUSTOMERS -> {
                    FloatingActionButton(
                        onClick = { showAddCustomerDialog = true },
                        containerColor = NavyDark,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_add_customer")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
                    }
                }
                AdminNavTab.TRANSACTIONS -> {
                    ExtendedFloatingActionButton(
                        onClick = { showAddBillDialog = true },
                        containerColor = NavyDark,
                        contentColor = Color.White,
                        icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = null) },
                        text = { Text("+ New Bill") },
                        modifier = Modifier.testTag("fab_add_bill")
                    )
                }
                else -> {}
            }
        },
        containerColor = LightSurface,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AdminNavTab.DASHBOARD -> AdminDashboardView(
                    stats = stats,
                    marketRates = marketRates,
                    marketItems = marketItems,
                    customers = customers,
                    transactions = transactions,
                    onToggleMarket = { adminViewModel.toggleMarketStatus(it) },
                    onNavigateToRates = { currentTab = AdminNavTab.DAILY_RATES },
                    onNavigateToCustomers = { currentTab = AdminNavTab.CUSTOMERS },
                    onNavigateToTransactions = { currentTab = AdminNavTab.TRANSACTIONS },
                    onAddCustomerClick = { showAddCustomerDialog = true },
                    onAddBillClick = {
                        preselectedCustomerForTx = null
                        showAddBillDialog = true
                    },
                    onAddPaymentClick = {
                        preselectedCustomerForTx = null
                        showAddPaymentDialog = true
                    },
                    onUpdateBalanceClick = { customerForBalanceUpdate = it },
                    onTransactionClick = { transactionForDetail = it }
                )
                AdminNavTab.CUSTOMERS -> AdminCustomersView(
                    customers = customers,
                    searchQuery = adminViewModel.searchQuery.collectAsState().value,
                    onSearchQueryChange = { adminViewModel.setSearchQuery(it) },
                    onEditCustomer = { customerToEdit = it },
                    onUpdateBalance = { customerForBalanceUpdate = it },
                    onAddBillForCustomer = { cust ->
                        preselectedCustomerForTx = cust
                        showAddBillDialog = true
                    },
                    onAddPaymentForCustomer = { cust ->
                        preselectedCustomerForTx = cust
                        showAddPaymentDialog = true
                    },
                    onToggleActive = { adminViewModel.toggleCustomerActive(it.id) },
                    onDeleteCustomer = { customerToDelete = it },
                    onAddCustomerClick = { showAddCustomerDialog = true }
                )
                AdminNavTab.TRANSACTIONS -> AdminTransactionsView(
                    transactions = transactions,
                    customers = customers,
                    onAddBillClick = {
                        preselectedCustomerForTx = null
                        showAddBillDialog = true
                    },
                    onAddPaymentClick = {
                        preselectedCustomerForTx = null
                        showAddPaymentDialog = true
                    },
                    onTransactionClick = { transactionForDetail = it }
                )
                AdminNavTab.DAILY_RATES -> AdminDailyRatesView(
                    marketRates = marketRates,
                    marketItems = marketItems,
                    onUpdateItemRate = { itemId, rate -> adminViewModel.updateItemRate(itemId, rate) },
                    onUpdateAllItemRates = { ratesMap -> adminViewModel.updateAllItemRates(ratesMap) },
                    onAddItemClick = { showAddItemDialog = true },
                    onRenameItemClick = { itemToRename = it },
                    onRemoveItemClick = { itemToRemove = it },
                    onToggleMarket = { adminViewModel.toggleMarketStatus(it) }
                )
                AdminNavTab.NOTIFICATIONS -> AdminNotificationsView(
                    notifications = notifications,
                    onSendBroadcast = { title, msg -> adminViewModel.sendBroadcastNotification(title, msg) }
                )
            }
        }
    }
}

@Composable
fun AdminDashboardView(
    stats: AdminDashboardStats,
    marketRates: MarketRates?,
    marketItems: List<MarketItem> = emptyList(),
    customers: List<Customer>,
    transactions: List<TransactionRecord> = emptyList(),
    onToggleMarket: (Boolean) -> Unit,
    onNavigateToRates: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToTransactions: () -> Unit = {},
    onAddCustomerClick: () -> Unit,
    onAddBillClick: () -> Unit = {},
    onAddPaymentClick: () -> Unit = {},
    onUpdateBalanceClick: (Customer) -> Unit,
    onTransactionClick: (TransactionRecord) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isMarketOpen = marketRates?.isMarketOpen ?: true

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Summary Bar
        Text(
            text = "Business Overview",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = NavyDark
        )

        // ==================== QUICK ACTION BUTTONS (BILL / PAYMENT / CUSTOMER) ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Quick Actions (فوری اندراج)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // New Bill Button (Mall Purchase)
                    Button(
                        onClick = onAddBillClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("button_quick_add_bill"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFCA5A5))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ Bill (مال خریداری)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // New Payment Button (Adaigi)
                    Button(
                        onClick = onAddPaymentClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("button_quick_add_payment"),
                        colors = ButtonDefaults.buttonColors(containerColor = PayableGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ Payment (ادائیگی)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Metrics Grid (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Customers
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToCustomers() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Customers", fontSize = 12.sp, color = SlateMuted)
                        Icon(Icons.Default.People, contentDescription = null, tint = NavyDark, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${stats.totalCustomers}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                    Text(
                        text = "${stats.activeCustomers} Active",
                        fontSize = 11.sp,
                        color = MarketOpenGreen
                    )
                }
            }

            // Net Ledger Balance
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Net Balance", fontSize = 12.sp, color = SlateMuted)
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = FormatUtils.formatPkr(stats.netBalance),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stats.netBalance >= 0) ReceivableRed else PayableGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (stats.netBalance >= 0) "Net Receivable" else "Net Payable",
                        fontSize = 11.sp,
                        color = SlateMuted
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Receivable (GET - Red)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ReceivableRedBg),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ReceivableRedBorder))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Receivable", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ReceivableRed)
                        Text("GET", fontSize = 11.sp, fontWeight = FontWeight.Black, color = ReceivableRed)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = FormatUtils.formatPkr(stats.totalReceivable),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = ReceivableRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("From Customers", fontSize = 11.sp, color = ReceivableRed.copy(alpha = 0.8f))
                }
            }

            // Total Payable (GIVE - Green)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PayableGreenBg),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PayableGreenBorder))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Payable", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PayableGreen)
                        Text("GIVE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = PayableGreen)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = FormatUtils.formatPkr(stats.totalPayable),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = PayableGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("To Customers", fontSize = 11.sp, color = PayableGreen.copy(alpha = 0.8f))
                }
            }
        }

        // ==================== TRANSACTION TOTALS ROW ====================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Mall Purchased (Bills)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToTransactions() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Bills (مال)", fontSize = 11.sp, color = SlateMuted)
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = NavyDark, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatUtils.formatPkr(stats.totalBillsAmount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("Purchased goods", fontSize = 10.sp, color = SlateMuted)
                }
            }

            // Total Payments Given
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToTransactions() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Paid (ادائیگی)", fontSize = 11.sp, color = SlateMuted)
                        Icon(Icons.Default.Payment, contentDescription = null, tint = PayableGreen, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatUtils.formatPkr(stats.totalPaymentsAmount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PayableGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("Cash & Bank transfers", fontSize = 10.sp, color = SlateMuted)
                }
            }
        }

        // ==================== MARKET STATUS CONTROL CARD ====================
        MarketStatusBanner(
            isOpen = isMarketOpen,
            lastUpdateDate = marketRates?.date,
            onToggleClick = { onToggleMarket(!isMarketOpen) }
        )

        // ==================== TODAY'S RATES QUICK SUMMARY ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Daily Market Rates", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                            if (!isMarketOpen) {
                                Surface(
                                    color = MarketClosedRedBg,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Closed (Hidden from Customers)",
                                        color = MarketClosedRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text("Date: ${marketRates?.date ?: "Today"}", fontSize = 12.sp, color = SlateMuted)
                    }
                    TextButton(onClick = onNavigateToRates) {
                        Text("Edit Rates →", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (marketItems.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        marketItems.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                rowItems.forEach { item ->
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(item.name, fontSize = 12.sp, color = SlateMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(FormatUtils.formatPkr(item.currentRate), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Item 1", fontSize = 11.sp, color = SlateMuted)
                            Text(FormatUtils.formatPkr(marketRates?.item1 ?: 0.0), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Item 2", fontSize = 11.sp, color = SlateMuted)
                            Text(FormatUtils.formatPkr(marketRates?.item2 ?: 0.0), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Item 3", fontSize = 11.sp, color = SlateMuted)
                            Text(FormatUtils.formatPkr(marketRates?.item3 ?: 0.0), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Item 4", fontSize = 11.sp, color = SlateMuted)
                            Text(FormatUtils.formatPkr(marketRates?.item4 ?: 0.0), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                        }
                    }
                }
            }
        }

        // ==================== RECENT TRANSACTIONS (BILLS & PAYMENTS) ====================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Recent Activity (تازہ ترین کھاتہ)", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                Text("Latest bills & payments", fontSize = 11.sp, color = SlateMuted)
            }
            TextButton(onClick = onNavigateToTransactions) {
                Text("View All (${transactions.size}) →", fontWeight = FontWeight.Bold)
            }
        }

        if (transactions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("No bills or payments recorded yet", fontSize = 13.sp, color = SlateMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onAddBillClick) {
                            Text("+ Add Bill")
                        }
                        OutlinedButton(onClick = onAddPaymentClick) {
                            Text("+ Add Payment")
                        }
                    }
                }
            }
        } else {
            transactions.take(4).forEach { tx ->
                TransactionCard(
                    transaction = tx,
                    onClick = { onTransactionClick(tx) }
                )
            }
        }

        // ==================== RECENT CUSTOMERS PREVIEW ====================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Customer Balances", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NavyDark)
            TextButton(onClick = onNavigateToCustomers) {
                Text("View All (${customers.size}) →", fontWeight = FontWeight.Bold)
            }
        }

        if (customers.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("No customers added yet", fontSize = 14.sp, color = SlateMuted)
                    Button(
                        onClick = onAddCustomerClick,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add First Customer")
                    }
                }
            }
        } else {
            customers.take(5).forEach { cust ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(cust.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                            Text("@${cust.username}", fontSize = 12.sp, color = SlateMuted)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = FormatUtils.formatPkr(cust.balance),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (cust.balanceType == BalanceType.RECEIVABLE) ReceivableRed else PayableGreen
                                )
                                BalanceBadge(balanceType = cust.balanceType, isLarge = false)
                            }

                            IconButton(onClick = { onUpdateBalanceClick(cust) }) {
                                Icon(Icons.Default.EditNote, contentDescription = "Update Balance", tint = NavyDark)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AdminDailyRatesView(
    marketRates: MarketRates?,
    marketItems: List<MarketItem> = emptyList(),
    onUpdateItemRate: (String, Double) -> Unit = { _, _ -> },
    onUpdateAllItemRates: (Map<String, Double>) -> Unit = {},
    onAddItemClick: () -> Unit = {},
    onRenameItemClick: (MarketItem) -> Unit = {},
    onRemoveItemClick: (MarketItem) -> Unit = {},
    onToggleMarket: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMarketOpen = marketRates?.isMarketOpen ?: true

    // Rate inputs map for all items
    val rateInputs = remember(marketItems) {
        mutableStateMapOf<String, String>().apply {
            marketItems.forEach { item ->
                put(item.id, item.currentRate.toString())
            }
        }
    }

    // Keep legacy fallback inputs in sync
    var r1Text by remember(marketRates?.item1) { mutableStateOf(marketRates?.item1?.toString() ?: "") }
    var r2Text by remember(marketRates?.item2) { mutableStateOf(marketRates?.item2?.toString() ?: "") }
    var r3Text by remember(marketRates?.item3) { mutableStateOf(marketRates?.item3?.toString() ?: "") }
    var r4Text by remember(marketRates?.item4) { mutableStateOf(marketRates?.item4?.toString() ?: "") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Daily Market Rates Management", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NavyDark)

            Button(
                onClick = onAddItemClick,
                colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("button_add_item")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Item", fontSize = 13.sp)
            }
        }

        // Market Status Toggle
        MarketStatusBanner(
            isOpen = isMarketOpen,
            lastUpdateDate = marketRates?.date,
            onToggleClick = { onToggleMarket(!isMarketOpen) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Set Rates for Today", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)
            if (marketRates?.updatedTime != null) {
                Text("Last updated: ${FormatUtils.formatTimeOnly(marketRates.updatedTime)}", fontSize = 11.sp, color = SlateMuted)
            }
        }

        if (marketItems.isNotEmpty()) {
            marketItems.forEach { item ->
                val currentInput = rateInputs[item.id] ?: item.currentRate.toString()
                AdminRateEditRow(
                    itemName = item.name,
                    currentRate = item.currentRate,
                    previousRate = item.previousRate,
                    inputText = currentInput,
                    onTextChange = { rateInputs[item.id] = it },
                    onUpdateSingle = {
                        val rate = rateInputs[item.id]?.toDoubleOrNull()
                        if (rate != null) onUpdateItemRate(item.id, rate)
                    },
                    onRenameItem = { onRenameItemClick(item) },
                    onRemoveItem = if (marketItems.size > 1) { { onRemoveItemClick(item) } } else null
                )
            }
        } else {
            // Fallback to Item 1 - Item 4
            AdminRateEditRow(
                itemName = "Item 1",
                currentRate = marketRates?.item1 ?: 0.0,
                previousRate = marketRates?.previousItem1,
                inputText = r1Text,
                onTextChange = { r1Text = it },
                onUpdateSingle = {
                    val rate = r1Text.toDoubleOrNull()
                    if (rate != null) onUpdateItemRate("item_1", rate)
                }
            )

            AdminRateEditRow(
                itemName = "Item 2",
                currentRate = marketRates?.item2 ?: 0.0,
                previousRate = marketRates?.previousItem2,
                inputText = r2Text,
                onTextChange = { r2Text = it },
                onUpdateSingle = {
                    val rate = r2Text.toDoubleOrNull()
                    if (rate != null) onUpdateItemRate("item_2", rate)
                }
            )

            AdminRateEditRow(
                itemName = "Item 3",
                currentRate = marketRates?.item3 ?: 0.0,
                previousRate = marketRates?.previousItem3,
                inputText = r3Text,
                onTextChange = { r3Text = it },
                onUpdateSingle = {
                    val rate = r3Text.toDoubleOrNull()
                    if (rate != null) onUpdateItemRate("item_3", rate)
                }
            )

            AdminRateEditRow(
                itemName = "Item 4",
                currentRate = marketRates?.item4 ?: 0.0,
                previousRate = marketRates?.previousItem4,
                inputText = r4Text,
                onTextChange = { r4Text = it },
                onUpdateSingle = {
                    val rate = r4Text.toDoubleOrNull()
                    if (rate != null) onUpdateItemRate("item_4", rate)
                }
            )
        }

        // Update All Rates Button
        Button(
            onClick = {
                if (marketItems.isNotEmpty()) {
                    val map = mutableMapOf<String, Double>()
                    marketItems.forEach { item ->
                        val entered = rateInputs[item.id]?.toDoubleOrNull() ?: item.currentRate
                        map[item.id] = entered
                    }
                    onUpdateAllItemRates(map)
                } else {
                    val r1 = r1Text.toDoubleOrNull() ?: marketRates?.item1 ?: 0.0
                    val r2 = r2Text.toDoubleOrNull() ?: marketRates?.item2 ?: 0.0
                    val r3 = r3Text.toDoubleOrNull() ?: marketRates?.item3 ?: 0.0
                    val r4 = r4Text.toDoubleOrNull() ?: marketRates?.item4 ?: 0.0
                    onUpdateAllItemRates(mapOf("item_1" to r1, "item_2" to r2, "item_3" to r3, "item_4" to r4))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("button_update_all_rates"),
            colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PublishedWithChanges, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Publish / Update All Rates", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun AdminRateEditRow(
    itemName: String,
    currentRate: Double,
    previousRate: Double?,
    inputText: String,
    onTextChange: (String) -> Unit,
    onUpdateSingle: () -> Unit,
    onRenameItem: (() -> Unit)? = null,
    onRemoveItem: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(itemName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    if (onRenameItem != null) {
                        IconButton(
                            onClick = onRenameItem,
                            modifier = Modifier.size(28.dp).testTag("button_rename_${itemName.replace(" ", "_").lowercase()}")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename item", tint = SlateMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (onRemoveItem != null) {
                        IconButton(
                            onClick = onRemoveItem,
                            modifier = Modifier.size(28.dp).testTag("button_delete_${itemName.replace(" ", "_").lowercase()}")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete item", tint = ReceivableRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Current: ${FormatUtils.formatPkr(currentRate)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NavyDark)
                    if (previousRate != null) {
                        Text("Prev: ${FormatUtils.formatPkr(previousRate)}", fontSize = 12.sp, color = SlateMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onTextChange,
                    label = { Text("Enter Rate (Rs.)") },
                    placeholder = { Text("e.g. 100.0") },
                    colors = appTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    modifier = Modifier.weight(1f).testTag("input_rate_${itemName.replace(" ", "_").lowercase()}"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Button(
                    onClick = onUpdateSingle,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SlateAccent),
                    modifier = Modifier.height(54.dp).testTag("button_update_${itemName.replace(" ", "_").lowercase()}")
                ) {
                    Text("Update")
                }
            }
        }
    }
}

@Composable
fun AdminCustomersView(
    customers: List<Customer>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onEditCustomer: (Customer) -> Unit,
    onUpdateBalance: (Customer) -> Unit,
    onAddBillForCustomer: (Customer) -> Unit = {},
    onAddPaymentForCustomer: (Customer) -> Unit = {},
    onToggleActive: (Customer) -> Unit,
    onDeleteCustomer: (Customer) -> Unit,
    onAddCustomerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredCustomers = customers.filter { cust ->
        val matchesSearch = cust.name.contains(searchQuery, ignoreCase = true) ||
                cust.username.contains(searchQuery, ignoreCase = true) ||
                cust.phone.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "ACTIVE" -> cust.isActive
            "INACTIVE" -> !cust.isActive
            "RECEIVABLE" -> cust.balanceType == BalanceType.RECEIVABLE
            "PAYABLE" -> cust.balanceType == BalanceType.PAYABLE
            else -> true
        }

        matchesSearch && matchesFilter
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search by name, username, or phone...") },
            colors = appTextFieldColors(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SlateMuted) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("search_customer_input"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL" to "All (${customers.size})", "ACTIVE" to "Active", "RECEIVABLE" to "GET (Red)", "PAYABLE" to "GIVE (Green)").forEach { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedFilter == key) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        // Customers List
        if (filteredCustomers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(48.dp))
                    Text("No matching customers found", fontSize = 15.sp, color = SlateMuted)
                    Button(
                        onClick = onAddCustomerClick,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
                    ) {
                        Text("Add New Customer")
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredCustomers, key = { it.id }) { cust ->
                    CustomerAdminCard(
                        customer = cust,
                        onEdit = { onEditCustomer(cust) },
                        onUpdateBalance = { onUpdateBalance(cust) },
                        onAddBill = { onAddBillForCustomer(cust) },
                        onAddPayment = { onAddPaymentForCustomer(cust) },
                        onToggleActive = { onToggleActive(cust) },
                        onDelete = { onDeleteCustomer(cust) }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerAdminCard(
    customer: Customer,
    onEdit: () -> Unit,
    onUpdateBalance: () -> Unit,
    onAddBill: () -> Unit = {},
    onAddPayment: () -> Unit = {},
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_customer_card_${customer.username}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = customer.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        if (customer.hasCustomRates) {
                            Surface(
                                color = GoldAccentBg,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Custom Rate",
                                    color = GoldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "@${customer.username} • ${customer.phone.ifBlank { "No phone" }}",
                        fontSize = 12.sp,
                        color = SlateMuted
                    )
                }

                // Status Badge
                Surface(
                    color = if (customer.isActive) MarketOpenGreenBg else MarketClosedRedBg,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable { onToggleActive() }
                ) {
                    Text(
                        text = if (customer.isActive) "Active" else "Deactivated",
                        color = if (customer.isActive) MarketOpenGreen else MarketClosedRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            // Balance Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Customer Balance", fontSize = 11.sp, color = SlateMuted)
                    Text(
                        text = FormatUtils.formatPkr(customer.balance),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (customer.balanceType == BalanceType.RECEIVABLE) ReceivableRed else PayableGreen
                    )
                }

                BalanceBadge(balanceType = customer.balanceType, isLarge = false)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Transaction Shortcuts (Bill / Payment)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddBill,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFFFCA5A5))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Bill (مال)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Button(
                    onClick = onAddPayment,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PayableGreen),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Payment (ادائیگی)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row (Balance / Edit / Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onUpdateBalance,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adjust Bal", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Rates", fontSize = 11.sp)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ReceivableRed)
                }
            }
        }
    }
}

// ==================== ADMIN TRANSACTIONS VIEW (BILLS & PAYMENTS LEDGER) ====================
@Composable
fun AdminTransactionsView(
    transactions: List<TransactionRecord>,
    customers: List<Customer>,
    onAddBillClick: () -> Unit,
    onAddPaymentClick: () -> Unit,
    onTransactionClick: (TransactionRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // ALL, BILL, PAYMENT

    val filteredTransactions = transactions.filter { tx ->
        val matchesSearch = tx.customerName.contains(searchQuery, ignoreCase = true) ||
                tx.itemName.contains(searchQuery, ignoreCase = true) ||
                tx.billNumber.contains(searchQuery, ignoreCase = true) ||
                tx.notes.contains(searchQuery, ignoreCase = true) ||
                tx.paymentMethod.contains(searchQuery, ignoreCase = true)

        val matchesType = when (selectedTypeFilter) {
            "BILL" -> tx.type == TransactionType.BILL
            "PAYMENT" -> tx.type == TransactionType.PAYMENT
            else -> true
        }

        matchesSearch && matchesType
    }

    val totalBills = transactions.filter { it.type == TransactionType.BILL }.sumOf { it.amount }
    val totalPayments = transactions.filter { it.type == TransactionType.PAYMENT }.sumOf { it.amount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Bills & Ledger (کھاتہ و بل)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                Text("Customer purchases & payment ledger", fontSize = 11.sp, color = SlateMuted)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onAddBillClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bill (مال)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAddPaymentClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PayableGreen),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Payment (ادائیگی)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Summary Bar (Total Bills vs Total Payments)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Bills (مال خریداری)", fontSize = 11.sp, color = SlateMuted)
                    Text(
                        text = FormatUtils.formatPkr(totalBills),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Payments (ادائیگی)", fontSize = 11.sp, color = SlateMuted)
                    Text(
                        text = FormatUtils.formatPkr(totalPayments),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PayableGreen
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search customer, item, bill #...") },
            colors = appTextFieldColors(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SlateMuted) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("search_transactions_input"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Type Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "ALL" to "All (${transactions.size})",
                "BILL" to "Bills / خریداری (${transactions.count { it.type == TransactionType.BILL }})",
                "PAYMENT" to "Payments / ادائیگی (${transactions.count { it.type == TransactionType.PAYMENT }})"
            ).forEach { (key, label) ->
                FilterChip(
                    selected = selectedTypeFilter == key,
                    onClick = { selectedTypeFilter = key },
                    label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedTypeFilter == key) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        // Transactions List
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(48.dp))
                    Text("No transactions found", fontSize = 15.sp, color = SlateMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onAddBillClick,
                            colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
                        ) {
                            Text("+ Add Bill")
                        }
                        Button(
                            onClick = onAddPaymentClick,
                            colors = ButtonDefaults.buttonColors(containerColor = PayableGreen)
                        ) {
                            Text("+ Add Payment")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionCard(
                        transaction = tx,
                        onClick = { onTransactionClick(tx) }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionCard(
    transaction: TransactionRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBill = transaction.type == TransactionType.BILL

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("tx_card_${transaction.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Type Badge Icon
                    Surface(
                        shape = CircleShape,
                        color = if (isBill) Color(0xFFFEF2F2) else PayableGreenBg,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isBill) Icons.Default.ShoppingCart else Icons.Default.Payment,
                                contentDescription = null,
                                tint = if (isBill) ReceivableRed else PayableGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = transaction.customerName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        Text(
                            text = "${transaction.date} • ${if (isBill) "Bill (مال خریداری)" else "Payment (${transaction.paymentMethod})"}",
                            fontSize = 11.sp,
                            color = SlateMuted
                        )
                    }
                }

                // Amount
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FormatUtils.formatPkr(transaction.amount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isBill) ReceivableRed else PayableGreen
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isBill) ReceivableRedBg else PayableGreenBg
                    ) {
                        Text(
                            text = if (isBill) "+ BILL" else "- PAYMENT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isBill) ReceivableRed else PayableGreen,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // If Bill, show quantity x rate details
            if (isBill && transaction.quantity > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${transaction.itemName}: ${transaction.quantity} ${transaction.unit} @ Rs. ${transaction.rate}/${transaction.unit}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = NavyDark
                        )
                        if (transaction.billNumber.isNotBlank()) {
                            Text(
                                text = "Bill #${transaction.billNumber}",
                                fontSize = 11.sp,
                                color = SlateMuted
                            )
                        }
                    }
                }
            }

            if (transaction.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Note: ${transaction.notes}",
                    fontSize = 11.sp,
                    color = SlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AdminRateHistoryView(
    rateHistory: List<RateHistoryEntry>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Daily Rate Update History", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NavyDark)
        Text("Immutable audit trail of all published market rates", fontSize = 12.sp, color = SlateMuted)

        Spacer(modifier = Modifier.height(12.dp))

        if (rateHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No history records yet", color = SlateMuted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(rateHistory, key = { it.id }) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(entry.date, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                Surface(
                                    color = if (entry.isMarketOpen) MarketOpenGreenBg else MarketClosedRedBg,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (entry.isMarketOpen) "Open" else "Closed",
                                        color = if (entry.isMarketOpen) MarketOpenGreen else MarketClosedRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(8.dp))

                            if (entry.itemsSnapshot.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    entry.itemsSnapshot.chunked(2).forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            rowItems.forEach { snap ->
                                                Text("${snap.itemName}: ${FormatUtils.formatPkr(snap.rate)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Item 1: ${FormatUtils.formatPkr(entry.item1)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text("Item 2: ${FormatUtils.formatPkr(entry.item2)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text("Item 3: ${FormatUtils.formatPkr(entry.item3)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text("Item 4: ${FormatUtils.formatPkr(entry.item4)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            if (entry.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Note: ${entry.note}", fontSize = 11.sp, color = SlateMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminNotificationsView(
    notifications: List<AppNotification>,
    onSendBroadcast: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showComposer by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Broadcast Notifications", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                Text("Send alerts directly to all customer devices", fontSize = 12.sp, color = SlateMuted)
            }

            Button(
                onClick = { showComposer = !showComposer },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (showComposer) "Hide" else "Compose")
            }
        }

        AnimatedVisibility(visible = showComposer) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Create Broadcast Message", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NavyDark)

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Notification Title") },
                        placeholder = { Text("e.g. Special Rate Notice") },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Notification Message") },
                        placeholder = { Text("Write message here...") },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 3
                    )

                    Button(
                        onClick = {
                            if (title.isNotBlank() && message.isNotBlank()) {
                                onSendBroadcast(title, message)
                                title = ""
                                message = ""
                                showComposer = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Send Broadcast to All Customers")
                    }
                }
            }
        }

        Text("Notification Log (${notifications.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No notifications recorded yet", color = SlateMuted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(notifications, key = { it.id }) { notif ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(notif.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                Text(FormatUtils.formatTimeOnly(notif.timestamp), fontSize = 11.sp, color = SlateMuted)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(notif.message, fontSize = 13.sp, color = SlateMuted)
                        }
                    }
                }
            }
        }
    }
}

