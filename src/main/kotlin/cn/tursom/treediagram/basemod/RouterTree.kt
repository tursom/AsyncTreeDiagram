package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.TreeDiagramHttpHandler.getRouterTree
import cn.tursom.treediagram.modinterface.BaseMod
import cn.tursom.treediagram.modinterface.ModPath
import cn.tursom.web.HttpContent

@ModPath("routerTree", "tree")
class RouterTree : BaseMod("返回路由树") {
    override suspend fun handle(uri: String, content: HttpContent) = getRouterTree()
    override suspend fun handle(content: HttpContent) {
        content.write(getRouterTree())
        content.finish()
    }
}