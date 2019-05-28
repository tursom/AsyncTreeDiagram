package cn.tursom.treediagram

import cn.tursom.asynclock.AsyncRWLockAbstractMap
import cn.tursom.asynclock.ReadWriteLockHashMap
import cn.tursom.database.async.sqlite.AsyncSqliteHelper
import cn.tursom.tools.background
import cn.tursom.treediagram.modinterface.*
import cn.tursom.treediagram.modloader.ModManager
import cn.tursom.web.ExceptionContent
import cn.tursom.web.HttpHandler
import cn.tursom.web.netty.NettyHttpContent
import cn.tursom.web.router.SuspendRouter
import cn.tursom.xml.Constructor
import cn.tursom.xml.ElementName
import cn.tursom.xml.Xml
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.PrintStream
import java.util.logging.FileHandler
import java.util.logging.Logger

object TreeDiagramHttpHandler : HttpHandler<NettyHttpContent> {
    private val serviceMap = ReadWriteLockHashMap<String?, AsyncRWLockAbstractMap<String, Service>>()
    private val router = SuspendRouter<BaseMod>()
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


    @JvmStatic
    val fileHandler = run {
        if (!File(TreeDiagramHttpHandler.config.logPath).exists()) {
            File(TreeDiagramHttpHandler.config.logPath).mkdirs()
        }
        FileHandler(
            "${config.logPath}/${config.logFile}%u.%g.xml",
            config.maxLogSize,
            config.logFileCount
        )
    }

    @JvmStatic
    val logger = run {
        val logger = Logger.getLogger("ModLogger")!!
        logger.addHandler(fileHandler)
        logger
    }
    val database = AsyncSqliteHelper(config.database)
    val modManager = ModManager(router)

    /**
     * 模组间通讯四大函数
     */
    suspend fun registerService(user: String?, service: Service) {
        val map = serviceMap.get(user) ?: suspend {
            val newMap = ReadWriteLockHashMap<String, Service>()
            serviceMap.set(user, newMap)
            newMap
        }()
        val oldService = map.get(service.id)
        oldService?.destroyService()
        service.initService(user)
        map.set(service.id, service)
    }

    /**
     * 模组间通讯四大函数
     */
    suspend fun removeService(user: String?, service: Service) {
        val map = serviceMap.get(user) ?: return
        map.remove(service.id)
    }

    /**
     * 模组间通讯四大函数
     */
    suspend fun sendMessage(user: String?, serviceId: String, message: Any?, timeout: Long = 60_000): Any? {
        val map = serviceMap.get(user)!!
        val service = map.get(serviceId)!!
        return withTimeout(timeout) {
            service.receiveMessage(message)
        }
    }

    /**
     * 模组间通讯四大函数
     */
    suspend fun connect(user: String?, serviceId: String): ModConnection? {
        val map = serviceMap.get(user) ?: return null
        val service = map.get(serviceId) ?: return null
        val newConnection = ModConnectionDescription(service)
        newConnection.run()
        return newConnection.clientConnection
    }

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
        @Constructor("setLogFileCount") val logFileCount: Int = 24,
        @Constructor("setDatabase") val database: String = "TreeDiagram.db"
    ) {
        fun setPort(port: String) = port.toIntOrNull() ?: 12345
        fun setLogPath(logPath: String?) = logPath ?: "log"
        fun setLogFile(modLogFile: String?) = modLogFile ?: "modLog"
        fun setMaxLogSize(maxLogSize: String?) = maxLogSize?.toIntOrNull() ?: 64 * 1024
        fun setLogFileCount(logFileCount: String?) = logFileCount?.toIntOrNull() ?: 24
        fun setDatabase(database: String?) = database ?: "TreeDiagram.db"
    }
}