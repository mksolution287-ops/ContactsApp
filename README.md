# Contacts App - Kotlin Compose

A modern, minimal Android contacts management application built with Jetpack Compose, Room database, and Material Design 3.

## Features

### Core Functionality
- ✅ **View All Contacts** - Browse your contacts in a clean, scrollable list
- ✅ **Search Contacts** - Real-time search by name, phone number, or email
- ✅ **Add New Contacts** - Create contacts with name, phone, email, and profile image
- ✅ **Edit Contacts** - Update any contact information
- ✅ **Delete Contacts** - Remove contacts with confirmation dialog
- ✅ **Sync Device Contacts** - Import contacts from your device
- ✅ **Favorite Contacts** - Mark contacts as favorites and filter by favorites
- ✅ **Profile Images** - Add/change profile pictures from device gallery
- ✅ **Local Storage** - All data persisted using Room database

### Design Features
- 🎨 **Minimal & Modern UI** - Clean interface following Material Design 3 guidelines
- 🌈 **Beautiful Color Scheme** - Indigo primary color with thoughtful accents
- 📱 **Responsive Layout** - Adapts to different screen sizes
- ✨ **Smooth Animations** - Polished transitions and interactions
- 🔍 **Intuitive Search** - Animated search bar with clear/close functionality
- ❤️ **Visual Feedback** - Clear indicators for favorites, empty states, and loading

## Tech Stack

### Architecture
- **MVVM Architecture** - Clean separation of concerns
- **Repository Pattern** - Data layer abstraction
- **Kotlin Coroutines & Flow** - Asynchronous operations and reactive data streams

### Libraries & Technologies
- **Jetpack Compose** - Modern declarative UI framework
- **Material 3** - Latest Material Design components
- **Room Database** - Local data persistence with SQLite
- **Navigation Compose** - Type-safe navigation
- **Coil** - Async image loading
- **Accompanist Permissions** - Runtime permissions handling
- **ViewModel & LiveData** - Lifecycle-aware components

## Project Structure

```
ContactsApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/contactsapp/
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── ContactDao.kt          # Room DAO
│   │   │   │   │   └── ContactDatabase.kt     # Room Database
│   │   │   │   ├── model/
│   │   │   │   │   └── Contact.kt             # Data model
│   │   │   │   └── repository/
│   │   │   │       └── ContactRepository.kt   # Repository layer
│   │   │   ├── ui/
│   │   │   │   ├── navigation/
│   │   │   │   │   └── Navigation.kt          # Navigation setup
│   │   │   │   ├── screens/
│   │   │   │   │   ├── ContactListScreen.kt   # Main list screen
│   │   │   │   │   └── ContactDetailScreen.kt # Detail/Edit screen
│   │   │   │   ├── theme/
│   │   │   │   │   ├── Color.kt               # Color definitions
│   │   │   │   │   ├── Theme.kt               # Theme configuration
│   │   │   │   │   └── Type.kt                # Typography
│   │   │   │   └── viewmodel/
│   │   │   │       ├── ContactViewModel.kt           # Main ViewModel
│   │   │   │       └── ContactViewModelFactory.kt    # ViewModel Factory
│   │   │   ├── util/
│   │   │   │   └── DeviceContactsHelper.kt    # Device contacts sync
│   │   │   ├── ContactsApplication.kt          # Application class
│   │   │   └── MainActivity.kt                 # Main activity
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   └── xml/
│   │   │       ├── backup_rules.xml
│   │   │       └── data_extraction_rules.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or higher
- Android SDK 26 (Android 8.0) or higher
- Gradle 8.0 or higher

### Installation Steps

1. **Clone or download the project**
   ```bash
   # If using git
   git clone <repository-url>
   
   # Or download and extract the ZIP file
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the ContactsApp folder
   - Click "OK"

3. **Sync Gradle**
   - Wait for Android Studio to sync Gradle files
   - If prompted, click "Sync Now"

