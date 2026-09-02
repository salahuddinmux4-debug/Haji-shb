package com.example.ui.admin

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.BalanceType
import com.example.model.Customer
import com.example.model.MarketItem
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.ui.common.BalanceBadge
import com.example.ui.theme.*
import com.example.util.FormatUtils

@Composable
fun AddCustomerDialog(
    items: List<MarketItem> = emptyList(),
    onDismiss: () -> Unit,
    onAddCustomer: (
        name: String,
        username: String,
        password: String,
        phone: String,
        balance: Double,
        balanceType: BalanceType,
        hasCustomRates: Boolean,
        c1: Double?,
        c2: Double?,
        c3: Double?,
        c4: Double?,
        customRatesMap: Map<String, Double>
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("") }
    var balanceType by remember { mutableStateOf(BalanceType.RECEIVABLE) }
    var hasCustomRates by remember { mutableStateOf(false) }

    val customRateInputs = remember(items) {
        mutableStateMapOf<String, String>().apply {
            items.forEach { put(it.id, "") }
        }
    }

    var customItem1 by remember { mutableStateOf("") }
    var customItem2 by remember { mutableStateOf("") }
    var customItem3 by remember { mutableStateOf("") }
    var customItem4 by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add New Customer",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = CardBorder)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (errorMessage != null) {
                        Surface(
                            color = ReceivableRedBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = ReceivableRed,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMessage = null },
                        label = { Text("Customer Full Name *") },
                        placeholder = { Text("e.g. Abdul Hameed") },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("input_customer_name"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; errorMessage = null },
                        label = { Text("Login Username *") },
                        placeholder = { Text("e.g. abdul_hameed") },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("input_customer_username"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Login Password *") },
                        placeholder = { Text("••••••••") },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("input_customer_password"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone / Mobile (Optional)") },
                        placeholder = { Text("0300-1234567") },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    // Balance Section
                    Text("Account Balance", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)

                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text("Initial Balance (Rs.)") },
                        placeholder = { Text("0") },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("input_customer_balance"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Balance Type Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = balanceType == BalanceType.RECEIVABLE,
                            onClick = { balanceType = BalanceType.RECEIVABLE },
                            label = { Text("GET / RECEIVABLE (Red)", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f).testTag("chip_receivable"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ReceivableRedBg,
                                selectedLabelColor = ReceivableRed
                            )
                        )
                        FilterChip(
                            selected = balanceType == BalanceType.PAYABLE,
                            onClick = { balanceType = BalanceType.PAYABLE },
                            label = { Text("GIVE / PAYABLE (Green)", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f).testTag("chip_payable"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PayableGreenBg,
                                selectedLabelColor = PayableGreen
                            )
                        )
                    }

                    // Customer Specific Rate Toggle
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Use Custom Rate for This Customer", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                            Text("Overrides the global market rate for this specific customer", fontSize = 11.sp, color = SlateMuted)
                        }
                        Switch(
                            checked = hasCustomRates,
                            onCheckedChange = { hasCustomRates = it },
                            modifier = Modifier.testTag("switch_custom_rate")
                        )
                    }

                    if (hasCustomRates) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (items.isNotEmpty()) {
                                items.forEach { item ->
                                    val currentVal = customRateInputs[item.id] ?: ""
                                    OutlinedTextField(
                                        value = currentVal,
                                        onValueChange = { customRateInputs[item.id] = it },
                                        label = { Text("${item.name} Custom Rate (Rs.)") },
                                        placeholder = { Text("Default: Rs. ${item.currentRate}") },
                                        colors = appTextFieldColors(),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            } else {
                                OutlinedTextField(
                                    value = customItem1,
                                    onValueChange = { customItem1 = it },
                                    label = { Text("Item 1 Custom Rate (Rs.)") },
                                    colors = appTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = customItem2,
                                    onValueChange = { customItem2 = it },
                                    label = { Text("Item 2 Custom Rate (Rs.)") },
                                    colors = appTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = customItem3,
                                    onValueChange = { customItem3 = it },
                                    label = { Text("Item 3 Custom Rate (Rs.)") },
                                    colors = appTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = customItem4,
                                    onValueChange = { customItem4 = it },
                                    label = { Text("Item 4 Custom Rate (Rs.)") },
                                    colors = appTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Please enter customer name"
                                return@Button
                            }
                            if (username.isBlank()) {
                                errorMessage = "Please enter login username"
                                return@Button
                            }
                            if (password.isBlank()) {
                                errorMessage = "Please enter login password"
                                return@Button
                            }

                            val bal = balanceText.toDoubleOrNull() ?: 0.0
                            val customMap = mutableMapOf<String, Double>()
                            customRateInputs.forEach { (k, v) ->
                                v.toDoubleOrNull()?.let { customMap[k] = it }
                            }

                            val c1 = customItem1.toDoubleOrNull() ?: items.getOrNull(0)?.let { customMap[it.id] }
                            val c2 = customItem2.toDoubleOrNull() ?: items.getOrNull(1)?.let { customMap[it.id] }
                            val c3 = customItem3.toDoubleOrNull() ?: items.getOrNull(2)?.let { customMap[it.id] }
                            val c4 = customItem4.toDoubleOrNull() ?: items.getOrNull(3)?.let { customMap[it.id] }

                            onAddCustomer(name, username, password, phone, bal, balanceType, hasCustomRates, c1, c2, c3, c4, customMap)
                        },
                        modifier = Modifier.weight(1f).testTag("button_save_customer"),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Customer")
                    }
                }
            }
        }
    }
}

