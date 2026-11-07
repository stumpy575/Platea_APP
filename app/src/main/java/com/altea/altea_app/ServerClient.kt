package com.altea.altea_app

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI

class ServerClient(
    private val address: String,
    private val listener: Listener
) {
    interface Listener {
        fun onEventReady(event: Event)
        fun onPurchaseSuccess(entries: List<PurchaseEntry>)
        fun onPurchaseError(message: String)
        fun onConnectionError(message: String)
        fun onConnectionClosed()
        fun onMessageReceived(message: String)
    }

    private var webSocketClient: WebSocketClient? =
        object : WebSocketClient(URI(address)) {

            override fun onOpen(handshakedata: ServerHandshake?) {
                // Al conectar pedimos el catálogo de eventos
                send(GET_EVENTOS)
            }

            override fun onMessage(message: String?) {
                if (message != null) {
                    listener.onMessageReceived(message)
                }
            }

            override fun onClose(
                code: Int,
                reason: String?,
                remote: Boolean
            ) {
                listener.onConnectionClosed()
            }

            override fun onError(ex: Exception?) {
                listener.onConnectionError(
                    ex?.message ?: "Error de conexión"
                )
            }
        }

    fun getTiposEntrada(eventoId: String) {
        val request = JSONObject()
            .put("type", "getTiposEntrada")
            .put("eventoId", eventoId)

        webSocketClient?.send(request.toString())
    }

    fun buyTickets(request: JSONObject) {
        webSocketClient?.send(request.toString())
    }

    fun connect() {
        webSocketClient?.connect()
    }

    fun close() {
        webSocketClient?.close()
        webSocketClient = null
    }

    companion object {
        // Mensaje que pide el catálogo de eventos al servidor
        private const val GET_EVENTOS = """{"type":"getEventos"}"""
    }
}