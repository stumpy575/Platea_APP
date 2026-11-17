package com.altea.altea_app.home

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Xml
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.altea.altea_app.model.Event
import com.altea.altea_app.model.PurchaseEntry
import com.altea.altea_app.R
import com.altea.altea_app.auth.AuthActivity
import com.altea.altea_app.cart.CartBottomSheetDialog
import com.altea.altea_app.data.ServerClient
import com.altea.altea_app.listings.ListingsFragment
import com.altea.altea_app.model.OwnedTicket
import com.altea.altea_app.tickets.TicketsFragment
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.xmlpull.v1.XmlSerializer
import java.net.URI

// Pantalla principal: cartelera de eventos, conexión WebSocket y carrito
class HomeActivity : AppCompatActivity() {

    // Datos del usuario autenticado, disponibles durante toda la sesión (se usarán al finalizar la compra)
    var userName: String = ""
    var userMail: String = ""

    private lateinit var cartBadge: TextView
    private lateinit var serverClient: ServerClient

    // Dirección del servidor conectado actualmente (se puede cambiar desde Ajustes)
    private var serverAddress: String = ""

    // Fragmentos de las dos pantallas
    private var listingsFragment: ListingsFragment? = null
    private var ticketsFragment: TicketsFragment? = null

    // Iconos y textos de la barra inferior, para marcar la pestaña activa
    private lateinit var listingsNavIcon: ImageView
    private lateinit var listingsNavLabel: TextView
    private lateinit var ticketsNavIcon: ImageView
    private lateinit var ticketsNavLabel: TextView

    // Evita volver al inicio de sesión más de una vez por una misma desconexión
    private var returningToLogin = false

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
        serverAddress = intent.getStringExtra(EXTRA_SERVER_ADDRESS).orEmpty()

        // La pestaña del carrito abre el resumen de entradas seleccionadas
        findViewById<View>(R.id.cart_nav_button)!!.setOnClickListener {
            showCart()
        }

        listingsNavIcon = findViewById(R.id.listings_nav_icon)
        listingsNavLabel = findViewById(R.id.listings_nav_label)
        ticketsNavIcon = findViewById(R.id.tickets_nav_icon)
        ticketsNavLabel = findViewById(R.id.tickets_nav_label)

        findViewById<View>(R.id.listings_nav_button)!!.setOnClickListener {
            showListings()
        }
        findViewById<View>(R.id.tickets_nav_button)!!.setOnClickListener {
            showTickets()
        }

        setupFragments(savedInstanceState)

