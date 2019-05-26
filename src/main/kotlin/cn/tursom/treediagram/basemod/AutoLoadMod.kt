package cn.tursom.treediagram.basemod

import cn.tursom.treediagram.TreeDiagramHttpHandler.modManager
import cn.tursom.treediagram.modinterface.BaseMod
import cn.tursom.treediagram.modinterface.ModPath
import cn.tursom.treediagram.modloader.ClassData
import cn.tursom.treediagram.token.token
import cn.tursom.web.HttpContent
import cn.tursom.xml.Vararg
import cn.tursom.xml.Xml
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Suppress("RedundantLambdaArrow")
@ModPath("AutoLoadMod", "AutoLoadMod/:type")
class AutoLoadMod : BaseMod("在系统启动时自动加载模组") {

    override suspend fun init() {
        super.init()
        GlobalScope.launch {
            @Suppress("SENSELESS_COMPARISON")
            while (modManager == null) delay(100)
            File(uploadRootPath).listFiles { it -> it.isDirectory }.forEach { path ->
                val configXml = File("$path/autoLoad.xml")
                if (!configXml.exists()) return@forEach
                val config = Xml.parse<AutoLoadConfig>(configXml)
                config.jar.forEach forEachConfig@{ jarConfig ->
                    val jarPath = "$path/${jarConfig.name ?: return@forEachConfig}"

                    logger.info("auto load mod load jar $jarPath")

                    val classes = jarConfig.classes
                    cn.tursom.treediagram.modloader.ModLoader.getClassLoader(
                        ClassData(jarPath, jarPath, classes),
                        path.name,
                        null,
                        true,
                        modManager
                    )
                }
            }
        }
    }

    override suspend fun handle(uri: String, content: HttpContent): Any? {
        if (content["type"] ?: "help" == "help") return help

        val token = content.token
        val user = token.usr!!
        val loadConfigPath = "$uploadRootPath/$user/autoLoad.xml"

        return Xml.parse<AutoLoadConfig>(loadConfigPath)
    }

    data class AutoLoadConfig(
        @Vararg var jar: ArrayList<JarConfig>
    )

    data class JarConfig(
        val name: String?,
        @Vararg val classes: ArrayList<String> = ArrayList()
    )
}
