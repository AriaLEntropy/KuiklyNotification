package io.github.arialentropy.notification.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.tencent.kuikly.core.render.android.IKuiklyRenderExport
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.css.ktx.toMap
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegator
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegatorDelegate
import io.github.arialentropy.notification.app.adapter.KRColorParserAdapter
import io.github.arialentropy.notification.app.adapter.KRFontAdapter
import io.github.arialentropy.notification.app.adapter.KRImageAdapter
import io.github.arialentropy.notification.app.adapter.KRLogAdapter
import io.github.arialentropy.notification.app.adapter.KRRouterAdapter
import io.github.arialentropy.notification.app.adapter.KRThreadAdapter
import io.github.arialentropy.notification.app.adapter.KRUncaughtExceptionHandlerAdapter
import io.github.arialentropy.notification.module.KRNotificationLaunch
import io.github.arialentropy.notification.module.KRNotificationModule
import org.json.JSONObject

class KuiklyRenderActivity : AppCompatActivity(), KuiklyRenderViewBaseDelegatorDelegate {

    private lateinit var hrContainerView: ViewGroup

    private val kuiklyRenderViewDelegator = KuiklyRenderViewBaseDelegator(this)

    private val pageName: String
        get() = intent.getStringExtra(KEY_PAGE_NAME)?.takeIf { it.isNotEmpty() } ?: "router"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hr)
        hrContainerView = findViewById(R.id.hr_container)
        kuiklyRenderViewDelegator.onAttach(hrContainerView, "", pageName, createPageData())
        // 冷启动 / 点击拉起时解析通知 payload
        KRNotificationLaunch.onNewIntent(this, intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        KRNotificationLaunch.onNewIntent(this, intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        kuiklyRenderViewDelegator.onDetach()
    }

    override fun onPause() {
        super.onPause()
        kuiklyRenderViewDelegator.onPause()
    }

    override fun onResume() {
        super.onResume()
        kuiklyRenderViewDelegator.onResume()
    }

    /** 注册跨端 Module */
    override fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerExternalModule(kuiklyRenderExport)
        with(kuiklyRenderExport) {
            moduleExport(KRNotificationModule.MODULE_NAME) {
                KRNotificationModule()
            }
        }
    }

    private fun createPageData(): Map<String, Any> {
        val param = argsToMap()
        // 供 Demo 页面 configure 使用
        param["hostActivity"] = KuiklyRenderActivity::class.java.name
        param["smallIconResId"] = android.R.drawable.ic_dialog_info
        return param
    }

    private fun argsToMap(): MutableMap<String, Any> {
        val jsonStr = intent.getStringExtra(KEY_PAGE_DATA) ?: return mutableMapOf()
        return JSONObject(jsonStr).toMap()
    }

    companion object {
        private const val KEY_PAGE_NAME = "pageName"
        private const val KEY_PAGE_DATA = "pageData"

        init {
            initKuiklyAdapter()
        }

        fun start(context: Context, pageName: String, pageData: JSONObject) {
            val starter = Intent(context, KuiklyRenderActivity::class.java)
            starter.putExtra(KEY_PAGE_NAME, pageName)
            starter.putExtra(KEY_PAGE_DATA, pageData.toString())
            context.startActivity(starter)
        }

        private fun initKuiklyAdapter() {
            with(KuiklyRenderAdapterManager) {
                krImageAdapter = KRImageAdapter(KRApplication.application)
                krLogAdapter = KRLogAdapter
                krUncaughtExceptionHandlerAdapter = KRUncaughtExceptionHandlerAdapter
                krFontAdapter = KRFontAdapter
                krColorParseAdapter = KRColorParserAdapter(KRApplication.application)
                krRouterAdapter = KRRouterAdapter
                krThreadAdapter = KRThreadAdapter()
            }
        }
    }
}
