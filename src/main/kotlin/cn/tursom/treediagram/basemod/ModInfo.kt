package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.TreeDiagramHttpHandler.getRoute
import cn.tursom.treediagram.TreeDiagramHttpHandler.modManager
import cn.tursom.treediagram.modinterface.*
import cn.tursom.treediagram.token.token
import cn.tursom.web.HttpContent
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap

@AbsPath("modInfo", "modInfo/:modName", "modInfo/:user/:modName", "help", "help/:modName", "help/:user/:modName")
@ModPath("modInfo", "modInfo/:modName", "modInfo/:user/:modName", "help", "help/:modName", "help/:user/:modName")
class ModInfo : BaseMod("查看模组信息") {
    private val modMap = ConcurrentHashMap<Pair<String?, BaseMod>, SoftReference<String>>()

    override fun init() {
        super.init()
    }

    override suspend fun handle(uri: String, content: HttpContent): String? {
        val user = content["user"] ?: try {
            content.token.usr
        } catch (e: Exception) {
            null
        }
        val mod = modManager.findMod(content["modName"] ?: "ModInfo", user) ?: return null
        val buff = modMap[user to mod]
        val buffStr = buff?.get()
        if (buffStr != null) {
            return buffStr
        }
        val baseRoute = "/mod/${if (user == null) "system" else "user/$user"}/"
        val sb = StringBuilder()
        sb.append("$mod\n")
        val help = mod.help
        if (help.isNotEmpty()) sb.append(help)
        sb.append("\nrouters:")
        if (user == null) {
            mod.absRouteList.forEach {
                sb.append("\n|- /$it")
            }
        }
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