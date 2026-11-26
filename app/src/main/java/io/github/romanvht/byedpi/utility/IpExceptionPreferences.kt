package io.github.romanvht.byedpi.utility

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import io.github.romanvht.byedpi.data.IpExceptionSettings

internal fun SharedPreferences.readIpExceptions(): IpExceptionSettings {
    return try {
        val encoded = getString(IpExceptionSettings.KEY, null)
        if (encoded != null) IpExceptionSettings.decode(encoded)
        else IpExceptionSettings.defaults.also { writeIpExceptions(it) }
    } catch (error: RuntimeException) {
        Log.w("IpExceptionPreferences", "Unable to read IP exceptions", error)
        IpExceptionSettings(false, emptyList())
    }
}

internal fun SharedPreferences.writeIpExceptions(settings: IpExceptionSettings) {
    edit { putString(IpExceptionSettings.KEY, settings.encode()) }
}
