package io.github.romanvht.byedpi.fragments

import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import io.github.romanvht.byedpi.R
import io.github.romanvht.byedpi.data.IpException
import io.github.romanvht.byedpi.data.IpExceptionSettings
import io.github.romanvht.byedpi.data.IpNetwork
import io.github.romanvht.byedpi.databinding.DialogIpExceptionBinding
import io.github.romanvht.byedpi.utility.findPreferenceNotNull
import io.github.romanvht.byedpi.utility.getPreferences
import io.github.romanvht.byedpi.utility.readIpExceptions
import io.github.romanvht.byedpi.utility.writeIpExceptions

class IpExceptionsFragment : PreferenceFragmentCompat() {
    private lateinit var settings: IpExceptionSettings
    private var dialog: AlertDialog? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.ip_exceptions, rootKey)
        val prefs = requireContext().getPreferences()
        settings = prefs.readIpExceptions()

        val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val notice = findPreferenceNotNull<Preference>("ip_exceptions_notice")
        notice.setSummary(if (supported) R.string.ip_exceptions_restart else R.string.ip_exceptions_unsupported)
        findPreferenceNotNull<Preference>("ip_exceptions_ipv6_notice").isVisible =
            supported && !prefs.getBoolean("ipv6_enable", false)

        val global = findPreferenceNotNull<SwitchPreference>("ip_exceptions_enabled")
        global.isChecked = settings.enabled
        global.setOnPreferenceChangeListener { _, newValue ->
            save(settings.copy(enabled = newValue == true), refreshEntries = false)
            true
        }
        findPreferenceNotNull<Preference>("ip_exceptions_add").setOnPreferenceClickListener {
            showEditor()
            true
        }
        findPreferenceNotNull<Preference>("ip_exceptions_presets").setOnPreferenceClickListener {
            showPresets()
            true
        }
        showEntries()
    }

    override fun onResume() {
        super.onResume()
        // Preference state restored during activity recreation may predate an import.
        findPreferenceNotNull<SwitchPreference>("ip_exceptions_enabled").isChecked = settings.enabled
    }

    override fun onDestroyView() {
        dialog?.dismiss()
        dialog = null
        super.onDestroyView()
    }

    private fun save(updated: IpExceptionSettings, refreshEntries: Boolean = true) {
        requireContext().getPreferences().writeIpExceptions(updated)
        settings = updated
        if (refreshEntries) showEntries()
    }

    private fun showEntries() {
        val category = findPreferenceNotNull<PreferenceCategory>("ip_exceptions_entries")
        // Keep existing rows attached so keyboard/controller focus survives ordinary edits.
        val rows = settings.entries.associateBy(IpException::cidr)
        for (index in category.preferenceCount - 1 downTo 0) {
            val row = category.getPreference(index)
            if (row.key !in rows) category.removePreference(row)
        }
        category.isVisible = rows.isNotEmpty()
        settings.entries.forEachIndexed { index, entry ->
            val row = category.findPreference<Preference>(entry.cidr) ?: Preference(requireContext()).apply {
                key = entry.cidr
                isPersistent = false
                category.addPreference(this)
            }
            row.order = index
            row.title = entry.name
            val state = if (entry.enabled) R.string.ip_exceptions_on else R.string.ip_exceptions_off
            row.summary = getString(R.string.ip_exceptions_entry_summary, entry.cidr, getString(state))
            row.setOnPreferenceClickListener {
                showEditor(entry)
                true
            }
        }
    }

    private fun showEditor(existing: IpException? = null) {
        val fields = DialogIpExceptionBinding.inflate(layoutInflater)
        fields.ipExceptionName.setText(existing?.name)
        fields.ipExceptionCidr.setText(existing?.cidr)
        fields.ipExceptionEnabled.isChecked = existing?.enabled ?: true
        val builder = AlertDialog.Builder(requireContext())
            .setTitle(if (existing == null) R.string.ip_exceptions_add else R.string.ip_exceptions_edit)
            .setView(fields.root)
            .setPositiveButton(R.string.ip_exceptions_save, null)
            .setNegativeButton(android.R.string.cancel, null)
        if (existing != null) {
            builder.setNeutralButton(R.string.ip_exceptions_delete) { _, _ ->
                save(settings.withoutEntry(existing.cidr))
            }
        }
        val editor = builder.create()
        dialog = editor
        editor.setOnShowListener {
            editor.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (saveEntry(fields, existing)) editor.dismiss()
            }
        }
        editor.show()
    }

    private fun saveEntry(fields: DialogIpExceptionBinding, existing: IpException?): Boolean {
        val name = fields.ipExceptionName.text.toString().trim()
        val network = IpNetwork.parse(fields.ipExceptionCidr.text.toString())
        if (name.isEmpty()) return showError(fields.ipExceptionName, R.string.ip_exceptions_name_required)
        if (network == null) return showError(fields.ipExceptionCidr, R.string.ip_exceptions_invalid)
        val entry = IpException(name, network.cidr, fields.ipExceptionEnabled.isChecked)
        val updated = settings.withEntry(entry, existing?.cidr)
            ?: return showError(fields.ipExceptionCidr, R.string.ip_exceptions_duplicate)
        save(updated)
        return true
    }

    private fun showError(field: EditText, message: Int): Boolean {
        field.requestFocus()
        field.error = getString(message)
        return false
    }

    private fun showPresets() {
        val presets = listOf(
            R.string.ip_exceptions_vrchat to IpExceptionSettings.defaults.entries.map { it.cidr },
            R.string.ip_exceptions_private to listOf("192.168.0.0/16", "10.0.0.0/8", "172.16.0.0/12"),
            R.string.ip_exceptions_mdns to listOf("224.0.0.251/32", "ff02::fb/128"),
            R.string.ip_exceptions_ssdp to listOf("239.255.255.250/32"),
            R.string.ip_exceptions_local_ipv6 to listOf("fe80::/10", "fc00::/7"),
        )
        val labels = presets.map { (name, routes) ->
            "${getString(name)}\n${routes.joinToString(", ")}"
        }.toTypedArray()
        dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.ip_exceptions_presets)
            .setItems(labels) { _, index ->
                val (name, routes) = presets[index]
                val entries = if (name == R.string.ip_exceptions_vrchat) IpExceptionSettings.defaults.entries
                    else routes.map { IpException(getString(name), checkNotNull(IpNetwork.parse(it)).cidr) }
                val updated = settings.withPreset(entries)
                if (updated == settings) {
                    Toast.makeText(requireContext(), R.string.ip_exceptions_preset_exists, Toast.LENGTH_SHORT).show()
                } else {
                    save(updated)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
