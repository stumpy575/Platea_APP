package com.altea.altea_app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI

class MainActivity : AppCompatActivity() {

    private lateinit var urlInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var mailInput: EditText
    private lateinit var loginButton: Button

    var webSocketClient: WebSocketClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
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

        // Interface
        urlInput = findViewById(R.id.serverUrlInput)
        nameInput = findViewById(R.id.nameInput)
        mailInput = findViewById(R.id.mailInput)
        loginButton = findViewById(R.id.login_button)

        loginButton.setOnClickListener {
            loginButton.isEnabled = false
            connectWs()
        }
    }

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

        // Comprueba si el url introducido es un url de websocket valido
        if (!address.startsWith("ws://") && !address.startsWith("wss://")) {
            showDialog("Error", "Dirección WebSocket no válida.")
            loginButton.isEnabled = true
            return
        }

        val serverUri = URI(address)
        webSocketClient = object : WebSocketClient(serverUri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                runOnUiThread {
                    showDialog("Conexión exitosa", "Se ha establecido la conexión con el servidor.")
                    loginButton.isEnabled = true
                }
            }

            override fun onMessage(message: String?) {
                runOnUiThread {
                    showDialog("Mensaje del servidor", "$message")
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                runOnUiThread {
                    showDialog("Desconexión", "Desconectado del servidor.")
                    loginButton.isEnabled = true
                }
            }

            override fun onError(ex: Exception?) {
                runOnUiThread {
                    showDialog("Error", "Error: ${ex?.message}")
                    loginButton.isEnabled = true
                }
            }
        }

        webSocketClient?.connect()
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun saveConfig(address: String, name: String, mail: String) {
        try {
            val serializer = android.util.Xml.newSerializer()

            // Abre o crea CONFIG.xml, MODE_PRIVATE: solo accesible desde esta app
            val output = openFileOutput("CONFIG.xml", MODE_PRIVATE)



        } catch (e: Exception) {
            showDialog("Error", "Error: $e")
        }
    }
}