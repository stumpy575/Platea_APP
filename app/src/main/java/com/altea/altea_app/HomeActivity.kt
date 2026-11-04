package com.altea.altea_app

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URI

class HomeActivity : AppCompatActivity() {

    private val events = ArrayList<Event>()

    private lateinit var eventAdapter: EventAdapter
    private var webSocketClient: WebSocketClient? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inflar la cartelera dentro del contenedor principal
        val contentContainer: FrameLayout = findViewById(R.id.content_container)
        layoutInflater.inflate(R.layout.view_listings, contentContainer, true)

        eventAdapter = EventAdapter(events) { event ->
            TicketBottomSheetDialog(this, event).show()
        }
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.adapter = eventAdapter

        connectToServer()
    }

    override fun onDestroy() {
        webSocketClient?.close()
        webSocketClient = null
        super.onDestroy()
    }

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

    private fun handleServerMessage(message: String?) {
        if (message == null) return

        val response = try {
            JSONObject(message)
        } catch (e: JSONException) {
            return
        }

        // El servidor responde con "eventos" o con "tiposEntrada"
        when (response.optString("type")) {
            "eventos" -> loadEvents(response.optJSONArray("eventos"))
            "tiposEntrada" -> loadTickets(
                response.optString("eventoId"),
                response.optJSONArray("tiposEntrada")
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

    companion object {
        const val EXTRA_SERVER_ADDRESS = "com.altea.altea_app.SERVER_ADDRESS"

        // FILTRO TEMPORAL DE DESARROLLO: zona de entradas que se muestran en la app
        private const val FILTER_ZONE = "Z2"

        private const val GET_EVENTOS = """{"type":"getEventos"}"""
    }
}
