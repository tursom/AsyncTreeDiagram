package cn.tursom.treediagram.modinterface

import cn.tursom.tools.background
import cn.tursom.treediagram.TreeDiagramHttpHandler

@Target(AnnotationTarget.CLASS)
annotation class ServiceId(val id: String)

@Target(AnnotationTarget.CLASS)
annotation class RegisterService

/**
 * 用于模组间通讯的接口
 * 必须手动注册
 */
interface Service {
    /**
     * 接受一个对象，处理后立即返回
     */
    suspend fun receiveMessage(message: Any?): Any?

    suspend fun getConnection(connection: ModConnection)
    suspend fun initService(user: String?)
    suspend fun destroyService()
}

val Service.id
    get() = javaClass.getAnnotation(ServiceId::class.java)?.id ?: javaClass.name!!

abstract class BaseService(val user: String?) : Service {
    init {
        if (javaClass.getAnnotation(RegisterService::class.java) != null) {
            background {
                TreeDiagramHttpHandler.registerService(user, this@BaseService)
            }
        }
    }

    override suspend fun receiveMessage(message: Any?): Any? = null

    override suspend fun getConnection(connection: ModConnection) {
        connection.close()
    }

    override suspend fun initService(user: String?) {
        logger.info("service $id init")
    }

    override suspend fun destroyService() {
        logger.info("service $id destroy")
    }

    companion object {
        @JvmStatic
        val fileHandler = TreeDiagramHttpHandler.fileHandler

        @JvmStatic
        val logger = TreeDiagramHttpHandler.logger
    }
}