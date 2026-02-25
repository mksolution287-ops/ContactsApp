# Quick Start Guide - Contacts App

## What You Have

A complete, production-ready Android contacts application with:

✅ **Full CRUD operations** (Create, Read, Update, Delete)
✅ **Room database** for local storage
✅ **Material Design 3** UI
✅ **Device contacts sync**
✅ **Search functionality**
✅ **Favorites filtering**
✅ **Profile image support**
✅ **Clean MVVM architecture**

## Fastest Way to Run

### Option 1: Android Studio (Recommended)
1. Open Android Studio
2. File > Open > Select the `ContactsApp` folder
3. Wait for Gradle sync (2-5 minutes first time)
4. Click the green "Run" button (▶️)
5. Select your device/emulator
6. Grant permissions when app launches

### Option 2: Command Line
```bash
cd ContactsApp
./gradlew installDebug
adb shell am start -n com.example.contactsapp/.MainActivity
```

## What to Test First

1. **Sync Contacts** - Tap refresh icon to import device contacts
2. **Add Contact** - Tap the + button, fill details, save
3. **Search** - Tap search icon, type to filter
4. **Favorites** - Tap heart icons to mark favorites
5. **Edit** - Tap any contact to edit details
6. **Profile Image** - Tap camera icon when editing

## File Structure Overview

```
ContactsApp/
├── app/
│   ├── build.gradle.kts              # Dependencies & build config
│   └── src/main/
│       ├── AndroidManifest.xml       # Permissions & app config
│       ├── java/.../
│       │   ├── data/                 # Database & models
│       │   ├── ui/                   # Screens & UI
│       │   └── MainActivity.kt       # Entry point
│       └── res/                      # Resources & XML
├── build.gradle.kts                  # Project-level config
└── README.md                         # Full documentation
```

## Key Files Explained

**MainActivity.kt** - App entry point, handles permissions
**ContactViewModel.kt** - Business logic & state management
**ContactListScreen.kt** - Main contacts list UI
**ContactDetailScreen.kt** - Edit/add contact UI
**ContactDatabase.kt** - Room database configuration
**Contact.kt** - Data model

## Customization Quick Tips

### Change Primary Color
Edit: `ui/theme/Color.kt`
```kotlin
val Primary = Color(0xFF6366F1)  // Change hex code
```

### Modify Database
Edit: `data/model/Contact.kt` (add fields)
Then: Increment version in `ContactDatabase.kt`

### Add New Screen
1. Create in `ui/screens/`
2. Add route in `ui/navigation/Navigation.kt`
3. Add navigation call

## Common Customizations

### Change App Name
Edit: `res/values/strings.xml`
```xml
<string name="app_name">Your Name</string>
```

### Change Package Name
1. Right-click package in Android Studio
2. Refactor > Rename
3. Update in AndroidManifest.xml

### Add New Contact Field
1. Add to `Contact.kt` data class
2. Update database version
3. Add UI field in `ContactDetailScreen.kt`
4. Update DAO queries if needed

## Dependencies Used

All managed in `app/build.gradle.kts`:
- Jetpack Compose (UI framework)
- Room (Database)
- Navigation (Screen routing)
- Coil (Image loading)
- Accompanist (Permissions)
- Material 3 (Design system)

## Build Variants

**Debug** - Default, includes debugging tools
**Release** - Optimized, requires signing key

## Minimum Requirements

- Android 8.0 (API 26) or higher
- 50MB free space
- Contact, Camera, Storage permissions

## Troubleshooting

**"Unresolved reference" errors**
→ File > Invalidate Caches > Restart

**Gradle sync failed**
→ Check internet connection, retry sync

**App crashes on launch**
→ Grant all permissions, reinstall app

**Can't import contacts**
→ Ensure READ_CONTACTS permission granted

## Next Steps

1. ✅ Run the app and test features
2. 📖 Read full README.md for details
3. 🎨 Customize colors and theme
4. 🚀 Add your own features
5. 📱 Deploy to Play Store (if desired)

## Support

- Read the detailed README.md
- Check inline code comments
- Android documentation: developer.android.com
- Compose documentation: developer.android.com/jetpack/compose

---

**You're all set! Just open in Android Studio and run.**
