package cn.tursom.treediagram.modinterface

import cn.tursom.treediagram.ReturnData
import cn.tursom.treediagram.TreeDiagramHttpHandler
import cn.tursom.treediagram.TreeDiagramHttpHandler.registerService
import cn.tursom.treediagram.TreeDiagramHttpHandler.removeService
import cn.tursom.web.HttpContent
import com.google.gson.Gson
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Suppress("MemberVisibilityCanBePrivate", "unused")
@NoBlocking
abstract class BaseMod(
    val description: String = "",
    val help: String = ""
) : Service {
    val user: String? = null

    /**
     * 模组私有目录
     * 在调用的时候会自动创建目录，不必担心目录不存在的问题
     * 如果有模组想储存文件请尽量使用这个目录
     */
    val modPath by lazy {
        val path = "mods/${this::class.java.name}/"
        val dir = File(path)
        if (!dir.exists()) dir.mkdirs()
        path
    }

    /**
     * 当模组被初始化时被调用
     */
    open suspend fun init(user: String?) {
        logger.log(Level.INFO, "mod $modName init by $user")
        if (javaClass.getAnnotation(RegisterService::class.java) != null) {
            registerService(user, this)
        }
    }

    /**
     * 处理模组调用请求
     * @return 一个用于表示返回数据的对象或者null
     */
    @Throws(Throwable::class)
    abstract suspend fun handle(
        uri: String,
        content: HttpContent
    ): Any?

    open suspend fun handle(content: HttpContent) {
        content.setResponseHeader("content-type", "application/json; charset=UTF-8")
        val ret = ReturnData(
            true,
            try {
                handle(content.uri, content)
            } catch (e: ModException) {
                e.message
            }
        )
        content.write(gson.toJson(ret)!!)
        content.finish()
    }

    /**
     * 当模组生命周期结束时被调用
     */
    open suspend fun destroy() {
        logger.log(Level.INFO, "mod $modName destroy")
        if (javaClass.getAnnotation(RegisterService::class.java) != null) {
            removeService(user, this)
        }
    }

    override suspend fun initService(user: String?) {
        logger.log(Level.INFO, "service $id init")
    }

    override suspend fun destroyService() {
        logger.log(Level.INFO, "service $id destroy")
    }

    override suspend fun getConnection(connection: ModConnection) {
        connection.close()
    }

    override suspend fun receiveMessage(message: Any?): Any? = null

    /**
     * 模组间通讯四大函数
     */
    suspend fun sendMessage(user: String?, serviceId: String, message: Any?, timeout: Long = 60_000): Any? {
        return TreeDiagramHttpHandler.sendMessage(user, serviceId, message, timeout)
    }

    /**
     * 模组间通讯四大函数
     */
    suspend fun connect(user: String?, serviceId: String): ModConnection? {
        return TreeDiagramHttpHandler.connect(user, serviceId)
    }

    /**
     * 方便获取ServletRequest里面的数据
     * 使得子类中可以直接使用request[ 参数名 ]的形式来获取数据
     */
    operator fun HttpContent.get(key: String): String? {
        return URLDecoder.decode(this.getHeader(key) ?: this.getParam(key) ?: return null, "utf-8")
    }

    val String.urlDecode: String
        get() = URLDecoder.decode(this, "utf-8")

    val String.urlEncode: String
        get() = URLEncoder.encode(this, "utf-8")

    override fun toString(): String {
        return if (description.isEmpty()) modName else "$modName: $description"
    }

    companion object {
        @Suppress("SpellCheckingInspection")
        @JvmStatic
        val gson = Gson()

        @JvmStatic
        val fileHandler = run {
            if (!File(TreeDiagramHttpHandler.config.logPath).exists()) {
                File(TreeDiagramHttpHandler.config.logPath).mkdirs()
            }
            FileHandler(
                "${TreeDiagramHttpHandler.config.logPath}/${TreeDiagramHttpHandler.config.logFile}%u.%g.xml",
                TreeDiagramHttpHandler.config.maxLogSize,
                TreeDiagramHttpHandler.config.logFileCount
            )
        }

        @JvmStatic
        val logger = run {
            val logger = Logger.getLogger("ModLogger")!!
            logger.addHandler(fileHandler)
            logger
        }

        @JvmStatic
        val uploadRootPath = "upload/"

        @JvmStatic
        fun getUploadPath(user: String) = "$uploadRootPath$user/"

        @JvmStatic
        val fileThreadPool: ExecutorService = Executors.newSingleThreadExecutor()

        @JvmStatic
        suspend fun readFile(file: File): ByteArray {
            return suspendCoroutine { cont ->
                fileThreadPool.execute {
                    cont.resume(file.readBytes())
                }
            }
        }
    }
}

val BaseMod.modName: String
    get() = this.javaClass.name

val BaseMod.simpName
    get() = modName.split(".").last()

val BaseMod.routeList: List<String>
    get() {
        val list = ArrayList<String>()
        val clazz = this.javaClass
        list.add(clazz.name)

        val path = clazz.getAnnotation(ModPath::class.java)
        if (path != null) {
            path.paths.forEach {
                list.add(it)
            }
        } else {
            list.add(clazz.name.split('.').last())
        }
        return list
    }

val BaseMod.absRouteList: List<String>
    get() {
        val list = ArrayList<String>()
        val clazz = this.javaClass
        val path = clazz.getAnnotation(AbsPath::class.java) ?: return list
        path.paths.forEach {
            list.add(it)
        }
        return list
    }
