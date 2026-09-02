package com.example.ui.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppNotification
import com.example.model.BalanceType
import com.example.model.Customer
import com.example.model.MarketRates
import com.example.model.RateHistoryEntry
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.ui.common.BalanceBadge
import com.example.ui.common.MarketStatusBanner
import com.example.ui.common.NetworkStatusBar
import com.example.ui.common.RateItemCard
import com.example.ui.theme.*
import com.example.util.FormatUtils
import androidx.compose.ui.text.style.TextOverflow

enum class CustomerNavTab {
    DASHBOARD,
    LEDGER,
    HISTORY,
    NOTIFICATIONS,
    PROFILE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    customerViewModel: CustomerViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customer by customerViewModel.customerFlow.collectAsState()
    val marketRates by customerViewModel.currentRatesFlow.collectAsState()
    val rateHistory by customerViewModel.rateHistoryFlow.collectAsState()
    val notifications by customerViewModel.notificationsFlow.collectAsState()
    val myTransactions by customerViewModel.myTransactionsFlow.collectAsState()
    val isOnline by customerViewModel.isOnlineFlow.collectAsState()
    val isAccountBlocked by customerViewModel.isAccountBlocked.collectAsState()

    var currentTab by remember { mutableStateOf(CustomerNavTab.DASHBOARD) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val unreadCount = notifications.count { !it.isRead }

    if (isAccountBlocked) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = "Account Deactivated",
                    fontWeight = FontWeight.Bold,
                    color = ReceivableRed
                )
            },
            text = {
                Text("Your account has been deactivated by the Administrator. Please contact Mujahid Accounts office for reactivation.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        customerViewModel.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
                ) {
                    Text("OK, Log Out")
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out of your account on this device?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        customerViewModel.logout()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mujahid Accounts",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = NavyDark
                        )
                        Text(
                            text = customer?.name?.let { "Welcome, $it" } ?: "Customer Portal",
                            fontSize = 12.sp,
                            color = SlateMuted
                        )
                    }
                },
                actions = {
                    // Online Status Indicator
                    Surface(
                        shape = CircleShape,
                        color = if (isOnline) MarketOpenGreenBg else Color(0xFFFEF3C7),
                        modifier = Modifier.padding(end = 6.dp)
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
                                    .background(if (isOnline) MarketOpenGreen else Color(0xFFD97706))
                            )
                            Text(
                                text = if (isOnline) "Live" else "Offline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) MarketOpenGreen else Color(0xFFD97706)
                            )
                        }
                    }

                    // Notification Bell
                    IconButton(
                        onClick = { currentTab = CustomerNavTab.NOTIFICATIONS },
                        modifier = Modifier.testTag("button_customer_notifications")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = ReceivableRed,
                                        contentColor = Color.White
                                    ) {
                                        Text("$unreadCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = NavyDark
                            )
                        }
                    }

                    // Logout Icon
                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.testTag("button_customer_logout")
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
                    selected = currentTab == CustomerNavTab.DASHBOARD,
                    onClick = { currentTab = CustomerNavTab.DASHBOARD },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    modifier = Modifier.testTag("nav_customer_dashboard")
                )
                NavigationBarItem(
                    selected = currentTab == CustomerNavTab.LEDGER,
                    onClick = { currentTab = CustomerNavTab.LEDGER },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Ledger") },
                    label = { Text("Ledger / کھاتہ") },
                    modifier = Modifier.testTag("nav_customer_ledger")
                )
                NavigationBarItem(
                    selected = currentTab == CustomerNavTab.HISTORY,
                    onClick = { currentTab = CustomerNavTab.HISTORY },
                    icon = { Icon(Icons.Default.History, contentDescription = "Rate History") },
                    label = { Text("Rates") },
                    modifier = Modifier.testTag("nav_customer_history")
                )
                NavigationBarItem(
                    selected = currentTab == CustomerNavTab.NOTIFICATIONS,
                    onClick = { currentTab = CustomerNavTab.NOTIFICATIONS },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(containerColor = ReceivableRed) { Text("$unreadCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                        }
                    },
                    label = { Text("Alerts") },
                    modifier = Modifier.testTag("nav_customer_alerts")
                )
                NavigationBarItem(
                    selected = currentTab == CustomerNavTab.PROFILE,
                    onClick = { currentTab = CustomerNavTab.PROFILE },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    modifier = Modifier.testTag("nav_customer_profile")
                )
            }
        },
        containerColor = LightSurface,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NetworkStatusBar(isOnline = isOnline)

            when (currentTab) {
                CustomerNavTab.DASHBOARD -> CustomerDashboardView(
                    customer = customer,
                    marketRates = marketRates,
                    transactions = myTransactions,
                    customerViewModel = customerViewModel,
                    onViewHistoryClick = { currentTab = CustomerNavTab.HISTORY },
                    onViewLedgerClick = { currentTab = CustomerNavTab.LEDGER }
                )
                CustomerNavTab.LEDGER -> CustomerLedgerView(
                    customer = customer,
                    transactions = myTransactions
                )
                CustomerNavTab.HISTORY -> CustomerRateHistoryView(
                    rateHistory = rateHistory
                )
                CustomerNavTab.NOTIFICATIONS -> CustomerNotificationsView(
                    notifications = notifications,
                    onMarkRead = { customerViewModel.markNotificationAsRead(it) }
                )
                CustomerNavTab.PROFILE -> CustomerProfileView(
                    customer = customer,
                    onLogoutClick = { showLogoutDialog = true }
                )
            }
        }
    }
}

