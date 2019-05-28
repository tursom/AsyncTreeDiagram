package cn.tursom.treediagram.modinterface

import cn.tursom.tools.background
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout

class ModConnectionDescription(
    val service: Service,
    private val clientChannel: Channel<Any> = Channel(),
    private val serverChannel: Channel<Any> = Channel()
) : Runnable {
    override fun run() {
        background {
            try {
                withTimeout(60_000) {
                    service.getConnection(serverConnection)
                }
            } finally {
                close()
            }
        }
    }

    val clientConnection = ModConnection(this, serverChannel, clientChannel)
    private val serverConnection = ModConnection(this, clientChannel, serverChannel)
    fun close() {
        clientChannel.close()
        serverChannel.close()
    }
}