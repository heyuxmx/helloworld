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
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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
import com.google.android.material.navigation.NavigationView
import com.heyu.zhudeapp.BuildConfig
import com.heyu.zhudeapp.Fragment.countdown.DatecountFragment
import com.heyu.zhudeapp.Fragment.post.PostFragment
import com.heyu.zhudeapp.Fragment.welcome.MineFragment
import com.heyu.zhudeapp.Fragment.welcome.WelcomeFragment
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.data.UpdateInfo
import com.heyu.zhudeapp.data.UserProfile
import com.heyu.zhudeapp.databinding.ActivityMainBinding
import com.heyu.zhudeapp.viewmodel.MainViewModel
import com.heyu.zhudeapp.viewmodel.UserManagementViewModel
import de.hdodenhof.circleimageview.CircleImageView
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
    private val userManagementViewModel: UserManagementViewModel by viewModels()
    private lateinit var drawerLayout: DrawerLayout
    private var currentFragment: Fragment? = null

    companion object {
        const val EXTRA_CHANGE_AVATAR_REQUEST = "EXTRA_CHANGE_AVATAR_REQUEST"
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
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
            val cropContractOptions = CropImageContractOptions(null, cropOptions)
            cropImageLauncher.launch(cropContractOptions)
        }
    }

    private val cropImageLauncher = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { uri ->
                userManagementViewModel.uploadAndupdateAvatar(uri)
                Toast.makeText(this, "正在上传头像...", Toast.LENGTH_SHORT).show()
            }
        } else {
            val exception = result.error
            Toast.makeText(this, "图片裁剪失败: ${exception?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = insets.top }
            binding.bottomNavigation.updatePadding(bottom = insets.bottom)
            windowInsets
        }

        setSupportActionBar(binding.toolbar)
        drawerLayout = binding.drawerLayout

        val onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        val toggle = object : ActionBarDrawerToggle(
            this, drawerLayout, binding.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        ) {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                super.onDrawerSlide(drawerView, slideOffset)
                onBackPressedCallback.isEnabled = slideOffset > 0
            }
        }
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_second -> showFragment(PostFragment::class.java)
                R.id.tab_third -> showFragment(DatecountFragment::class.java)
                R.id.tab_fourth -> showFragment(MineFragment::class.java)
                else -> showFragment(WelcomeFragment::class.java) // Default to WelcomeFragment
            }
            true
        }

        if (savedInstanceState == null) {
            showFragment(WelcomeFragment::class.java)
        }

        userManagementViewModel.currentUser.observe(this) { user ->
            if (user?.username.isNullOrEmpty()) {
                showUserSelectionDialog()
            }
            updateNavHeader(user)
        }
        userManagementViewModel.fetchCurrentUser()

        handleIntent(intent)
        supportFragmentManager.setFragmentResultListener("profile_updated", this) { _, _ ->
            userManagementViewModel.fetchCurrentUser()
            Toast.makeText(this, "用户资料已更新", Toast.LENGTH_SHORT).show()
        }

        checkForUpdates()
        requestSmsPermission()
        checkUpdateAnnouncement() // 检查并显示更新公告
    }

    /**
     * 检查是否需要显示版本更新公告
     */
    private fun checkUpdateAnnouncement() {
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val lastSeenVersion = prefs.getInt("last_seen_version", 0)
        val currentVersion = BuildConfig.VERSION_CODE

        if (currentVersion > lastSeenVersion) {
            showNewVersionFeaturesDialog()
            // 更新记录的版本号，确保该版本只弹一次
            prefs.edit().putInt("last_seen_version", currentVersion).apply()
        }
    }

    /**
     * 显示新版本功能弹窗
     */
    private fun showNewVersionFeaturesDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("🚀 发现新版本特性")
            .setMessage(
                "本次更新带飞你的体验：\n\n" +
                "1️⃣ 【退出登录】功能上线，支持随时切号或重新登录。\n" +
                "2️⃣ 【极致刷图】引入1GB暴力本地缓存，图片加载速度提升300%，滑得再快也不卡顿。\n" +
                "3️⃣ 【原像素视频】支持保存原像素视频到本地库，看过的视频 0 毫秒秒开，画质拉满。"
            )
            .setPositiveButton("知道了，这就去爽") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun requestSmsPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted.
            }
            shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS) -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle("需要权限")
                    .setMessage("此应用需要发送短信的权限来通知对方用户。")
                    .setPositiveButton("好的") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                    }
                    .show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            }
        }
    }

    private fun showUserSelectionDialog() {
        val users = arrayOf("高猪猪", "徐大王")
        MaterialAlertDialogBuilder(this)
            .setTitle("请选择你的身份")
            .setItems(users) { _, which ->
                val selectedUser = users[which]
                userManagementViewModel.switchUser(selectedUser)
            }
            .setCancelable(false)
            .show()
    }

    private fun updateNavHeader(user: UserProfile?) {
        val headerView = binding.navView.getHeaderView(0)
        val navUsername = headerView.findViewById<TextView>(R.id.nav_header_username)
        val navProfileImage = headerView.findViewById<CircleImageView>(R.id.nav_header_profile_image)
        val editUsernameButton = headerView.findViewById<ImageButton>(R.id.edit_username_button)

        user?.let { userProfile ->
            navUsername.text = userProfile.username
            Glide.with(this)
                .load(userProfile.avatarUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(navProfileImage)

            navProfileImage.setOnClickListener {
                val intent = Intent(this, ProfileActivity::class.java).apply {
                    putExtra(ProfileActivity.EXTRA_IMAGE_URL, userProfile.avatarUrl)
                }
                imageViewerLauncher.launch(intent)
            }

            editUsernameButton.setOnClickListener {
                userProfile.username?.let { currentUsername ->
                    showEditUsernameDialog(currentUsername)
                }
            }
        } ?: run {
            navUsername.text = "未登录"
            navProfileImage.setImageResource(R.drawable.ic_default_avatar)
            navProfileImage.setOnClickListener(null)
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
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {}

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
        currentFragment = targetFragment

        transaction.commit()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {}
            R.id.nav_settings -> {}
            R.id.nav_logout -> {
                userManagementViewModel.logout()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun checkForUpdates() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentVersionCode = try {
                    val packageInfo = packageManager.getPackageInfo(packageName, 0)
                    packageInfo.versionCode
                } catch (e: PackageManager.NameNotFoundException) { -1 }

                if (currentVersionCode == -1) return@launch

                val client = io.ktor.client.HttpClient(io.ktor.client.engine.android.Android)
                val response: io.ktor.client.statement.HttpResponse = client.get(BuildConfig.UPDATE_JSON_URL)
                val jsonString = response.bodyAsText()
                client.close()

                val updateInfo = Json.decodeFromString<UpdateInfo>(jsonString)

                if (updateInfo.latestVersionCode > currentVersionCode) {
                    withContext(Dispatchers.Main) { showUpdateDialog(updateInfo.downloadUrl) }
                }
            } catch (e: Exception) {
                e.printStackTrace() // 更新检查失败时静默处理，不打扰用户
            }
        }
    }

    private fun showUpdateDialog(downloadUrl: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("发现新版本")
            .setMessage("不更新你就是🐖中🐖")
            .setPositiveButton("立即更新") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                startActivity(intent)
            }
            .setNegativeButton("我是猪", null)
            .setCancelable(false)
            .show()
    }
}