@Composable
fun EditCustomerDialog(
    customer: Customer,
    items: List<MarketItem> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Customer, newPassword: String?) -> Unit
) {
    var name by remember { mutableStateOf(customer.name) }
    var phone by remember { mutableStateOf(customer.phone) }
    var newPassword by remember { mutableStateOf("") }
    var hasCustomRates by remember { mutableStateOf(customer.hasCustomRates) }

    val customRateInputs = remember(items, customer) {
        mutableStateMapOf<String, String>().apply {
            items.forEach { item ->
                val existing = customer.customRatesMap[item.id]
                put(item.id, existing?.toString() ?: "")
            }
        }
    }

    var customItem1 by remember { mutableStateOf(customer.customRateItem1?.toString() ?: "") }
    var customItem2 by remember { mutableStateOf(customer.customRateItem2?.toString() ?: "") }
    var customItem3 by remember { mutableStateOf(customer.customRateItem3?.toString() ?: "") }
    var customItem4 by remember { mutableStateOf(customer.customRateItem4?.toString() ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Edit Customer Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = CardBorder)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (errorMessage != null) {
                        Surface(
                            color = ReceivableRedBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = errorMessage!!, color = ReceivableRed, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Customer Name") },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = customer.username,
                        onValueChange = {},
                        label = { Text("Username (Fixed)") },
                        enabled = false,
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Reset Password (Leave blank to keep unchanged)") },
                        placeholder = { Text("New password") },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Use Custom Rate for This Customer", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                            Text("Set customer-specific rate if required", fontSize = 11.sp, color = SlateMuted)
                        }
                        Switch(
                            checked = hasCustomRates,
                            onCheckedChange = { hasCustomRates = it }
                        )
                    }

                    if (hasCustomRates) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (items.isNotEmpty()) {
                                items.forEach { item ->
                                    val currentVal = customRateInputs[item.id] ?: ""
                                    OutlinedTextField(
                                        value = currentVal,
                                        onValueChange = { customRateInputs[item.id] = it },
                                        label = { Text("${item.name} Custom Rate (Rs.)") },
                                        placeholder = { Text("Default: Rs. ${item.currentRate}") },
                                        colors = appTextFieldColors(),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            } else {
                                OutlinedTextField(
                                    value = customItem1,
                                    onValueChange = { customItem1 = it },
                                    label = { Text("Item 1 Custom Rate (Rs.)") },
                                    colors = appTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = customItem2,
                                    onValueChange = { customItem2 = it },
                                    label = { Text("Item 2 Custom Rate (Rs.)") },
                                    colors = appTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = customItem3,
                                    onValueChange = { customItem3 = it },
                                    label = { Text("Item 3 Custom Rate (Rs.)") },
                                    colors = appTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = customItem4,
                                    onValueChange = { customItem4 = it },
                                    label = { Text("Item 4 Custom Rate (Rs.)") },
                                    colors = appTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Customer name cannot be blank"
                                return@Button
                            }

                            val customMap = mutableMapOf<String, Double>()
                            customRateInputs.forEach { (k, v) ->
                                v.toDoubleOrNull()?.let { customMap[k] = it }
                            }

                            val updated = customer.copy(
                                name = name.trim(),
                                phone = phone.trim(),
                                hasCustomRates = hasCustomRates,
                                customRatesMap = customMap,
                                customRateItem1 = customItem1.toDoubleOrNull() ?: items.getOrNull(0)?.let { customMap[it.id] },
                                customRateItem2 = customItem2.toDoubleOrNull() ?: items.getOrNull(1)?.let { customMap[it.id] },
                                customRateItem3 = customItem3.toDoubleOrNull() ?: items.getOrNull(2)?.let { customMap[it.id] },
                                customRateItem4 = customItem4.toDoubleOrNull() ?: items.getOrNull(3)?.let { customMap[it.id] }
                            )
                            onSave(updated, if (newPassword.isNotBlank()) newPassword else null)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateBalanceDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onUpdateBalance: (newBalance: Double, balanceType: BalanceType, note: String) -> Unit
) {
    var balanceText by remember { mutableStateOf(customer.balance.toString()) }
    var balanceType by remember { mutableStateOf(customer.balanceType) }
    var note by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Update Balance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Text(
                    text = "Customer: ${customer.name}",
                    fontSize = 13.sp,
                    color = SlateMuted
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage != null) {
                    Text(errorMessage!!, color = ReceivableRed, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Balance Amount (Rs.)") },
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("input_new_balance"),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Text("Balance Type", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyDark)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = balanceType == BalanceType.RECEIVABLE,
                        onClick = { balanceType = BalanceType.RECEIVABLE },
                        label = { Text("GET (Red)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).testTag("select_receivable"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ReceivableRedBg,
                            selectedLabelColor = ReceivableRed
                        )
                    )
                    FilterChip(
                        selected = balanceType == BalanceType.PAYABLE,
                        onClick = { balanceType = BalanceType.PAYABLE },
                        label = { Text("GIVE (Green)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).testTag("select_payable"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PayableGreenBg,
                            selectedLabelColor = PayableGreen
                        )
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Reason (Optional)") },
                    placeholder = { Text("e.g. Payment received or invoice adjustment") },
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = balanceText.toDoubleOrNull()
                    if (bal == null) {
                        errorMessage = "Please enter a valid numeric amount"
                        return@Button
                    }
                    onUpdateBalance(bal, balanceType, note)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                modifier = Modifier.testTag("button_confirm_update_balance")
            ) {
                Text("Update Balance")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddMarketItemDialog(
    onDismiss: () -> Unit,
    onAddItem: (name: String, initialRate: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add New Market Item", fontWeight = FontWeight.Bold, color = NavyDark)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Text(errorMessage!!, color = ReceivableRed, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Item Name *") },
                    placeholder = { Text("e.g. Item 5 or Product Name") },
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("input_add_item_name"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it; errorMessage = null },
                    label = { Text("Initial Rate (Rs.) *") },
                    placeholder = { Text("e.g. 500.0") },
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("input_add_item_rate"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter an item name"
                        return@Button
                    }
                    val rate = rateText.toDoubleOrNull()
                    if (rate == null || rate < 0) {
                        errorMessage = "Please enter a valid rate (e.g. 100.0)"
                        return@Button
                    }
                    onAddItem(name.trim(), rate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                modifier = Modifier.testTag("button_confirm_add_item")
            ) {
                Text("Add Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditMarketItemNameDialog(
    item: MarketItem,
    onDismiss: () -> Unit,
    onRename: (newName: String) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Rename Item", fontWeight = FontWeight.Bold, color = NavyDark)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Text(errorMessage!!, color = ReceivableRed, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Item Name *") },
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_item_name"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Item name cannot be empty"
                        return@Button
                    }
                    onRename(name.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                modifier = Modifier.testTag("button_confirm_rename_item")
            ) {
                Text("Save Name")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ==================== ADD BILL / MAAL PURCHASE DIALOG ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBillDialog(
    customers: List<Customer>,
    marketItems: List<MarketItem>,
    preSelectedCustomer: Customer? = null,
    onDismiss: () -> Unit,
    onAddBill: (
        customerId: String,
        itemId: String?,
        itemName: String,
        quantity: Double,
        unit: String,
        rate: Double,
        totalAmount: Double,
        billNumber: String,
        notes: String,
        date: String
    ) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf(preSelectedCustomer ?: customers.firstOrNull()) }
    var selectedItem by remember { mutableStateOf(marketItems.firstOrNull()) }
    var customItemName by remember { mutableStateOf("") }
    var isCustomItemMode by remember { mutableStateOf(false) }

    var quantityText by remember { mutableStateOf("1") }
    var selectedUnit by remember { mutableStateOf("Bags (بورے)") }
    var rateText by remember {
        val initialRate = selectedCustomer?.let { cust ->
            selectedItem?.let { item ->
                cust.customRatesMap[item.id] ?: item.currentRate
            }
        } ?: selectedItem?.currentRate ?: 0.0
        mutableStateOf(if (initialRate > 0) initialRate.toString() else "")
    }
    var totalAmountText by remember { mutableStateOf("") }
    var isManualTotal by remember { mutableStateOf(false) }

    var billNumber by remember {
        mutableStateOf("BILL-${System.currentTimeMillis().toString().takeLast(5)}")
    }
    var billDate by remember { mutableStateOf(FormatUtils.formatDateOnly()) }
    var notes by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var itemDropdownExpanded by remember { mutableStateOf(false) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    val units = listOf("Bags (بورے)", "Maund (من)", "Kg (کلو)", "Cartons (کارٹن)", "Units (عدد)", "Truck (گاڑی)")

    // Update rate when customer or item changes if not in custom mode
    fun updateAutoRate(cust: Customer?, item: MarketItem?) {
        if (cust != null && item != null && !isCustomItemMode) {
            val r = cust.customRatesMap[item.id] ?: item.currentRate
            rateText = r.toString()
            val q = quantityText.toDoubleOrNull() ?: 1.0
            if (!isManualTotal) {
                totalAmountText = String.format(java.util.Locale.US, "%.0f", q * r)
            }
        }
    }

    // Auto-calculate total amount when qty or rate changes
    LaunchedEffect(quantityText, rateText, isManualTotal) {
        if (!isManualTotal) {
            val q = quantityText.toDoubleOrNull() ?: 0.0
            val r = rateText.toDoubleOrNull() ?: 0.0
            val calculated = q * r
            if (calculated > 0) {
                totalAmountText = String.format(java.util.Locale.US, "%.0f", calculated)
            }
        }
    }

    val scrollState = rememberScrollState()

    // Calculated balance impact preview
    val billAmount = totalAmountText.toDoubleOrNull() ?: 0.0
    val currentCust = selectedCustomer
    val (newPreviewBalance, newPreviewType) = remember(currentCust, billAmount) {
        if (currentCust != null) {
            val signed = if (currentCust.balanceType == BalanceType.RECEIVABLE) currentCust.balance else -currentCust.balance
            val newSigned = signed + billAmount
            if (newSigned >= 0) Pair(newSigned, BalanceType.RECEIVABLE) else Pair(-newSigned, BalanceType.PAYABLE)
        } else Pair(0.0, BalanceType.RECEIVABLE)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = ReceivableRedBg,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = ReceivableRed, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text(
                                text = "New Bill (مال خریداری)",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )
                            Text(
                                text = "Goods / Maal purchase from customer",
                                fontSize = 11.sp,
                                color = SlateMuted
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = CardBorder)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (errorMessage != null) {
                        Surface(
                            color = ReceivableRedBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = ReceivableRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // 1. CUSTOMER SELECTOR
                    Text("1. Customer (گاہک کا نام) *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    ExposedDropdownMenuBox(
                        expanded = customerDropdownExpanded,
                        onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCustomer?.let { "${it.name} (${it.phone.ifBlank { "@" + it.username }})" } ?: "Select Customer",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                            colors = appTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("select_bill_customer"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = customerDropdownExpanded,
                            onDismissRequest = { customerDropdownExpanded = false }
                        ) {
                            customers.forEach { cust ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(cust.name, fontWeight = FontWeight.Bold, color = NavyDark)
                                            Text(
                                                "Current Balance: ${FormatUtils.formatPkr(cust.balance)} (${cust.balanceType.name})",
                                                fontSize = 11.sp,
                                                color = if (cust.balanceType == BalanceType.RECEIVABLE) ReceivableRed else PayableGreen
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCustomer = cust
                                        customerDropdownExpanded = false
                                        errorMessage = null
                                        updateAutoRate(cust, selectedItem)
                                    }
                                )
                            }
                        }
                    }

                    // 2. ITEM SELECTION
                    Text("2. Item / Product (مال / آئٹم) *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isCustomItemMode,
                            onClick = {
                                isCustomItemMode = false
                                updateAutoRate(selectedCustomer, selectedItem)
                            },
                            label = { Text("Market Items") },
                            leadingIcon = { if (!isCustomItemMode) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                        FilterChip(
                            selected = isCustomItemMode,
                            onClick = { isCustomItemMode = true },
                            label = { Text("Custom / Other Item") },
                            leadingIcon = { if (isCustomItemMode) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }

                    if (!isCustomItemMode) {
                        ExposedDropdownMenuBox(
                            expanded = itemDropdownExpanded,
                            onExpandedChange = { itemDropdownExpanded = !itemDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedItem?.let { "${it.name} - Rate: ${FormatUtils.formatPkr(it.currentRate)}" } ?: "Select Item",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemDropdownExpanded) },
                                colors = appTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .testTag("select_bill_item"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = itemDropdownExpanded,
                                onDismissRequest = { itemDropdownExpanded = false }
                            ) {
                                marketItems.forEach { itm ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(itm.name, fontWeight = FontWeight.SemiBold)
                                                Text(FormatUtils.formatPkr(itm.currentRate), color = SlateMuted)
                                            }
                                        },
                                        onClick = {
                                            selectedItem = itm
                                            itemDropdownExpanded = false
                                            updateAutoRate(selectedCustomer, itm)
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = customItemName,
                            onValueChange = { customItemName = it; errorMessage = null },
                            label = { Text("Custom Item / Crop Name *") },
                            placeholder = { Text("e.g. Gandum, Chawal, Kapas, Makai...") },
                            colors = appTextFieldColors(),
                            modifier = Modifier.fillMaxWidth().testTag("input_custom_item_name"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    // 3. QUANTITY, UNIT & RATE
                    Text("3. Quantity, Unit & Rate *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = {
                                quantityText = it
                                errorMessage = null
                            },
                            label = { Text("Qty / Weight *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = appTextFieldColors(),
                            modifier = Modifier.weight(1f).testTag("input_bill_quantity"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        // Unit selector
                        ExposedDropdownMenuBox(
                            expanded = unitDropdownExpanded,
                            onExpandedChange = { unitDropdownExpanded = !unitDropdownExpanded },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            OutlinedTextField(
                                value = selectedUnit,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Unit") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                                colors = appTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .testTag("select_bill_unit"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = unitDropdownExpanded,
                                onDismissRequest = { unitDropdownExpanded = false }
                            ) {
                                units.forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(u) },
                                        onClick = {
                                            selectedUnit = u
                                            unitDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Rate & Total Amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = rateText,
                            onValueChange = {
                                rateText = it
                                errorMessage = null
                            },
                            label = { Text("Rate / Unit (Rs.) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = appTextFieldColors(),
                            modifier = Modifier.weight(1f).testTag("input_bill_rate"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = totalAmountText,
                            onValueChange = {
                                totalAmountText = it
                                isManualTotal = true
                                errorMessage = null
                            },
                            label = { Text("Total Bill (Rs.) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = appTextFieldColors(),
                            modifier = Modifier.weight(1.2f).testTag("input_bill_total_amount"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    // 4. BILL NO & DATE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = billNumber,
                            onValueChange = { billNumber = it },
                            label = { Text("Bill / Invoice #") },
                            colors = appTextFieldColors(),
                            modifier = Modifier.weight(1f).testTag("input_bill_number"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = billDate,
                            onValueChange = { billDate = it },
                            label = { Text("Date") },
                            colors = appTextFieldColors(),
                            modifier = Modifier.weight(1f).testTag("input_bill_date"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    // Notes / Description
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Description / Notes (تفصیل)") },
                        placeholder = { Text("e.g. 50 Bora Gandum Kharidari, Driver name...") },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("input_bill_notes"),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 2
                    )

                    // 5. LIVE BALANCE IMPACT PREVIEW CARD
                    if (selectedCustomer != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = NavyDark.copy(alpha = 0.05f)),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Khata Balance Impact (کھاتہ پر اثر):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Current Balance:", fontSize = 12.sp, color = SlateMuted)
                                    Text(
                                        "${FormatUtils.formatPkr(selectedCustomer!!.balance)} (${selectedCustomer!!.balanceType.name})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedCustomer!!.balanceType == BalanceType.RECEIVABLE) ReceivableRed else PayableGreen
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("+ Bill (Mall Purchase):", fontSize = 12.sp, color = ReceivableRed, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "+ ${FormatUtils.formatPkr(billAmount)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ReceivableRed
                                    )
                                }
                                HorizontalDivider(color = CardBorder)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("New Balance (نیا بقایا):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            FormatUtils.formatPkr(newPreviewBalance),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (newPreviewType == BalanceType.RECEIVABLE) ReceivableRed else PayableGreen
                                        )
                                        BalanceBadge(balanceType = newPreviewType)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (selectedCustomer == null) {
                                errorMessage = "Please select a customer"
                                return@Button
                            }
                            val finalItemName = if (isCustomItemMode) customItemName.trim() else (selectedItem?.name ?: "")
                            if (finalItemName.isBlank()) {
                                errorMessage = "Please provide an item or crop name"
                                return@Button
                            }
                            val q = quantityText.toDoubleOrNull() ?: 0.0
                            val r = rateText.toDoubleOrNull() ?: 0.0
                            val total = totalAmountText.toDoubleOrNull() ?: 0.0

                            if (total <= 0) {
                                errorMessage = "Total bill amount must be greater than 0"
                                return@Button
                            }

                            onAddBill(
                                selectedCustomer!!.id,
                                if (!isCustomItemMode) selectedItem?.id else null,
                                finalItemName,
                                q,
                                selectedUnit,
                                r,
                                total,
                                billNumber.trim(),
                                notes.trim(),
                                billDate.trim()
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ReceivableRed),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("button_save_bill"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Bill (بل درج کریں)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==================== ADD PAYMENT / ADAIGI DIALOG ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentDialog(
    customers: List<Customer>,
    preSelectedCustomer: Customer? = null,
    onDismiss: () -> Unit,
    onAddPayment: (
        customerId: String,
        amount: Double,
        paymentMethod: String,
        referenceNo: String,
        notes: String,
        date: String
    ) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf(preSelectedCustomer ?: customers.firstOrNull()) }
    var amountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("Cash (نقد)") }
    var referenceNo by remember { mutableStateOf("") }
    var paymentDate by remember { mutableStateOf(FormatUtils.formatDateOnly()) }
    var notes by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var methodDropdownExpanded by remember { mutableStateOf(false) }

    val methods = listOf("Cash (نقد)", "Bank Transfer (بینک)", "Cheque (چیک)", "EasyPaisa / JazzCash", "Online")

    val scrollState = rememberScrollState()

    // Calculated balance impact preview
    val payAmount = amountText.toDoubleOrNull() ?: 0.0
    val currentCust = selectedCustomer
    val (newPreviewBalance, newPreviewType) = remember(currentCust, payAmount) {
        if (currentCust != null) {
            val signed = if (currentCust.balanceType == BalanceType.RECEIVABLE) currentCust.balance else -currentCust.balance
            val newSigned = signed - payAmount
            if (newSigned >= 0) Pair(newSigned, BalanceType.RECEIVABLE) else Pair(-newSigned, BalanceType.PAYABLE)
        } else Pair(0.0, BalanceType.RECEIVABLE)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = PayableGreenBg,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Payment, contentDescription = null, tint = PayableGreen, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text(
                                text = "New Payment (ادائیگی / وصولی)",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )
                            Text(
                                text = "Payment given to customer in Cash/Bank",
                                fontSize = 11.sp,
                                color = SlateMuted
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = CardBorder)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (errorMessage != null) {
                        Surface(
                            color = ReceivableRedBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = ReceivableRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // 1. CUSTOMER SELECTOR
                    Text("1. Customer (گاہک کا نام) *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    ExposedDropdownMenuBox(
                        expanded = customerDropdownExpanded,
                        onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCustomer?.let { "${it.name} (${it.phone.ifBlank { "@" + it.username }})" } ?: "Select Customer",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                            colors = appTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("select_payment_customer"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = customerDropdownExpanded,
                            onDismissRequest = { customerDropdownExpanded = false }
                        ) {
                            customers.forEach { cust ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(cust.name, fontWeight = FontWeight.Bold, color = NavyDark)
                                            Text(
                                                "Current Balance: ${FormatUtils.formatPkr(cust.balance)} (${cust.balanceType.name})",
                                                fontSize = 11.sp,
                                                color = if (cust.balanceType == BalanceType.RECEIVABLE) ReceivableRed else PayableGreen
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCustomer = cust
                                        customerDropdownExpanded = false
                                        errorMessage = null
                                    }
                                )
                            }
                        }
                    }

                    // 2. PAYMENT AMOUNT
                    Text("2. Amount & Payment Method *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            errorMessage = null
                        },
                        label = { Text("Payment Amount (رقم - Rs.) *") },
                        placeholder = { Text("e.g. 50000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("input_payment_amount"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    // Payment Method selector
                    ExposedDropdownMenuBox(
                        expanded = methodDropdownExpanded,
                        onExpandedChange = { methodDropdownExpanded = !methodDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedMethod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Mode") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodDropdownExpanded) },
                            colors = appTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("select_payment_method"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = methodDropdownExpanded,
                            onDismissRequest = { methodDropdownExpanded = false }
                        ) {
                            methods.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m) },
                                    onClick = {
                                        selectedMethod = m
                                        methodDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 3. REFERENCE NO & DATE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = referenceNo,
                            onValueChange = { referenceNo = it },
                            label = { Text("Reference / Receipt #") },
                            placeholder = { Text("Optional") },
                            colors = appTextFieldColors(),
                            modifier = Modifier.weight(1f).testTag("input_payment_reference"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = paymentDate,
                            onValueChange = { paymentDate = it },
                            label = { Text("Date") },
                            colors = appTextFieldColors(),
                            modifier = Modifier.weight(1f).testTag("input_payment_date"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    // Notes / Description
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Description / Notes (تفصیل)") },
                        placeholder = { Text("e.g. Cash payment given, cheque details...") },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("input_payment_notes"),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 2
                    )

                    // 4. LIVE BALANCE IMPACT PREVIEW CARD
                    if (selectedCustomer != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = NavyDark.copy(alpha = 0.05f)),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Khata Balance Impact (کھاتہ پر اثر):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Current Balance:", fontSize = 12.sp, color = SlateMuted)
                                    Text(
                                        "${FormatUtils.formatPkr(selectedCustomer!!.balance)} (${selectedCustomer!!.balanceType.name})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedCustomer!!.balanceType == BalanceType.RECEIVABLE) ReceivableRed else PayableGreen
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("- Payment Paid (ادائیگی):", fontSize = 12.sp, color = PayableGreen, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "- ${FormatUtils.formatPkr(payAmount)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PayableGreen
                                    )
                                }
                                HorizontalDivider(color = CardBorder)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("New Balance (نیا بقایا):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            FormatUtils.formatPkr(newPreviewBalance),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (newPreviewType == BalanceType.RECEIVABLE) ReceivableRed else PayableGreen
                                        )
                                        BalanceBadge(balanceType = newPreviewType)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (selectedCustomer == null) {
                                errorMessage = "Please select a customer"
                                return@Button
                            }
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt <= 0) {
                                errorMessage = "Payment amount must be greater than 0"
                                return@Button
                            }

                            onAddPayment(
                                selectedCustomer!!.id,
                                amt,
                                selectedMethod.trim(),
                                referenceNo.trim(),
                                notes.trim(),
                                paymentDate.trim()
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PayableGreen),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("button_save_payment"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Payment (ادائیگی درج کریں)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==================== TRANSACTION DETAIL DIALOG ====================

@Composable
fun TransactionDetailDialog(
    transaction: TransactionRecord,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    if (showConfirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Delete Transaction?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this record? Customer balance will be automatically recalculated and adjusted.") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ReceivableRed)
                ) {
                    Text("Delete Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isBill = transaction.type == TransactionType.BILL
                        Surface(
                            color = if (isBill) ReceivableRedBg else PayableGreenBg,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isBill) Icons.Default.ShoppingCart else Icons.Default.Payment,
                                    contentDescription = null,
                                    tint = if (isBill) ReceivableRed else PayableGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (isBill) "Bill Details (مال خریداری)" else "Payment Receipt (ادائیگی)",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )
                            Text(
                                text = "Ref: ${transaction.billNumber}",
                                fontSize = 11.sp,
                                color = SlateMuted
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = CardBorder)

                // Info Rows
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailRow("Customer:", transaction.customerName)
                    DetailRow("Date:", transaction.date.ifBlank { FormatUtils.formatDateTime(transaction.timestamp) })
                    DetailRow("Type:", if (transaction.type == TransactionType.BILL) "Mall Purchase (Bill)" else "Payment (Adaigi)")
                    
                    if (transaction.type == TransactionType.BILL && transaction.itemName.isNotBlank()) {
                        DetailRow("Item / Maal:", transaction.itemName)
                        if (transaction.quantity > 0) {
                            DetailRow("Quantity:", "${transaction.quantity} ${transaction.unit}")
                        }
                        if (transaction.rate > 0) {
                            DetailRow("Rate:", "${FormatUtils.formatPkr(transaction.rate)} per ${transaction.unit}")
                        }
                    }

                    if (transaction.paymentMethod.isNotBlank()) {
                        DetailRow("Payment Mode:", transaction.paymentMethod)
                    }

                    DetailRow(
                        "Total Amount:",
                        FormatUtils.formatPkr(transaction.amount),
                        valueColor = if (transaction.type == TransactionType.BILL) ReceivableRed else PayableGreen,
                        isBold = true
                    )

                    if (transaction.notes.isNotBlank()) {
                        DetailRow("Notes:", transaction.notes)
                    }

                    HorizontalDivider(color = CardBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Balance After Entry:", fontSize = 12.sp, color = SlateMuted)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                FormatUtils.formatPkr(transaction.balanceAfter),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (transaction.balanceTypeAfter == BalanceType.RECEIVABLE) ReceivableRed else PayableGreen
                            )
                            BalanceBadge(balanceType = transaction.balanceTypeAfter)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (onDelete != null) {
                        OutlinedButton(
                            onClick = { showConfirmDelete = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ReceivableRed),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = NavyDark,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = SlateMuted)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}
