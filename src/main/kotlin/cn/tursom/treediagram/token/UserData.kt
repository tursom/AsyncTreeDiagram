package cn.tursom.treediagram.token

import cn.tursom.database.annotation.*
import cn.tursom.database.async.AsyncSqlAdapter
import cn.tursom.database.clauses.clause
import cn.tursom.database.sqlStr
import cn.tursom.tools.fromJson
import cn.tursom.tools.sha256
import cn.tursom.treediagram.TreeDiagramHttpHandler
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking

@TableName("users")
data class UserData(
    @NotNull val username: String,
    @NotNull val password: String,
    @NotNull @Setter("setLevel") @FieldType("TEXT") @Getter("getLevel") val level: List<String>
) {
    fun setLevel(level: String): List<String> {
        return Gson().fromJson(level)
    }

    fun getLevel(): String {
        return Gson().toJson(level).sqlStr
    }
}

internal val userTable = runBlocking {
    TreeDiagramHttpHandler.database.createTable(UserData::class.java)
    "users"
}

internal suspend fun findUser(username: String): UserData? {
    val adapter = AsyncSqlAdapter(UserData::class.java)
    TreeDiagramHttpHandler.database.select(adapter, null, where = clause {
        !UserData::username equal !username
    }, maxCount = 1)
    return if (adapter.count() == 0) null
    else adapter[0]
}

suspend fun tryLogin(username: String, password: String): Boolean {
    //查询用户数据
    val userData = findUser(username)
    return "$username$password$username$password".sha256() == userData?.password
}