        returningToLogin = false
        connectToServer()
    }

    // Cierra la conexión WebSocket al salir de la pantalla
    override fun onDestroy() {
        if (::serverClient.isInitialized) {
            serverClient.close()
        }

        super.onDestroy()
    }

    // Abre el WebSocket con la dirección del servidor actual
    private fun connectToServer() {
        if (serverAddress.isBlank()) return

        serverClient = ServerClient(serverAddress, userMail, object : ServerClient.Listener {
            override fun onEventsLoaded(events: List<Event>) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    listingsFragment?.updateEvents(events)
                }
            }

            override fun onOwnedTicketsLoaded(tickets: List<OwnedTicket>) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    ticketsFragment?.updateTickets(tickets)
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
                    if (isFinishing || isDestroyed || returningToLogin) {
                        return@runOnUiThread
                    }
                    returningToLogin = true
                    returnToLogin()
                }
            }

            override fun onConnectionClosed() {
                runOnUiThread {
                    if (isFinishing || isDestroyed || returningToLogin) {
                        return@runOnUiThread
                    }
                    returningToLogin = true
                    returnToLogin()
                }
            }
        })

        serverClient.connect()
    }

    // Cierra la sesión actual y vuelve a la pantalla de inicio de sesión
    private fun returnToLogin() {
        if (isFinishing || isDestroyed) return

        if (::serverClient.isInitialized) {
            serverClient.close()
        }

        val intent = Intent(this, AuthActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }

    // Abre el diálogo de ajustes con los datos de la sesión actual
    fun showSettingsDialog() {
        if (isFinishing || isDestroyed) return

        SettingsDialog(
            this,
            currentAddress = serverAddress,
            currentName = userName,
            currentMail = userMail
        ) { address, name ->
            applySettings(address, name)
        }.show()
    }

    // Aplica los ajustes solo después de validar la nueva conexión si hace falta
    private fun applySettings(newAddress: String, newName: String) {
        if (newAddress.isEmpty()) {
            showDialog(
                getString(R.string.connection_error_title),
                getString(R.string.settings_empty_url)
            )
            return
        }

        if (newName.isEmpty()) {
            showDialog(
                getString(R.string.connection_error_title),
                getString(R.string.settings_empty_name)
            )
            return
        }

        val addressChanged = newAddress != serverAddress
        val nameChanged = newName != userName

        if (!addressChanged && !nameChanged) return

        // Si solo cambia el nombre, no hace falta reconectar
        if (!addressChanged) {
            userName = newName
            intent.putExtra(EXTRA_USER_NAME, userName)
            saveConfig(serverAddress, userName, userMail)
            return
        }

        // Comprueba la nueva URL antes de cerrar la conexión actual
        validateServerConnection(newAddress) { valid ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread

                if (!valid) {
                    showDialog(
                        getString(R.string.connection_error_title),
                        getString(R.string.settings_connection_error)
                    )
                    return@runOnUiThread
                }

                applyNewServer(newAddress, newName)
            }
        }
    }

    // Cierra la conexión actual y recarga todos los datos desde la nueva URL
    private fun applyNewServer(newAddress: String, newName: String) {
        serverClient.close()

        serverAddress = newAddress
        userName = newName
        intent.putExtra(EXTRA_SERVER_ADDRESS, serverAddress)
        intent.putExtra(EXTRA_USER_NAME, userName)
        saveConfig(serverAddress, userName, userMail)

        // Limpia los datos antiguos mientras llega el catálogo del nuevo servidor
        listingsFragment?.updateEvents(emptyList())
        ticketsFragment?.updateTickets(emptyList())
        returningToLogin = false

        connectToServer()
        updateCartBadge()
    }

    // Abre una conexión temporal para comprobar que la nueva URL responde
    private fun validateServerConnection(address: String, onResult: (Boolean) -> Unit) {
        val uri = try {
            URI(address)
        } catch (e: Exception) {
            onResult(false)
            return
        }

        var finished = false
        var client: WebSocketClient? = null
        val timeoutHandler = Handler(Looper.getMainLooper())

        fun finish(valid: Boolean) {
            if (finished) return
            finished = true
            timeoutHandler.removeCallbacksAndMessages(null)

            try {
                client?.close()
            } catch (e: Exception) {
                // La conexión de prueba ya puede estar cerrada
            }

            onResult(valid)
        }

        try {
            client = object : WebSocketClient(uri) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    finish(true)
                }

                override fun onMessage(message: String?) = Unit

                override fun onClose(
                    code: Int,
                    reason: String?,
                    remote: Boolean
                ) {
                    finish(false)
                }

                override fun onError(ex: Exception?) {
                    finish(false)
                }
            }
        } catch (e: Exception) {
            onResult(false)
            return
        }

        timeoutHandler.postDelayed({ finish(false) }, VALIDATION_TIMEOUT_MS)

        try {
            client.connect()
        } catch (e: Exception) {
            finish(false)
        }
    }

    // Guarda la configuración de la sesión en CONFIG.xml
    private fun saveConfig(address: String, name: String, mail: String) {
        try {
            val serializer = Xml.newSerializer()

            openFileOutput("CONFIG.xml", MODE_PRIVATE).use { output ->
                serializer.setOutput(output, "UTF-8")
                serializer.startDocument("UTF-8", true)
                serializer.startTag(null, "config")

                addConfigNode(serializer, "address", address)
                addConfigNode(serializer, "name", name)
                addConfigNode(serializer, "mail", mail)

                serializer.endTag(null, "config")
                serializer.endDocument()
            }
        } catch (e: Exception) {
            showDialog("Error", "Error: $e")
        }
    }

    private fun addConfigNode(
        serializer: XmlSerializer,
        tag: String,
        value: String
    ) {
        serializer.startTag(null, tag)
        serializer.text(value)
        serializer.endTag(null, tag)
    }

    // Añade los dos fragmentos la primera vez, o los recupera tras recrear la actividad
    private fun setupFragments(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            val listings = ListingsFragment()
            val tickets = TicketsFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.screen_container, listings, TAG_LISTINGS)
                .add(R.id.screen_container, tickets, TAG_TICKETS)
                .hide(tickets)
                .commit()

            listingsFragment = listings
            ticketsFragment = tickets
        } else {
            listingsFragment =
                supportFragmentManager.findFragmentByTag(TAG_LISTINGS) as? ListingsFragment
            ticketsFragment =
                supportFragmentManager.findFragmentByTag(TAG_TICKETS) as? TicketsFragment

            // Al girar la pantalla se vuelve a la cartelera para que la barra
            // inferior y la pantalla visible coincidan.
            showListings()
        }
    }

    // Muestra la cartelera
    private fun showListings() {
        val listings = listingsFragment
        val tickets = ticketsFragment

        if (listings != null && tickets != null) {
            supportFragmentManager.beginTransaction()
                .show(listings)
                .hide(tickets)
                .commit()
        }

        updateNavSelection(showListings = true)
    }

    // Muestra las entradas compradas
    private fun showTickets() {
        val listings = listingsFragment
        val tickets = ticketsFragment

        if (listings != null && tickets != null) {
            supportFragmentManager.beginTransaction()
                .hide(listings)
                .show(tickets)
                .commit()
        }

        updateNavSelection(showListings = false)
    }

    // Colorea la pestaña activa y deja la otra en gris
    private fun updateNavSelection(showListings: Boolean) {
        val accent = ContextCompat.getColor(this, R.color.accent)
        val unselected = ContextCompat.getColor(this, R.color.nav_unselected)

        listingsNavIcon.imageTintList =
            ColorStateList.valueOf(if (showListings) accent else unselected)
        listingsNavLabel.setTextColor(if (showListings) accent else unselected)

        ticketsNavIcon.imageTintList =
            ColorStateList.valueOf(if (showListings) unselected else accent)
        ticketsNavLabel.setTextColor(if (showListings) unselected else accent)
    }

    // Suma las entradas de todos los eventos y actualiza el contador del carrito
    fun updateCartBadge() {
        val count = serverClient.getCartCount()

        cartBadge.text = count.toString()
        cartBadge.isVisible = count > 0
    }

    // Abre el carrito con las entradas seleccionadas
    private fun showCart() {
        CartBottomSheetDialog(
            this,
            serverClient.getCartItems(),
            onQuantityChange = { item, quantity ->
                serverClient.updateCartQuantity(item, quantity)
                updateCartBadge()
            },
            onRemoveItem = { item ->
                serverClient.removeCartItem(item)
                updateCartBadge()
            }
        ) {
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

        // Refresca "Mis Entradas" para incluir las entradas recién compradas
        serverClient.requestOwnedTickets()

        updateCartBadge()

        // El diálogo muestra los localizadores en un bloque destacado
        AppDialog(
            this,
            title = getString(R.string.purchase_success_title),
            message = getString(R.string.purchase_success_message),
            detailLabel = getString(R.string.purchase_locators_label),
            detail = references.joinToString("\n\n")
        ).show()
    }

    // Muestra un diálogo de aviso o de error
    private fun showDialog(title: String, message: String) {
        AppDialog(this, title, message).show()
    }

    // Constantes de la pantalla
    companion object {
        // Clave para pasar la dirección del servidor desde el inicio de sesión
        const val EXTRA_SERVER_ADDRESS = "com.altea.altea_app.SERVER_ADDRESS"

        // Claves para pasar los datos del usuario autenticado
        const val EXTRA_USER_NAME = "com.altea.altea_app.USER_NAME"
        const val EXTRA_USER_MAIL = "com.altea.altea_app.USER_MAIL"

        // Etiquetas para recuperar los fragmentos al recrear la actividad
        private const val TAG_LISTINGS = "listings"
        private const val TAG_TICKETS = "tickets"

        // Tiempo máximo para validar la conexión con una nueva URL
        private const val VALIDATION_TIMEOUT_MS = 8000L
    }
}
