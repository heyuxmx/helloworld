package com.heyu.zhudeapp.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.heyu.zhudeapp.Fragment.DatecountFragment
import com.heyu.zhudeapp.Fragment.PostFragment
import com.heyu.zhudeapp.Fragment.WelcomeFragment
import com.heyu.zhudeapp.Fragment.MineFragment
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.databinding.ActivityMainBinding
import com.heyu.zhudeapp.service.MyFirebaseMessagingService
import com.heyu.zhudeapp.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fragmentlist: List<Fragment>
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fragmentlist = listOf(WelcomeFragment(), PostFragment(), DatecountFragment(), MineFragment())
        // Make sure to show the first fragment initially if not already handled
        if (savedInstanceState == null) {
            showFragment(fragmentlist[0])
        }

        binding.bottomNavigation.setOnItemSelectedListener {
            item -> when (item.itemId) {
                R.id.tab_first -> {
                    showFragment(fragmentlist[0])
                    true
                }
                R.id.tab_second -> {
                    showFragment(fragmentlist[1])
                    true
                }
                R.id.tab_third -> {
                    showFragment(fragmentlist[2])
                    true
                }
                R.id.tab_fourth -> {
                    showFragment(fragmentlist[3])
                    true
                }
                else -> {
                    false
                }
            }
        }

        // Handle the intent that started the activity, in case it's from a notification
        handleIntent(intent)
    }

    // The parameter must be non-nullable (Intent) to correctly override the parent method.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /**
     * Checks the intent for a post ID and passes it to the shared ViewModel.
     */
    private fun handleIntent(intent: Intent) {
        if (intent.hasExtra(MyFirebaseMessagingService.EXTRA_POST_ID)) {
            val postId = intent.getStringExtra(MyFirebaseMessagingService.EXTRA_POST_ID)
            Log.d("MainActivity", "Notification click received with post ID: $postId")

            if (postId != null) {
                // Switch to the second tab where the post list is located
                binding.bottomNavigation.selectedItemId = R.id.tab_second

                // Pass the post ID to the shared ViewModel. This is a robust way to communicate
                // with the fragment, regardless of its lifecycle state.
                mainViewModel.onPostIdReceived(postId)
                Log.d("MainActivity", "Posted post ID $postId to MainViewModel.")
            }
        }
    }


    private var currentFragment: Fragment? = null
    // Changed R.id.main to R.id.fragment_container_view
    // Also corrected the show/hide logic for fragments for proper replacement
    private fun showFragment(fragment: Fragment) { // Removed MainActivity. extension receiver, not needed here
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()

        // If there's a current fragment and it's different from the new one, hide it.
        currentFragment?.let {
            if (it != fragment) {
                ft.hide(it)
            }
        }

        // If the fragment is already added, just show it.
        // Otherwise, add it.
        if (fragment.isAdded) {
            ft.show(fragment)
        } else {
            ft.add(R.id.fragment_container_view, fragment)
        }

        // If the new fragment is different from the old one, set it as current.
        if (currentFragment != fragment) {
            currentFragment = fragment
        }

        ft.commit()
    }


}