@Composable
fun CustomerDashboardView(
    customer: Customer?,
    marketRates: MarketRates?,
    transactions: List<TransactionRecord> = emptyList(),
    customerViewModel: CustomerViewModel,
    onViewHistoryClick: () -> Unit,
    onViewLedgerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isMarketOpen = marketRates?.isMarketOpen ?: true
    val effectiveRates = customerViewModel.getEffectiveRates(customer, marketRates)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome,",
                    fontSize = 14.sp,
                    color = SlateMuted
                )
                Text(
                    text = customer?.name ?: "Customer",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NavyDark
                )
            }

            Surface(
                color = NavyPrimary,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (customer?.name?.firstOrNull() ?: 'C').uppercase(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==================== MY BALANCE CARD ====================
        val balance = customer?.balance ?: 0.0
        val balanceType = customer?.balanceType ?: BalanceType.RECEIVABLE
        val isReceivable = balanceType == BalanceType.RECEIVABLE

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("customer_balance_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isReceivable) ReceivableRedBg else PayableGreenBg
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    if (isReceivable) ReceivableRedBorder else PayableGreenBorder
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = if (isReceivable) ReceivableRed else PayableGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "MY BALANCE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = if (isReceivable) ReceivableRed else PayableGreen
                        )
                    }

                    BalanceBadge(balanceType = balanceType, isLarge = false)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = FormatUtils.formatPkr(balance),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isReceivable) ReceivableRed else PayableGreen,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isReceivable)
                                "Amount you will receive from business"
                            else
                                "Amount you have to pay to business",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = NavyDark
                        )
                        Text(
                            text = if (isReceivable) "GET" else "GIVE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isReceivable) ReceivableRed else PayableGreen
                        )
                    }
                }
            }
        }

        // ==================== RECENT TRANSACTIONS / LEDGER QUICK CARD ====================
        if (transactions.isNotEmpty()) {
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = NavyDark, modifier = Modifier.size(18.dp))
                            Text("Recent Transactions (کھاتہ)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                        }

                        TextButton(onClick = onViewLedgerClick) {
                            Text("View All", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateAccent)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    transactions.take(3).forEach { tx ->
                        val isBill = tx.type == TransactionType.BILL
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isBill) Color(0xFFFEF2F2) else PayableGreenBg,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isBill) Icons.Default.ShoppingCart else Icons.Default.Payment,
                                            contentDescription = null,
                                            tint = if (isBill) ReceivableRed else PayableGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = if (isBill) "Bill: ${tx.itemName} (${tx.quantity} ${tx.unit})" else "Payment (${tx.paymentMethod})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NavyDark
                                    )
                                    Text(text = tx.date, fontSize = 11.sp, color = SlateMuted)
                                }
                            }

                            Text(
                                text = (if (isBill) "+" else "-") + " " + FormatUtils.formatPkr(tx.amount),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBill) ReceivableRed else PayableGreen
                            )
                        }
                        if (tx != transactions.take(3).last()) {
                            HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
        }

        // ==================== MARKET STATUS BANNER ====================
        MarketStatusBanner(
            isOpen = isMarketOpen,
            lastUpdateDate = marketRates?.date
        )

        if (isMarketOpen) {
            // ==================== DAILY MARKET RATES HEADER ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Market Rates",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                    if (marketRates?.updatedTime != null) {
                        Text(
                            text = "Last updated: ${FormatUtils.formatDateTime(marketRates.updatedTime)}",
                            fontSize = 12.sp,
                            color = SlateMuted
                        )
                    }
                }

                TextButton(
                    onClick = onViewHistoryClick,
                    modifier = Modifier.testTag("button_view_rate_history")
                ) {
                    Text(
                        text = "History →",
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                }
            }

            // ==================== ITEM RATES ====================
            effectiveRates.forEach { rateInfo ->
                RateItemCard(
                    itemName = rateInfo.itemName,
                    currentRate = rateInfo.currentRate,
                    previousRate = rateInfo.previousRate,
                    isCustomRate = rateInfo.isCustomRate,
                    isMarketOpen = isMarketOpen,
                    lastUpdateText = marketRates?.updatedTime?.let { FormatUtils.formatDateTime(it) }
                )
            }
        } else {
            // ==================== MARKET CLOSED NOTICE (NO RATES SHOWN) ====================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("market_closed_notice_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFECDD3))),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MarketClosedRedBg,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Market Closed",
                                tint = MarketClosedRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Text(
                        text = "Market is Closed",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )

                    Text(
                        text = "Rates are currently unavailable because the market is closed. Live daily rates will be displayed here as soon as trading resumes.",
                        fontSize = 13.sp,
                        color = SlateMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = onViewHistoryClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyDark)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Rate History", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun CustomerLedgerView(
    customer: Customer?,
    transactions: List<TransactionRecord>,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, BILL, PAYMENT
    var selectedTxForDetail by remember { mutableStateOf<TransactionRecord?>(null) }

    val filteredTransactions = when (selectedFilter) {
        "BILL" -> transactions.filter { it.type == TransactionType.BILL }
        "PAYMENT" -> transactions.filter { it.type == TransactionType.PAYMENT }
        else -> transactions
    }

    val totalPurchased = transactions.filter { it.type == TransactionType.BILL }.sumOf { it.amount }
    val totalPaid = transactions.filter { it.type == TransactionType.PAYMENT }.sumOf { it.amount }

    if (selectedTxForDetail != null) {
        val tx = selectedTxForDetail!!
        val isBill = tx.type == TransactionType.BILL
        AlertDialog(
            onDismissRequest = { selectedTxForDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if (isBill) Icons.Default.ShoppingCart else Icons.Default.Payment,
                        contentDescription = null,
                        tint = if (isBill) ReceivableRed else PayableGreen
                    )
                    Text(if (isBill) "Bill Details (مال بل)" else "Payment Voucher (ادائیگی رسید)", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Date:", color = SlateMuted, fontSize = 13.sp)
                        Text(tx.date, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    if (isBill && tx.billNumber.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bill Number:", color = SlateMuted, fontSize = 13.sp)
                            Text("#${tx.billNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    if (isBill && tx.itemName.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Item Purchased:", color = SlateMuted, fontSize = 13.sp)
                            Text(tx.itemName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Quantity / Rate:", color = SlateMuted, fontSize = 13.sp)
                            Text("${tx.quantity} ${tx.unit} @ Rs. ${tx.rate}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                    if (!isBill) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Payment Mode:", color = SlateMuted, fontSize = 13.sp)
                            Text(tx.paymentMethod, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Amount:", color = SlateMuted, fontSize = 14.sp)
                        Text(FormatUtils.formatPkr(tx.amount), fontWeight = FontWeight.Black, fontSize = 16.sp, color = if (isBill) ReceivableRed else PayableGreen)
                    }
                    if (tx.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Note / Description: ${tx.notes}", fontSize = 12.sp, color = SlateMuted)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedTxForDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
                ) {
                    Text("Close")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Balance Header Card
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
                        Text("Current Balance (موجودہ بقایا)", fontSize = 12.sp, color = SlateMuted)
                        Text(
                            text = FormatUtils.formatPkr(customer?.balance ?: 0.0),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = if (customer?.balanceType == BalanceType.RECEIVABLE) ReceivableRed else PayableGreen
                        )
                    }
                    BalanceBadge(balanceType = customer?.balanceType ?: BalanceType.RECEIVABLE, isLarge = false)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Purchases (مال خریدا)", fontSize = 11.sp, color = SlateMuted)
                        Text(FormatUtils.formatPkr(totalPurchased), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Paid (کل ادائیگی)", fontSize = 11.sp, color = SlateMuted)
                        Text(FormatUtils.formatPkr(totalPaid), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PayableGreen)
                    }
                }
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "ALL" to "All (${transactions.size})",
                "BILL" to "Purchased Bills (${transactions.count { it.type == TransactionType.BILL }})",
                "PAYMENT" to "Payments (${transactions.count { it.type == TransactionType.PAYMENT }})"
            ).forEach { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedFilter == key) FontWeight.Bold else FontWeight.Normal) }
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
                    Text("No transactions recorded yet", fontSize = 15.sp, color = SlateMuted)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredTransactions, key = { it.id }) { tx ->
                    val isBill = tx.type == TransactionType.BILL
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTxForDetail = tx }
                            .testTag("cust_tx_card_${tx.id}"),
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isBill) Color(0xFFFEF2F2) else PayableGreenBg,
                                        modifier = Modifier.size(38.dp)
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
                                            text = if (isBill) {
                                                if (tx.itemName.isNotBlank()) "Bill: ${tx.itemName}" else "Bill Purchase (مال)"
                                            } else {
                                                "Payment (${tx.paymentMethod})"
                                            },
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NavyDark
                                        )
                                        Text(
                                            text = "${tx.date}${if (isBill && tx.billNumber.isNotBlank()) " • Bill #${tx.billNumber}" else ""}",
                                            fontSize = 12.sp,
                                            color = SlateMuted
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = (if (isBill) "+" else "-") + " " + FormatUtils.formatPkr(tx.amount),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isBill) ReceivableRed else PayableGreen
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isBill) ReceivableRedBg else PayableGreenBg
                                    ) {
                                        Text(
                                            text = if (isBill) "PURCHASE" else "PAID",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isBill) ReceivableRed else PayableGreen,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            if (isBill && tx.quantity > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Qty: ${tx.quantity} ${tx.unit} @ Rs. ${tx.rate}/${tx.unit}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = NavyDark,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            if (tx.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Note: ${tx.notes}",
                                    fontSize = 11.sp,
                                    color = SlateMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerRateHistoryView(
    rateHistory: List<RateHistoryEntry>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rate History Timeline",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )
            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${rateHistory.size} Records",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateMuted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (rateHistory.isEmpty()) {
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
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = SlateMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No rate history records yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlateMuted
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rateHistory, key = { it.id }) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rate_history_entry_${entry.id}"),
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
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = NavyDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = entry.date,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark
                                    )
                                }

                                if (entry.isMarketOpen) {
                                    Surface(
                                        color = MarketOpenGreenBg,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Market Open",
                                            color = MarketOpenGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        color = MarketClosedRedBg,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Market Closed",
                                            color = MarketClosedRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(10.dp))

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
                                    Column {
                                        Text("Item 1", fontSize = 11.sp, color = SlateMuted)
                                        Text(FormatUtils.formatPkr(entry.item1), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                    }
                                    Column {
                                        Text("Item 2", fontSize = 11.sp, color = SlateMuted)
                                        Text(FormatUtils.formatPkr(entry.item2), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                    }
                                    Column {
                                        Text("Item 3", fontSize = 11.sp, color = SlateMuted)
                                        Text(FormatUtils.formatPkr(entry.item3), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                    }
                                    Column {
                                        Text("Item 4", fontSize = 11.sp, color = SlateMuted)
                                        Text(FormatUtils.formatPkr(entry.item4), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                    }
                                }
                            }

                            if (entry.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Note: ${entry.note}",
                                    fontSize = 11.sp,
                                    color = SlateMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerNotificationsView(
    notifications: List<AppNotification>,
    onMarkRead: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Notifications & Rate Alerts",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = NavyDark
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (notifications.isEmpty()) {
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
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = SlateMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No notifications yet",
                        fontSize = 16.sp,
                        color = SlateMuted
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMarkRead(notif.id) }
                            .testTag("notification_item_${notif.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.isRead) Color.White else Color(0xFFF8FAFC)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (notif.isRead) CardBorder else InfoBlue.copy(alpha = 0.4f)
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (notif.isRead) Color(0xFFE2E8F0) else InfoBlueBg,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = if (notif.isRead) SlateMuted else InfoBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = notif.title,
                                        fontSize = 14.sp,
                                        fontWeight = if (notif.isRead) FontWeight.SemiBold else FontWeight.Bold,
                                        color = NavyDark
                                    )
                                    Text(
                                        text = FormatUtils.formatTimeOnly(notif.timestamp),
                                        fontSize = 11.sp,
                                        color = SlateMuted
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = notif.message,
                                    fontSize = 13.sp,
                                    color = SlateMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerProfileView(
    customer: Customer?,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "My Account & Settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = NavyDark
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = NavyDark,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (customer?.name?.firstOrNull() ?: 'U').uppercase(),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = customer?.name ?: "Customer Name",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )

                Text(
                    text = "@${customer?.username ?: "username"}",
                    fontSize = 13.sp,
                    color = SlateMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = if (customer?.isActive == true) MarketOpenGreenBg else MarketClosedRedBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (customer?.isActive == true) "● Active Account" else "● Suspended",
                        color = if (customer?.isActive == true) MarketOpenGreen else MarketClosedRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Account Details",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Phone / Contact", fontSize = 13.sp, color = SlateMuted)
                    Text(customer?.phone?.ifBlank { "Not provided" } ?: "Not provided", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = NavyDark)
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Account ID", fontSize = 13.sp, color = SlateMuted)
                    Text(customer?.id ?: "", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = NavyDark)
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Account Type", fontSize = 13.sp, color = SlateMuted)
                    Text(
                        text = if (customer?.hasCustomRates == true) "Custom Rate Partner" else "Standard Market Rates",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (customer?.hasCustomRates == true) GoldAccent else NavyDark
                    )
                }
            }
        }

        // Security Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MarketOpenGreen, modifier = Modifier.size(18.dp))
                    Text("Security & Persistent Session", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                }
                Text(
                    text = "Your login session is securely stored on this device. You will not be prompted to log in again unless you log out or your account is deactivated by the administrator.",
                    fontSize = 12.sp,
                    color = SlateMuted
                )
            }
        }

        // Logout Button
        OutlinedButton(
            onClick = onLogoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("button_profile_logout"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ReceivableRed),
            border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ReceivableRed))
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out from Device", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
