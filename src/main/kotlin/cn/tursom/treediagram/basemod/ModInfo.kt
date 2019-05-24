package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.TreeDiagramHttpHandler.getRoute
import cn.tursom.treediagram.TreeDiagramHttpHandler.modManager
import cn.tursom.treediagram.modinterface.BaseMod
import cn.tursom.treediagram.modinterface.ModPath
import cn.tursom.treediagram.modinterface.routeList
import cn.tursom.treediagram.token.token
import cn.tursom.web.HttpContent
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap

@ModPath("modInfo", "modInfo/:modName", "modInfo/:user/:modName")
class ModInfo : BaseMod("查看模组信息") {
    private val modMap = ConcurrentHashMap<Pair<String?, BaseMod>, SoftReference<String>>()

    override suspend fun handle(uri: String, content: HttpContent): String? {
        val user = content["user"] ?: try {
            content.token.usr
        } catch (e: Exception) {
            null
        }
        val mod = modManager.findMod(content["modName"]!!, user) ?: return null
        val buff = modMap[user to mod]
        val buffStr = buff?.get()
        if (buffStr != null) {
            return buffStr
        }
        if (buff != null) {
            modMap.remove(user to mod)
        }
        val baseRoute = "/mod/${if (user == null) "system" else "user/$user"}/"
        val sb = StringBuilder()
        sb.append("$mod\n")
        val help = mod.help()
        if (help.isNotEmpty()) sb.append(help)
        sb.append("routers:")
        mod.routeList.forEach {
            val route = baseRoute + it
            val routeMod = getRoute(route).first
            if (mod === routeMod) {
                sb.append("\n|- $route")
            }
        }
        val str = sb.toString()
        modMap[user to mod] = SoftReference(str)
        return str
    }

    override suspend fun handle(content: HttpContent) {
        content.write(handle(content.uri, content) ?: "未找到模组")
        content.finish()
    }
}