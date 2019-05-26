package cn.tursom.treediagram

import cn.tursom.web.netty.AsyncNettyHttpServer
import com.google.gson.Gson

val gson = Gson()

fun main() {
    val port = TreeDiagramHttpHandler.config.port
    val server = AsyncNettyHttpServer(port, TreeDiagramHttpHandler)
    server.run()
    println("server started on port: $port")
}