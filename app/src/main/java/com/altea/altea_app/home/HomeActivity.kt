package com.altea.altea_app.home

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
import com.altea.altea_app.model.Event
import com.altea.altea_app.model.PurchaseEntry
import com.altea.altea_app.R
import com.altea.altea_app.data.ServerClient

// Pantalla principal: cartelera de eventos, conexión WebSocket y carrito
class HomeActivity : AppCompatActivity() {

    // Datos del usuario autenticado, disponibles durante toda la sesión (se usarán al finalizar la compra)
    var userName: String = ""
    var userMail: String = ""

    private lateinit var eventAdapter: EventAdapter
    private lateinit var cartBadge: TextView
    private lateinit var serverClient: ServerClient

    // Evita mostrar más de un diálogo si se produce un error y después se cierra la conexión
    private var connectionErrorShown = false

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

        eventAdapter = EventAdapter(mutableListOf()) { event ->
            TicketBottomSheetDialog(this, event) {
                updateCartBadge()
            }.show()
        }
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.adapter = eventAdapter

        connectionErrorShown = false
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

        serverClient = ServerClient(address, object : ServerClient.Listener {
            override fun onEventsLoaded(events: List<Event>) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    eventAdapter.updateEvents(events)
                }
            }

            override fun onPurchaseSuccess(entries: List<PurchaseEntry>) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    showPurchaseSuccess(entries)
                }
            }

            override fun onPurchaseError(message: String) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    showDialog(getString(R.string.purchase_error_title), message)
                }
            }

            override fun onConnectionError(message: String) {
                runOnUiThread {
                    if (isFinishing || isDestroyed || connectionErrorShown) {
                        return@runOnUiThread
                    }
                    connectionErrorShown = true
                    showDialog(getString(R.string.connection_error_title), message)
                }
            }

            override fun onConnectionClosed() {
                runOnUiThread {
                    if (isFinishing || isDestroyed || connectionErrorShown) {
                        return@runOnUiThread
                    }
                    connectionErrorShown = true
                    showDialog(
                        getString(R.string.connection_error_title),
                        getString(R.string.connection_closed_message)
                    )
                }
            }
        })

        serverClient.connect()
    }

    // Suma las entradas de todos los eventos y actualiza el contador del carrito
    private fun updateCartBadge() {
        val count = serverClient.getCartCount()

        cartBadge.text = count.toString()
        cartBadge.isVisible = count > 0
    }

    // Abre el carrito con las entradas seleccionadas
    private fun showCart() {
        CartBottomSheetDialog(this, serverClient.getCartItems()) {
            serverClient.buyTickets(serverClient.getCartItems(), userName, userMail)
        }.show()
    }

    // Muestra los localizadores generados y vacía el carrito al completar la compra
    private fun showPurchaseSuccess(entries: List<PurchaseEntry>) {

        val references = entries.map { entry ->

            val eventName = serverClient.getEventName(entry.eventoId)
            val ticketName = serverClient.getTicketName(
                entry.eventoId,
                entry.tipoEntradaId
            )

            "%s — %s\n%s".format(
                eventName ?: entry.eventoId,
                ticketName ?: entry.tipoEntradaId,
                entry.localizador
            )
        }

        // Vacía el carrito.
        serverClient.clearCart()

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
    }
}
