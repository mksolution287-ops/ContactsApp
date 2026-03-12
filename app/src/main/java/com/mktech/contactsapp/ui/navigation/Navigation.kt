package com.mktech.contactsapp.ui.navigation

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import com.mktech.contactsapp.data.model.CallLog
import com.mktech.contactsapp.ui.screens.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.mktech.contactsapp.ui.viewmodel.ContactViewModel
import com.mktech.contactsapp.data.AnalyticsTracker
import com.mktech.contactsapp.data.repository.SettingsRepository
import kotlinx.coroutines.launch
import com.mktech.contactsapp.R

// ── Route constants ──────────────────────────────────────────────────────────
object Routes {
    const val CONTACTS       = "contacts"
    const val RECENTS        = "recents"
    const val DIALER         = "dialer"
    const val SETTINGS       = "settings"
    const val CONTACT_DETAIL = "contact_detail/{contactId}"
    fun contactDetail(id: Long?) = "contact_detail/${id ?: -1L}"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactNavigation(
    navController: NavHostController,
    viewModel: ContactViewModel,
) {
    val contacts    by viewModel.contacts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showFavOnly by viewModel.showFavoritesOnly.collectAsState()
    val isLoading   by viewModel.isLoading.collectAsState()
    val missedCount by viewModel.missedCallCount.collectAsState()
    val dialNumber  by viewModel.dialPadNumber.collectAsState()
    val settings    by viewModel.settings.collectAsState()
    val context     = LocalContext.current

    val allLogs    by viewModel.allResolvedCallLogs.collectAsState()
    val missedLogs by viewModel.missedResolvedCalls.collectAsState()
    val activity = LocalContext.current as? Activity

    // ── Track screen views on every route change ─────────────────────────────
    val currentBack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBack?.destination?.route
    val settingsRepository = SettingsRepository(context)

    LaunchedEffect(currentRoute) {
        when (currentRoute) {
            Routes.CONTACTS -> AnalyticsTracker.logScreenView("ContactList",   "ContactListScreen")
            Routes.RECENTS  -> AnalyticsTracker.logScreenView("Recents",       "CallLogsScreen")
            Routes.DIALER   -> AnalyticsTracker.logScreenView("Dialer",        "DialPadScreen")
            Routes.SETTINGS -> AnalyticsTracker.logScreenView("Settings",      "SettingsScreen")
            Routes.CONTACT_DETAIL -> AnalyticsTracker.logScreenView("ContactDetail", "ContactDetailScreen")
        }
    }

    val bottomItems = listOf(
        BottomNavItem(Routes.CONTACTS, stringResource(R.string.contacts), Icons.Default.People),
        BottomNavItem(Routes.RECENTS,  stringResource(R.string.recents),  Icons.Default.History, missedCount),
        BottomNavItem(Routes.DIALER,   stringResource(R.string.dialer),   Icons.Default.Dialpad),
        BottomNavItem(Routes.SETTINGS, stringResource(R.string.settings), Icons.Default.Settings)
    )

    val showBottomBar = currentRoute in listOf(
        Routes.CONTACTS, Routes.RECENTS, Routes.DIALER, Routes.SETTINGS
    )

    Scaffold(
        topBar = {
            when (currentRoute) {
                Routes.CONTACTS -> ContactsTopBar(
                    searchQuery       = searchQuery,
                    showFavoritesOnly = showFavOnly,
                    onSearchChange    = { query ->
                        viewModel.updateSearchQuery(query)
                        // Log search only when user has typed at least 2 chars
                        if (query.length >= 2) {
                            AnalyticsTracker.logSearchUsed(contacts.size)
                        }
                    },
                    onToggleFavorites = {
                        viewModel.toggleFavoritesFilter()
                        AnalyticsTracker.logFilterToggled(!showFavOnly) // toggled state
                    },
                    onSyncContacts = {
                        viewModel.loadDeviceContacts()
                        viewModel.loadDeviceCallLogs()
                        AnalyticsTracker.logEvent("contacts_synced")   // sync tapped
                    }
                )
                Routes.RECENTS  -> SimpleTopBar(stringResource(R.string.recents))
                Routes.DIALER   -> SimpleTopBar(stringResource(R.string.dialer))
                Routes.SETTINGS -> SimpleTopBar(stringResource(R.string.settings))
                else            -> {}
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(tonalElevation = 4.dp) {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                // ── Track bottom nav tab taps ────────────────
                                AnalyticsTracker.logBottomNavTapped(item.route)

                                navController.navigate(item.route) {
                                    popUpTo(Routes.CONTACTS) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon = {
                                if (item.badgeCount > 0) {
                                    BadgedBox(badge = {
                                        Badge { Text(item.badgeCount.toString()) }
                                    }) {
                                        Icon(item.icon, contentDescription = item.label)
                                    }
                                } else {
                                    Icon(item.icon, contentDescription = item.label)
                                }
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = Routes.RECENTS,
            modifier         = Modifier.padding(padding)
        ) {

            // ── Contacts list ────────────────────────────────────────────
            composable(Routes.CONTACTS) {
                ContactListScreen(
                    contacts          = contacts,
                    showFavoritesOnly = showFavOnly,
                    isLoading         = isLoading,
                    settings          = settings,
                    onContactClick    = { contact ->
                        AnalyticsTracker.logContactOpened(contact.id, contact.isFavorite)
                        navController.navigate(Routes.contactDetail(contact.id))
                    },
                    onToggleFavorite  = { id, currentState ->
                        AnalyticsTracker.logFavoriteToggled(id, !currentState)
                        viewModel.toggleFavorite(id, currentState)
                    },
                    onAddContact      = {
                        AnalyticsTracker.logEvent("add_contact_tapped", mapOf("source" to "contacts_fab"))
                        navController.navigate(Routes.contactDetail(null))
                    },
                    onCallContact     = { number ->
                        AnalyticsTracker.logContactCalled(hasImage = false)
                        viewModel.makeCall(context = context, number)
                    }
                )
            }

            // ── Recents / Call logs ──────────────────────────────────────
            composable(Routes.RECENTS) {

                // Track how many missed calls are visible on entry
                LaunchedEffect(missedCount) {
                    if (missedCount > 0) {
                        AnalyticsTracker.logEvent("missed_calls_viewed",
                            mapOf("missed_count" to missedCount.toString()))
                    }
                }

                val lifecycleOwner = LocalLifecycleOwner.current

                        LaunchedEffect(lifecycleOwner) {
                            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                                viewModel.loadDeviceCallLogs()  // auto-sync every time screen resumes
                            }
                        }

                CallLogsScreen(
                    allLogs    = allLogs,
                    missedLogs = missedLogs,
                    onCallBack = { number ->
                        AnalyticsTracker.logEvent("callback_tapped",
                            mapOf("source" to "recents"))
                        viewModel.makeCall(context = context, number)
                    },
                    onDeleteLog = { log ->
                        AnalyticsTracker.logEvent("call_log_deleted")
                        viewModel.deleteCallLog(log)
                    },
                    onClearAll  = {
                        AnalyticsTracker.logEvent("call_logs_cleared_all")
                        viewModel.clearAllCallLogs()
                    },
                    onSyncLogs  = {
                        AnalyticsTracker.logEvent("call_logs_synced")
                        viewModel.loadDeviceCallLogs()
                    },
                    onContactClick = { phoneNumber ->
                        AnalyticsTracker.logEvent("recents_contact_tapped",
                            mapOf("source" to "recents_list"))
                        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                        scope.launch {
                            val contact = viewModel.contactRepository.getContactByPhone(phoneNumber)
                            if (contact != null) {
                                AnalyticsTracker.logContactOpened(contact.id, contact.isFavorite)
                                viewModel.dialPadClear()
                                navController.navigate(Routes.contactDetail(contact.id))
                            } else {
                                AnalyticsTracker.logEvent("new_contact_from_recents")
                                viewModel.dialPadSetNumber(phoneNumber)
                                navController.navigate(Routes.contactDetail(-1L))
                            }
                        }
                    }
                )
            }

            // ── Dial pad ─────────────────────────────────────────────────
            composable(Routes.DIALER) {
                val dialNumber by viewModel.dialPadNumber.collectAsState()

                // Track when user has typed a full-length number (10+ digits)
                LaunchedEffect(dialNumber) {
                    if (dialNumber.length == 10) {
                        AnalyticsTracker.logEvent("dialpad_full_number_entered")
                    }
                }

                val matchingContacts by remember {
                    derivedStateOf {
                        if (dialNumber.isNotEmpty()) viewModel.getMatchingContacts(dialNumber)
                        else emptyList()
                    }
                }

                // Track when dialpad suggestions appear
                LaunchedEffect(matchingContacts.size) {
                    if (matchingContacts.isNotEmpty()) {
                        AnalyticsTracker.logEvent("dialpad_suggestions_shown",
                            mapOf("suggestion_count" to matchingContacts.size.toString()))
                    }
                }

                DialPadScreen(
                    number    = dialNumber,
                    onKeyPress = { key ->
                        viewModel.dialPadAppend(key)
                        AnalyticsTracker.logEvent("dialpad_key_pressed") // fires per key
                    },
                    onDelete  = {
                        viewModel.dialPadDelete()
                        AnalyticsTracker.logEvent("dialpad_backspace")
                    },
                    onCall    = {
                        if (dialNumber.isNotEmpty()) {
                            AnalyticsTracker.logEvent("dialpad_call_initiated",
                                mapOf("number_length" to dialNumber.length.toString()))
                            viewModel.makeCall(context, dialNumber)
                        }
                    },
                    onSaveContact = {
                        AnalyticsTracker.logEvent("save_contact_from_dialpad")
                        navController.navigate(Routes.contactDetail(-1L))
                    },
                    matchingContacts = matchingContacts,
                    settings         = settings,
                    onContactClick   = { contact ->
                        AnalyticsTracker.logContactOpened(contact.id, contact.isFavorite)
                        navController.navigate(Routes.contactDetail(contact.id))
                    },
                    onCallContact    = { phoneNumber ->
                        AnalyticsTracker.logEvent("quick_call_from_suggestion",
                            mapOf("source" to "dialpad_suggestion"))
                        viewModel.makeCall(context, phoneNumber)
                    }
                )
            }

            // ── Settings ─────────────────────────────────────────────────
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    settings            = settings,
                    onThemeChange       = { theme ->
                        AnalyticsTracker.logEvent("setting_changed",
                            mapOf("setting" to "theme", "value" to theme.toString()))
                        viewModel.setTheme(theme)
                    },
                    onAccentColorChange = { color ->
                        AnalyticsTracker.logEvent("setting_changed",
                            mapOf("setting" to "accent_color", "value" to color.toString()))
                        viewModel.setAccentColor(color)
                    },
                    onSortOrderChange   = { sort ->
                        AnalyticsTracker.logEvent("setting_changed",
                            mapOf("setting" to "sort_order", "value" to sort.toString()))
                        viewModel.setSortOrder(sort)
                    },
                    onShowPhoneChange   = { show ->
                        AnalyticsTracker.logEvent("setting_changed",
                            mapOf("setting" to "show_phone", "value" to show.toString()))
                        viewModel.setShowPhone(show)
                    },
                    onConfirmDeleteChange = { confirm ->
                        AnalyticsTracker.logEvent("setting_changed",
                            mapOf("setting" to "confirm_delete", "value" to confirm.toString()))
                        viewModel.setConfirmDelete(confirm)
                    },
                    onLanguageChange = { lang ->
                        AnalyticsTracker.logEvent("setting_changed",
                            mapOf("setting" to "language", "value" to lang.code))
                        viewModel.setLanguage(lang)
                        activity?.recreate()   // ← applies new locale immediately
                    }
                )
            }

            // ── Contact detail / edit ────────────────────────────────────
            composable(
                route     = Routes.CONTACT_DETAIL,
                arguments = listOf(navArgument("contactId") {
                    type = NavType.LongType; defaultValue = -1L
                })
            ) { back ->
                val contactId     = back.arguments?.getLong("contactId") ?: -1L
                val isNew         = contactId == -1L
                val scope         = rememberCoroutineScope()
                var contact       by remember { mutableStateOf<com.mktech.contactsapp.data.model.Contact?>(null) }
                var loading       by remember { mutableStateOf(!isNew) }
                val prefilledPhone by viewModel.dialPadNumber.collectAsState()
                var callHistory   by remember { mutableStateOf<List<CallLog>>(emptyList()) }

                // Track whether opened as new or existing
                LaunchedEffect(contactId) {
                    if (isNew) {
                        AnalyticsTracker.logScreenView("NewContact", "ContactDetailScreen")
                        AnalyticsTracker.logEvent("new_contact_screen_opened",
                            mapOf("has_prefilled_phone" to prefilledPhone.isNotEmpty().toString()))
                    } else {
                        AnalyticsTracker.logScreenView("EditContact", "ContactDetailScreen")
                        loading = true
                        contact = viewModel.getContactById(contactId)
                        AnalyticsTracker.logContactOpened(contactId, contact?.isFavorite ?: false)
                        loading = false
                    }
                }

                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    ContactDetailScreen(
                        contact        = contact,
                        isNewContact   = isNew,
                        prefilledPhone = if (isNew) prefilledPhone else null,
                        callHistory    = callHistory,
                        onSave = { updated ->
                            scope.launch {
                                Log.d("ContactSave", "onSave called isNew=$isNew contact=$updated")
                                if (isNew) {
                                    AnalyticsTracker.logContactAdded()
                                    viewModel.addContact(updated)
                                    viewModel.dialPadClear()
                                } else {
                                    AnalyticsTracker.logEvent("contact_updated",
                                        mapOf("contact_id" to updated.id.toString()))
                                    Log.d("ContactSave", "Calling updateContact id=${updated.id}")
                                    viewModel.updateContact(updated)
                                }
                                navController.popBackStack()
                            }
                        },
                        onDelete = {
                            scope.launch {
                                contact?.let {
                                    AnalyticsTracker.logEvent("contact_deleted",
                                        mapOf("contact_id" to it.id.toString()))
                                    viewModel.deleteContact(it)
                                }
                                navController.popBackStack()
                            }
                        },
                        onBack = {
                            AnalyticsTracker.logEvent("contact_detail_back_pressed",
                                mapOf("was_new" to isNew.toString()))
                            navController.popBackStack()
                        },
                        onCallNow = @androidx.annotation.RequiresPermission(android.Manifest.permission.CALL_PHONE) { number ->
                            AnalyticsTracker.logContactCalled(hasImage = contact?.profileImageUri != null)
                            viewModel.makeCall(context, number)
                        }
                    )
                }
            }
        }
    }
}

// ── Top bars ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactsTopBar(
    searchQuery: String,
    showFavoritesOnly: Boolean,
    onSearchChange: (String) -> Unit,
    onToggleFavorites: () -> Unit,
    onSyncContacts: () -> Unit
) {
    var searching by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            AnimatedContent(targetState = searching, label = "search") { s ->
                if (s) {
                    TextField(
                        value         = searchQuery,
                        onValueChange = onSearchChange,
                        modifier      = Modifier.fillMaxWidth(),
                        placeholder   = { Text(stringResource(R.string.search_contacts)) },
                        singleLine    = true,
                        colors        = TextFieldDefaults.colors(
                            focusedContainerColor   = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor   = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                } else {
                    Text(stringResource(R.string.contacts), fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall)
                }
            }
        },
        actions = {
            IconButton(onClick = {
                val opening = !searching
                searching = opening
                if (!searching) onSearchChange("")
                AnalyticsTracker.logEvent(
                    if (opening) "search_opened" else "search_closed"
                )
            }) {
                Icon(
                    if (searching) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search"
                )
            }
            IconButton(onClick = onToggleFavorites) {
                Icon(
                    if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorites",
                    tint = if (showFavoritesOnly) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onSyncContacts) {
                Icon(Icons.Default.Sync, contentDescription = "Sync")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTopBar(title: String) {
    TopAppBar(
        title = {
            Text(title, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}