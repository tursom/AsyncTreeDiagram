package cn.tursom.treediagram.modinterface

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