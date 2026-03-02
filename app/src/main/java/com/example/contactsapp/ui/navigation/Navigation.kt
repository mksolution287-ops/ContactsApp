package com.example.contactsapp.ui.navigation

import android.annotation.SuppressLint
import android.content.Context
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
import com.example.contactsapp.data.model.CallLog
import com.example.contactsapp.ui.screens.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.platform.LocalContext
import com.example.contactsapp.ui.viewmodel.ContactViewModel
import kotlinx.coroutines.launch

// ── Route constants ──────────────────────────────────────────────────────────
object Routes {
    const val CONTACTS   = "contacts"
    const val RECENTS    = "recents"
    const val DIALER     = "dialer"
    const val SETTINGS   = "settings"
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
//    calllogviewmodel: CallLogViewModel
) {
    val contacts        by viewModel.contacts.collectAsState()
    val searchQuery     by viewModel.searchQuery.collectAsState()
    val showFavOnly     by viewModel.showFavoritesOnly.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
//    val allLogs         by viewModel.allCallLogs.collectAsState()
//    val missedLogs      by viewModel.missedCalls.collectAsState()
    val missedCount     by viewModel.missedCallCount.collectAsState()
    val dialNumber      by viewModel.dialPadNumber.collectAsState()
    val settings        by viewModel.settings.collectAsState()

    val currentBack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBack?.destination?.route
    val context = LocalContext.current

    val allLogs    by viewModel.allResolvedCallLogs.collectAsState()
    val missedLogs by viewModel.missedResolvedCalls.collectAsState()


    val bottomItems = listOf(
        BottomNavItem(Routes.CONTACTS, "Contacts", Icons.Default.People),
        BottomNavItem(Routes.RECENTS,  "Recents",  Icons.Default.History, missedCount),
        BottomNavItem(Routes.DIALER,   "Dialer",   Icons.Default.Dialpad),
        BottomNavItem(Routes.SETTINGS, "Settings", Icons.Default.Settings)
    )

    val showBottomBar = currentRoute in listOf(
        Routes.CONTACTS, Routes.RECENTS, Routes.DIALER, Routes.SETTINGS
    )

    Scaffold(
        topBar = {
            when (currentRoute) {
                Routes.CONTACTS -> ContactsTopBar(
                    searchQuery        = searchQuery,
                    showFavoritesOnly  = showFavOnly,
                    onSearchChange     = viewModel::updateSearchQuery,
                    onToggleFavorites  = viewModel::toggleFavoritesFilter,
                    onSyncContacts     = {
                        viewModel.loadDeviceContacts()
                        viewModel.loadDeviceCallLogs()
                    }
                )
                Routes.RECENTS  -> SimpleTopBar("Recents")
                Routes.DIALER   -> SimpleTopBar("Dial Pad")
                Routes.SETTINGS -> SimpleTopBar("Settings")
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
                                navController.navigate(item.route) {
                                    popUpTo(Routes.CONTACTS) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
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
            navController  = navController,
            startDestination = Routes.RECENTS,
            modifier       = Modifier.padding(padding)
        ) {
            // ── Contacts list ────────────────────────────────────────────
            composable(Routes.CONTACTS) {
                ContactListScreen(
                    contacts          = contacts,
                    showFavoritesOnly = showFavOnly,
                    isLoading         = isLoading,
                    settings          = settings,
                    onContactClick    = { navController.navigate(Routes.contactDetail(it.id)) },
                    onToggleFavorite  = viewModel::toggleFavorite,
                    onAddContact      = { navController.navigate(Routes.contactDetail(null)) },
                    onCallContact     = { viewModel.makeCall(context = context ,it) }
                )
            }

            // ── Recents / Call logs ──────────────────────────────────────
            composable(Routes.RECENTS) {
                CallLogsScreen(
                    allLogs    = allLogs,
                    missedLogs = missedLogs,
                    onCallBack = { number ->
//                        viewModel.dialPadSetNumber(number)
//                        navController.navigate(Routes.DIALER) {
//                            launchSingleTop = true
//                        }
                        viewModel.makeCall(context = context ,number)
                    },
                    onDeleteLog = viewModel::deleteCallLog,
                    onClearAll  = viewModel::clearAllCallLogs,
                    onSyncLogs  = viewModel::loadDeviceCallLogs,
                    onContactClick = { phoneNumber ->  // ← ADD THIS
                        // Find contact by phone number
                        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                        scope.launch {
                            val contact = viewModel.contactRepository.getContactByPhone(phoneNumber)
                            if (contact != null) {
                                viewModel.dialPadClear()
                                navController.navigate(Routes.contactDetail(contact.id))
                            } else {
                                // Navigate to create new contact with pre-filled phone
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

                val matchingContacts by remember {
                    derivedStateOf {
                        if (dialNumber.isNotEmpty()) {
                            viewModel.getMatchingContacts(dialNumber)
                        } else {
                            emptyList()
                        }
                    }
                }

                DialPadScreen(
                    number    = dialNumber,
                    onKeyPress = viewModel::dialPadAppend,
                    onDelete  = viewModel::dialPadDelete,
                    onCall    = {
                        if (dialNumber.isNotEmpty()) viewModel.makeCall(context, dialNumber)
                    },
                    onSaveContact = {
                        navController.navigate(Routes.contactDetail(-1L))
                    },
                    matchingContacts = matchingContacts,
                    settings = settings,
                    onContactClick = { contact ->  // ← Click to view details
                        navController.navigate(Routes.contactDetail(contact.id))
                    },
                    onCallContact = { phoneNumber ->  // ← ADD THIS: Quick call
                        viewModel.makeCall(context,phoneNumber)
                    }
                )
            }

            // ── Settings ─────────────────────────────────────────────────
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    settings              = settings,
                    onThemeChange         = viewModel::setTheme,
                    onAccentColorChange   = viewModel::setAccentColor,
                    onSortOrderChange     = viewModel::setSortOrder,
                    onShowPhoneChange     = viewModel::setShowPhone,
                    onConfirmDeleteChange = viewModel::setConfirmDelete
                )
            }

            // ── Contact detail / edit ────────────────────────────────────
            // ── Contact detail / edit ────────────────────────────────────
            composable(
                route     = Routes.CONTACT_DETAIL,
                arguments = listOf(navArgument("contactId") {
                    type = NavType.LongType; defaultValue = -1L
                })
            ) { back ->
                val contactId  = back.arguments?.getLong("contactId") ?: -1L
                val isNew      = contactId == -1L
                val scope      = rememberCoroutineScope()
                var contact    by remember { mutableStateOf<com.example.contactsapp.data.model.Contact?>(null) }
                var loading    by remember { mutableStateOf(!isNew) }

                val prefilledPhone by viewModel.dialPadNumber.collectAsState()

                // ← ADD THIS: Get call history for this contact
                var callHistory by remember { mutableStateOf<List<CallLog>>(emptyList()) }

                LaunchedEffect(contactId) {
                    if (!isNew) {
                        loading = true
                        contact = viewModel.getContactById(contactId)
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
                        callHistory    = callHistory,  // ← ADD THIS
                        onSave = { updated ->
                            scope.launch {
                                Log.d("ContactSave", "onSave called isNew=$isNew contact=$updated")
                                if (isNew) {
                                    viewModel.addContact(updated)
                                    viewModel.dialPadClear()
                                } else {
                                    Log.d("ContactSave", "Calling updateContact id=${updated.id}")
                                    viewModel.updateContact(updated)
                                }
                                navController.popBackStack()
                            }
                        },
                        onDelete = {
                            scope.launch {
                                contact?.let { viewModel.deleteContact(it) }
                                navController.popBackStack()
                            }
                        },
                        onBack = { navController.popBackStack() },
                        onCallNow = @androidx.annotation.RequiresPermission(android.Manifest.permission.CALL_PHONE) { viewModel.makeCall(context,it) }
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
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search contacts…") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor   = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor   = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                } else {
                    Text("Contacts", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall)
                }
            }
        },
        actions = {
            IconButton(onClick = { searching = !searching; if (!searching) onSearchChange("") }) {
                Icon(if (searching) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search")
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
        title = { Text(title, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}
