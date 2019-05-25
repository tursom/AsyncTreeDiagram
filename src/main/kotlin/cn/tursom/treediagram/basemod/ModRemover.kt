package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.TreeDiagramHttpHandler.modManager
import cn.tursom.treediagram.modinterface.AbsPath
import cn.tursom.treediagram.modinterface.BaseMod
import cn.tursom.treediagram.modinterface.ModException
import cn.tursom.treediagram.modinterface.ModPath
import cn.tursom.treediagram.token.token
import cn.tursom.web.HttpContent

@AbsPath("removeMod", "removeMod/:modName", "removeMod/:system/:modName")
@ModPath("removeMod", "removeMod/:modName", "removeMod/:system/:modName")
class ModRemover : BaseMod(
    "卸载模组",
    "需要提供token\n" +
            "@param modName 模组名\n" +
            "@param system 是否为系统模组，true为是，其他为否"
) {
    override suspend fun handle(uri: String, content: HttpContent): Any? {
        val token = content.token
        val user = token.usr!!
        val modName = content["modName"]!!
        val system = content["system"] == "true"
        modManager.removeMod(
            modManager.findMod(
                modName,
                if (system) {
                    if (token.lev!!.contains("admin")) {
                        null
                    } else {
                        throw ModException("用户无该权限")
                    }
                } else {
                    user
                }
            )
                ?: throw ModException("无法找到模组：$modName")
        )
        return modName
    }
}