package com.moez.QKSMS.feature.gateway

import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.moez.QKSMS.R
import com.moez.QKSMS.common.base.QkViewModel
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.rxkotlin.withLatestFrom
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.*
import javax.inject.Inject


class GatewayViewModel @Inject constructor(
    private val context: Context,
    private val sharedPrefs: SharedPreferences
) : QkViewModel<GatewayView, GatewayState>(GatewayState()) {

    companion object {
        const val PREFERENCE_KEY = "gateway_api_key"
    }

    init {
        newState {
            GatewayState(
                getKey(),
                getAddressList(),
                GatewayServiceUtil.isServiceRunning(context)
            )
        }
    }

    @Suppress("DEPRECATION")
    override fun bindView(view: GatewayView) {
        super.bindView(view)

        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?

        view.tokenClickIntent
                .autoDisposable(view.scope())
                .subscribe {
                    clipboard?.text = it
                    Toast.makeText(context, R.string.gateway_copied_toast, Toast.LENGTH_SHORT).show()
                }

        view.stateClickIntent
                .withLatestFrom(state.map { it.running }) { _, running -> running }
                .autoDisposable(view.scope())
                .subscribe { running ->
                    val intent = Intent(context, GatewayService::class.java)
                    when (running) {
                        true -> {}
                        false -> ContextCompat.startForegroundService(context, intent)
                    }
                    newState { copy(running = !running) }
                }
    }

    private fun getKey(): String {
        var key = sharedPrefs.getString(PREFERENCE_KEY, null)
        if (key == null) {
            key = UUID.randomUUID().toString().substring(0, 8)
            sharedPrefs.edit().putString(PREFERENCE_KEY, key).apply()
        }
        return key
    }

    private fun getAddressList(): List<String> {
        val result = mutableListOf<String>()
        NetworkInterface.getNetworkInterfaces().toList().forEach { networkInterface ->
            networkInterface.inetAddresses.toList().forEach { address ->
                if (!address.isLoopbackAddress && address is Inet4Address) {
                    result.add("http:/${address}:${GatewayService.DEFAULT_PORT}")
                }
            }
        }
        return result
    }

}
