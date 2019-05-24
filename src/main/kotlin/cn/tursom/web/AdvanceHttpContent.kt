package cn.tursom.web

interface AdvanceHttpContent : HttpContent {
    fun addParam(key: String, value: String)
}

fun AdvanceHttpContent.addParam(param: Pair<String, String>) = addParam(param.first, param.second)