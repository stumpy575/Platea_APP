package com.altea.altea_app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI

class AuthActivity : AppCompatActivity() {

    private lateinit var urlInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var mailInput: EditText
    private lateinit var loginButton: Button

    var webSocketClient: WebSocketClient? = null

    private val localNetworkPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                connectWs()
            } else {
                loginButton.isEnabled = true
                showDialog(
                    "Permiso necesario",
                    "La app necesita acceso a la red local para conectar con el servidor."
                )
            }
        }

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

        // Interface
        urlInput = findViewById(R.id.serverUrlInput)
        nameInput = findViewById(R.id.nameInput)
        mailInput = findViewById(R.id.mailInput)
        loginButton = findViewById(R.id.login_button)

        loginButton.setOnClickListener {
            if (needsLocalNetworkPermission()) {
                localNetworkPermissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
            } else {
                loginButton.isEnabled = false
                connectWs()
            }
        }
    }

    private fun needsLocalNetworkPermission(): Boolean {
        return Build.VERSION.SDK_INT >= 36 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_LOCAL_NETWORK
            ) != PackageManager.PERMISSION_GRANTED
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

        val serverUri = URI(address)
        webSocketClient = object : WebSocketClient(serverUri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                // Guarda los datos introducidos si la conexion fue exitosa
                saveConfig(address, name, mail)

                runOnUiThread {
                    // Conexión establecida: pasamos a la pantalla principal
                    val intent = Intent(this@AuthActivity, HomeActivity::class.java)
                        .putExtra(HomeActivity.EXTRA_SERVER_ADDRESS, address)
                    startActivity(intent)
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

            // Utiliza el archivo en UTF-8
            serializer.setOutput(output, "UTF-8")
            // Cabecera del XML
            serializer.startDocument("UTF-8", true)

            // Nodo principal
            serializer.startTag(null, "config")

            // Introducir nodos
            addNode(serializer, "address", address)
            addNode(serializer, "name", name)
            addNode(serializer, "mail", mail)

            // Cerrar nodo principal
            serializer.endTag(null, "config")
            serializer.endDocument()

            output.close()
        } catch (e: Exception) {
            showDialog("Error", "Error: $e")
        }
    }

    private fun addNode(
        serializer: org.xmlpull.v1.XmlSerializer,
        tag: String,
        value: String
    ) {
        serializer.startTag(null, tag)
        serializer.text(value)
        serializer.endTag(null, tag)
    }
}