package com.example.contactsapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.contactsapp.data.model.AppSettings
import com.example.contactsapp.data.model.Contact

private val dialKeys = listOf(
    Triple("1", "", ""),
    Triple("2", "ABC", ""),
    Triple("3", "DEF", ""),
    Triple("4", "GHI", ""),
    Triple("5", "JKL", ""),
    Triple("6", "MNO", ""),
    Triple("7", "PQRS", ""),
    Triple("8", "TUV", ""),
    Triple("9", "WXYZ", ""),
    Triple("*", "", ""),
    Triple("0", "+", ""),
    Triple("#", "", "")
)

@Composable
fun DialPadScreen(
    number: String,
    onKeyPress: (String) -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onSaveContact: () -> Unit,
    matchingContacts: List<Contact> = emptyList(),
    settings: AppSettings,
    onContactClick: (Contact) -> Unit = {},
    onCallContact: (String) -> Unit = {}  // ← ADD THIS
) {
    val haptic = LocalHapticFeedback.current

    Box(modifier = Modifier.fillMaxSize()) {

        // ← Matching contacts list - positioned at top with fixed height
        AnimatedVisibility(
            visible = matchingContacts.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val uniqueContacts = matchingContacts
                .distinctBy { it.phoneNumber } // Only keep first occurrence of each phone number
                .sortedBy { if (settings.sortByFirstName) it.name else it.name.split(" ").lastOrNull() ?: it.name }


            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp), // ← Fixed max height
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(uniqueContacts, key = { it.id }) { contact ->
                    ContactSuggestionItem(
                        contact = contact,
                        onClick = { onContactClick(contact) },
                        onCallClick = { onCallContact(contact.phoneNumber) }  // ← ADD THIS
                    )
                }
            }
        }

        // ← Dial pad - always at bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 16.dp).background(color = MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Number display
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = number,
                    transitionSpec = {
                        fadeIn() + slideInVertically { -it / 3 } togetherWith
                                fadeOut() + slideOutVertically { it / 3 }
                    },
                    label = "dial_number"
                ) { num ->
                    Text(
                        text = num.ifEmpty { " " },
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(end = 56.dp)
                    )
                }

                // Add contact button
                this@Column.AnimatedVisibility(
                    visible = number.length >= 5,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    IconButton(onClick = onSaveContact) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = "Save contact",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Dial grid
            val rows = dialKeys.chunked(3)
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { (digit, letters, _) ->
                        DialKey(
                            digit = digit,
                            letters = letters,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onKeyPress(digit)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Call row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(72.dp))

                // Call button
                Box(
                    modifier = Modifier
                        .size(55.dp)
                        .clip(CircleShape)
                        .background(
                            if (number.isNotEmpty())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable(
                            enabled = number.isNotEmpty(),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCall()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = if (number.isNotEmpty()) Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Backspace
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .clickable(
                            enabled = number.isNotEmpty(),
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDelete()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    this@Row.AnimatedVisibility(visible = number.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ← UPDATED: Contact suggestion item with call button
@Composable
private fun ContactSuggestionItem(
    contact: Contact,
    onClick: () -> Unit,
    onCallClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)  // ← Whole row clickable for details
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (contact.profileImageUri != null) {
                    AsyncImage(
                        model = contact.profileImageUri,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = contact.getInitials(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.getDisplayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // ← ADD THIS: Call button
            IconButton(
                onClick = onCallClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = "Call ${contact.name}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DialKey(
    digit: String,
    letters: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = digit,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

@Composable
@Suppress("DEPRECATION")
private fun ripple(bounded: Boolean) =
    rememberRipple(bounded = bounded)