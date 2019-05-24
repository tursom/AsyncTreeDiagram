package cn.tursom

import cn.tursom.tools.sendGet
import cn.tursom.treediagram.TreeDiagramHttpHandler
import cn.tursom.web.netty.AsyncNettyHttpServer

fun main() {
    val port = 12345
    val server = AsyncNettyHttpServer(port, TreeDiagramHttpHandler)
    server.run()
    println("server started")
    println(sendGet("http://127.0.0.1:12345"))
    println(sendGet("http://127.0.0.1:12345/hello"))
}