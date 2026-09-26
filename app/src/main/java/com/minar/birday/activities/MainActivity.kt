package com.minar.birday.activities

import android.Manifest
import android.animation.ValueAnimator
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioAttributes.Builder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.behavior.HideViewOnScrollBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.minar.birday.R
import com.minar.birday.databinding.ActivityMainBinding
import com.minar.birday.databinding.NavTabBinding
import com.minar.birday.fragments.dialogs.ImportContactsBottomSheet
import com.minar.birday.fragments.dialogs.InsertEventBottomSheet
import com.minar.birday.model.Event
import com.minar.birday.model.EventResult
import com.minar.birday.preferences.backup.BirdayExporter
import com.minar.birday.preferences.backup.BirdayImporter
import com.minar.birday.preferences.backup.CalendarExporter
import com.minar.birday.preferences.backup.CalendarImporter
import com.minar.birday.preferences.backup.ContactsImporter
import com.minar.birday.preferences.backup.CsvExporter
import com.minar.birday.preferences.backup.CsvImporter
import com.minar.birday.preferences.backup.JsonExporter
import com.minar.birday.preferences.backup.JsonImporter
import com.minar.birday.utilities.AppRater
import com.minar.birday.utilities.addInsetsByMargin
import com.minar.birday.utilities.applyBottomProgressiveBlur
import com.minar.birday.utilities.applyLoopingAnimatedVectorDrawable
import com.minar.birday.utilities.applyUserTheme
import com.minar.birday.utilities.eventToResult
import com.minar.birday.utilities.formatTextPreview
import com.minar.birday.utilities.getThemeColor
import com.minar.birday.utilities.resultToEvent
import com.minar.birday.utilities.shareUri
import com.minar.birday.utilities.showIfNotAdded
import com.minar.birday.viewmodels.MainViewModel
import com.minar.birday.widgets.EventWidgetProvider
import com.minar.birday.widgets.MinimalWidgetProvider
import com.minar.birday.workers.ImportContactsWorker
import com.minar.birday.persistence.EventDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    val mainViewModel: MainViewModel by viewModels()
    private lateinit var sharedPrefs: SharedPreferences
    internal lateinit var binding: ActivityMainBinding
    private lateinit var navTabs: List<NavTab>
    private lateinit var backHomeCallback: OnBackPressedCallback
    private var selectedTabIndex = 0

    // Read every time: the activity keeps config changes, so this flips without a recreation
    private val isNavRail: Boolean
        get() = resources.getBoolean(R.bool.nav_rail)
    private var deleteActionActive = false
    private var renderedActionMode: NavActionMode? = null

    // Read by the fragments that need to know whether the navbar leaves its space free
    var navbarHidesOnScroll = false
        private set

    // A destination of the floating navbar, together with the icon and label of its tab
    private data class NavTab(
        val binding: NavTabBinding,
        @param:IdRes val destination: Int,
        @param:DrawableRes val icon: Int,
        @param:StringRes val label: Int,
    )

    // What the detached action button on the right of the navbar does right now
    private enum class NavActionMode { NEW_EVENT, DELETE_SEARCH, ABOUT }

    companion object {
        val GestureInterpolator: Interpolator = PathInterpolatorCompat.create(0f, 0f, 0f, 1f)

        // Material 3 emphasized decelerate, gentler than the gesture one above
        val NavTabInterpolator: Interpolator = PathInterpolatorCompat.create(0.05f, 0.7f, 0.1f, 1f)

        // One beat for the whole tab change: bounds, colors and label move together
        const val NAV_TAB_DURATION = 500L

        // An event to open in the details, from outside the app (the Axiris search)
        const val EXTRA_EVENT_ID = "com.minar.birday.extra.EVENT_ID"
    }

    private val navController: NavController
        get() {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.navHostFragment) as NavHostFragment
            return navHostFragment.navController
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        // Create the notification channel and check the permission on Tiramisu
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Contacts permission is asked after the response to this permission on tiramisu
            if (askNotificationPermission())
            // Ask for contacts permission, if the first permission is already granted
                askContactsPermission()
        } else {
            askContactsPermission()
        }
        createNotificationChannel()

        // Show the introduction for the first launch
        if (sharedPrefs.getBoolean("first", true)) {
            sharedPrefs.edit {
                putBoolean("first", false)
                // Set default accent based on the Android version
                when (Build.VERSION.SDK_INT) {
                    in 23..29 -> putString("accent_color", "blue")
                    31 -> putString("accent_color", "system")
                    else -> putString("accent_color", "monet")
                }
            }
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Set the base theme and the accent
        applyUserTheme(sharedPrefs)

        // Set the task appearance in recent apps
        @Suppress("DEPRECATION")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setTaskDescription(
                ActivityManager.TaskDescription(
                    getString(R.string.app_name),
                    R.mipmap.ic_launcher,
                    ContextCompat.getColor(this, R.color.deepGray)
                )
            )
        } else setTaskDescription(
            ActivityManager.TaskDescription(
                getString(R.string.app_name),
                ContextCompat.getDrawable(this, R.mipmap.ic_launcher)?.toBitmap(),
                ContextCompat.getColor(this, R.color.deepGray)
            )
        )

        // Initialize the binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Prepare the back home callback
        backHomeCallback = object : OnBackPressedCallback(enabled = false) {
            override fun handleOnBackPressed() {
                selectNavTab(0)
            }
        }

        // The floating navbar keeps its own selection, so tab, icon state and label all move
        // together. Tapping the tab already selected pops whatever secondary destination is on top
        navTabs = listOf(
            NavTab(
                binding.navTabHome, R.id.navigationMain,
                R.drawable.nav_animation_home, R.string.title_home
            ),
            NavTab(
                binding.navTabFavorites, R.id.navigationFavorites,
                R.drawable.nav_animation_favorites, R.string.title_favorites
            ),
            NavTab(
                binding.navTabSettings, R.id.navigationSettings,
                R.drawable.nav_animation_settings, R.string.title_settings
            ),
        )
        // Only a tab destination goes back to home: on a secondary one the navigation pops first
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val onTab = navTabs.any { it.destination == destination.id }
            backHomeCallback.isEnabled = onTab && selectedTabIndex != 0
        }
        navTabs.forEachIndexed { index, tab ->
            tab.binding.tabIcon.setImageResource(tab.icon)
            tab.binding.tabLabel.setText(tab.label)
            tab.binding.root.contentDescription = getString(tab.label)
            tab.binding.root.setOnClickListener {
                vibrate()
                if (index == selectedTabIndex) popSecondaryDestination()
                else selectNavTab(index)
            }
        }
        applyNavbarPlacement()

        // Rating stuff
        AppRater.appLaunched(this)

        // The single action button replaces the two fabs: same actions, chosen by where the user is
        binding.navAction.setOnClickListener {
            vibrate()
            when (currentNavActionMode()) {
                NavActionMode.NEW_EVENT -> {
                    val bottomSheet = InsertEventBottomSheet(this)
                    if (bottomSheet.isAdded) return@setOnClickListener
                    bottomSheet.show(supportFragmentManager, "insert_event_bottom_sheet")
                }

                NavActionMode.DELETE_SEARCH -> confirmDeleteSearch()
                NavActionMode.ABOUT ->
                    navController.navigate(R.id.action_navigationSettings_to_aboutFragment)
            }
        }
        // Show a quick description of the action
        binding.navAction.setOnLongClickListener {
            vibrate()
            showSnackbar(
                getString(
                    when (currentNavActionMode()) {
                        NavActionMode.NEW_EVENT -> R.string.new_event_description
                        NavActionMode.DELETE_SEARCH -> R.string.delete_search_title
                        NavActionMode.ABOUT -> R.string.about_description
                    }
                )
            )
            true
        }
        renderNavAction()

        // Enable edge to edge, but specify the navigation bar color for android < Q
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
        // The colorOutlineVariant color is used to store the deep gray and fix the navbar color on older devices
            enableEdgeToEdge(
                navigationBarStyle = SystemBarStyle.dark(
                    getThemeColor(
                        R.attr.colorOutlineVariant,
                        this
                    )
                )
            )
        else {
            enableEdgeToEdge()
            window.isNavigationBarContrastEnforced = false
        }
        binding.navHostFragment.addInsetsByMargin(top = true, right = true, left = true)
        binding.floatingNavbar.addInsetsByMargin(bottom = true, left = true, right = true)

        applyNavbarHideOnScroll(sharedPrefs.getBoolean("hide_scroll", false))

        // Soften the content sliding under the navbar, when the user asked for it
        applyEdgeBlur(sharedPrefs.getBoolean("edge_blur", false))

        // Auto import on launch
        val autoImportEnabled = sharedPrefs.getBoolean("auto_import", false)
        if (autoImportEnabled) {
            val currentLaunchTime = System.currentTimeMillis()
            val lastLaunch = sharedPrefs.getLong("last_launch", 0L)

            // Only launch the auto import if 3 minutes are passed
            if (lastLaunch + (3 * 60 * 1000) < currentLaunchTime) {
                sharedPrefs.edit { putLong("last_launch", currentLaunchTime) }
                thread {
                    ContactsImporter(this, null)
                        .importContacts(this, withDialog = false)
                }
            }

            // Schedule periodic WorkManager job that checks weekly if an import is needed
            try {
                val importRequest = PeriodicWorkRequestBuilder<ImportContactsWorker>(
                    7, TimeUnit.DAYS
                ).build()

                WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "import_contacts_periodic",
                    ExistingPeriodicWorkPolicy.KEEP,
                    importRequest
                )
            } catch (_: Exception) {
                // ignore scheduling issues on older devices / vendors
            }
        } else {
            // Cancel any previously scheduled import worker when disabled
            try {
                WorkManager.getInstance(this).cancelUniqueWork("import_contacts_periodic")
            } catch (_: Exception) {
            }
        }

        // Only the next events, without considering the search string, ordered
        mainViewModel.allEventsUnfiltered.observe(this)
        {
            // Update the widgets and the stats, to avoid strange behaviors when searching
            updateWidget()
            mainViewModel.getStats(it, this)
        }

        onBackPressedDispatcher.addCallback(this, backHomeCallback)
        openEventFromIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openEventFromIntent(intent)
    }

    // Opens the details of the event named by EXTRA_EVENT_ID, if any, then forgets it
    private fun openEventFromIntent(intent: Intent?) {
        val id = intent?.getIntExtra(EXTRA_EVENT_ID, -1) ?: -1
        if (id < 0) return
        intent?.removeExtra(EXTRA_EVENT_ID)
        lifecycleScope.launch {
            val event = withContext(Dispatchers.IO) {
                EventDatabase.getBirdayDatabase(this@MainActivity).eventDao()
                    .getOrderedEventsStatic().firstOrNull { it.id == id }
            } ?: return@launch
            // From wherever the app was: back to the list first, so Back leads home
            navController.popBackStack(R.id.navigationMain, false)
            navController.navigate(
                R.id.detailsFragment,
                androidx.core.os.bundleOf("event" to event, "position" to -1)
            )
        }
    }

    override fun onDestroy() {
        // TODO Experimental settings
        val autoExport = sharedPrefs.getBoolean("auto_export", false)
        val lastExport = sharedPrefs.getLong("last_auto_export", 0)
        val exportFolderUri = sharedPrefs.getString("export_folder", "")?.toUri()
        // Max one auto export every 30 seconds
        val allowedExportTime = lastExport + 30
        val currentTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        if (autoExport && currentTime > allowedExportTime) {
            sharedPrefs.edit { putLong("last_auto_export", currentTime) }
            val thread = Thread {
                BirdayExporter.exportEvents(this, uri = exportFolderUri, autoBackup = true)
            }
            thread.start()
        }
        super.onDestroy()
    }

    private fun NavController.navigateWithOptions(@IdRes destination: Int) {
        // Only way to use custom animations with the bottom navigation bar
        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setEnterAnim(R.animator.nav_enter_anim)
            .setExitAnim(R.animator.nav_exit_anim)
            .setPopEnterAnim(R.animator.nav_pop_enter_anim)
            .setPopExitAnim(R.animator.nav_pop_exit_anim)
            .setPopUpTo(R.id.nav_graph, true)
            .build()

        navigate(destination, null, options)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Manage refresh from settings, since there's a bug where the refresh doesn't work properly
        val refreshed = sharedPrefs.getBoolean("refreshed", false)
        if (refreshed) {
            sharedPrefs.edit { putBoolean("refreshed", false) }
            super.onSaveInstanceState(outState)
        } else {
            // Dirty, dirty fix to avoid TransactionTooBigException:
            // it will restore the home fragment when the theme is changed from system for example,
            // and the app is in recent apps. No issues for screen rotations, keyboard and so on
            super.onSaveInstanceState(Bundle())
        }
    }

    // Update the existing widgets with the newest data and the onclick action
    private fun updateWidget() {
        val intentUpcoming = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        intentUpcoming.component = ComponentName(this, EventWidgetProvider::class.java)
        sendBroadcast(intentUpcoming)

        val intentMinimal = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        intentMinimal.component = ComponentName(this, MinimalWidgetProvider::class.java)
        sendBroadcast(intentMinimal)
    }

    // Create the NotificationChannel. This code does nothing when it already exists
    private fun createNotificationChannel() {
        val soundUri =
            (ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.birday_notification).toUri()
        val attributes: AudioAttributes = Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        val name = getString(R.string.events_notification_channel)
        val descriptionText = getString(R.string.events_channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("events_channel", name, importance).apply {
            description = descriptionText
        }
        // Additional tuning over sound, vibration and notification light
        channel.setSound(soundUri, attributes)
        channel.enableLights(true)
        channel.lightColor = Color.GREEN
        channel.enableVibration(true)
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // Choose a backup registering a callback and following the latest guidelines
    val selectBackup =
        registerForActivityResult(ActivityResultContracts.GetContent()) { fileUri: Uri? ->
            try {
                if (fileUri == null) return@registerForActivityResult
                // Select the correct importer. Always use native import, except for JSON and csv
                when (getFileName(fileUri).split(".").last()) {
                    "json" -> {
                        val jsonImporter = JsonImporter(this, null)
                        jsonImporter.importEventsJson(this, fileUri)
                    }

                    "csv", "xls", "xlsx" -> {
                        val csvImporter = CsvImporter(this, null)
                        csvImporter.importEventsCsv(this, fileUri)
                    }

                    else -> {
                        val birdayImporter = BirdayImporter(this, null)
                        birdayImporter.importEvents(this, fileUri)
                    }
                }
            } catch (e: IOException) {
                // Invalid file, other errors, can't even try to import
                e.printStackTrace()
                showSnackbar(getString(R.string.birday_import_failure))
            }
        }

    // Birday DB backup
    val saveBackup =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val uri: Uri = result.data?.data ?: return@registerForActivityResult
            // Save the selected uri to make it accessible for auto backup
            sharedPrefs.edit(commit = true) { putString("export_folder", uri.toString()) }
            lifecycleScope.launch {
                val exportedPath = withContext(Dispatchers.IO) {
                    BirdayExporter.exportEvents(
                        applicationContext,
                        uri,
                        false
                    )
                }
                if (exportedPath.isNotEmpty()) {
                    showSnackbar(getString(R.string.birday_export_success))
                    shareUri(this@MainActivity, uri)
                } else {
                    showSnackbar(getString(R.string.birday_export_failure))
                }
            }
        }

    // CSV backup
    val saveCsv =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launch {
                val exportedPath = withContext(Dispatchers.IO) {
                    CsvExporter.exportEventsCsv(applicationContext, uri)
                }
                if (exportedPath.isNotEmpty()) {
                    showSnackbar(getString(R.string.birday_export_success))
                    shareUri(this@MainActivity, uri)
                } else {
                    showSnackbar(getString(R.string.birday_export_failure))
                }
            }
        }

    // JSON backup
    val saveJson =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launch {
                val exportedPath = withContext(Dispatchers.IO) {
                    JsonExporter.exportEventsJson(applicationContext, uri)
                }
                if (exportedPath.isNotEmpty()) {
                    showSnackbar(getString(R.string.birday_export_success))
                    shareUri(this@MainActivity, uri)
                } else {
                    showSnackbar(getString(R.string.birday_export_failure))
                }
            }
        }


    // Some utility functions, used from every fragment connected to this activity

    // Given an uri, find the file name
    private fun getFileName(uri: Uri): String {
        var result = ""
        if (uri.scheme == "content") {
            val cursor: Cursor? = contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )
            cursor.use {
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex == -1) return@use
                    result = cursor.getString(columnIndex)
                }
            }
        }
        return result
    }

    // Vibrate using a standard vibration pattern
    // or use system Haptic feedback if vibration is disabled
    fun vibrate() {
        val active = sharedPrefs.getBoolean("vibration", true)
        if (!active) return

        // Deprecated for no reason
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                this.getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        // Create a short vibration for earlier Android versions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
            vib.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        // Or use system Haptic feedback if available
        else
            if (vib.areEffectsSupported(VibrationEffect.EFFECT_CLICK)[0] == Vibrator.VIBRATION_EFFECT_SUPPORT_YES)
                vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    }

    // Animate an animated vector drawable thus centralizing this operation
    fun animateAvd(
        imageView: ImageView,
        avd: Int = R.drawable.animated_experimental_danger,
        endDelay: Long = 0
    ) {
        val loopAnimation = sharedPrefs.getBoolean("loop_avd", true)
        imageView.applyLoopingAnimatedVectorDrawable(
            animatedVector = avd,
            disableLooping = !loopAnimation,
            endDelay = endDelay
        )
    }

    // Show a snackbar containing a given text and an optional action, with a 5 seconds duration
    fun showSnackbar(
        content: String,
        attachView: View? = null,
        action: (() -> Unit)? = null,
        actionText: String? = null,
    ) {
        val snackbar = Snackbar.make(binding.root, content, 5000)
        snackbar.isGestureInsetBottomIgnored = true
        if (attachView != null)
            snackbar.anchorView = attachView
        else
            snackbar.anchorView = binding.floatingNavbar
        if (action != null) {
            snackbar.setActionTextColor(getThemeColor(android.R.attr.colorSecondary, this))
            snackbar.setAction(actionText) {
                action()
            }
        }
        snackbar.show()
    }

    // Show a generic loading indicator
    fun showLoadingIndicator() {
        binding.birdayLoadingIndicator.visibility = View.VISIBLE
    }

    // Hide the generic loading indicator
    fun hideLoadingIndicator() {
        binding.birdayLoadingIndicator.visibility = View.GONE
    }

    // Insert a previously deleted event back in the database
    fun insertBack(eventResult: EventResult) {
        mainViewModel.insert(resultToEvent(eventResult))
    }

    // Force refresh the stats, useful when the events are the same, but something else changes
    fun forceRefreshStats() {
        val events = mainViewModel.allEventsUnfiltered.value
        if (events != null)
            mainViewModel.getStats(events, this)
    }

    // Swap the navbar action between adding an event and deleting the current search results
    fun toggleDeleteFab(active: Boolean = false) {
        if (deleteActionActive == active) return
        deleteActionActive = active
        renderNavAction()
    }

    // Ask before wiping whatever the current search matched, then offer an undo
    private fun confirmDeleteSearch() {
        val searchedEvents = mainViewModel.allEvents.value
        if (searchedEvents.isNullOrEmpty()) return
        // Native dialog
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_db_dialog_title))
            .setMessage(getString(R.string.delete_search_confirm))
            .setIcon(R.drawable.ic_delete_24dp)
            .setPositiveButton(resources.getString(android.R.string.ok)) { dialog, _ ->
                dialog.dismiss()
                mainViewModel.deleteAll(searchedEvents.map { resultToEvent(it) })
                showSnackbar(
                    getString(R.string.deleted),
                    actionText = getString(R.string.cancel),
                    action = fun() {
                        mainViewModel.insertAll(searchedEvents.map { resultToEvent(it) })
                    })
            }
            .setNegativeButton(resources.getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Bottom bar in portrait, side rail in landscape, re-applied on rotation since nothing is
    // inflated again. Bar margins are left alone, they carry the insets
    private fun applyNavbarPlacement() {
        val rail = isNavRail
        val margin = resources.getDimensionPixelSize(R.dimen.floating_navbar_margin)
        val spacing = resources.getDimensionPixelSize(R.dimen.nav_tab_spacing)
        val tabSize = resources.getDimensionPixelSize(R.dimen.nav_tab_height)
        val tabPadding =
            if (rail) 0 else resources.getDimensionPixelSize(R.dimen.nav_tab_padding)

        binding.floatingNavbar.orientation =
            if (rail) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        binding.floatingNavbar.gravity =
            if (rail) Gravity.CENTER_HORIZONTAL else Gravity.CENTER_VERTICAL
        binding.floatingNavbar.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            gravity = if (rail) Gravity.END or Gravity.CENTER_VERTICAL
            else Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }
        // Beside the rail the whole fragment steps aside, card included
        binding.navHostFragment.updatePaddingRelative(
            end = if (rail) resources.getDimensionPixelSize(R.dimen.floating_navbar_space) else 0
        )
        binding.navTabs.orientation =
            if (rail) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL

        navTabs.forEachIndexed { index, tab ->
            tab.binding.root.updateLayoutParams<LinearLayout.LayoutParams> {
                width = if (rail) tabSize else LinearLayout.LayoutParams.WRAP_CONTENT
                height = tabSize
                marginStart = if (!rail && index > 0) spacing else 0
                topMargin = if (rail && index > 0) spacing else 0
            }
            tab.binding.root.gravity = if (rail) Gravity.CENTER else Gravity.CENTER_VERTICAL
            tab.binding.root.updatePaddingRelative(start = tabPadding, end = tabPadding)
        }

        binding.navAction.updateLayoutParams<LinearLayout.LayoutParams> {
            marginStart = if (rail) 0 else margin
            topMargin = if (rail) margin else 0
        }

        renderNavTabs(animate = false)
    }

    // Everything keyed to the edge the navbar sits on has to be redone by hand on rotation
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyNavbarPlacement()
        applyNavbarHideOnScroll(navbarHidesOnScroll)
    }

    // Attached and dropped live, so the option needs no restart
    fun applyNavbarHideOnScroll(enabled: Boolean) {
        navbarHidesOnScroll = enabled
        val params = binding.floatingNavbar.layoutParams as CoordinatorLayout.LayoutParams
        params.behavior = if (enabled) HideViewOnScrollBehavior<View>().apply {
            setViewEdge(
                if (!isNavRail) HideViewOnScrollBehavior.EDGE_BOTTOM
                else if (isRtl) HideViewOnScrollBehavior.EDGE_LEFT
                else HideViewOnScrollBehavior.EDGE_RIGHT
            )
        } else null
        binding.floatingNavbar.layoutParams = params
        // Off while the bar sits off screen would strand it there
        if (!enabled) binding.floatingNavbar.run {
            animate().cancel()
            translationY = 0f
            translationX = 0f
            visibility = View.VISIBLE
        }
    }

    // Navigate to the destination of a tab, keeping the navbar and the back callback in sync
    private fun selectNavTab(index: Int) {
        selectedTabIndex = index
        // Only the home tab exits the app on back, the others go back to it first
        backHomeCallback.isEnabled = index != 0
        renderNavTabs(animate = true)
        renderNavAction()
        navController.navigateWithOptions(navTabs[index].destination)
    }

    // Only do something if there's something in the back stack (only in event details)
    private fun popSecondaryDestination() {
        if (navController.currentBackStackEntry != null &&
            (navController.currentDestination?.label == "fragment_details" ||
                    navController.currentDestination?.label == "fragment_overview" ||
                    navController.currentDestination?.label == "fragment_about" ||
                    navController.currentDestination?.label == "fragment_experimental_settings")
        ) navController.popBackStack()
    }

    // Bounds, label and colors on one beat, icons morph after: together they fight each other
    private fun renderNavTabs(animate: Boolean) {
        // Material 3 roles: neutral bar, tinted active destination, idle tabs transparent
        val selectedContainer = getThemeColor(R.attr.colorSecondaryContainer, this)
        val selectedContent = getThemeColor(R.attr.colorOnSecondaryContainer, this)
        val idleContent = getThemeColor(R.attr.colorOnSurfaceVariant, this)

        navTabs.forEachIndexed { index, tab ->
            val selected = index == selectedTabIndex
            animateTabLabel(tab, selected && !isNavRail, animate)
            tab.binding.root.isSelected = selected
            val container = if (selected) selectedContainer else 0
            val content = if (selected) selectedContent else idleContent
            animateTint(tab.binding.root.backgroundTintList?.defaultColor, container, animate) {
                tab.binding.root.backgroundTintList = ColorStateList.valueOf(it)
            }
            animateTint(tab.binding.tabIcon.imageTintList?.defaultColor, content, animate) {
                tab.binding.tabIcon.imageTintList = ColorStateList.valueOf(it)
                tab.binding.tabLabel.setTextColor(it)
            }
        }
        // The icons morph once the pill has finished growing around the label
        if (animate) binding.floatingNavbar.postDelayed(::morphNavTabIcons, NAV_TAB_DURATION)
        else morphNavTabIcons()
    }

    // The label drives the width, so the pill wrapping it grows and shrinks on the same animation
    private fun animateTabLabel(tab: NavTab, selected: Boolean, animate: Boolean) {
        val label = tab.binding.tabLabel
        // Gone in the layout, the width and the alpha are what hide it from here on
        label.isVisible = true
        val params = label.layoutParams as ViewGroup.MarginLayoutParams
        val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        label.measure(unspecified, unspecified)
        val fullWidth = label.measuredWidth
        val fullMargin = resources.getDimensionPixelSize(R.dimen.nav_tab_label_margin)

        fun apply(fraction: Float) {
            params.width = (fullWidth * fraction).toInt()
            params.marginStart = (fullMargin * fraction).toInt()
            label.alpha = fraction
            label.layoutParams = params
            label.setTag(R.id.tag_nav_label_fraction, fraction)
        }

        val to = if (selected) 1f else 0f
        val from = label.getTag(R.id.tag_nav_label_fraction) as? Float ?: (1f - to)
        if (!animate || from == to) {
            apply(to)
            return
        }
        ValueAnimator.ofFloat(from, to).apply {
            duration = NAV_TAB_DURATION
            interpolator = NavTabInterpolator
            addUpdateListener { apply(it.animatedValue as Float) }
        }.start()
    }

    // Reads the current selection, so a tab changed mid animation still lands right
    private fun morphNavTabIcons() = navTabs.forEachIndexed { index, tab ->
        tab.binding.tabIcon.isChecked = index == selectedTabIndex
    }

    // Cross-fade the tint, so the color lands together with the bounds
    private fun animateTint(from: Int?, to: Int, animate: Boolean, apply: (Int) -> Unit) {
        if (!animate || from == null || from == to) {
            apply(to)
            return
        }
        ValueAnimator.ofArgb(from, to).apply {
            duration = NAV_TAB_DURATION
            interpolator = NavTabInterpolator
            addUpdateListener { apply(it.animatedValue as Int) }
        }.start()
    }

    // The action button means a different thing on every tab, and while a search is running
    private fun currentNavActionMode(): NavActionMode = when {
        selectedTabIndex == 2 -> NavActionMode.ABOUT
        deleteActionActive -> NavActionMode.DELETE_SEARCH
        else -> NavActionMode.NEW_EVENT
    }

    // Every looping vector registers a callback that restarts it forever, so the icon is only
    // rebuilt when the mode really changed: switching tab otherwise stacks up a loop each time
    private fun renderNavAction() {
        val mode = currentNavActionMode()
        if (mode == renderedActionMode) return
        renderedActionMode = mode
        val icon = binding.navActionIcon
        when (mode) {
            NavActionMode.NEW_EVENT -> {
                icon.contentDescription = getString(R.string.new_event)
                animateAvd(icon, R.drawable.animated_add_event, 5000L)
            }

            NavActionMode.DELETE_SEARCH -> {
                icon.contentDescription = getString(R.string.delete_search_title)
                animateAvd(icon, R.drawable.animated_delete, 3000L)
            }

            NavActionMode.ABOUT -> {
                icon.contentDescription = getString(R.string.about_title)
                animateAvd(icon, R.drawable.animated_info, 2000L)
            }
        }
    }

    // The blur is a runtime shader, which exists only from Android 13 on
    fun applyEdgeBlur(enabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        binding.navHostFragment.applyBottomProgressiveBlur(
            enabled = enabled,
            bandPx = resources.getDimension(R.dimen.edge_blur_band)
        )
    }

    // The rail follows the layout direction, and so do the behaviors keyed to its edge
    private val isRtl: Boolean
        get() = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

    // Show a dialog to select the events to import
    fun showImportDialog(
        events: List<Event>,
        title: String? = null,
        message: String? = null,
        icon: Int = R.drawable.ic_backup_restore_24dp,
        showSnack: Boolean = true,
        onInserted: (() -> Unit)? = null
    ) {
        // Shouldn't happen
        if (events.isEmpty()) return

        // Prepare the dialog content
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val surnameFirst = sp.getBoolean("surname_first", false)
        val items: Array<CharSequence> = events.map { ev ->
            formatTextPreview(
                eventToResult(ev),
                this,
                surnameFirst = surnameFirst,
                multiline = false
            )
        }.toTypedArray()

        // Default: all the events are unselected
        val checked = BooleanArray(items.size) { false }

        val builder = MaterialAlertDialogBuilder(this)
        if (!title.isNullOrBlank()) builder.setTitle(title)
        if (!message.isNullOrBlank()) builder.setMessage(message)
        builder.setIcon(icon)

        builder.setMultiChoiceItems(items, checked) { _, which, isChecked ->
            checked[which] = isChecked
        }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val toInsert = events.filterIndexed { i, _ -> checked[i] }
                if (toInsert.isNotEmpty()) {
                    mainViewModel.insertAll(toInsert)
                    if (showSnack) {
                        showSnackbar(getString(R.string.import_success))
                    }
                    onInserted?.invoke()
                } else {
                    if (showSnack) showSnackbar(getString(R.string.import_nothing_found))
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(android.R.string.selectAll, null)

        val dialog = builder.create()

        dialog.setOnShowListener {
            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            val listView = dialog.listView
            neutralButton.setOnClickListener {
                val allSelected = checked.all { it }
                val newValue = !allSelected
                for (i in checked.indices) {
                    checked[i] = newValue
                    listView.setItemChecked(i, newValue)
                }
            }
            listView.setOnItemClickListener { _, _, position, _ ->
                checked[position] = listView.isItemChecked(position)
            }
        }
        dialog.show()
    }

    // Ask contacts permission
    fun askContactsPermission(code: Int = 101): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                code
            )
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    // Ask read calendar permission
    fun askCalendarPermission(code: Int = 301): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CALENDAR),
                code
            )
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    // Ask write calendar permission
    fun askWriteCalendarPermission(code: Int = 401): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_CALENDAR),
                code
            )
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    // Ask notification permission
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun askNotificationPermission(code: Int = 201): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                code
            )
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    // Manage user response to permission requests
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            // Contacts at startup, show a snackbar only for permission denied (don't ask again not selected)
            101 -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS))
                        showSnackbar(getString(R.string.missing_permission_contacts))
                } else if (grantResults.isNotEmpty() /* && grantResults[0] == PackageManager.PERMISSION_GRANTED */) {
                    // Show Bottom sheet for import
                    ImportContactsBottomSheet().showIfNotAdded(
                        supportFragmentManager,
                        ImportContactsBottomSheet.TAG
                    )
                }
            }
            // Contacts while trying to import from contacts
            102 -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS))
                        showSnackbar(
                            getString(R.string.missing_permission_contacts),
                            actionText = getString(R.string.cancel),
                            action = fun() {
                                askContactsPermission()
                            })
                    else showSnackbar(
                        getString(R.string.missing_permission_contacts_forever),
                        actionText = getString(R.string.title_settings),
                        action = fun() {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            })
                        })
                } else {
                    val contactImporter = ContactsImporter(this, null)
                    contactImporter.importContacts(this)
                }
            }
            // Notifications request at startup, plus contacts after
            201 -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))
                        showSnackbar(
                            getString(R.string.missing_permission_notifications),
                            actionText = getString(R.string.cancel),
                            action =
                                fun() {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        askNotificationPermission()
                                    }
                                })
                    else showSnackbar(
                        getString(R.string.missing_permission_notifications_forever),
                        actionText = getString(R.string.title_settings),
                        action = fun() {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            })
                        })
                }
                // Request contacts permission in every case
                askContactsPermission()
            }
            // Calendar permission when importing from calendar
            302 -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR))
                        showSnackbar(
                            getString(R.string.missing_permission_calendar),
                            actionText = getString(R.string.cancel),
                            action = fun() {
                                askCalendarPermission()
                            })
                    else showSnackbar(
                        getString(R.string.missing_permission_calendar_forever),
                        actionText = getString(R.string.title_settings),
                        action = fun() {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            })
                        })
                } else {
                    val calendarImporter = CalendarImporter(this, null)
                    calendarImporter.importCalendar(this)
                }
            }

            402 -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CALENDAR))
                        showSnackbar(
                            getString(R.string.missing_permission_calendar),
                            actionText = getString(R.string.cancel),
                            action = fun() {
                                askWriteCalendarPermission()
                            })
                    else showSnackbar(
                        getString(R.string.missing_permission_calendar_forever),
                        actionText = getString(R.string.title_settings),
                        action = fun() {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            })
                        })
                } else {
                    val calendarExporter = CalendarExporter(this, null)
                    calendarExporter.exportCalendar(this)
                }
            }
        }
    }
}
