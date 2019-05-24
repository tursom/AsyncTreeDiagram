package cn.tursom.treediagram

import cn.tursom.database.async.sqlite.AsyncSqliteHelper
import cn.tursom.treediagram.modinterface.BaseMod
import cn.tursom.treediagram.modloader.ModManager
import cn.tursom.web.AsyncHttpHandler
import cn.tursom.web.ExceptionContent
import cn.tursom.web.netty.NettyHttpContent
import cn.tursom.web.router.SuspendRouter

object TreeDiagramHttpHandler : AsyncHttpHandler<NettyHttpContent> {
    private val router = SuspendRouter<BaseMod>()
    val database = AsyncSqliteHelper("TreeDiagram.db")
    val modManager = ModManager(router)

    suspend fun getRouterTree() = router.suspendToString()

    override suspend fun handle(content: NettyHttpContent) {
        if (content.uri == "/") {
            content.write(router.suspendToString())
            content.finish()
            return
        }

        val (mod, path) = router.get(content.uri)
        if (mod == null) {
            content.responseCode = 404
            content.finish()
        } else {
            path.forEach { content.addParam(it.first, it.second) }

            try {
                mod.handle(content)
            } catch (e: Exception) {
                e.printStackTrace()
                content.responseCode = 500
                content.responseBody.reset()
                content.write("${e.javaClass}: ${e.message}".toByteArray())
                content.finish()
            }
        }
    }

    override suspend fun exception(content: ExceptionContent) {
        content.cause.printStackTrace()
    }
}