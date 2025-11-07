package com.heyu.zhudeapp.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.heyu.zhudeapp.Fragment.DatecountFragment
import com.heyu.zhudeapp.Fragment.MineFragment
import com.heyu.zhudeapp.Fragment.PostFragment
import com.heyu.zhudeapp.Fragment.WelcomeFragment
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.databinding.ActivityMainBinding
import com.heyu.zhudeapp.viewmodel.MainViewModel
import com.heyu.zhudeapp.viewmodel.UserManagementViewModel
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
    private val userManagementViewModel: UserManagementViewModel by viewModels()
    private lateinit var drawerLayout: DrawerLayout

    companion object {
        const val EXTRA_CHANGE_AVATAR_REQUEST = "EXTRA_CHANGE_AVATAR_REQUEST"
    }

    private val imageViewerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data?.getBooleanExtra(EXTRA_CHANGE_AVATAR_REQUEST, false) == true) {
            // User has requested to change avatar from the viewer, now launch the cropper.
            val cropOptions = CropImageOptions(
                guidelines = CropImageView.Guidelines.ON,
                cropShape = CropImageView.CropShape.OVAL,
                aspectRatioX = 1,
                aspectRatioY = 1
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                R.id.tab_first -> showFragment(WelcomeFragment::class.java)
                R.id.tab_second -> showFragment(PostFragment::class.java)
                R.id.tab_third -> showFragment(DatecountFragment::class.java)
                R.id.tab_fourth -> showFragment(MineFragment::class.java)
                else -> return@setOnItemSelectedListener false
            }
            true
        }

        // getAndSaveFcmToken(BuildConfig.USER_ID)

        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.tab_first
        }

        handleIntent(intent)

        supportFragmentManager.setFragmentResultListener("profile_updated", this) { _, _ ->
            userManagementViewModel.fetchCurrentUser() // Re-fetch to update nav header
            Toast.makeText(this, "用户资料已更新", Toast.LENGTH_SHORT).show()
        }

        setupNavHeader()
    }

    private fun setupNavHeader() {
        val headerView = binding.navView.getHeaderView(0)
        val navUsername = headerView.findViewById<TextView>(R.id.nav_header_username)
        val navProfileImage = headerView.findViewById<CircleImageView>(R.id.nav_header_profile_image)
        val editUsernameButton = headerView.findViewById<ImageButton>(R.id.edit_username_button)

        userManagementViewModel.currentUser.observe(this) { user ->
            user?.let { userProfile ->
                navUsername.text = userProfile.username
                Glide.with(this)
                    .load(userProfile.avatarUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .into(navProfileImage)

                navProfileImage.setOnClickListener {
                    val intent = Intent(this, ImageViewerActivity::class.java).apply {
                        putExtra(ImageViewerActivity.EXTRA_IMAGE_URL, userProfile.avatarUrl)
                    }
                    imageViewerLauncher.launch(intent)
                }

                editUsernameButton.setOnClickListener {
                    userProfile.username?.let { currentUsername ->
                        showEditUsernameDialog(currentUsername)
                    }
                }
            }
        }
        userManagementViewModel.fetchCurrentUser()
    }

    private fun showEditUsernameDialog(currentUsername: String) {
        val editText = EditText(this).apply {
            setText(currentUsername)
        }

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
        supportFragmentManager.findFragmentByTag(supportFragmentManager.fragments.find { it.isVisible }?.tag)?.let {
            outState.putString("KEY_CURRENT_FRAGMENT_TAG", it.tag)
        }
    }

    // private fun getAndSaveFcmToken(userId: String) {
    //     // ... (FCM token logic remains the same)
    // }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }



    private fun handleIntent(intent: Intent?) {
        // ... (handleIntent logic remains the same)
    }

    private fun showFragment(fragmentClass: Class<out Fragment>) {
        val fragmentTag = fragmentClass.name
        val fragmentManager = supportFragmentManager
        var fragment = fragmentManager.findFragmentByTag(fragmentTag)

        val transaction = fragmentManager.beginTransaction()

        // Hide the current visible fragment
        fragmentManager.fragments.find { it.isVisible }?.let { transaction.hide(it) }

        if (fragment == null) {
            fragment = fragmentClass.newInstance()
            transaction.add(binding.fragmentContainerView.id, fragment, fragmentTag)
        } else {
            transaction.show(fragment)
        }

        transaction.commit()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_home -> {
                // Handle the home action
            }
            R.id.nav_settings -> {
                // Handle the settings action
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
