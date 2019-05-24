package cn.tursom.treediagram.modinterface

import cn.tursom.treediagram.ReturnData
import cn.tursom.web.HttpContent
import com.google.gson.Gson
import java.io.File
import java.io.Serializable
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.logging.Level
import java.util.logging.Logger

@Suppress("MemberVisibilityCanBePrivate", "unused")
@NoBlocking
abstract class BaseMod(
    val description: String = "",
    val help: () -> String = { "" }
) {

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
    open fun init() {
        logger.log(Level.INFO, "mod $modName init")
    }

    /**
     * 处理模组调用请求
     * @return 一个用于表示返回数据的对象或者null
     */
    @Throws(Throwable::class)
    abstract suspend fun handle(
        uri: String,
        content: HttpContent
    ): Serializable?

    open suspend fun handle(content: HttpContent) {
        val ret = ReturnData(
            true,
            try {
                handle(content.uri, content)
            } catch (e: ModException) {
                e.message
            } catch (e: Throwable) {
                e.printStackTrace()
                if (e.message != null)
                    "${e.javaClass}: ${e.message}"
                else
                    e.javaClass.toString()
            }
        )
        content.write(gson.toJson(ret)!!)
        content.finish()
    }

    /**
     * 当模组生命周期结束时被调用
     */
    open fun destroy() {
        logger.log(Level.INFO, "mod $modName destroy")
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
        @JvmStatic
        val gson = Gson()
        @JvmStatic
        val logger = Logger.getLogger("ModLogger")!!
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
        val path = clazz.getAnnotation(ModPath::class.java)
        if (path != null) {
            path.path.forEach {
                list.add(it)
            }
        } else {
            list.add(clazz.name.split('.').last())
        }
        list.add(clazz.name)
        return list
    }