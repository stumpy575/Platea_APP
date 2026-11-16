package com.altea.altea_app.data

import com.altea.altea_app.model.CartItem
import com.altea.altea_app.model.Event
import com.altea.altea_app.model.OwnedTicket
import com.altea.altea_app.model.PurchaseEntry
import com.altea.altea_app.model.Seat
import com.altea.altea_app.model.Venue
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URI

class ServerClient(
    private val address: String,
    private val userMail: String,
    private val listener: Listener,
    private val autoLoadCatalog: Boolean = true
) {

    // En definitiva es un ArrayList, pero se ve que esta forma
    // es mas natural en Kotlin.
    private val events = mutableListOf<Event>()

    // Se utiliza para trackear los eventos que aun no han cargado
    // sus tipos de tickets. (Previene errores)
    private val pendingEvents = mutableMapOf<String, Event>()

    // Venues recibidos del servidor, indexados por su identificador
    private val venues = mutableMapOf<String, Venue>()

    // Indica si ya han llegado los venues y si los eventos están listos.
    // Los eventos se notifican cuando ambos datos están disponibles.
    private var venuesLoaded = false
    private var eventsReady = false

    private var closing = false

    // Petición de asientos que se enviará en cuanto se abra la conexión
    private var pendingSeatRequest: Pair<String, String>? = null

    interface Listener {
        fun onEventsLoaded(events: List<Event>)
        fun onOwnedTicketsLoaded(tickets: List<OwnedTicket>)
        fun onPurchaseSuccess(entries: List<PurchaseEntry>)
        fun onPurchaseError(message: String)
        fun onConnectionError(message: String)
        fun onConnectionClosed()
        fun onSeatsLoaded(seats: List<Seat>, rows: Int, seatsPerRow: Int) {}
    }

    private var webSocketClient: WebSocketClient? =
        object : WebSocketClient(URI(address)) {

            override fun onOpen(handshakedata: ServerHandshake?) {
                // Al conectar pedimos el catálogo, salvo que solo se quieran asientos
                if (autoLoadCatalog) {
                    send(GET_EVENTOS)
                    requestVenues()
                    requestOwnedTickets()
                }

                pendingSeatRequest?.let { (eventoId, tipoEntradaId) ->
                    sendSeatRequest(eventoId, tipoEntradaId)
                }
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
        eventsReady = false

        // Convierte el JSONArray en una lista de objetos Event.
        val parsedEvents = ServerMessageParser.parseEvents(eventsArray)

        // Guardamos todos los eventos pendientes con su venue.
        for (event in parsedEvents) {
            event.venue = venues[event.venueId]
            pendingEvents[event.id] = event
        }

        // Si no hay eventos, notificamos directamente.
        if (pendingEvents.isEmpty()) {
            eventsReady = true
            notifyEventsIfReady()
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
            tiposEntrada ?: JSONArray()
        )

        // Actualizamos el evento.
        event.tickets = tickets
        event.venue = venues[event.venueId]
        pendingEvents.remove(eventoId)

        if (tickets.isNotEmpty()) {
            events.add(event)
        }

        // Comprobamos si ya hemos recibido todos los tickets.
        if (pendingEvents.isEmpty()) {
            eventsReady = true
            notifyEventsIfReady()
        }
    }

    // Guarda los venues recibidos y completa los eventos
    private fun loadVenues(venuesArray: JSONArray?) {
        val parsedVenues = ServerMessageParser.parseVenues(
            venuesArray ?: JSONArray()
        )

        venues.clear()
        for (venue in parsedVenues) {
            venues[venue.id] = venue
        }

        venuesLoaded = true

        for (event in events) {
            event.venue = venues[event.venueId]
        }
        for (event in pendingEvents.values) {
            event.venue = venues[event.venueId]
        }

        notifyEventsIfReady()
    }

    // Notifica los eventos cuando ya hay tickets y venues cargados
    private fun notifyEventsIfReady() {
        if (venuesLoaded && eventsReady) {
            listener.onEventsLoaded(events.toList())
        }
    }

    private fun requestVenues() {
        val request = JSONObject()
            .put("type", "getRecintos")

        webSocketClient?.send(request.toString())
    }

    private fun getTiposEntrada(eventoId: String) {
        val request = JSONObject()
            .put("type", "getTiposEntrada")
            .put("eventoId", eventoId)

        webSocketClient?.send(request.toString())
    }

    fun requestSeats(eventoId: String, tipoEntradaId: String) {
        if (webSocketClient?.isOpen == true) {
            sendSeatRequest(eventoId, tipoEntradaId)
        } else {
            pendingSeatRequest = eventoId to tipoEntradaId
        }
    }

    private fun sendSeatRequest(eventoId: String, tipoEntradaId: String) {
        pendingSeatRequest = null

        val request = JSONObject()
            .put("type", "getAsientos")
            .put("eventoId", eventoId)
            .put("tipoEntradaId", tipoEntradaId)

        webSocketClient?.send(request.toString())
    }

    fun requestOwnedTickets() {
        if (userMail.isBlank()) return

        val request = JSONObject()
            .put("type", "getMisEntradas")
            .put("correo", userMail)

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

            "recintos" -> {
                loadVenues(response.optJSONArray("recintos"))
            }

            "tiposEntrada" -> {
                loadTickets(
                    response.optString("eventoId"),
                    response.optJSONArray("tiposEntrada")
                )
            }

            "misEntradas" -> {
                val tickets = ServerMessageParser.parseOwnedTickets(
                    response.optJSONArray("entradas") ?: JSONArray()
                )

                listener.onOwnedTicketsLoaded(tickets)
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

            "asientos" -> {
                val seats = ServerMessageParser.parseSeats(
                    response.optJSONArray("asientos") ?: JSONArray()
                )

                listener.onSeatsLoaded(
                    seats,
                    response.optInt("filas", 0),
                    response.optInt("asientosPorFila", 0)
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
    }
}
