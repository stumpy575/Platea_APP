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
class HomeActivity : AppCompatActivity(), ServerClient.Listener {

    // Lista de eventos que se muestran en la cartelera

    // En definitiva es un ArrayList, pero se ve que esta forma
    // es mas natural en Kotlin.
    private val events = mutableListOf<Event>()
    // Se utiliza para trackear los eventos que aun no han cargado
    // sus tipos de tickets. (Previene errores)
    private val pendingEvents = mutableMapOf<String, Event>()

    // -------------------------------------------------

    // Datos del usuario autenticado, disponibles durante toda la sesión (se usarán al finalizar la compra)
    var userName: String = ""
    var userMail: String = ""

    private lateinit var eventAdapter: EventAdapter
    private lateinit var cartBadge: TextView
    private lateinit var serverClient: ServerClient

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
        if (::serverClient.isInitialized) {
            serverClient.close()
        }

        super.onDestroy()
    }

    // Abre el WebSocket con la dirección usada en el inicio de sesión
    private fun connectToServer() {
        // La dirección es la misma que se usó en el inicio de sesión
        val address = intent.getStringExtra(EXTRA_SERVER_ADDRESS) ?: return

        serverClient = ServerClient(address, this)

        serverClient.connect()
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

    // Carga los eventos recibidos del servidor, los almacena temporalmente
    // y solicita sus tipos de entrada.
    private fun loadEvents(eventsArray: JSONArray?) {
        // Operador Elvis, comprueba si es null y sale de la funcion si lo es.
        eventsArray ?: return

        // Limpia los eventos para cargarlos de nuevo
        events.clear()
        pendingEvents.clear()

        // Recorre los eventos devueltos
        // until hace que no se incluya el ultimo numero,
        // asi que por eso no hacemos eventos.lenght() - 1
        for (i in 0 until eventsArray.length()) {
            // Obtiene el evento en la posicion i, pero si
            // no es valido (null), salta a la siguiente iteracion
            val eventJSON = eventsArray.optJSONObject(i) ?: continue

            // Transforma el JSONObject en un Event
            val event = Event(
                id = eventJSON.optString("id"),
                name = eventJSON.optString("nom"),
                date = eventJSON.optString("dataHora"),
                img = eventJSON.optString("imatge"),
                description = eventJSON.optString("descripcio"),
                tickets = emptyList()
            )
            // Ahora el evento que hemos conseguido, lo agregamos a
            // la lista de eventos pendientes de recibir los tipos
            // de entradas que tiene.
            pendingEvents[event.id] = event

            // Pide los tipos de tickets de este evento
            // La respuesta en vez de almacenarla en una variable,
            // la recibiremos por onMessage, que es mas correcto.
            serverClient.getTiposEntrada(event.id)
        }
    }

    // Guarda las entradas recibidas en su evento y actualiza el precio de la cartelera
    private fun loadTickets(eventoId: String, tiposEntrada: JSONArray?) {
        // Control de nulls
        tiposEntrada ?: return
        val event = pendingEvents[eventoId] ?: return

        // Transforma el array de tickets json a una lista de objetos Tickets
        val tickets = ServerMessageParser.parseTickets(tiposEntrada)
        if (tickets.isNotEmpty()) {
            event.tickets = tickets
            events.add(event)
            pendingEvents.remove(eventoId)
            eventAdapter.notifyDataSetChanged()
        }
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

        serverClient.buyTickets(request)
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

    override fun onEventReady(event: Event) {
        TODO("Not yet implemented")
    }

    override fun onPurchaseSuccess(entries: List<PurchaseEntry>) {
        TODO("Not yet implemented")
    }

    override fun onPurchaseError(message: String) {
        TODO("Not yet implemented")
    }

    override fun onConnectionError(message: String) {
        TODO("Not yet implemented")
    }

    override fun onConnectionClosed() {
        TODO("Not yet implemented")
    }

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            handleServerMessage(message)
        }
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
    }
}
