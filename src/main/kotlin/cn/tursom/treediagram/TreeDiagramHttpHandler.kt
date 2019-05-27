package cn.tursom.treediagram

import cn.tursom.database.async.sqlite.AsyncSqliteHelper
import cn.tursom.tools.background
import cn.tursom.treediagram.modinterface.BaseMod
import cn.tursom.treediagram.modloader.ModManager
import cn.tursom.web.ExceptionContent
import cn.tursom.web.HttpHandler
import cn.tursom.web.netty.NettyHttpContent
import cn.tursom.web.router.SuspendRouter
import cn.tursom.xml.Constructor
import cn.tursom.xml.ElementName
import cn.tursom.xml.Xml
import java.io.File
import java.io.PrintStream
import java.util.logging.FileHandler

object TreeDiagramHttpHandler : HttpHandler<NettyHttpContent> {
    private val router = SuspendRouter<BaseMod>()
    val database = AsyncSqliteHelper("TreeDiagram.db")
    val config = run {
        val configFile = File("config.xml")
        if (!configFile.exists()) {
            configFile.createNewFile()
            configFile.outputStream().use {
                it.write(Xml.toXml(Config()).toByteArray())
            }
        }
        Xml.parse<Config>(configFile)
    }
    val fileHandler = run {
        if (!File(config.logPath).exists()) {
            File(config.logPath).mkdirs()
        }
        FileHandler("${config.logPath}/${config.logFile}%u.%g.xml", config.maxLogSize, config.logFileCount)
    }
    val modManager = ModManager(router)

    suspend fun getRouterTree() = router.suspendToString()

    suspend fun getRoute(route: String) = router.get(route)

    override fun handle(content: NettyHttpContent) = background {
        val (mod, path) = router.get(content.uri)
        if (mod == null) {
            content.responseCode = 404
            content.finish()
        } else {
            path.forEach { content.addParam(it.first, it.second) }

            try {
                mod.handle(content)
            } catch (e: Throwable) {
                e.printStackTrace()
                content.responseCode = 500
                content.reset()
                e.printStackTrace(PrintStream(content.responseBody))
                content.finish()
            }
        }
    }

    override fun exception(e: ExceptionContent) {
        e.cause.printStackTrace()
    }

    @Suppress("unused")
    @ElementName("config")
    data class Config(
        @Constructor("setPort") val port: Int = 12345,
        @Constructor("setLogPath") val logPath: String = "log",
        @Constructor("setLogFile") val logFile: String = "modLog",
        @Constructor("setMaxLogSize") val maxLogSize: Int = 64 * 1024,
        @Constructor("setLogFileCount") val logFileCount: Int = 24
    ) {
        fun setPort(port: String) = port.toIntOrNull() ?: 12345
        fun setLogPath(logPath: String?) = logPath ?: "log"
        fun setLogFile(modLogFile: String?) = modLogFile ?: "modLog"
        fun setMaxLogSize(maxLogSize: String?) = maxLogSize?.toIntOrNull() ?: 64 * 1024
        fun setLogFileCount(logFileCount: String?) = logFileCount?.toIntOrNull() ?: 24
    }
}