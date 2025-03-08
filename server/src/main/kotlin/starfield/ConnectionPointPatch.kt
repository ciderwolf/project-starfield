package io.ktor.server.plugins

import io.ktor.http.*
import kotlin.reflect.KProperty

private class AssignableWithDelegate<T : Any>(val property: () -> T) {
    private var assigned: T? = null

    operator fun getValue(thisRef: Any, property: KProperty<*>): T = assigned ?: property()

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        assigned = value
    }
}

class MutableOriginConnectionPoint internal constructor (
    delegate: RequestConnectionPoint
) : RequestConnectionPoint {

    override var version: String by AssignableWithDelegate { delegate.version }
    override var uri: String by AssignableWithDelegate { delegate.uri }
    override var method: HttpMethod by AssignableWithDelegate { delegate.method }
    override var scheme: String by AssignableWithDelegate { delegate.scheme }

    override var host: String by AssignableWithDelegate { delegate.serverHost }
    override var localHost: String by AssignableWithDelegate { delegate.localHost }
    override var serverHost: String by AssignableWithDelegate { delegate.serverHost }
    override var localAddress: String by AssignableWithDelegate { delegate.localAddress }

    override var port: Int by AssignableWithDelegate { delegate.serverPort }
    override var localPort: Int by AssignableWithDelegate { delegate.localPort }
    override var serverPort: Int by AssignableWithDelegate { delegate.serverPort }
    override var remoteHost: String by AssignableWithDelegate { delegate.remoteHost }
    override var remotePort: Int by AssignableWithDelegate { delegate.remotePort }
    override var remoteAddress: String by AssignableWithDelegate { delegate.remoteAddress }
}