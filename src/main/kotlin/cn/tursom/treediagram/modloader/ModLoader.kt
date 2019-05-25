package cn.tursom.treediagram.modloader

import cn.tursom.treediagram.modinterface.BaseMod
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.lang.reflect.InvocationTargetException
import java.net.URL
import java.net.URLClassLoader

/**
 * 用于加载模组
 * 模组可由网络或者本地加载
 * 亦可将配置写入一个文件中
 * 会优先尝试从本地加载模组
 * 本地文件不存在则会从网络加载模组
 *
 * @param configData 配置管理器
 * @param loadInstantly 是否立即加载，默认为真
 */
class ModLoader(
    configData: ClassData,
    private val user: String? = null,
    rootPath: String? = null,
    loadInstantly: Boolean = true,
    val modManager: ModManager
) {
    //要加载的类名
    private val className: Array<String> = configData.classname!!
    //类加载器
    private val myClassLoader: ClassLoader? = try {
        val file = if (rootPath == null) {
            File(configData.path!!)
        } else {
            File(rootPath + configData.path!!)
        }
        //如果文件不存在，抛出一个文件不存在异常
        if (!file.exists()) throw FileNotFoundException()
        val url = file.toURI().toURL()
        URLClassLoader(arrayOf(url), Thread.currentThread().contextClassLoader)
    } catch (e: Exception) {
        //从文件加载模组失败，尝试从网络加载模组
        URLClassLoader(arrayOf(URL(configData.url!!)), Thread.currentThread().contextClassLoader)
    }

    init {
        //如果需要立即加载模组
        //则会在对象构造时自动加载模组
        if (loadInstantly) {
            //自动加载模组
            GlobalScope.launch { load() }
        }
    }

    /**
     * 辅助构造函数
     * config是一个ClassData类的json格式对象
     */
    constructor(
        config: String,
        user: String? = null,
        rootPath: String? = null,
        loadInstantly: Boolean = true,
        modManager: ModManager
    ) : this(
        ConfigManager(config).getData(ClassData::class.java)!!,
        user,
        rootPath,
        loadInstantly,
        modManager
    )

    /**
     * 手动加载模组
     * @return 是否所有的模组都加载成功
     */
    suspend fun load() {
        className.forEach { className ->
            try {
                //获取一个指定模组的对象
                val modClass = myClassLoader!!.loadClass(className)
                val modObject = modClass.newInstance() as BaseMod
                //加载模组
                if (user == null)
                    modManager.loadMod(modObject)
                else {
                    modManager.loadMod(user, modObject)
                }
            } catch (e: InvocationTargetException) {
                throw e.targetException
            }
        }
    }
}

