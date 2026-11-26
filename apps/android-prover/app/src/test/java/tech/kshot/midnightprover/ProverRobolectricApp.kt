package tech.kshot.midnightprover

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import org.robolectric.Shadows

/**
 * Joe nit 6: Robolectric-only host for Compose UI tests. Registers
 * [ComponentActivity] with MAIN/LAUNCHER so `createComposeRule()` can
 * start it, without putting an extra exported activity in the debug APK
 * (we no longer depend on `debugImplementation ui-test-manifest`).
 */
class ProverRobolectricApp : ProverApplication() {
    override fun onCreate() {
        super.onCreate()
        val cn = ComponentName(packageName, ComponentActivity::class.java.name)
        val info = ActivityInfo().apply {
            name = cn.className
            packageName = cn.packageName
            applicationInfo = this@ProverRobolectricApp.applicationInfo
            exported = true
        }
        val pm = Shadows.shadowOf(packageManager)
        pm.addOrUpdateActivity(info)
        pm.addIntentFilterForActivity(cn, IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        })
    }
}
