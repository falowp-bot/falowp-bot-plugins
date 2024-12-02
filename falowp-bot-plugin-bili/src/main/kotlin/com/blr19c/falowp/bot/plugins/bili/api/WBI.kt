package com.blr19c.falowp.bot.plugins.bili.api

import com.blr19c.falowp.bot.plugins.bili.api.api.WBI_NAV
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLDecoder
import java.security.MessageDigest

object WBI {
    private val mixinKeyEncTab = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
        27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0,
        1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
    )

    suspend fun BiliClient.wbiParams(params: Map<String, String>): Map<String, String> {
        return getWbiImg().enc(params)
    }

    private data class WbiImg(
        @field:JsonProperty("img_url")
        val imgUrl: String,
        @field:JsonProperty("sub_url")
        val subUrl: String,
    ) {
        private val mixinKey: String
            get() = (splitUrl(imgUrl) + splitUrl(subUrl)).let { s ->
                buildString {
                    repeat(32) {
                        append(s[mixinKeyEncTab[it]])
                    }
                }
            }

        suspend fun enc(params: Map<String, String>): Map<String, String> {
            val map = mutableMapOf<String, String>(
                "dm_img_list" to "[]",
                "dm_img_str" to "V2ViR0wgMS4wIChPcGVuR0wgRVMgMi4wIENocm9taXVtKQ",
                "dm_cover_img_str" to "QU5HTEUgKEFwcGxlLCBBTkdMRSBNZXRhbCBSZW5kZXJlcjogQXBwbGUgTTEgUHJvLCBVbnNwZWNpZmllZCBWZXJzaW9uKUdvb2dsZSBJbmMuIChBcHBsZS",
                "dm_img_inter" to """{"ds":[],"wh":[3795,2140,61],"of":[308,616,308]}""",
                "w_webid" to getAccessId(),
            )
            val finalParams = params.toMutableMap()
            finalParams.putAll(map)
            val sortedMap = finalParams.toSortedMap()
            val wts = System.currentTimeMillis() / 1000
            sortedMap["wts"] = wts.toString()
            sortedMap["w_rid"] = (sortedMap.toQueryString() + mixinKey).toMD5()
            return sortedMap
        }

        private fun splitUrl(url: String): String {
            return url.removeSuffix("/").split("/").last().split(".").first()
        }

        private val hexDigits = "0123456789abcdef".toCharArray()

        fun ByteArray.toHexString() = buildString(this.size shl 1) {
            this@toHexString.forEach { byte ->
                append(hexDigits[byte.toInt() ushr 4 and 15])
                append(hexDigits[byte.toInt() and 15])
            }
        }

        fun String.toMD5(): String {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(this.toByteArray())
            return digest.toHexString()
        }

        fun Map<String, String>.toQueryString() = this.entries.joinToString("&") { (k, v) ->
            "${k.encodeURLParameter()}=${v.toString().encodeURLParameter()}"
        }

    }

    private suspend fun BiliClient.getWbiImg(): WbiImg {
        val wbiNode = this.get(WBI_NAV)
        val imgUrl = wbiNode["wbi_img"]["img_url"].asText()
        val subUrl = wbiNode["wbi_img"]["sub_url"].asText()
        return WbiImg(imgUrl, subUrl)
    }

    private suspend fun getAccessId(): String {
        val uid = DatabaseCookiesStorage.getAll().find { it.name == "DedeUserID" }!!.value
        val dynamicUrl = "https://space.bilibili.com/$uid/dynamic"
        val client = BiliClient()
        val dynamicResponse = client.getText(dynamicUrl)
        val dynamicText = dynamicResponse
        val pattern = """<script id="__RENDER_DATA__" type="application/json">(.*?)</script>"""
        val regex = Regex(pattern, RegexOption.DOT_MATCHES_ALL)
        val renderData = regex.find(dynamicText)?.groupValues?.get(1) ?: ""
        val decodedData = URLDecoder.decode(renderData, "UTF-8")
        val json = Json.decodeFromString<JsonObject>(decodedData)
        val accessId = json["access_id"]?.jsonPrimitive?.content ?: ""
        return accessId
    }
}

