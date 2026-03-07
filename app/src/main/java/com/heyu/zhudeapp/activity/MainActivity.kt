package com.heyu.zhudeapp.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.heyu.zhudeapp.BuildConfig
import com.heyu.zhudeapp.Fragment.game.SudokuFragment
import com.heyu.zhudeapp.Fragment.post.PostFragment
import com.heyu.zhudeapp.Fragment.welcome.CoupleFragment
import com.heyu.zhudeapp.Fragment.welcome.WelcomeFragment
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.adapter.CountdownAdapter
import com.heyu.zhudeapp.data.UpdateInfo
import com.heyu.zhudeapp.data.UserProfile
import com.heyu.zhudeapp.databinding.ActivityMainBinding
import com.heyu.zhudeapp.model.CountdownItem
import com.heyu.zhudeapp.util.LunarCalendar
import com.heyu.zhudeapp.util.ThemeManager
import com.heyu.zhudeapp.viewmodel.MainViewModel
import com.heyu.zhudeapp.viewmodel.UserManagementViewModel
import de.hdodenhof.circleimageview.CircleImageView
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
    private val userManagementViewModel: UserManagementViewModel by viewModels()
    private lateinit var drawerLayout: DrawerLayout
    private var currentFragment: Fragment? = null

    // 自定义抽屉子视图
    private lateinit var drawerFlipper: ViewFlipper
    private lateinit var navHeaderProfileImage: CircleImageView
    private lateinit var navHeaderUsername: TextView
    private lateinit var editUsernameButton: ImageButton
    private lateinit var themeOptionDefault: LinearLayout
    private lateinit var themeOptionTech: LinearLayout
    private lateinit var themeIndicatorDefault: View
    private lateinit var themeIndicatorTech: View
    private lateinit var anniversaryAdapter: CountdownAdapter

    companion object {
        const val EXTRA_CHANGE_AVATAR_REQUEST = "EXTRA_CHANGE_AVATAR_REQUEST"
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "发送短信的权限对于通知功能至关重要", Toast.LENGTH_LONG).show()
            }
        }

    private val imageViewerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data?.getBooleanExtra(EXTRA_CHANGE_AVATAR_REQUEST, false) == true) {
            val cropOptions = CropImageOptions(
                guidelines = CropImageView.Guidelines.ON,
                cropShape = CropImageView.CropShape.OVAL,
                aspectRatioX = 1,
                aspectRatioY = 1,
                fixAspectRatio = true
            )
            cropImageLauncher.launch(CropImageContractOptions(null, cropOptions))
        }
    }

    private val cropImageLauncher = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { uri ->
                userManagementViewModel.uploadAndupdateAvatar(uri)
                Toast.makeText(this, "正在上传头像...", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "图片裁剪失败: ${result.error?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = insets.top }
            binding.bottomNavigation.updatePadding(bottom = insets.bottom)
            // Apply status bar padding to settings/anniversary toolbar in drawer
            findViewById<LinearLayout>(R.id.settings_toolbar)?.setPadding(
                0, insets.top, 0, 0
            )
            findViewById<LinearLayout>(R.id.anniversary_toolbar)?.setPadding(
                0, insets.top, 0, 0
            )
            windowInsets
        }

        setSupportActionBar(binding.toolbar)
        drawerLayout = binding.drawerLayout

        setupDrawerViews()
        setupDrawerToggle()
        setupBottomNavigation()

        if (savedInstanceState == null) {
            showFragment(WelcomeFragment::class.java)
        }

        userManagementViewModel.currentUser.observe(this) { user ->
            if (user?.username.isNullOrEmpty()) showUserSelectionDialog()
            updateNavHeader(user)
        }
        userManagementViewModel.fetchCurrentUser()

        supportFragmentManager.setFragmentResultListener("profile_updated", this) { _, _ ->
            userManagementViewModel.fetchCurrentUser()
            Toast.makeText(this, "用户资料已更新", Toast.LENGTH_SHORT).show()
        }

        checkForUpdates()
        requestSmsPermission()
        checkUpdateAnnouncement()
    }

    private fun setupDrawerViews() {
        // 找到 drawer_content.xml 中的各视图（<include> 后直接可通过 findViewById 访问）
        drawerFlipper = findViewById(R.id.drawer_flipper)
        navHeaderProfileImage = findViewById(R.id.nav_header_profile_image)
        navHeaderUsername = findViewById(R.id.nav_header_username)
        editUsernameButton = findViewById(R.id.edit_username_button)
        themeOptionDefault = findViewById(R.id.theme_option_default)
        themeOptionTech = findViewById(R.id.theme_option_tech)
        themeIndicatorDefault = findViewById(R.id.theme_indicator_default)
        themeIndicatorTech = findViewById(R.id.theme_indicator_tech)

        // 主菜单：主页
        findViewById<LinearLayout>(R.id.drawer_item_home).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // 主菜单：纪念日 → 滑入纪念日页
        findViewById<LinearLayout>(R.id.drawer_item_anniversary).setOnClickListener {
            openAnniversaryPanel()
        }

        // 主菜单：设置 → 滑入设置页
        findViewById<LinearLayout>(R.id.drawer_item_settings).setOnClickListener {
            openSettingsPanel()
        }

        // 主菜单：退出登录
        findViewById<LinearLayout>(R.id.drawer_item_logout).setOnClickListener {
            userManagementViewModel.logout()
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // 设置页：返回按钮
        findViewById<ImageButton>(R.id.settings_back_button).setOnClickListener {
            closeSubPanel()
        }

        // 纪念日页：返回按钮
        findViewById<ImageButton>(R.id.anniversary_back_button).setOnClickListener {
            closeSubPanel()
        }

        // 纪念日页：RecyclerView
        anniversaryAdapter = CountdownAdapter(mutableListOf())
        val anniversaryRecyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.anniversary_recycler_view)
        anniversaryRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        anniversaryRecyclerView.adapter = anniversaryAdapter

        // 设置页：主题选项
        updateThemeCheckmarks()
        themeOptionDefault.setOnClickListener {
            applyAndSaveTheme(ThemeManager.THEME_DEFAULT)
        }
        themeOptionTech.setOnClickListener {
            applyAndSaveTheme(ThemeManager.THEME_TECH)
        }
    }

    private fun openSettingsPanel() {
        drawerFlipper.inAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_from_right)
        drawerFlipper.outAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_out_to_left)
        drawerFlipper.displayedChild = 1
    }

    private fun openAnniversaryPanel() {
        loadAnniversaryData()
        drawerFlipper.inAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_from_right)
        drawerFlipper.outAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_out_to_left)
        drawerFlipper.displayedChild = 2
    }

    private fun closeSubPanel() {
        drawerFlipper.inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        drawerFlipper.outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right)
        drawerFlipper.displayedChild = 0
    }

    private fun updateThemeCheckmarks() {
        val current = ThemeManager.getCurrent(this)
        val isDefault = current == ThemeManager.THEME_DEFAULT
        themeOptionDefault.setBackgroundResource(
            if (isDefault) R.drawable.bg_theme_option_selected else R.drawable.bg_theme_option_unselected
        )
        themeOptionTech.setBackgroundResource(
            if (!isDefault) R.drawable.bg_theme_option_selected else R.drawable.bg_theme_option_unselected
        )
        themeIndicatorDefault.visibility = if (isDefault) View.VISIBLE else View.GONE
        themeIndicatorTech.visibility = if (!isDefault) View.VISIBLE else View.GONE
    }

    private fun loadAnniversaryData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val today = LocalDate.now()
            val items = mutableListOf<CountdownItem>()

            // 纪念日
            val anniversaries = mapOf(
                "见面纪念日" to (12 to 31),
                "在一起的纪念日" to (12 to 2),
                "情人节" to (2 to 14),
                "520" to (5 to 20)
            )
            val lunarAnniversaries = mapOf(
                "七夕节" to (7 to 7)
            )
            for ((name, date) in anniversaries) {
                val nextDate = getNextGregorianDate(today, date.first, date.second)
                items.add(createAnniversaryCountdownItem(name, nextDate, today))
            }
            for ((name, date) in lunarAnniversaries) {
                val nextDate = LunarCalendar.getLunarDate(date.first, date.second)
                items.add(createAnniversaryCountdownItem(name, nextDate, today))
            }

            // 生日
            val birthdays = mapOf(
                "小高的生日" to (10 to 27),
                "小徐的生日" to (2 to 11)
            )
            for ((name, date) in birthdays) {
                val nextDate = getNextGregorianDate(today, date.first, date.second)
                items.add(createCountdownItem(name, nextDate, today))
            }

            // 节日
            val gregorianHolidays = mapOf(
                "元旦" to (1 to 1),
                "清明节" to (4 to 5),
                "劳动节" to (5 to 1),
                "国庆节" to (10 to 1)
            )
            val lunarHolidays = mapOf(
                "春节" to (1 to 1),
                "端午节" to (5 to 5),
                "中秋节" to (8 to 15)
            )
            for ((name, date) in gregorianHolidays) {
                val nextDate = getNextGregorianDate(today, date.first, date.second)
                items.add(createCountdownItem(name, nextDate, today))
            }
            for ((name, date) in lunarHolidays) {
                val nextDate = LunarCalendar.getLunarDate(date.first, date.second)
                items.add(createCountdownItem(name, nextDate, today))
            }

            items.sortBy { it.daysRemaining }

            withContext(Dispatchers.Main) {
                anniversaryAdapter.updateList(items)
            }
        }
    }

    private fun getNextGregorianDate(today: LocalDate, month: Int, day: Int): LocalDate {
        var date = LocalDate.of(today.year, month, day)
        if (date.isBefore(today)) date = date.plusYears(1)
        return date
    }

    private fun createAnniversaryCountdownItem(name: String, nextDate: LocalDate, today: LocalDate): CountdownItem {
        val daysRemaining = ChronoUnit.DAYS.between(today, nextDate)
        val displayName = if (name == "在一起的纪念日" || name == "见面纪念日") {
            val n = nextDate.year - 2024
            if (n > 0) "第${n}个${name}" else name
        } else name
        val dateOverride = String.format("%d年%d月%d日", nextDate.year, nextDate.monthValue, nextDate.dayOfMonth)
        return CountdownItem(name = displayName, month = nextDate.monthValue, day = nextDate.dayOfMonth,
            dateOverride = dateOverride, daysRemaining = daysRemaining, isDeletable = false)
    }

    private fun createCountdownItem(name: String, nextDate: LocalDate, today: LocalDate): CountdownItem {
        val daysRemaining = ChronoUnit.DAYS.between(today, nextDate)
        val dateOverride = String.format("%d年%d月%d日", nextDate.year, nextDate.monthValue, nextDate.dayOfMonth)
        return CountdownItem(name = name, month = nextDate.monthValue, day = nextDate.dayOfMonth,
            dateOverride = dateOverride, daysRemaining = daysRemaining, isDeletable = false)
    }

    private fun applyAndSaveTheme(theme: String) {
        if (ThemeManager.getCurrent(this) == theme) return
        ThemeManager.setTheme(this, theme)
        recreate()
    }

    private fun setupDrawerToggle() {
        val onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (drawerFlipper.displayedChild != 0) {
                    closeSubPanel()
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        val toggle = object : ActionBarDrawerToggle(
            this, drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        ) {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                super.onDrawerSlide(drawerView, slideOffset)
                onBackPressedCallback.isEnabled = slideOffset > 0
            }

            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                // 关闭抽屉时重置到主菜单页（无动画）
                drawerFlipper.inAnimation = null
                drawerFlipper.outAnimation = null
                drawerFlipper.displayedChild = 0
            }
        }
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_second -> showFragment(PostFragment::class.java)
                R.id.tab_third -> showFragment(SudokuFragment::class.java)
                R.id.tab_fourth -> showFragment(CoupleFragment::class.java)
                else -> showFragment(WelcomeFragment::class.java)
            }
            true
        }
    }

    private fun updateNavHeader(user: UserProfile?) {
        user?.let { userProfile ->
            navHeaderUsername.text = userProfile.username
            Glide.with(this)
                .load(userProfile.avatarUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(navHeaderProfileImage)

            navHeaderProfileImage.setOnClickListener {
                val intent = Intent(this, ProfileActivity::class.java).apply {
                    putExtra(ProfileActivity.EXTRA_IMAGE_URL, userProfile.avatarUrl)
                }
                imageViewerLauncher.launch(intent)
            }

            editUsernameButton.setOnClickListener {
                showEditUsernameDialog(userProfile.username)
            }
        } ?: run {
            navHeaderUsername.text = "未登录"
            navHeaderProfileImage.setImageResource(R.drawable.ic_default_avatar)
            navHeaderProfileImage.setOnClickListener(null)
            editUsernameButton.setOnClickListener(null)
        }
    }

    private fun showEditUsernameDialog(currentUsername: String) {
        val editText = EditText(this).apply { setText(currentUsername) }
        MaterialAlertDialogBuilder(this)
            .setTitle("修改用户名")
            .setView(editText)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val newUsername = editText.text.toString().trim()
                if (newUsername.isNotEmpty() && newUsername != currentUsername) {
                    userManagementViewModel.updateUsername(newUsername)
                } else {
                    Toast.makeText(this, "用户名不能为空或与之前相同", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentFragment?.tag?.let { outState.putString("KEY_CURRENT_FRAGMENT_TAG", it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun showFragment(fragmentClass: Class<out Fragment>) {
        val fragmentTag = fragmentClass.name
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        currentFragment?.let { transaction.hide(it) }
        val targetFragment = fragmentManager.findFragmentByTag(fragmentTag)
            ?: fragmentClass.newInstance().also {
                transaction.add(binding.fragmentContainerView.id, it, fragmentTag)
            }
        transaction.show(targetFragment)
        val prevFragment = currentFragment
        currentFragment = targetFragment
        transaction.runOnCommit {
            // Notify WelcomeFragment of visibility changes
            (prevFragment as? WelcomeFragment)?.onBecomeHidden()
            (targetFragment as? WelcomeFragment)?.onBecomeVisible()
        }
        transaction.commit()
    }

    private fun showUserSelectionDialog() {
        val users = arrayOf("高猪猪", "徐大王")
        MaterialAlertDialogBuilder(this)
            .setTitle("请选择你的身份")
            .setItems(users) { _, which ->
                userManagementViewModel.switchUser(users[which])
            }
            .setCancelable(false)
            .show()
    }

    private fun checkUpdateAnnouncement() {
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val lastSeenVersion = prefs.getInt("last_seen_version", 0)
        if (BuildConfig.VERSION_CODE > lastSeenVersion) {
            showNewVersionFeaturesDialog()
            prefs.edit().putInt("last_seen_version", BuildConfig.VERSION_CODE).apply()
        }
    }

    private fun showNewVersionFeaturesDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("🚀 发现新版本特性")
            .setMessage(
                "本次更新带飞你的体验：\n\n" +
                "1️⃣ 【退出登录】功能上线，支持随时切号或重新登录。\n" +
                "2️⃣ 【极致刷图】引入1GB暴力本地缓存，图片加载速度提升300%，滑得再快也不卡顿。\n" +
                "3️⃣ 【原像素视频】支持保存原像素视频到本地库，看过的视频 0 毫秒秒开，画质拉满。"
            )
            .setPositiveButton("知道了，这就去爽") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }

    private fun requestSmsPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED -> {}
            shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS) -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle("需要权限")
                    .setMessage("此应用需要发送短信的权限来通知对方用户。")
                    .setPositiveButton("好的") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                    }
                    .show()
            }
            else -> requestPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    private fun checkForUpdates() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val currentVersionCode = packageInfo.versionCode
                val client = io.ktor.client.HttpClient(io.ktor.client.engine.android.Android)
                val response: io.ktor.client.statement.HttpResponse = client.get(BuildConfig.UPDATE_JSON_URL)
                val jsonString = response.bodyAsText()
                client.close()
                val updateInfo = Json.decodeFromString<UpdateInfo>(jsonString)
                if (updateInfo.latestVersionCode > currentVersionCode) {
                    withContext(Dispatchers.Main) { showUpdateDialog(updateInfo.downloadUrl) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showUpdateDialog(downloadUrl: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("发现新版本")
            .setMessage("不更新你就是🐖中🐖")
            .setPositiveButton("立即更新") { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)))
            }
            .setNegativeButton("我是猪", null)
            .setCancelable(false)
            .show()
    }
}
