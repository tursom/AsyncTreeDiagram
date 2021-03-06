package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.modinterface.AbsPath
import cn.tursom.treediagram.modinterface.BaseMod
import cn.tursom.treediagram.modinterface.ModPath
import cn.tursom.treediagram.modinterface.NoBlocking
import cn.tursom.treediagram.token.token
import cn.tursom.web.HttpContent
import java.io.File

/**
 * 获取上传的文件的列表
 */
@NoBlocking
@AbsPath("UploadFileList", "fileList")
@ModPath("UploadFileList", "fileList")
class GetUploadFileList : BaseMod("获取上传的文件的列表") {
    override suspend fun handle(
        uri: String,
        content: HttpContent
    ): List<String> {
        val token = content.token
        val uploadPath = "${uploadRootPath}${token.usr}/"
        val uploadDir = File(uploadPath)
        val fileList = ArrayList<String>()
        uploadDir.listFiles()?.forEach {
            fileList.add(it.path.split('/').last())
        }
        return fileList
    }
}
