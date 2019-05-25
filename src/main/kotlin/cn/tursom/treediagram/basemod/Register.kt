package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.modinterface.AbsPath
import cn.tursom.treediagram.modinterface.BaseMod
import cn.tursom.treediagram.modinterface.ModPath
import cn.tursom.treediagram.token.register
import cn.tursom.web.HttpContent

@AbsPath("register", "register/:username")
@ModPath("register", "register/:username")
class Register : BaseMod("注册用户") {
    override suspend fun handle(
        uri: String,
        content: HttpContent
    ) = try {
        register(content)
    } catch (cause: Throwable) {
        cause.printStackTrace()
        null
    }

    override suspend fun handle(content: HttpContent) {
        content.write(register(content))
        content.finish()
    }
}