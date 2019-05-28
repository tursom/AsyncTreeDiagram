package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.TreeDiagramHttpHandler.modManager
import cn.tursom.treediagram.modinterface.AbsPath
import cn.tursom.treediagram.modinterface.BaseMod
import cn.tursom.treediagram.modinterface.ModPath
import cn.tursom.web.HttpContent
import jdk.nashorn.internal.objects.NativeArray.forEach
import java.util.concurrent.ConcurrentHashMap

@AbsPath("modTree", "modTree/:user", "mod", "mod/system", "mod/:user", "mods", "mods/system", "mods/:user")
@ModPath("modTree", "modTree/:user", "mod", "mod/system", "mod/:user", "mods", "mods/system", "mods/:user")
class ModTree : BaseMod("返回模组树") {
    override suspend fun handle(uri: String, content: HttpContent) =
        if (uri == "/mod/system" || uri == "/mods/system") {
            modManager.getModTree("system")
        } else {
            modManager.getModTree(content["user"])
        }

    override suspend fun handle(content: HttpContent) {
        content.setResponseHeader("content-type", "text/plain; charset=UTF-8")
        content.write(handle(content.uri, content))
        content.finish()
    }
}