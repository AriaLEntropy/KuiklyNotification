package io.github.arialentropy.notification.module

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * 无 UI Fragment，承载运行时权限结果回传。
 * Module 不是 Activity，无法直接拿到 onRequestPermissionsResult，因此借 Fragment 转发。
 */
class KRNotificationPermissionFragment : Fragment() {

    private var callback: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != REQUEST_CODE) return
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        callback?.invoke(granted)
        callback = null
        if (isAdded) {
            fragmentManager?.beginTransaction()?.remove(this)?.commitAllowingStateLoss()
        }
    }

    companion object {
        private const val REQUEST_CODE = 0x4E01
        private const val TAG = "KRNotificationPermission"

        fun request(activity: FragmentActivity, callback: (Boolean) -> Unit) {
            val existing = activity.supportFragmentManager
                .findFragmentByTag(TAG) as? KRNotificationPermissionFragment
            val fragment = existing ?: KRNotificationPermissionFragment()
            fragment.callback = callback
            if (existing == null) {
                activity.supportFragmentManager.beginTransaction()
                    .add(fragment, TAG)
                    .commitAllowingStateLoss()
                activity.supportFragmentManager.executePendingTransactions()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                fragment.requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE
                )
            }
        }
    }
}
