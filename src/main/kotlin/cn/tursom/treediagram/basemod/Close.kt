package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.ReturnData
import cn.tursom.treediagram.modinterface.AbsPath
import cn.tursom.treediagram.modinterface.BaseMod
import cn.tursom.treediagram.modinterface.ModException
import cn.tursom.treediagram.modinterface.ModPath
import cn.tursom.treediagram.token.token
import cn.tursom.web.HttpContent
import java.util.logging.Level
import kotlin.system.exitProcess

@AbsPath("close/:message", "close")
@ModPath("close/:message", "close")
class Close : BaseMod("关闭服务器，需要 admin 等级的权限") {
    override suspend fun handle(uri: String, content: HttpContent): Any? {
        val token = content.token
        if (token.lev?.contains("admin") != true) throw ModException("you are not admin")
        val message = content["message"]
        logger.log(Level.WARNING, "server closed: $message")
        content.write(gson.toJson(ReturnData(true, message)))
        content.finish()
        exitProcess(0)
    }
}