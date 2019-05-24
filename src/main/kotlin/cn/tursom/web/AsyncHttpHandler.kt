package cn.tursom.web

interface AsyncHttpHandler<T : HttpContent> {
    suspend fun handle(content: T)

    suspend fun exception(content: ExceptionContent)
}