4. **Build the project**
   - Go to Build > Make Project
   - Or press Ctrl+F9 (Windows/Linux) / Cmd+F9 (Mac)

5. **Run the app**
   - Connect an Android device or start an emulator
   - Click the "Run" button or press Shift+F10
   - Grant permissions when prompted

## Permissions

The app requires the following permissions:

- **READ_CONTACTS** - Read device contacts for syncing
- **WRITE_CONTACTS** - (Optional) Write contacts back to device
- **CAMERA** - Take profile pictures
- **READ_MEDIA_IMAGES** - Select images from gallery

## Usage Guide

### First Launch
1. Grant the requested permissions
2. Tap the refresh icon to sync device contacts
3. Contacts will be loaded into the app

### Adding a Contact
1. Tap the floating "+" button
2. Fill in contact details (name and phone are required)
3. Tap the camera icon to add a profile picture
4. Toggle the favorite switch if desired
5. Tap "Save Contact"

### Editing a Contact
1. Tap on any contact from the list
2. Modify the desired fields
3. Tap "Save Contact"

### Searching Contacts
1. Tap the search icon in the top bar
2. Type to search by name, phone, or email
3. Tap the X to clear search

### Favorites
1. Tap the heart icon on any contact to add/remove from favorites
2. Tap the heart icon in the top bar to filter favorites only
3. Tap again to show all contacts

### Deleting a Contact
1. Open the contact detail screen
2. Tap the delete (trash) icon
3. Confirm deletion in the dialog

## Database Schema

### Contact Table
```kotlin
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val email: String = "",
    val profileImageUri: String? = null,
    val isFavorite: Boolean = false,
    val deviceContactId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

## Design Philosophy

### Minimal & Clean
- Generous white space
- Subtle shadows and borders
- Clear visual hierarchy
- Consistent spacing (8dp grid)

### Color Palette
- **Primary**: Indigo (#6366F1) - Modern and professional
- **Accent**: Purple (#8B5CF6) - Complementary highlight
- **Background**: Light gray (#FAFAFA) - Easy on eyes
- **Surface**: White (#FFFFFF) - Content cards
- **Error**: Red (#EF4444) - Destructive actions

### Typography
- System fonts for consistency
- Clear font size hierarchy
- Medium/SemiBold weights for emphasis
- Ample line height for readability

## Key Features Implementation

### Room Database
```kotlin
@Database(entities = [Contact::class], version = 1)
abstract class ContactDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
}
```

### Reactive Data Flow
```kotlin
val contacts: StateFlow<List<Contact>> = combine(
    _searchQuery,
    _showFavoritesOnly
) { query, favoritesOnly ->
    // Reactive filtering based on search and favorites
}
```

### Material 3 Components
- TopAppBar with actions
- FloatingActionButton
- OutlinedTextField with icons
- Cards with elevation
- AlertDialog for confirmations

## Performance Optimizations

- **LazyColumn** for efficient list rendering
- **Flow** for reactive data streaming
- **Coroutines** for background operations
- **Image caching** with Coil
- **Database indexing** on name field

## Future Enhancements

Potential features to add:
- [ ] Contact groups/labels
- [ ] Export/Import contacts (VCF)
- [ ] Dark theme
- [ ] Contact sharing
- [ ] Backup to cloud
- [ ] Multiple phone numbers per contact
- [ ] Custom ringtones
- [ ] Contact notes
- [ ] Birthday reminders

## Troubleshooting

### Common Issues

**App crashes on launch**
- Ensure all permissions are granted
- Check if minimum SDK version is met
- Clear app data and restart

**Images not loading**
- Grant storage/media permissions
- Check if file URI is valid
- Ensure Coil dependency is properly synced

**Database errors**
- Try uninstalling and reinstalling the app
- Check Room schema matches entity definitions

**Build errors**
- Sync Gradle files
- Clean and rebuild project
- Update Android Studio and SDKs

## License

This project is open source and available for educational purposes.

## Contact

For questions or suggestions, please open an issue on the repository.

---

**Built with ❤️ using Jetpack Compose**
