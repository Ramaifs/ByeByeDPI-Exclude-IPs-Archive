package io.github.romanvht.byedpi

import com.google.gson.Gson
import io.github.romanvht.byedpi.data.AppSettings
import io.github.romanvht.byedpi.data.IpException
import io.github.romanvht.byedpi.data.IpExceptionSettings
import io.github.romanvht.byedpi.data.IpNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class IpExceptionSettingsTest {
    @Test
    fun normalizesIpv4HostsAndMasksNetworkAddresses() {
        assertEquals("192.168.0.149/32", cidr(" 192.168.0.149 "))
        assertEquals(cidr("192.168.0.149/32"), cidr("192.168.0.149"))
        assertEquals("192.168.0.0/24", cidr("192.168.0.149/24"))
        assertEquals("128.0.0.0/1", cidr("192.168.0.149/1"))
        assertEquals("172.16.0.0/12", cidr("172.31.7.4/12"))
    }

    @Test
    fun normalizesEquivalentIpv6AddressesAndPrefixes() {
        assertEquals(cidr("fe80::1/128"), cidr("FE80:0:0:0:0:0:0:1"))
        assertEquals(cidr("fe80::/10"), cidr("febf::1234/10"))
        assertEquals(cidr("fc00::/7"), cidr("fd12:3456::/7"))
        assertEquals(cidr("::ffff:c000:201/128"), cidr("::ffff:192.0.2.1"))
        assertTrue(IpNetwork.parse("::ffff:192.0.2.1")!!.isIpv6)
        assertNotNull(cidr("ff02::fb/128"))
    }

    @Test
    fun rejectsNamesNonNumericFormsAndDefaultRoutes() {
        listOf(
            "", "example.com", "localhost", "127.1", "01.0.0.1", "256.1.2.3",
            "1.2.3.4/", "1.2.3.4/+24", "1.2.3.4/24/32", "1.2.3.4/33", "1.2.3.4/-1",
            "0.0.0.0/0", "192.168.0.149/00", "::/0", "ffff::1/0", "::1/129",
            "[::1]", "fe80::1%wlan0", "1:::2", "1::2::3", ":1:2:3:4:5:6:7",
            "1:2:3:4:5:6:7:8::", "1:2:3:4:5:6:7", "::ffff:256.1.2.3",
        ).forEach { assertNull(it, cidr(it)) }
    }

    @Test
    fun routesRespectBothSwitchesAndVpnIpFamilies() {
        val defaults = IpExceptionSettings.defaults
        assertEquals(listOf("192.168.0.0/24", "224.0.0.251/32"), defaults.activeRoutes(false))
        val ipv6 = IpException("IPv6", cidr("ff02::fb")!!)
        val settings = defaults.copy(entries = listOf(defaults.entries.first().copy(enabled = false), ipv6))
        assertEquals(listOf(ipv6.cidr), settings.activeRoutes(true))
        assertTrue(settings.activeRoutes(false).isEmpty())
        assertTrue(settings.copy(enabled = false).activeRoutes(true).isEmpty())
    }

    @Test
    fun entriesCanBeAddedEditedDisabledAndRemovedWithoutReseeding() {
        val empty = IpExceptionSettings(true, emptyList())
        val newEntry = IpException("Tracker", cidr("192.168.0.149")!!)
        val added = empty.withEntry(newEntry)!!
        assertNull(added.withEntry(newEntry.copy(name = "Duplicate", cidr = cidr("192.168.0.149/32")!!)))
        val updated = newEntry.copy(name = "Edited", cidr = cidr("192.168.0.149/24")!!, enabled = false)
        val edited = added.withEntry(updated, newEntry.cidr)!!
        val restored = IpExceptionSettings.decode(edited.encode())
        assertEquals(listOf(updated), restored.entries)
        assertTrue(restored.activeRoutes(false).isEmpty())
        assertEquals(empty, IpExceptionSettings.decode(restored.withoutEntry(updated.cidr).encode()))
    }

    @Test
    fun presetsLeaveExistingEntriesUntouched() {
        val existing = IpException("Renamed", "192.168.0.0/24", false)
        val settings = IpExceptionSettings(false, listOf(existing))
        val merged = settings.withPreset(IpExceptionSettings.defaults.entries)
        assertEquals(IpExceptionSettings(false, listOf(existing, IpExceptionSettings.defaults.entries.last())), merged)
        assertEquals(merged, merged.withPreset(IpExceptionSettings.defaults.entries))
    }

    @Test
    fun backupRoundTripsSwitchesNamesAndEmptyLists() {
        val settings = IpExceptionSettings(false, listOf(IpException("OSC \u0422\u0440\u0435\u043a\u0435\u0440", cidr("::1")!!, false)))
        for (state in listOf(settings, settings.copy(entries = emptyList()), IpExceptionSettings.defaults)) {
            val backup = AppSettings(
                app = "test", version = "1", history = null, apps = null, domainLists = null,
                settings = mapOf(IpExceptionSettings.KEY to state.encode()),
            )
            val restored = Gson().fromJson(Gson().toJson(backup), AppSettings::class.java)
            assertEquals(state, IpExceptionSettings.decode(restored.settings!![IpExceptionSettings.KEY] as String))
        }
    }

    @Test
    fun importRejectsInvalidAndEquivalentDuplicateRoutes() {
        fun entry(network: String) = IpException("Test", network)
        listOf(
            listOf(entry("0.0.0.0/0")),
            listOf(entry("192.168.0.0/24"), entry("192.168.0.149/24")),
            listOf(entry("fe80::/10"), entry("FEBF:0:0:0:0:0:0:1/10")),
        ).forEach {
            assertThrows(IllegalArgumentException::class.java) {
                IpExceptionSettings.decode(IpExceptionSettings(true, it).encode())
            }
        }
    }

    private fun cidr(value: String): String? = IpNetwork.parse(value)?.cidr
}
