package cn.tursom.treediagram.basemod

import cn.tursom.tools.fromJson
import cn.tursom.treediagram.TreeDiagramHttpHandler.modManager
import cn.tursom.treediagram.modinterface.AbsPath
import cn.tursom.treediagram.modinterface.BaseMod
import cn.tursom.treediagram.modinterface.ModException
import cn.tursom.treediagram.modinterface.ModPath
import cn.tursom.treediagram.modloader.ClassData
import cn.tursom.treediagram.token.token
import cn.tursom.web.HttpContent

/**
 * 模组加载模组
 * 用于加载一个模组
 *
 * 需要提供的参数有：
 * mod 要加载的模组的信息，结构为json序列化后的ClassData数据
 * system 可选，是否加入系统模组，需要admin权限
 *
 * 本模组会根据提供的信息自动寻找模组并加载
 * 模组加载的根目录为使用Upload上传的根目录
 */
@AbsPath("loadmod", "loadmod/:jarPath", "loadmod/:jarPath/:className", "loadmod/:jarPath/:className/:system")
@ModPath("loadmod", "loadmod/:jarPath", "loadmod/:jarPath/:className", "loadmod/:jarPath/:className/:system")
class ModLoader : BaseMod(
    "加载模组", "使用方法：\n" +
            "首先使用Upload模组上传到服务器目录\n" +
            "提供上传文件的相对目录（即上传文件时提供的文件名）与需要加载的class即可"
) {
    override suspend fun handle(uri: String, content: HttpContent): Any {
        if (content["jarPath"] == "help") {
            return help
        }
        val token = content.token
        val modData = content["modData"]
        val modLoader = if (modData != null) {
            cn.tursom.treediagram.modloader.ModLoader.getClassLoader(
                gson.fromJson(modData),
                if (content["system"] != "true") {
                    token.usr
                } else {
                    null
                },
                getUploadPath(token.usr!!),
                false,
                modManager
            )
        } else {
            val className = content["className"]
            val jarPath = content["jarPath"]
            jarPath ?: throw ModException("no mod get")
            cn.tursom.treediagram.modloader.ModLoader.getClassLoader(
                ClassData(
                    jarPath,
                    jarPath,
                    if (className != null) listOf(className) else null
                ),
                if (content["system"] != "true") {
                    token.usr
                } else {
                    null
                },
                getUploadPath(token.usr!!),
                false,
                modManager
            )
        }
        return modLoader.load()
    }
}