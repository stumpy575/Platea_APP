package com.altea.altea_app.data

import com.altea.altea_app.model.CartItem
import com.altea.altea_app.model.Event
import com.altea.altea_app.model.PurchaseEntry
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URI

class ServerClient(
    private val address: String,
    private val listener: Listener
) {

    // En definitiva es un ArrayList, pero se ve que esta forma
    // es mas natural en Kotlin.
    private val events = mutableListOf<Event>()

    // Se utiliza para trackear los eventos que aun no han cargado
    // sus tipos de tickets. (Previene errores)
    private val pendingEvents = mutableMapOf<String, Event>()

    private var closing = false

    interface Listener {
        fun onEventsLoaded(events: List<Event>)
        fun onPurchaseSuccess(entries: List<PurchaseEntry>)
        fun onPurchaseError(message: String)
        fun onConnectionError(message: String)
        fun onConnectionClosed()
    }

    private var webSocketClient: WebSocketClient? =
        object : WebSocketClient(URI(address)) {

            override fun onOpen(handshakedata: ServerHandshake?) {
                // Al conectar pedimos el catálogo de eventos
                send(GET_EVENTOS)
            }

            override fun onMessage(message: String?) {
                if (message == null) return
                handleServerMessage(message)
            }

            override fun onClose(
                code: Int,
                reason: String?,
                remote: Boolean
            ) {
                if (!closing) {
                    listener.onConnectionClosed()
                }
            }

            override fun onError(ex: Exception?) {
                listener.onConnectionError(
                    ex?.message ?: "Error de conexión"
                )
            }
        }

    // Carga los eventos recibidos del servidor, los almacena temporalmente
    // y solicita sus tipos de entrada.
    private fun loadEvents(eventsArray: JSONArray?) {
        // Operador Elvis, comprueba si es null y sale de la funcion si lo es.
        eventsArray ?: return

        // Limpia los eventos para cargarlos de nuevo
        events.clear()
        pendingEvents.clear()

        // Convierte el JSONArray en una lista de objetos Event.
        val parsedEvents = ServerMessageParser.parseEvents(eventsArray)

        // Guardamos todos los eventos pendientes.
        for (event in parsedEvents) {
            pendingEvents[event.id] = event
        }

        // Si no hay eventos, notificamos directamente.
        if (pendingEvents.isEmpty()) {
            listener.onEventsLoaded(emptyList())
            return
        }

        // Solicitamos los tickets de cada evento.
        for (event in parsedEvents) {
            getTiposEntrada(event.id)
        }
    }

    // Guarda las entradas recibidas en su evento y actualiza el precio de la cartelera
    private fun loadTickets(eventoId: String, tiposEntrada: JSONArray?) {
        // Control de nulls
        val event = pendingEvents[eventoId] ?: return

        // Transforma el array de tickets json a una lista de objetos Tickets
        val tickets = ServerMessageParser.parseTickets(
            tiposEntrada ?: JSONArray(),
            FILTER_ZONE
        )

        // Actualizamos el evento.
        event.tickets = tickets
        pendingEvents.remove(eventoId)

        if (tickets.isNotEmpty()) {
            events.add(event)
        }

        // Comprobamos si ya hemos recibido todos los tickets.
        if (pendingEvents.isEmpty()) {
            listener.onEventsLoaded(events.toList())
        }
    }

    private fun getTiposEntrada(eventoId: String) {
        val request = JSONObject()
            .put("type", "getTiposEntrada")
            .put("eventoId", eventoId)

        webSocketClient?.send(request.toString())
    }

    // Procesa los mensajes del servidor (eventos y tipos de entrada)
    private fun handleServerMessage(message: String?) {
        if (message == null) return

        val response = try {
            JSONObject(message)
        } catch (e: JSONException) {
            return
        }

        // El servidor responde con "eventos", "tiposEntrada" o el resultado de la compra
        when (response.optString("type")) {

            "eventos" -> {
                loadEvents(response.optJSONArray("eventos"))
            }

            "tiposEntrada" -> {
                loadTickets(
                    response.optString("eventoId"),
                    response.optJSONArray("tiposEntrada")
                )
            }

            "compraOK" -> {
                val entries = ServerMessageParser.parsePurchaseEntries(
                    response.getJSONArray("localizadores")
                )

                listener.onPurchaseSuccess(entries)
            }

            "compraError" -> {
                listener.onPurchaseError(
                    response.optString("message", "Error al realizar la compra")
                )
            }
        }
    }

    fun buyTickets(
        items: List<CartItem>,
        userName: String,
        userMail: String
    ) {
        if (items.isEmpty()) return

        val entradas = JSONArray()
        for (item in items) {
            entradas.put(
                JSONObject()
                    .put("eventoId", item.eventoId)
                    .put("tipoEntradaId", item.tipoEntradaId)
                    .put("cantidad", item.quantity)
            )
        }

        val request = JSONObject()
            .put("type", "comprarEntradas")
            .put("nombre", userName)
            .put("correo", userMail)
            .put("entradas", entradas)

        webSocketClient?.send(request.toString())
    }

    // Reúne las entradas seleccionadas de todos los eventos.
    fun getCartItems(): List<CartItem> {
        val items = mutableListOf<CartItem>()

        for (event in events) {
            for (ticket in event.tickets) {

                if (ticket.quantity > 0) {
                    items.add(
                        CartItem(
                            eventoId = event.id,
                            tipoEntradaId = ticket.id,
                            eventName = event.name,
                            ticketName = ticket.name,
                            price = ticket.price,
                            quantity = ticket.quantity
                        )
                    )
                }
            }
        }

        return items
    }

    fun getCartCount(): Int {
        return events.sumOf { event ->
            event.tickets.sumOf { ticket ->
                ticket.quantity
            }
        }
    }

    // Vacía todas las cantidades seleccionadas del carrito.
    fun clearCart() {
        for (event in events) {
            for (ticket in event.tickets) {
                ticket.quantity = 0
            }
        }
    }

    fun getEventName(eventoId: String): String? {
        return events.firstOrNull { it.id == eventoId }?.name
    }

    fun getTicketName(eventoId: String, tipoEntradaId: String): String? {
        return events
            .firstOrNull { it.id == eventoId }
            ?.tickets
            ?.firstOrNull { it.id == tipoEntradaId }
            ?.name
    }

    fun connect() {
        webSocketClient?.connect()
    }

    fun close() {
        closing = true
        webSocketClient?.close()
        webSocketClient = null
    }

    companion object {
        // Mensaje que pide el catálogo de eventos al servidor
        private const val GET_EVENTOS = """{"type":"getEventos"}"""
        private const val FILTER_ZONE = "Z2"
    }
}
