package cn.tursom.treediagram

import cn.tursom.web.netty.AsyncNettyHttpServer
import cn.tursom.xml.ElementName
import cn.tursom.xml.Xml
import com.google.gson.Gson
import java.io.File

@ElementName("config")
data class Config(val port: Int = 12345)

val gson = Gson()

fun main() {
    val configFile = File("config.xml")
    if (!configFile.exists()) {
        configFile.createNewFile()
        configFile.outputStream().use {
            it.write(Xml.toXml(Config()).toByteArray())
        }
    }
    val config = Xml.parse<Config>(configFile)
    val port = config.port
    val server = AsyncNettyHttpServer(port, TreeDiagramHttpHandler)
    server.run()
    println("server started on port: $port")
}