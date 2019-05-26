package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.modinterface.AbsPath
import cn.tursom.treediagram.modinterface.BaseMod
import cn.tursom.treediagram.modinterface.ModPath
import cn.tursom.treediagram.token.token
import cn.tursom.web.HttpContent
import java.io.File


@AbsPath("download", "download/:fileName", "download/*")
@ModPath("download", "download/:fileName", "download/*")
class Download : BaseMod("下载文件") {
    override suspend fun handle(uri: String, content: HttpContent): ByteArray? {
        val token = content.token
        val uploadPath = getUploadPath(token.usr!!)
        val file = File("$uploadPath${content["fileName"] ?: return null}")
        if (!file.exists()) return null
        return readFile(file)
    }

    override suspend fun handle(content: HttpContent) {
        val fileData = handle(content.uri, content)
        if (fileData == null) {
            content.responseCode = 404
            content.finish()
        } else {
//            content.setResponseHeader("content-type", "charset=UTF-8")
            content.write(fileData)
            content.finish()
        }
    }
}