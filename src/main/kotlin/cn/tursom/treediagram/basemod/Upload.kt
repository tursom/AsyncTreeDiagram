package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.TreeDiagramHttpHandler.registerService
import cn.tursom.treediagram.modinterface.*
import cn.tursom.treediagram.token.token
import cn.tursom.web.HttpContent
import java.io.File

/**
 * 文件上传模组
 * 需要提供两个参数：
 * filename要上传的文件名称
 * file或者file64
 * file与file64的去别在于file是文本文件的原文件内容，file64是base64编码后的文件内容
 * 返回的是上传到服务器的目录
 */
@AbsPath("upload/:type/:filename", "upload/:filename", "upload")
@ModPath("upload/:type/:filename", "upload/:filename", "upload")
@NeedBody(10 * 1024 * 1024)
@RegisterService
class Upload : BaseMod("上传文件") {

    override suspend fun receiveMessage(message: Any?): Any? {
        message as String?
        return if (message == null) {
            "$uploadRootPath/system"
        } else {
            "$uploadRootPath/$message"
        }
    }

    override suspend fun handle(uri: String, content: HttpContent): Any {
        val token = content.token

        //确保上传用目录可用
        val uploadPath = getUploadPath(token.usr!!)
        if (!File(uploadPath).exists()) {
            File(uploadPath).mkdirs()
        }

        val filename = content["filename"] ?: throw ModException("filename not found")

        val file = File("$uploadPath$filename")

        val stream = when (val uploadType = content.getParam("type")
            ?: content.getHeader("type")
            ?: "append") {
            "create" ->
                if (file.exists()) throw ModException("file exist")
                else file.outputStream()
            "append" -> file.outputStream()
            "delete" -> {
                file.delete()
                return "file \"$filename\" deleted"
            }
            "exist" -> {
                return file.exists()
            }
            else -> throw ModException(
                "unsupported upload type \"$uploadType\", please use one of [\"create\", \"append\"(default), " +
                        "\"delete\", \"exist\"] as an upload type"
            )
        }

        // 写入文件
        fileThreadPool.execute {
            stream.use {
                it.write(content.body, content.bodyOffSet, content.readableBytes)
                it.flush()
            }
        }

        content.setResponseHeader("filename", filename)
        //返回上传的文件名
        return filename
    }
}