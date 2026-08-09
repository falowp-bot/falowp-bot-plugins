@file:Suppress("SpellCheckingInspection")

package com.blr19c.falowp.bot.plugins.bili.api

import com.blr19c.falowp.bot.plugins.bili.api.api.WBI_NAV
import com.blr19c.falowp.bot.system.json.safeString
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.http.*
import java.security.MessageDigest

/**
 * 给需要鉴权的请求补上 WBI 签名
 */
object WBI {
    /**
     * B 站规定的密钥打乱顺序
     */
    private val mixinKeyEncTab = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
        27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0,
        1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
    )

    /**
     * 签好参数后返回一份新 Map
     */
    suspend fun BiliClient.wbiParams(params: Map<String, String>): Map<String, String> {
        return getWbiImg().enc(params)
    }

    /**
     * 导航接口下发的两段 WBI 密钥地址
     */
    private data class WbiImg(
        @field:JsonProperty("img_url")
        val imgUrl: String,
        @field:JsonProperty("sub_url")
        val subUrl: String,
    ) {
        /**
         * 把两段密钥按固定顺序混合成签名密钥
         */
        private val mixinKey: String
            get() = (splitUrl(imgUrl) + splitUrl(subUrl)).let { s ->
                buildString {
                    repeat(32) {
                        append(s[mixinKeyEncTab[it]])
                    }
                }
            }

        /**
         * 加上风控参数、时间戳和签名
         */
        fun enc(params: Map<String, String>): Map<String, String> {
            val map = mutableMapOf(
                "dm_img_list" to "[]",
                "dm_img_str" to "V2ViR0wgMS4wIChPcGVuR0wgRVMgMi4wIENocm9taXVtKQ",
                "dm_cover_img_str" to "QU5HTEUgKEFwcGxlLCBBTkdMRSBNZXRhbCBSZW5kZXJlcjogQXBwbGUgTTQsIFVuc3BlY2lmaWVkIFZlcnNpb24pR29vZ2xlIEluYy4gKEFwcGxlKQ",
                "dm_img_inter" to """[{"x":3564,"y":1509,"z":0,"timestamp":1,"k":75,"type":0},{"x":3745,"y":999,"z":23,"timestamp":546,"k":116,"type":0}]""",
                //"w_webid" to getAccessId(),
            )
            val finalParams = params.toMutableMap()
            finalParams.putAll(map)
            val sortedMap = finalParams.toSortedMap()
            val wts = System.currentTimeMillis() / 1000
            sortedMap["wts"] = wts.toString()
            sortedMap["w_rid"] = (sortedMap.toQueryString() + mixinKey).toMD5()
            return sortedMap
        }

        /**
         * 从图片地址中取出不带后缀的文件名
         */
        private fun splitUrl(url: String): String {
            return url.removeSuffix("/").split("/").last().split(".").first()
        }

        private val hexDigits = "0123456789abcdef".toCharArray()

        /**
         * 转成小写十六进制文本
         */
        fun ByteArray.toHexString() = buildString(this.size shl 1) {
            this@toHexString.forEach { byte ->
                append(hexDigits[byte.toInt() ushr 4 and 15])
                append(hexDigits[byte.toInt() and 15])
            }
        }

        /**
         * 计算 MD5
         */
        fun String.toMD5(): String {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(this.toByteArray())
            return digest.toHexString()
        }

        /**
         * 按查询参数格式拼接内容
         */
        fun Map<String, String>.toQueryString() = this.entries.joinToString("&") { (k, v) ->
            "${k.encodeURLParameter()}=${v.encodeURLParameter()}"
        }

    }

    /**
     * 从导航接口获取当前使用的 WBI 密钥
     */
    private suspend fun BiliClient.getWbiImg(): WbiImg {
        val wbiNode = this.get(WBI_NAV)
        val imgUrl = wbiNode["wbi_img"]["img_url"].safeString()
        val subUrl = wbiNode["wbi_img"]["sub_url"].safeString()
        return WbiImg(imgUrl, subUrl)
    }
}
