package cn.tursom.treediagram.basemod

import cn.tursom.tools.fromJson
import cn.tursom.treediagram.datastruct.MultipleEmailData
import cn.tursom.treediagram.modinterface.BaseMod
import cn.tursom.treediagram.token.token
import cn.tursom.web.HttpContent
import com.google.gson.Gson
import java.io.Serializable


class MultipleEmail : BaseMod("群发邮件，每个邮件的内容都不同") {
    override suspend fun handle(
        uri: String,
        content: HttpContent
    ): Serializable? {
        content.token
        try {
            val groupEmailData = gson.fromJson<MultipleEmailData>(content["message"]!!)
            groupEmailData.send()
        } catch (cause: Exception) {
            return "${cause::class.java}: ${cause.message}"
        }
        return "true"
    }

    companion object {
        private val gson = Gson()
    }
}
