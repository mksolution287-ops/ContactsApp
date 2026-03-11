package com.mktech.contactsapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mktech.contactsapp.data.model.CallType
import com.mktech.contactsapp.data.model.ResolvedCallLog  // ← only ResolvedCallLog, no CallLog import
import java.text.SimpleDateFormat
import java.util.*
import com.mktech.contactsapp.R

enum class CallLogFilter { ALL, MISSED, INCOMING, OUTGOING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLogsScreen(
    allLogs: List<ResolvedCallLog>,
    missedLogs: List<ResolvedCallLog>,
    onCallBack: (String) -> Unit,
    onDeleteLog: (Long) -> Unit,
    onClearAll: () -> Unit,
    onSyncLogs: () -> Unit,
    onContactClick: (String) -> Unit
) {
    var filter by remember { mutableStateOf(CallLogFilter.ALL) }
    var showClearDialog by remember { mutableStateOf(false) }

    val displayed = when (filter) {
        CallLogFilter.ALL      -> allLogs
        CallLogFilter.MISSED   -> missedLogs
        CallLogFilter.INCOMING -> allLogs.filter { it.callType == CallType.INCOMING }
        CallLogFilter.OUTGOING -> allLogs.filter { it.callType == CallType.OUTGOING }
    }

    val incomingCount = allLogs.count { it.callType == CallType.INCOMING }
    val outgoingCount = allLogs.count { it.callType == CallType.OUTGOING }

    Column(modifier = Modifier.fillMaxSize()) {

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = filter == CallLogFilter.ALL,
                    onClick = { filter = CallLogFilter.ALL },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp))
                            Text("All (${allLogs.size})")
                        }
                    }
                )
            }
            item {
                FilterChip(
                    selected = filter == CallLogFilter.MISSED,
                    onClick = { filter = CallLogFilter.MISSED },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.PhoneMissed, null,
                                modifier = Modifier.size(16.dp),
                                tint = if (missedLogs.isNotEmpty())
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("Missed (${missedLogs.size})")
                        }
                    }
                )
            }
            item {
                FilterChip(
                    selected = filter == CallLogFilter.INCOMING,
                    onClick = { filter = CallLogFilter.INCOMING },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CallReceived, null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Incoming ($incomingCount)")
                        }
                    }
                )
            }
            item {
                FilterChip(
                    selected = filter == CallLogFilter.OUTGOING,
                    onClick = { filter = CallLogFilter.OUTGOING },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CallMade, null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF10B981)
                            )
                            Text("Outgoing ($outgoingCount)")
                        }
                    }
                )
            }
        }

        // Action buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onSyncLogs) {
                Icon(Icons.Default.Sync, contentDescription = "Sync call logs",
                    tint = MaterialTheme.colorScheme.primary)
            }
            if (allLogs.isNotEmpty()) {
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (displayed.isEmpty()) {
            CallLogEmptyState(filter)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                val grouped = displayed.groupBy { log ->
                    when {
                        isToday(log.timestamp)     -> "Today"
                        isYesterday(log.timestamp) -> "Yesterday"
                        else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                            .format(Date(log.timestamp))
                    }
                }

                grouped.forEach { (dateLabel, logs) ->
                    item {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(logs, key = { it.id }) { log ->
                        CallLogItem(
                            log        = log,
                            onCallBack = { onCallBack(log.phoneNumber) },
                            onDelete   = { onDeleteLog(log.id) },
                            onClick    = { onContactClick(log.phoneNumber) }
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_call_history_title)) },
            text  = { Text(stringResource(R.string.clear_call_history_message)) },
            confirmButton = {
                TextButton(
                    onClick = { showClearDialog = false; onClearAll() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.clear_all)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallLogItem(
    log: ResolvedCallLog,   // ← was CallLog, now ResolvedCallLog
    onCallBack: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true }
            else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (log.profileImageUri != null) {
                        AsyncImage(
                            model = log.profileImageUri,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = log.getInitials(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.contactName.ifBlank { log.phoneNumber },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = callTypeIcon(log.callType),
                            contentDescription = null,
                            tint = callTypeColor(log.callType),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formatTime(log.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (log.durationSeconds > 0) {
                            Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = log.getFormattedDuration(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(onClick = onCallBack) {
                    Icon(Icons.Default.Call, contentDescription = "Call back",
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun CallLogEmptyState(filter: CallLogFilter) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = when (filter) {
                    CallLogFilter.MISSED   -> Icons.Default.PhoneMissed
                    CallLogFilter.INCOMING -> Icons.Default.CallReceived
                    CallLogFilter.OUTGOING -> Icons.Default.CallMade
                    else                   -> Icons.Default.History
                },
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when (filter) {
                    CallLogFilter.MISSED   -> "No missed calls"
                    CallLogFilter.INCOMING -> "No incoming calls"
                    CallLogFilter.OUTGOING -> "No outgoing calls"
                    else                   -> "No call history"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun callTypeColor(type: CallType): Color = when (type) {
    CallType.MISSED   -> MaterialTheme.colorScheme.error
    CallType.INCOMING -> MaterialTheme.colorScheme.primary
    CallType.OUTGOING -> Color(0xFF10B981)
}

private fun callTypeIcon(type: CallType) = when (type) {
    CallType.MISSED   -> Icons.Default.PhoneMissed
    CallType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived
    CallType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade
}

private fun formatTime(ts: Long): String {
    val fmt = if (isToday(ts))
        SimpleDateFormat("h:mm a", Locale.getDefault())
    else
        SimpleDateFormat("MMM d · h:mm a", Locale.getDefault())
    return fmt.format(Date(ts))
}

private fun isToday(ts: Long): Boolean {
    val c1 = Calendar.getInstance()
    val c2 = Calendar.getInstance().apply { timeInMillis = ts }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(ts: Long): Boolean {
    val c1 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val c2 = Calendar.getInstance().apply { timeInMillis = ts }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}