package cn.tursom.treediagram.modloader

import cn.tursom.treediagram.modinterface.*
import cn.tursom.web.router.SuspendRouter
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

class ModManager(private val router: SuspendRouter<BaseMod>) {
    private val logger = Logger.getLogger("ModManager")!!
    private val systemModMap = ConcurrentHashMap<String, BaseMod>()
    private val userModMapMap: ConcurrentHashMap<String, ConcurrentHashMap<String, BaseMod>> = ConcurrentHashMap()

    @Volatile
    var lastChangeTime: Long = System.currentTimeMillis()

    val sysModMap: Map<String, BaseMod>
        get() = systemModMap
    val userModMap: Map<String, Map<String, BaseMod>>
        get() = userModMapMap

    init {
        //加载系统模组
        runBlocking {
            arrayOf(
                cn.tursom.treediagram.basemod.Echo(),
                cn.tursom.treediagram.basemod.Email(),
                cn.tursom.treediagram.basemod.GroupEmail(),
                cn.tursom.treediagram.basemod.MultipleEmail(),
                cn.tursom.treediagram.basemod.ModLoader(),
                cn.tursom.treediagram.basemod.Upload(),
                cn.tursom.treediagram.basemod.GetUploadFileList(),
                cn.tursom.treediagram.basemod.Register(),
                cn.tursom.treediagram.basemod.Login(),
                cn.tursom.treediagram.basemod.Close(),
                cn.tursom.treediagram.basemod.LoadedMod(),
                cn.tursom.treediagram.basemod.RouterTree(),
                cn.tursom.treediagram.basemod.ModInfo(),
                cn.tursom.treediagram.basemod.ModTree()
            ).forEach {
                loadMod(it)
            }
        }
    }

    val systemMod: Set<String>
        get() {
            val pathSet = HashSet<String>()
            systemModMap.forEach { (_: String, u: BaseMod) ->
                u.routeList.forEach {
                    pathSet.add(it)
                }
            }
            return pathSet
        }

    fun getUserMod(user: String?): Set<String>? {
        val pathSet = HashSet<String>()
        userModMapMap[user ?: return null]?.forEach { (_: String, u: BaseMod) ->
            u.routeList.forEach {
                pathSet.add(it)
            }
        }
        return pathSet
    }

    fun findMod(modName: String, user: String? = null): BaseMod? {
        val modMap = if (user != null) {
            userModMapMap[user]
        } else {
            systemModMap
        } ?: return null
        return modMap[modName]
    }

    /**
     * 加载模组
     * 将模组的注册信息加载进系统中
     */
    internal suspend fun loadMod(mod: BaseMod) {
        //输出日志信息
        logger.info("loading mod: $mod")

        //记得销毁被替代的模组
        removeMod(mod)

        //调用模组的初始化函数
        mod.init()

        //将模组的信息加载到系统中
        systemModMap[mod.modName] = mod
        systemModMap[mod.modName.split('.').last()] = mod

        mod.routeList.forEach {
            addRoute("/mod/system/$it", mod)
        }

        mod.absRouteList.forEach {
            addRoute("/$it", mod)
        }

        lastChangeTime = System.currentTimeMillis()

    }

    /**
     * 加载模组
     * 将模组的注册信息加载进系统中
     */
    suspend fun loadMod(user: String, mod: BaseMod): String {
        //输出日志信息
        logger.info("loading mod: ${mod::class.java.name}\nuser: $user")

        //记得销毁被替代的模组
        removeMod(user, mod)

        //调用模组的初始化函数
        mod.init()

        //将模组的信息加载到系统中
        val userModMap = (userModMapMap[user] ?: run {
            val modMap = ConcurrentHashMap<String, BaseMod>()
            userModMapMap[user] = modMap
            modMap
        })

        userModMap[mod.modName] = mod
        userModMap[mod.simpName] = mod

        mod.routeList.forEach {
            addRoute("/mod/user/$user/$it", mod)
            addRoute("/user/$user/$it", mod)
        }

        lastChangeTime = System.currentTimeMillis()

        return mod.modName
    }

    suspend fun loadMod(configData: ClassData, user: String? = null, rootPath: String? = null): Boolean {
        // 要加载的类名
        val className: Array<String> = configData.classname!!
        // 类加载器
        val myClassLoader: ClassLoader? = try {
            val file = if (rootPath == null) {
                File(configData.path!!)
            } else {
                File(rootPath + configData.path!!)
            }
            // 如果文件不存在，抛出一个文件不存在异常
            if (!file.exists()) throw FileNotFoundException()
            val url = file.toURI().toURL()
            URLClassLoader(arrayOf(url), Thread.currentThread().contextClassLoader)
        } catch (e: Exception) {
            // 从文件加载模组失败，尝试从网络加载模组
            URLClassLoader(arrayOf(URL(configData.url!!)), Thread.currentThread().contextClassLoader)
        }
        // 是否所有的模组都加载成功
        var allSuccessful = true
        className.forEach { className1 ->
            try {
                // 获取一个指定模组的对象
                val modClass = myClassLoader!!.loadClass(className1)
                val modObject = modClass.getConstructor().newInstance() as BaseMod
                // 加载模组
                if (user == null)
                    loadMod(modObject)
                else {
                    removeMod(user, modObject)
                    loadMod(user, modObject)
                }
            } catch (e: NoSuchMethodException) {
                // 如果失败，将标志位置否
                allSuccessful = false
            }
        }
        lastChangeTime = System.currentTimeMillis()
        return allSuccessful
    }

    /**
     * 卸载模组
     */
    suspend fun removeMod(user: String, mod: BaseMod) {
        logger.info("user $user try remove mod: $mod")
        val userModMap = userModMapMap[user] ?: return
        val modObject = userModMap[mod.modName] ?: return
        logger.info("user $user remove mod: $mod")
        try {
            modObject.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        userModMap.remove(modObject.modName)
        if (modObject === userModMap[modObject.modName.split('.').last()])
            userModMap.remove(modObject.modName.split('.').last())
        modObject.routeList.forEach {
            val route = "/mod/user/$user/$it"
            logger.info("try delete route $route")
            if (modObject === router.get(route).first) {
                logger.info("delete route $route")
                router.delRoute(route)
            }
        }
        lastChangeTime = System.currentTimeMillis()
    }

    /**
     * 卸载模组
     */
    suspend fun removeMod(mod: BaseMod) {
        logger.info("try remove system mod: $mod")
        val modObject = systemModMap[mod.modName] ?: return
        logger.info("remove system mod: $mod")
        try {
            modObject.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        systemModMap.remove(modObject.modName)
        if (modObject === systemModMap[modObject.modName.split('.').last()])
            systemModMap.remove(modObject.modName.split('.').last())
        modObject.routeList.forEach {
            val route = "/mod/system/$it"
            logger.info("try delete route $route")
            if (modObject === router.get(route).first) {
                logger.info("delete route $route")
                router.delRoute(route)
            }
        }
        lastChangeTime = System.currentTimeMillis()
    }

    private fun addRoute(fullRoute: String, mod: BaseMod) {
        router.set(fullRoute, mod)
    }
}