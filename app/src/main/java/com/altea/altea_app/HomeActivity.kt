package com.altea.altea_app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URI

// Pantalla principal: cartelera de eventos, conexión WebSocket y carrito
class HomeActivity : AppCompatActivity() {

    // Lista de eventos que se muestran en la cartelera
    private val events = ArrayList<Event>()

    // Datos del usuario autenticado, disponibles durante toda la sesión (se usarán al finalizar la compra)
    var userName: String = ""
    var userMail: String = ""

    private lateinit var eventAdapter: EventAdapter
    private lateinit var cartBadge: TextView
    private var webSocketClient: WebSocketClient? = null

    // Infla la cartelera, prepara la barra inferior y conecta con el servidor
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cartBadge = findViewById(R.id.cart_badge)

        // Recupera los datos del usuario introducidos en el inicio de sesión
        userName = intent.getStringExtra(EXTRA_USER_NAME).orEmpty()
        userMail = intent.getStringExtra(EXTRA_USER_MAIL).orEmpty()

        // La pestaña del carrito abre el resumen de entradas seleccionadas
        findViewById<View>(R.id.cart_nav_button)!!.setOnClickListener {
            showCart()
        }

        eventAdapter = EventAdapter(events) { event ->
            TicketBottomSheetDialog(this, event) {
                updateCartBadge()
            }.show()
        }
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.adapter = eventAdapter

        connectToServer()
    }

    // Cierra la conexión WebSocket al salir de la pantalla
    override fun onDestroy() {
        webSocketClient?.close()
        webSocketClient = null
        super.onDestroy()
    }

    // Abre el WebSocket con la dirección usada en el inicio de sesión
    private fun connectToServer() {
        // La dirección es la misma que se usó en el inicio de sesión
        val address = intent.getStringExtra(EXTRA_SERVER_ADDRESS) ?: return

        webSocketClient = object : WebSocketClient(URI(address)) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                // Al conectar pedimos el catálogo de eventos
                send(GET_EVENTOS)
            }

            override fun onMessage(message: String?) {
                runOnUiThread { handleServerMessage(message) }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) = Unit

            override fun onError(ex: Exception?) = Unit
        }
        webSocketClient?.connect()
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
            "eventos" -> loadEvents(response.optJSONArray("eventos"))
            "tiposEntrada" -> loadTickets(
                response.optString("eventoId"),
                response.optJSONArray("tiposEntrada")
            )
            "compraOK" -> showPurchaseSuccess(response.optJSONArray("localizadores"))
            "compraError" -> showDialog(
                getString(R.string.purchase_error_title),
                response.optString("message", getString(R.string.purchase_error_message))
            )
        }
    }

    // Convierte la respuesta de eventos y pide las entradas de cada uno
    private fun loadEvents(eventos: JSONArray?) {
        eventos ?: return

        events.clear()

        for (i in 0 until eventos.length()) {
            val evento = eventos.optJSONObject(i) ?: continue

            val event = Event(
                id = evento.optString("id"),
                name = evento.optString("nom"),
                date = evento.optString("dataHora"),
                price = 0.0,
                img = evento.optString("imatge"),
                description = evento.optString("descripcio"),
                tickets = emptyList()
            )
            events.add(event)

            // Cada evento tiene sus propios tipos de entrada
            val request = JSONObject()
                .put("type", "getTiposEntrada")
                .put("eventoId", event.id)
            webSocketClient?.send(request.toString())
        }

        eventAdapter.notifyDataSetChanged()
    }

    // Guarda las entradas recibidas en su evento y actualiza el precio de la cartelera
    private fun loadTickets(eventoId: String, tiposEntrada: JSONArray?) {
        tiposEntrada ?: return
        val event = events.firstOrNull { it.id == eventoId } ?: return

        val tickets = ArrayList<Ticket>()
        for (i in 0 until tiposEntrada.length()) {
            val tipoEntrada = tiposEntrada.optJSONObject(i) ?: continue

            // FILTRO TEMPORAL DE DESARROLLO: se descartan las entradas que no son de la zona Z2
            if (tipoEntrada.optString("zona") != FILTER_ZONE) continue

            tickets.add(
                Ticket(
                    id = tipoEntrada.optString("id"),
                    name = tipoEntrada.optString("nom"),
                    price = tipoEntrada.optDouble("preu", 0.0)
                )
            )
        }

        if (tickets.isEmpty()) {
            // Con el filtro temporal, los eventos sin entradas de esa zona no se muestran
            events.remove(event)
        } else {
            event.tickets = tickets
            // En la cartelera se muestra la entrada más barata del evento
            event.price = tickets.minOfOrNull { it.price } ?: 0.0
        }

        eventAdapter.notifyDataSetChanged()
    }

    // Suma las entradas de todos los eventos y actualiza el contador del carrito
    private fun updateCartBadge() {
        val count = events.sumOf { event -> event.tickets.sumOf { it.quantity } }
        cartBadge.text = count.toString()
        cartBadge.isVisible = count > 0
    }

    // Reúne las entradas seleccionadas de todos los eventos
    private fun cartItems(): List<CartItem> {
        val items = ArrayList<CartItem>()
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

    // Abre el carrito con las entradas seleccionadas
    private fun showCart() {
        CartBottomSheetDialog(this, cartItems()) {
            buyTickets()
        }.show()
    }

    // Envía al servidor la compra de las entradas del carrito
    private fun buyTickets() {
        val socket = webSocketClient ?: return
        val items = cartItems()
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

        socket.send(request.toString())
    }

    // Muestra los localizadores generados y vacía el carrito al completar la compra
    private fun showPurchaseSuccess(localizadores: JSONArray?) {
        val references = ArrayList<String>()
        if (localizadores != null) {
            for (i in 0 until localizadores.length()) {
                val item = localizadores.optJSONObject(i) ?: continue

                val eventoId = item.optString("eventoId")
                val tipoEntradaId = item.optString("tipoEntradaId")
                val localizador = item.optString("localizador")
                if (localizador.isEmpty()) continue

                // Busca el evento y el tipo de entrada para mostrar sus nombres
                val event = events.firstOrNull { it.id == eventoId }
                val ticket = event?.tickets?.firstOrNull { it.id == tipoEntradaId }

                references.add(
                    "%s — %s\n%s".format(
                        event?.name ?: eventoId,
                        ticket?.name ?: tipoEntradaId,
                        localizador
                    )
                )
            }
        }

        for (event in events) {
            for (ticket in event.tickets) {
                ticket.quantity = 0
            }
        }
        updateCartBadge()

        showDialog(
            getString(R.string.purchase_success_title),
            getString(R.string.purchase_success_message, references.joinToString("\n\n"))
        )
    }

    // Muestra un diálogo de aviso o de error
    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.accept, null)
            .show()
    }

    // Constantes de la pantalla
    companion object {
        // Clave para pasar la dirección del servidor desde el inicio de sesión
        const val EXTRA_SERVER_ADDRESS = "com.altea.altea_app.SERVER_ADDRESS"

        // Claves para pasar los datos del usuario autenticado
        const val EXTRA_USER_NAME = "com.altea.altea_app.USER_NAME"
        const val EXTRA_USER_MAIL = "com.altea.altea_app.USER_MAIL"

        // FILTRO TEMPORAL DE DESARROLLO: zona de entradas que se muestran en la app
        private const val FILTER_ZONE = "Z2"

        // Mensaje que pide el catálogo de eventos al servidor
        private const val GET_EVENTOS = """{"type":"getEventos"}"""
    }
}
