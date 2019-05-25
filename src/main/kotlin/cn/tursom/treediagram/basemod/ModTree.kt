package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.TreeDiagramHttpHandler.modManager
import cn.tursom.treediagram.modinterface.AbsPath
import cn.tursom.treediagram.modinterface.BaseMod
import cn.tursom.treediagram.modinterface.ModPath
import cn.tursom.web.HttpContent
import java.util.concurrent.ConcurrentHashMap

@AbsPath("modTree", "modTree/:user", "mod", "mod/system", "mod/:user", "mods", "mods/system", "mods/:user")
@ModPath("modTree", "modTree/:user", "mod", "mod/system", "mod/:user", "mods", "mods/system", "mods/:user")
class ModTree : BaseMod("返回模组树") {
    @Volatile
    private var cacheTime: Long = 0
    @Volatile
    private var cache: String = ""
    private var userCache = ConcurrentHashMap<String, Pair<Long, String>>()
    @Volatile
    private var systemCacheTime: Long = 0
    @Volatile
    private var systemCache: String = ""

    fun getSystemTree(): String {
        if (modManager.lastChangeTime < systemCacheTime) {
            return systemCache
        }
        val sb = StringBuilder()
        sb.append("system\n")
        modManager.sysModMap.forEach { (t, u) ->
            sb.append("|- id=$t\n|  $u\n")
        }
        systemCache = sb.toString()
        systemCacheTime = System.currentTimeMillis()
        return systemCache
    }

    fun getUserTree(user: String): String {
        val sb = StringBuilder()
        val cachePair = userCache[user]
        if (cachePair != null) {
            val (time, cache) = cachePair
            if (time > modManager.lastChangeTime) {
                return cache
            }
        }
        sb.append("user\n")
        modManager.userModMap[user]?.forEach { (t, u) ->
            sb.append("|- $t: $u\n")
        }
        val str = sb.toString()
        userCache[user] = System.currentTimeMillis() to str
        return str
    }

    override suspend fun handle(uri: String, content: HttpContent): String {
        return when (val user = content["user"]) {
            null -> {
                if (uri == "/mod/system" || uri == "/mods/system") return getSystemTree()
                if (modManager.lastChangeTime < cacheTime) return cache

                val sb = StringBuilder()
                sb.append(getSystemTree())
                sb.append("user\n")
                modManager.userModMap.forEach { (t, u) ->
                    sb.append("|- $t\n")
                    u.forEach { (t, u) ->
                        sb.append("|  |- id=$t\n|  |  $u\n")
                    }
                }
                cache = sb.toString()
                cacheTime = System.currentTimeMillis()
                cache
            }
            "system" -> getSystemTree()
            else -> getUserTree(user)
        }
    }

    override suspend fun handle(content: HttpContent) {
        content.write(handle(content.uri, content))
        content.finish()
    }
}