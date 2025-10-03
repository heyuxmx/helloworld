package com.heyu.zhudeapp.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.heyu.zhudeapp.Fragment.FirstFragment
import com.heyu.zhudeapp.Fragment.SecondFragment
import com.heyu.zhudeapp.Fragment.ThirdFragment
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.databinding.ActivityMainBinding
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.HttpClient
import com.heyu.zhudeapp.data.Post



class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fragmentlist: List<Fragment>
    /**
     * 创建Supabase客户端实例，并安装Postgrest模块。
     */
    val supabase = createSupabaseClient(
        supabaseUrl = "https://bvgtzgxscnqhugjirgzp.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJ2Z3R6Z3hzY25xaHVnamlyZ3pwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTk1MDA5NTYsImV4cCI6MjA3NTA3Njk1Nn0.bSF7FkLgvFwsJOODgG8AKtLBpF-OPyzaUfoWSUmoFes"
    ) {
        install(Postgrest)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fragmentlist = listOf(FirstFragment(), SecondFragment(), ThirdFragment())
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
                else -> {
                    false
                }
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