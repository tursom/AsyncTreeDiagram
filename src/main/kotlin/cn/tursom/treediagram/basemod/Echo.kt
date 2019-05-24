package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.modinterface.BaseMod
import cn.tursom.treediagram.modinterface.ModPath
import cn.tursom.treediagram.modinterface.NoBlocking
import cn.tursom.web.HttpContent
import java.io.Serializable

@NoBlocking
@ModPath("echo/:message", "echo", "echo/*")
class Echo : BaseMod("原样返回:message") {
    override suspend fun handle(
        uri: String,
        content: HttpContent
    ): Serializable? {
        return content["message"] ?: content.getParams("*")?.toTypedArray()
    }
}