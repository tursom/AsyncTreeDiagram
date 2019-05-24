package cn.tursom.web

import cn.tursom.web.router.SuspendRouter
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val router = SuspendRouter<Int>()
    router.set("/:abc/:def", 1)
    router.delRoute("/:abc/:defb")
    router.set("/:aa/:dd", 1)
    println(router)
}