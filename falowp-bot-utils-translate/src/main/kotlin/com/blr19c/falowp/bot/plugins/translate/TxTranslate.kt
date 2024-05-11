package com.blr19c.falowp.bot.plugins.translate

import com.blr19c.falowp.bot.system.pluginConfigProperty
import com.tencentcloudapi.common.Credential
import com.tencentcloudapi.tmt.v20180321.TmtClient
import com.tencentcloudapi.tmt.v20180321.models.TextTranslateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 腾讯翻译
 */
object TxTranslate {

    private val client by lazy {
        TmtClient(
            Credential(
                pluginConfigProperty("tx.secretId"),
                pluginConfigProperty("tx.secretKey")

            ),
            pluginConfigProperty("tx.region")
        )
    }

    /**
     * 中文转英文
     */
    suspend fun cnToEn(query: String): String {
        if (query.isBlank()) {
            return query
        }
        val chineseRegex = Regex("[\\u4E00-\\u9FA5]+")
        if (!chineseRegex.containsMatchIn(query)) {
            return query
        }
        val request = TextTranslateRequest()
        request.sourceText = query
        request.source = "zh"
        request.target = "en"
        request.projectId = 0L
        val response = withContext(Dispatchers.IO) {
            client.TextTranslate(request)
        }
        return response.targetText
    }

    /**
     * 英文转中文
     */
    suspend fun enToCn(query: String): String {
        if (query.isBlank()) {
            return query
        }
        val request = TextTranslateRequest()
        request.sourceText = query
        request.source = "en"
        request.target = "zh"
        request.projectId = 0L
        val response = withContext(Dispatchers.IO) {
            client.TextTranslate(request)
        }
        return response.targetText
    }
}