package com.example.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BalanceType
import com.example.ui.theme.*
import com.example.util.FormatUtils

@Composable
fun BalanceBadge(
    balanceType: BalanceType,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false
) {
    val isReceivable = balanceType == BalanceType.RECEIVABLE
    val bgColor = if (isReceivable) ReceivableRedBg else PayableGreenBg
    val textColor = if (isReceivable) ReceivableRed else PayableGreen
    val borderColor = if (isReceivable) ReceivableRedBorder else PayableGreenBorder
    val text = if (isReceivable) "GET / RECEIVABLE" else "GIVE / PAYABLE"
    val icon = if (isReceivable) Icons.Default.CallReceived else Icons.Default.CallMade

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(if (isLarge) 12.dp else 8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(if (isLarge) 12.dp else 8.dp))
            .testTag("balance_type_badge"),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isLarge) 14.dp else 8.dp,
                vertical = if (isLarge) 8.dp else 4.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = textColor,
                modifier = Modifier.size(if (isLarge) 18.dp else 14.dp)
            )
            Text(
                text = text,
                color = textColor,
                fontSize = if (isLarge) 14.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun MarketStatusBanner(
    isOpen: Boolean,
    lastUpdateDate: String? = null,
    modifier: Modifier = Modifier,
    onToggleClick: (() -> Unit)? = null
) {
    val bgColor = if (isOpen) MarketOpenGreenBg else MarketClosedRedBg
    val contentColor = if (isOpen) MarketOpenGreen else MarketClosedRed
    val borderColor = if (isOpen) Color(0xFFA7F3D0) else Color(0xFFFECDD3)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onToggleClick != null) Modifier.clickable { onToggleClick() } else Modifier)
            .testTag("market_status_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(borderColor))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(contentColor)
                )
                Column {
                    Text(
                        text = if (isOpen) "🟢 MARKET OPEN" else "🔴 MARKET CLOSED",
                        color = contentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    if (!isOpen) {
                        Text(
                            text = "Market is closed. Rates are not available.",
                            color = contentColor.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "Trading & Live Daily Rates are Active",
                            color = contentColor.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (onToggleClick != null) {
                FilledTonalButton(
                    onClick = onToggleClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = contentColor.copy(alpha = 0.15f),
                        contentColor = contentColor
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isOpen) "Close Market" else "Open Market",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RateItemCard(
    itemName: String,
    currentRate: Double,
    previousRate: Double? = null,
    isCustomRate: Boolean = false,
    isMarketOpen: Boolean = true,
    lastUpdateText: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rate_item_card_${itemName.replace(" ", "_").lowercase()}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = NavyDark,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = itemName.replace("Item ", "#"),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = itemName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (isCustomRate) {
                    Surface(
                        color = GoldAccentBg,
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFDE68A)))
                    ) {
                        Text(
                            text = "Special Rate",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = if (isMarketOpen) "Current Rate" else "Last Available Rate",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = FormatUtils.formatPkr(currentRate),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }

                if (previousRate != null && previousRate > 0) {
                    val diff = currentRate - previousRate
                    val diffColor = when {
                        diff > 0 -> PayableGreen
                        diff < 0 -> ReceivableRed
                        else -> TextSecondary
                    }
                    val diffText = when {
                        diff > 0 -> "+${FormatUtils.formatPkr(diff)}"
                        diff < 0 -> "-${FormatUtils.formatPkr(kotlin.math.abs(diff))}"
                        else -> "No change"
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Previous: ${FormatUtils.formatPkr(previousRate)}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = diffText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = diffColor
                        )
                    }
                }
            }

            if (lastUpdateText != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Updated: $lastUpdateText",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    if (!isMarketOpen) {
                        Text(
                            text = "Market Closed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MarketClosedRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkStatusBar(isOnline: Boolean, modifier: Modifier = Modifier) {
    if (!isOnline) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .testTag("offline_banner"),
            color = Color(0xFFFEF3C7)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "Offline",
                    tint = Color(0xFFB45309),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Offline Mode — Showing last synchronized data",
                    color = Color(0xFFB45309),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
