package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.TreeDiagramHttpHandler.rootRoute
import cn.tursom.treediagram.TreeDiagramHttpHandler.routeLastChangeTime
import cn.tursom.treediagram.modinterface.AbsPath
import cn.tursom.treediagram.modinterface.BaseMod
import cn.tursom.treediagram.modinterface.ModPath
import cn.tursom.treediagram.modinterface.id
import cn.tursom.web.HttpContent
import cn.tursom.web.router.SuspendRouterNode

@AbsPath("", "index.html")
@ModPath("", "index.html")
class HtmlIndex : BaseMod() {
    private var cache: String = ""
    private var cacheTime: Long = 0

    private suspend fun toString(node: SuspendRouterNode<BaseMod>, stringBuilder: StringBuilder, indentation: String) {
        if (node.empty) {
            return
        }
        if (indentation.isNotEmpty()) {
            stringBuilder.append(indentation)
            stringBuilder.append("-&nbsp;")
        }
        val mod = node.value
        stringBuilder.append(
            "<a href=\"${node.fullRoute}\">${node.lastRoute}</a>${
            if (mod != null)
                "&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"/help/${
                if (mod.user != null) "${mod.user}/" else ""
                }${mod.id}\">$mod</a>" else ""
            }<br />"
        )

        node.forEach {
            toString(it, stringBuilder, if (indentation.isEmpty()) "|" else "$indentation&nbsp;&nbsp;|")
        }
        return
    }

    override suspend fun handle(uri: String, content: HttpContent): String {
        if (cacheTime < routeLastChangeTime) {
            val stringBuilder = StringBuilder()
            toString(rootRoute, stringBuilder, "")
            cache = stringBuilder.toString()
            cacheTime = System.currentTimeMillis()
        }
        return cache
    }

    override suspend fun handle(content: HttpContent) {
        content.setResponseHeader("content-type", "text/html; charset=UTF-8")
        content.write("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>index</title></head><body>")
        content.write(handle(content.uri, content))
        content.write("</body></html>")
        content.finish()
    }
}