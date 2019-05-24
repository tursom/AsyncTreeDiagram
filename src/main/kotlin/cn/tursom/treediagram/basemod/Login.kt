package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.modinterface.BaseMod
import cn.tursom.treediagram.modinterface.ModPath
import cn.tursom.treediagram.token.login
import cn.tursom.web.HttpContent
import java.io.Serializable

@ModPath("login", "login/:name")
class Login : BaseMod("登录") {
    override suspend fun handle(
        uri: String,
        content: HttpContent
    ): Serializable? {
        val username = content["name"]
        val password = content["password"]
        return login(username, password)
    }

    override suspend fun handle(content: HttpContent) {
        val username = content["name"]
        val password = content["password"]
        content.write(login(username, password))
        content.finish()
    }
}