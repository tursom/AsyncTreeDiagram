package cn.tursom.treediagram.basemod

import cn.tursom.tools.background
import cn.tursom.treediagram.TreeDiagramHttpHandler.modManager
import cn.tursom.treediagram.modinterface.BaseMod
import cn.tursom.treediagram.modinterface.ModException
import cn.tursom.treediagram.modinterface.ModPath
import cn.tursom.treediagram.modloader.ClassData
import cn.tursom.treediagram.token.token
import cn.tursom.web.HttpContent
import cn.tursom.xml.Constructor
import cn.tursom.xml.Xml
import kotlinx.coroutines.delay
import org.dom4j.Element
import java.io.File

@Suppress("RedundantLambdaArrow")
@ModPath("AutoLoadMod", "AutoLoadMod/:type", "AutoLoadMod/:type/:jar", "AutoLoadMod/:type/:jar/:className")
class AutoLoadMod : BaseMod("在系统启动时自动加载模组") {

    override suspend fun init(user: String?) {
        super.init(user)
        background {
            delay(100)
            @Suppress("SENSELESS_COMPARISON")
            while (modManager == null) delay(20)
            File(uploadRootPath).listFiles { it -> it.isDirectory }?.forEach { path ->
                logger.info("自动加载模组正在加载路径：$path")
                val configXml = File("$path/autoLoad.xml")
                if (!configXml.exists()) return@forEach
                val config = Xml.parse<AutoLoadConfig>(configXml)
                config.jar.forEach forEachConfig@{ (jarName, classes) ->
                    val jarPath = "$path/$jarName"
                    logger.info("自动加载模组正在加载路径jar包：$jarPath")
                    cn.tursom.treediagram.modloader.ModLoader.getClassLoader(
                        ClassData(jarPath, jarPath, if (classes.isNotEmpty()) classes.toList() else null),
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
        val type = content["type"] ?: "help"
        if (type == "help") return help

        val token = content.token
        val user = token.usr!!
        val loadConfigPath = "$uploadRootPath$user/autoLoad.xml"
        val config = Xml.parse<AutoLoadConfig>(File(loadConfigPath))

        if (
            when (type) {
                "addJar" -> {
                    val jarName = content["jar"] ?: throw ModException("无法找到jar文件名")
                    config.jar[jarName] = HashSet()
                    true
                }
                "addClass" -> {
                    val jarName = content["jar"] ?: throw ModException("无法找到jar文件名")
                    val jar = config.jar[jarName] ?: run {
                        val newSet = HashSet<String>()
                        config.jar[jarName] = newSet
                        newSet
                    }
                    jar.add(content["className"] ?: throw ModException("无法找到类名"))
                    true
                }
                "get" -> {
                    false
                }
                else -> {
                    throw ModException("无法找到对应类型")
                }
            }
        ) {
            fileThreadPool.execute {
                File("$uploadRootPath$user/autoLoad.xml").delete()
                File("$uploadRootPath$user/autoLoad.xml").outputStream().use {
                    it.write(Xml.toXml(config, "config").toByteArray())
                }
            }
        }

        return config
    }

    @Suppress("unused")
    data class AutoLoadConfig(
        @Constructor("setJar") var jar: HashMap<String, HashSet<String>>
    ) {
        fun setJar(element: Element): HashMap<String, HashSet<String>> {
            val map = HashMap<String, HashSet<String>>()
            element.elements("jar").forEach {
                it as Element
                val jarPath = (it.element("name") ?: return@forEach).text ?: return@forEach
                val list = HashSet<String>()
                it.elements("class")?.forEach { clazz ->
                    clazz as Element
                    list.add(clazz.text)
                }
                map[jarPath] = list
            }
            return map
        }
    }
}
