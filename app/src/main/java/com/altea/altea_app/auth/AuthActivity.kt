package com.altea.altea_app.auth

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Xml
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.altea.altea_app.home.HomeActivity
import com.altea.altea_app.R
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer
import java.io.File
import java.net.URI

// Pantalla de inicio de sesión: valida los datos y conecta con el servidor WebSocket
class AuthActivity : AppCompatActivity() {

    // Campos del formulario de conexión
    private lateinit var urlInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var mailInput: EditText
    private lateinit var loginButton: Button

    // Sirve para saber cuando se esta cerrando la conexión
    // porque estamos yendo a otra activity o porque algo
    // ha salido mal mientras nos conectabamos. (muy improbable pero bueno)
    private var navigatingToHome = false

    private var webSocketClient: WebSocketClient? = null

    // Pide permiso de red local antes de conectar
    private val localNetworkPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                connectWs()
            } else {
                loginButton.isEnabled = true
                AlertDialog.Builder(this)
                    .setTitle("Permiso necesario")
                    .setMessage(
                        "La app necesita acceso a la red local para conectar con el servidor. " +
                            "Puedes concederlo en Ajustes > Aplicaciones > Altea > Permisos > Dispositivos cercanos."
                    )
                    .setPositiveButton("Abrir ajustes") { _, _ ->
                        startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:$packageName")
                            )
                        )
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }

    // Inicializa la interfaz y el botón de conexión
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            val horizontalPadding = (28 * resources.displayMetrics.density).toInt()
            val verticalPadding = (32 * resources.displayMetrics.density).toInt()

            v.setPadding(
                systemBars.left + horizontalPadding,
                systemBars.top + verticalPadding,
                systemBars.right + horizontalPadding,
                systemBars.bottom + verticalPadding
            )
            insets
        }

        // Referencias a los campos y al botón del formulario
        urlInput = findViewById(R.id.serverUrlInput)
        nameInput = findViewById(R.id.nameInput)
        mailInput = findViewById(R.id.mailInput)
        loginButton = findViewById(R.id.login_button)

        // Carga la configuracion del xml si existe
        loadConfig()

        // Al pulsar conectar: pide permiso si hace falta y abre el WebSocket
        loginButton.setOnClickListener {
            if (needsLocalNetworkPermission()) {
                localNetworkPermissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
            } else {
                loginButton.isEnabled = false
                connectWs()
            }
        }
    }

    override fun onDestroy() {
        webSocketClient?.close()
        webSocketClient = null
        super.onDestroy()
    }

    // Comprueba si hace falta el permiso de red local
    private fun needsLocalNetworkPermission(): Boolean {
        return Build.VERSION.SDK_INT >= 37 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_LOCAL_NETWORK
            ) != PackageManager.PERMISSION_GRANTED
    }

    // Abre la conexión WebSocket con la dirección del formulario y navega al inicio al conectar
    private fun connectWs() {

        if (webSocketClient?.isOpen == true) {
            webSocketClient?.close()
            loginButton.isEnabled = true
            return
        }

        val address = urlInput.text.toString().trim()
        val name = nameInput.text.toString().trim()
        val mail = mailInput.text.toString().trim()

        // Comprobaciones de campos vacios
        if (address.isEmpty()) {
            showDialog("Error", "Introduce una dirección WebSocket.")
            loginButton.isEnabled = true
            return
        } else if (name.isEmpty()) {
            showDialog("Error", "Introduce un nombre.")
            loginButton.isEnabled = true
            return
        } else if (mail.isEmpty()) {
            showDialog("Error", "Introduce un correo.")
            loginButton.isEnabled = true
            return
        }

        val serverUri = URI(address)
        webSocketClient = object : WebSocketClient(serverUri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                // Guarda los datos introducidos si la conexion fue exitosa
                saveConfig(address, name, mail)

                runOnUiThread {
                    // Chequeo de seguridad en caso que el usuario haga guarradas
                    // mientras se conecta al websocket
                    if (isFinishing || isDestroyed) return@runOnUiThread

                    navigatingToHome = true
                    // Cerramos la conexión ws para que no se duplique al abrirla
                    // de nuevo en la otra activity
                    webSocketClient?.close()
                    webSocketClient = null

                    // Conexión establecida: pasamos a la pantalla principal con los datos del usuario
                    startActivity(
                        Intent(this@AuthActivity, HomeActivity::class.java)
                        .putExtra(HomeActivity.EXTRA_SERVER_ADDRESS, address)
                        .putExtra(HomeActivity.EXTRA_USER_NAME, name)
                        .putExtra(HomeActivity.EXTRA_USER_MAIL, mail)
                    )
                    // Para que el usuario no pueda volver
                    // dandole para atras
                    finish()
                }
            }

            // Muestra por pantalla los mensajes que envía el servidor
            override fun onMessage(message: String?) {
                runOnUiThread {
                    showDialog("Mensaje del servidor", "$message")
                }
            }

            // Avisa de la desconexión y reactiva el botón de conectar
            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                if (navigatingToHome) return
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    showDialog("Desconexión", "Desconectado del servidor.")
                    loginButton.isEnabled = true
                }
            }

            // Muestra el error de conexión y reactiva el botón de conectar
            override fun onError(ex: Exception?) {
                runOnUiThread {
                    showDialog("Error", "Error: ${ex?.message}")
                    loginButton.isEnabled = true
                }
            }
        }

        webSocketClient?.connect()
    }

    // Muestra un diálogo de aviso o de error
    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Aceptar", null)
            .show()
    }

    // Guarda la dirección, el nombre y el correo en CONFIG.xml
    private fun saveConfig(address: String, name: String, mail: String) {
        try {
            val serializer = Xml.newSerializer()

            // Abre o crea CONFIG.xml, MODE_PRIVATE: solo accesible desde esta app
            openFileOutput("CONFIG.xml", MODE_PRIVATE).use { output ->

                serializer.setOutput(output, "UTF-8")
                serializer.startDocument("UTF-8", true)

                serializer.startTag(null, "config")

                addNode(serializer, "address", address)
                addNode(serializer, "name", name)
                addNode(serializer, "mail", mail)

                serializer.endTag(null, "config")
                serializer.endDocument()
            }

        } catch (e: Exception) {
            showDialog("Error", "Error: $e")
        }
    }

    private fun loadConfig() {
        val configFile = File(filesDir, "CONFIG.xml")

        if (!configFile.exists()) {
            return
        }

        try {
            val factory = Xml.newPullParser()

            var address = ""
            var name = ""
            var mail = ""

            configFile.inputStream().use { input ->

                factory.setInput(input, "UTF-8")

                var eventType = factory.eventType

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        when (factory.name) {
                            "address" -> address = factory.nextText()
                            "name" -> name = factory.nextText()
                            "mail" -> mail = factory.nextText()
                        }
                    }
                    eventType = factory.next()
                }
            }

            // Cargar los datos en el formulario
            urlInput.setText(address)
            nameInput.setText(name)
            mailInput.setText(mail)

        } catch (e: Exception) {
            showDialog("Error", "Error leyendo CONFIG.xml: $e")
        }
    }

    // Añade un nodo con su valor al XML de configuración
    private fun addNode(
        serializer: XmlSerializer,
        tag: String,
        value: String
    ) {
        serializer.startTag(null, tag)
        serializer.text(value)
        serializer.endTag(null, tag)
    }
}