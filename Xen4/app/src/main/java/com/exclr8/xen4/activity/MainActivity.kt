package com.exclr8.xen4.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.onNavDestinationSelected
import com.exclr8.xen4.R
import com.exclr8.xen4.sharedPref.SharedPreference
import com.exclr8.xen4.url.Urls
import com.exclr8.xen4.url.Urls.Companion.baseUrl
import com.exclr8.xen4.viewModel.ViewModelXen
import com.google.android.material.navigation.NavigationView

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private val dl: DrawerLayout by lazy { findViewById(R.id.dlMain) }
    private lateinit var actionBarDrawer: ActionBarDrawerToggle
    private lateinit var vm: ViewModelXen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //View Model
        vm = ViewModelProvider(this)[ViewModelXen::class.java]

        getWindow().setFlags( WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)

        val baseUrl = SharedPreference(this).getValueString("baseUrl")

        //Fake Notification Intent
        //val testUrl = "https://xen4-uat.azurewebsites.net/CUI/TaskList/OpenWorkItemByInstance?instanceId=8&activityGuid=46a73823-e522-4d51-9cf8-b852153b6726"
        //val testUrl = "https://xen4-uat.azurewebsites.net/CUI/Tasklist/OpenWorkItemByInstance?instanceId=19&activityGuid=e9319778-16a3-4d31-abcf-75a28e9e7bd8"
        //val intent = Intent(this, MainActivity::class.java)
        //intent.putExtra("url", testUrl)

        val intentM = intent.getStringExtra("url")

        Log.i(TAG, "Notification Url: $intentM")
        if (intentM != null) {
            vm.setHasUrlIntentVM(true)
            vm.setTaskActivityUrlVM(intentM)
            //Toast.makeText(this, intentM, Toast.LENGTH_SHORT).show()
        }

        //Nav Controller
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fcvMain) as NavHostFragment?
        val navController = navHostFragment!!.navController

        val nv = findViewById<NavigationView>(R.id.nvMain)
        nv.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.title) {
                "Home" -> navController.navigate(R.id.homeFragment)
                "About" -> navController.navigate(R.id.aboutFragment)
                "Logout" -> logout()
            }
            //This is for maintaining the behavior of the Navigation view
            onNavDestinationSelected(menuItem, navController)
            dl.closeDrawer(GravityCompat.START)
            true
        }

        actionBarDrawer = ActionBarDrawerToggle(
            this,
            dl,
            R.string.nav_open,
            R.string.nav_close
        )

        dl.addDrawerListener(actionBarDrawer)
        actionBarDrawer.syncState()
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            hide()
        }
        //Back Button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFrag = navController.currentDestination?.displayName.toString()
                dl.closeDrawer(GravityCompat.START)
                if (currentFrag == "com.exclr8.xen4:id/homeFragment" || currentFrag == "com.exclr8.xen4:id/loginFragment") {
                    moveTaskToBack(true)
                } else if (vm.forgotPassword && currentFrag == "com.exclr8.xen4:id/webViewFragment" || currentFrag == "com.exclr8.xen4:id/passwordResetFragment") {
                    navController.navigate(R.id.loginFragment)
                } else {
                    navController.navigate(R.id.homeFragment)
                }
            }
        })
    }

    private fun logout() {
        SharedPreference(this).clearSharedPreference()
        SharedPreference(this).save("baseUrl", baseUrl)
        SharedPreference(this).save("shouldClearWebView", true)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fcvMain) as NavHostFragment?
        val navController = navHostFragment!!.navController
        navController.navigate(R.id.loginFragment)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fcvMain) as NavHostFragment?
        val navController = navHostFragment!!.navController
        if (navController.currentDestination?.displayName == "com.exclr8.xen4:id/webViewFragment") {
            navController.navigate(R.id.homeFragment)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setDisplayShowCustomEnabled(false)
            }
        }
        if (actionBarDrawer.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}