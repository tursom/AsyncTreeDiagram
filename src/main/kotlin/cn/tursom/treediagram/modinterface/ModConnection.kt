package cn.tursom.treediagram.modinterface

import kotlinx.coroutines.channels.Channel

class ModConnection(
    private val parent: ModConnectionDescription,
    private val sendChannel: Channel<Any>,
    private val recvChannel: Channel<Any>
) {
    suspend fun send(message: Any) {
        sendChannel.send(message)
    }

    suspend fun recv(): Any {
        return recvChannel.receive()
    }

    fun close() {
        parent.close()
    }
}