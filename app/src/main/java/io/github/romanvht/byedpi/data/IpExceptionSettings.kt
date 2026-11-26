package io.github.romanvht.byedpi.data

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.Inet6Address
import java.net.InetAddress

internal class IpNetwork private constructor(
    val address: InetAddress,
    val prefixLength: Int,
) {
    val isIpv6: Boolean get() = address is Inet6Address
    val cidr: String get() = "${address.hostAddress}/$prefixLength"

    companion object {
        private val decimal = Regex("[0-9]+")
        private val octet = Regex("0|[1-9][0-9]{0,2}")
        private val hextet = Regex("[0-9a-fA-F]{1,4}")

        fun parse(input: String): IpNetwork? {
            val parts = input.trim().split('/', limit = 3)
            if (parts.size > 2) return null
            val host = parts[0]
            val ipv6 = ':' in host
            val bytes = (if (ipv6) parseIpv6(host) else parseIpv4(host)) ?: return null
            val bits = bytes.size * 8
            val prefix = if (parts.size == 1) bits else {
                parts[1].takeIf(decimal::matches)?.toIntOrNull() ?: return null
            }
            if (prefix !in 1..bits) return null

            for (index in bytes.indices) {
                val keep = (prefix - index * 8).coerceIn(0, 8)
                bytes[index] = (bytes[index].toInt() and (0xff shl (8 - keep))).toByte()
            }
            val address = if (ipv6) Inet6Address.getByAddress(null, bytes, -1)
                else InetAddress.getByAddress(bytes)
            return IpNetwork(address, prefix)
        }

        private fun parseIpv4(host: String): ByteArray? {
            val blocks = host.split('.')
            if (blocks.size != 4) return null
            return blocks.map { block ->
                if (!octet.matches(block)) return null
                val value = block.toInt()
                if (value > 255) return null
                value.toByte()
            }.toByteArray()
        }

        private fun parseIpv6(host: String): ByteArray? {
            // A dotted IPv4 tail takes the place of the final two hextets.
            val expanded = if ('.' in host) {
                val tail = parseIpv4(host.substringAfterLast(':')) ?: return null
                val high = ((tail[0].toInt() and 255) shl 8) or (tail[1].toInt() and 255)
                val low = ((tail[2].toInt() and 255) shl 8) or (tail[3].toInt() and 255)
                "${host.substringBeforeLast(':')}:${high.toString(16)}:${low.toString(16)}"
            } else host
            val halves = expanded.split("::", limit = 3)
            if (halves.size > 2) return null
            val left = parseHextets(halves[0]) ?: return null
            val right = parseHextets(halves.getOrElse(1) { "" }) ?: return null
            val count = left.size + right.size
            val groups = if (halves.size == 2) {
                if (count >= 8) return null
                left + List(8 - count) { 0 } + right
            } else {
                if (count != 8) return null
                left
            }
            return ByteArray(16) { index ->
                val word = groups[index / 2]
                (if (index % 2 == 0) word shr 8 else word).toByte()
            }
        }

        private fun parseHextets(value: String): List<Int>? {
            if (value.isEmpty()) return emptyList()
            return value.split(':').map {
                if (!hextet.matches(it)) return null
                it.toInt(16)
            }
        }
    }
}

internal data class IpException(val name: String, val cidr: String, val enabled: Boolean = true)

internal data class IpExceptionSettings(
    val enabled: Boolean,
    val entries: List<IpException>,
) {
    fun activeRoutes(ipv6: Boolean): List<String> {
        if (!enabled) return emptyList()
        return entries.filter { it.enabled }.mapNotNull { entry ->
            IpNetwork.parse(entry.cidr)?.takeIf { ipv6 || !it.isIpv6 }?.cidr
        }
    }

    fun withEntry(entry: IpException, previousCidr: String? = null): IpExceptionSettings? {
        if (entries.any { it.cidr == entry.cidr && it.cidr != previousCidr }) return null
        val updated = if (previousCidr == null) entries + entry else {
            entries.map { if (it.cidr == previousCidr) entry else it }
        }
        return copy(entries = updated)
    }

    fun withoutEntry(cidr: String): IpExceptionSettings =
        copy(entries = entries.filter { it.cidr != cidr })

    fun withPreset(entries: List<IpException>): IpExceptionSettings =
        copy(entries = (this.entries + entries).distinctBy(IpException::cidr))

    fun encode(): String = JsonObject().apply {
        addProperty("enabled", enabled)
        add("entries", JsonArray().apply {
            entries.forEach { entry ->
                add(JsonObject().apply {
                    addProperty("name", entry.name)
                    addProperty("cidr", entry.cidr)
                    addProperty("enabled", entry.enabled)
                })
            }
        })
    }.toString()

    companion object {
        const val KEY = "vpn_ip_exceptions"

        val defaults = IpExceptionSettings(true, listOf(
            IpException("Local Network", "192.168.0.0/24"),
            IpException("mDNS Discovery", "224.0.0.251/32"),
        ))

        fun decode(encoded: String): IpExceptionSettings {
            val root = JsonParser.parseString(encoded).asJsonObject
            val seen = mutableSetOf<String>()
            val entries = root.getAsJsonArray("entries").map { value ->
                val entry = value.asJsonObject
                val name = entry.get("name").strictString().trim()
                val network = IpNetwork.parse(entry.get("cidr").strictString())
                require(name.isNotEmpty() && network != null && seen.add(network.cidr))
                IpException(name, network.cidr, entry.get("enabled").strictBoolean())
            }
            return IpExceptionSettings(root.get("enabled").strictBoolean(), entries)
        }

        private fun JsonElement?.strictString(): String {
            require(this != null && isJsonPrimitive && asJsonPrimitive.isString)
            return asString
        }

        private fun JsonElement?.strictBoolean(): Boolean {
            require(this != null && isJsonPrimitive && asJsonPrimitive.isBoolean)
            return asBoolean
        }
    }
}
