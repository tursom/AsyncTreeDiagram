package cn.tursom.treediagram.modloader

import cn.tursom.asynclock.AsyncLockHashMap
import cn.tursom.asynclock.AsyncLockMap
import cn.tursom.asynclock.WriteLockHashMap
import cn.tursom.treediagram.modinterface.*
import cn.tursom.web.router.SuspendRouter
import kotlinx.coroutines.runBlocking
import java.util.logging.Logger
import kotlin.reflect.jvm.javaField

class ModManager(private val router: SuspendRouter<BaseMod>) {
    private val logger = Logger.getLogger("ModManager")!!
    private val systemModMap = WriteLockHashMap<String, BaseMod>()
    private val userModMapMap: AsyncLockHashMap<String, AsyncLockHashMap<String, BaseMod>> = WriteLockHashMap()

    @Volatile
    var lastChangeTime: Long = System.currentTimeMillis()

    val sysModMap: AsyncLockMap<String, BaseMod>
        get() = systemModMap

    @Suppress("UNCHECKED_CAST")
    val userModMap: AsyncLockMap<String, AsyncLockMap<String, BaseMod>>
        get() = userModMapMap as AsyncLockMap<String, AsyncLockMap<String, BaseMod>>

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
                cn.tursom.treediagram.basemod.Help(),
                cn.tursom.treediagram.basemod.ModTree(),
                cn.tursom.treediagram.basemod.ModRemover(),
                cn.tursom.treediagram.basemod.Download(),
                cn.tursom.treediagram.basemod.AutoLoadMod()
            ).forEach {
                loadMod(it)
            }
        }
    }

    suspend fun getSystemMod(): Set<String> {
        val pathSet = HashSet<String>()
        systemModMap.forEach { _: String, u: BaseMod ->
            u.routeList.forEach {
                pathSet.add(it)
            }
        }
        return pathSet
    }

    suspend fun getUserMod(user: String?): Set<String>? {
        val pathSet = HashSet<String>()
        userModMapMap.get(user ?: return null)?.forEach { _: String, u: BaseMod ->
            u.routeList.forEach {
                pathSet.add(it)
            }
        }
        return pathSet

    }

    suspend fun findMod(modName: String, user: String? = null): BaseMod? {
        return if (user != null) {
            (userModMapMap.get(user) ?: return null).get(modName)
        } else {
            systemModMap.get(modName)
        }
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
        mod.init(null)

        //将模组的信息加载到系统中
        systemModMap.set(mod.modName, mod)
        systemModMap.set(mod.modName.split('.').last(), mod)


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

        modUserField.set(mod, user)

        //调用模组的初始化函数
        mod.init(user)

        //将模组的信息加载到系统中
        val userModMap = (userModMapMap.get(user) ?: run {
            val modMap = WriteLockHashMap<String, BaseMod>()
            userModMapMap.set(user, modMap)
            modMap
        })

        userModMap.set(mod.modName, mod)
        userModMap.set(mod.simpName, mod)

        mod.routeList.forEach {
            addRoute("/mod/user/$user/$it", mod)
            addRoute("/user/$user/$it", mod)
        }

        lastChangeTime = System.currentTimeMillis()

        return mod.modName
    }

    /**
     * 卸载模组
     */
    suspend fun removeMod(user: String, mod: BaseMod) {
        logger.info("user $user try remove mod: $mod")

        val userModMap = userModMapMap.get(user) ?: return
        val modObject = userModMap.get(mod.modName) ?: return
        logger.info("user $user remove mod: $mod")
        try {
            modObject.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (userModMap.contains(modObject.modName)) {
            userModMap.remove(modObject.modName)
            if (modObject === userModMap.get(modObject.modName.split('.').last()))
                userModMap.remove(modObject.modName.split('.').last())
        }
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
        val modObject = systemModMap.get(mod.modName) ?: return
        logger.info("remove system mod: $mod")
        try {
            modObject.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (systemModMap.contains(modObject.modName)) {
            systemModMap.remove(modObject.modName)
            if (modObject === systemModMap.get(modObject.modName.split('.').last()))
                systemModMap.remove(modObject.modName.split('.').last())
        }
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

    private suspend fun addRoute(fullRoute: String, mod: BaseMod) {
        router.set(fullRoute, mod)
    }

    companion object {
        @JvmStatic
        private val modUserField = BaseMod::user.javaField!!

        init {
            modUserField.isAccessible = true
        }
    }
}