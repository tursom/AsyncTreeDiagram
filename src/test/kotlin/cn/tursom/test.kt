package cn.tursom

import cn.tursom.socket.utils.sendGet
import cn.tursom.treediagram.TreeDiagramHttpHandler
import cn.tursom.web.netty.NettyHttpServer

fun main() {
    val port = 12345
    val server = NettyHttpServer(port, TreeDiagramHttpHandler)
    server.run()
    println("server started")
    println(sendGet("http://127.0.0.1:12345"))
    println(sendGet("http://127.0.0.1:12345/hello"))
}