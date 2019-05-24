package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.TreeDiagramHttpHandler.modManager
import cn.tursom.treediagram.modinterface.BaseMod
import cn.tursom.treediagram.modinterface.ModPath
import cn.tursom.web.HttpContent
import java.io.Serializable

@ModPath("loadedMod", "loadedMod/:user")
class LoadedMod : BaseMod("返回已经加载的模组") {

    override suspend fun handle(uri: String, content: HttpContent): Serializable? {
        return LoadedModData(
            modManager.systemMod,
            modManager.getUserMod(content["user"])
        )
    }

    data class LoadedModData(val systemMod: Set<String>, val userMod: Set<String>?) : Serializable
}