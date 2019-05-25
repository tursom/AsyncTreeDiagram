package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.modinterface.*
import cn.tursom.web.HttpContent

@AbsPath("echo", "echo/*", "echo/:message")
@ModPath("echo", "echo/*", "echo/:message")
class Echo : BaseMod("原样返回:message") {
    override suspend fun handle(
        uri: String,
        content: HttpContent
    ): String {
        return content["message"] ?: {
            val sb = StringBuilder()
            content.getParams("*")?.forEach {
                sb.append("${it.urlDecode}/")
            } ?: throw ModException("no message get")
            sb.deleteCharAt(sb.length - 1).toString()
        }()
    }

    override suspend fun handle(content: HttpContent) {
        content.write(handle(content.uri, content))
        content.finish()
    }
}