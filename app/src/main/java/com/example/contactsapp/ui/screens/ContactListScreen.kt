package com.example.contactsapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.contactsapp.data.model.AppSettings
import com.example.contactsapp.data.model.Contact
import kotlinx.coroutines.launch

@Composable
fun ContactListScreen(
    contacts: List<Contact>,
    showFavoritesOnly: Boolean,
    isLoading: Boolean,
    settings: AppSettings,
    onContactClick: (Contact) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    onAddContact: () -> Unit,
    onCallContact: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            contacts.isEmpty() -> {
                EmptyContactsState(showFavoritesOnly)
            }
            else -> {
                val uniqueContacts = contacts
                    .distinctBy { it.phoneNumber }
                    .sortedBy {
                        if (settings.sortByFirstName) it.name
                        else it.name.split(" ").lastOrNull() ?: it.name
                    }

                val grouped = uniqueContacts
                    .groupBy { it.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#" }
                    .toSortedMap(compareBy { if (it == "#") "~" else it })

                // Build a flat index: letter -> first list index of that section
                val letterIndexMap = remember(grouped) {
                    val map = mutableMapOf<String, Int>()
                    var idx = 0
                    grouped.forEach { (letter, group) ->
                        map[letter] = idx   // header item
                        idx += 1 + group.size
                    }
                    map
                }

                val availableLetters = grouped.keys.toList()

                // Active letter being touched on scrubber
                var activeLetter by remember { mutableStateOf<String?>(null) }

                Row(modifier = Modifier.fillMaxSize()) {
                    // ── Main list ────────────────────────────────────────
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            bottom = 80.dp,
                            top = 4.dp,
                            end = 4.dp  // make room for scrubber
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        grouped.forEach { (letter, group) ->
                            item {
                                Text(
                                    text = letter,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(
                                        start = 20.dp, top = 12.dp, bottom = 4.dp
                                    )
                                )
                            }
                            items(group, key = { it.id }) { contact ->
                                ContactListItem(
                                    contact          = contact,
                                    showPhone        = settings.showPhoneNumberInList,
                                    onClick          = { onContactClick(contact) },
                                    onToggleFavorite = { onToggleFavorite(contact.id, contact.isFavorite) },
                                    onCall           = { onCallContact(contact.phoneNumber) }
                                )
                            }
                        }
                    }

                    // ── Alphabetical scrubber ────────────────────────────
                    AlphabetScrubber(
                        letters = availableLetters,
                        activeLetter = activeLetter,
                        onLetterSelected = { letter ->
                            activeLetter = letter
                            letterIndexMap[letter]?.let { idx ->
                                coroutineScope.launch {
                                    listState.scrollToItem(idx)
                                }
                            }
                        },
                        onDone = { activeLetter = null }
                    )
                }

                // ── Floating letter bubble ───────────────────────────────
                if (activeLetter != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(64.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeLetter!!,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = onAddContact,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = androidx.compose.ui.graphics.Color.White
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Add contact")
        }
    }
}

@Composable
private fun AlphabetScrubber(
    letters: List<String>,
    activeLetter: String?,
    onLetterSelected: (String) -> Unit,
    onDone: () -> Unit
) {
    if (letters.isEmpty()) return

    var scrubberTopY by remember { mutableStateOf(0f) }
    var scrubberHeight by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(20.dp)
            .padding(vertical = 8.dp)
            .onGloballyPositioned { coords ->
                scrubberTopY = coords.positionInRoot().y
                scrubberHeight = coords.size.height.toFloat()
            }
            .pointerInput(letters) {
                // Handle tap
                detectTapGestures(
                    onTap = { offset ->
                        val fraction = (offset.y / scrubberHeight).coerceIn(0f, 1f)
                        val idx = (fraction * letters.size).toInt().coerceIn(0, letters.size - 1)
                        onLetterSelected(letters[idx])
                        onDone()
                    }
                )
            }
            .pointerInput(letters) {
                // Handle drag
                detectDragGestures(
                    onDragEnd = { onDone() },
                    onDragCancel = { onDone() }
                ) { change, _ ->
                    val relativeY = change.position.y
                    val fraction = (relativeY / scrubberHeight).coerceIn(0f, 1f)
                    val idx = (fraction * letters.size).toInt().coerceIn(0, letters.size - 1)
                    onLetterSelected(letters[idx])
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxHeight()
        ) {
            letters.forEach { letter ->
                val isActive = letter == activeLetter
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(if (isActive) 22.dp else 18.dp)
                        .then(
                            if (isActive) Modifier.background(
                                MaterialTheme.colorScheme.primary, CircleShape
                            ) else Modifier
                        )
                ) {
                    Text(
                        text = letter,
                        fontSize = if (isActive) 11.sp else 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.primary,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactListItem(
    contact: Contact,
    showPhone: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCall: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (contact.profileImageUri != null) {
                    AsyncImage(
                        model = contact.profileImageUri,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(CircleShape),
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
                if (showPhone && contact.phoneNumber.isNotEmpty()) {
                    Text(
                        text = contact.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            IconButton(onClick = onCall, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Call, contentDescription = "Call",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (contact.isFavorite) Icons.Filled.Favorite
                    else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (contact.isFavorite) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyContactsState(showFavoritesOnly: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (showFavoritesOnly) Icons.Default.Favorite
                else Icons.Default.People,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (showFavoritesOnly) "No favorites yet" else "No contacts",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (showFavoritesOnly) "Tap ♥ on a contact to add favorites"
                else "Tap sync or + to add contacts",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}