package com.altea.altea_app.home

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import com.altea.altea_app.R

// Diálogo de ajustes de la sesión: URL del servidor, nombre y correo.
// El correo no se puede editar y el botón de guardar solo se activa con cambios.
class SettingsDialog(
    context: Context,
    private val currentAddress: String,
    private val currentName: String,
    currentMail: String,
    private val onSave: (String, String) -> Unit
) : Dialog(context) {

    private lateinit var addressInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var saveButton: View

    init {
        setContentView(R.layout.dialog_settings)
        setCanceledOnTouchOutside(true)

        // Fondo transparente: las esquinas redondeadas las dibuja el layout
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Ancho: 88 % de la pantalla; alto: el que necesite el contenido
        val screenWidth = context.resources.displayMetrics.widthPixels
        window?.setLayout(
            (screenWidth * 0.88f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window?.setGravity(Gravity.CENTER)
        window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.setDimAmount(0.5f)

        bindContent(currentMail)
    }

    // Rellena los campos y prepara los botones del diálogo
    private fun bindContent(currentMail: String) {
        addressInput = findViewById(R.id.settings_server_url)
        nameInput = findViewById(R.id.settings_name)
        saveButton = findViewById(R.id.settings_save_button)

        val mailInput = findViewById<EditText>(R.id.settings_mail)
        mailInput.setText(currentMail)
        mailInput.isEnabled = false
        mailInput.isFocusable = false
        mailInput.isClickable = false

        addressInput.setText(currentAddress)
        nameInput.setText(currentName)
        findViewById<TextView>(R.id.settings_save_text).text =
            getContext().getString(R.string.save)

        // El botón solo se activa cuando la URL o el nombre cambian
        val changeWatcher = object : TextWatcher {
            override fun beforeTextChanged(
                text: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                text: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) = Unit

            override fun afterTextChanged(text: Editable?) {
                updateSaveState()
            }
        }
        addressInput.addTextChangedListener(changeWatcher)
        nameInput.addTextChangedListener(changeWatcher)

        saveButton.setOnClickListener {
            if (!saveButton.isEnabled) return@setOnClickListener

            val address = addressInput.text.toString().trim()
            val name = nameInput.text.toString().trim()

            dismiss()
            onSave(address, name)
        }

        findViewById<View>(R.id.settings_cancel_button).setOnClickListener {
            dismiss()
        }

        updateSaveState()
    }

    // Activa o desactiva el botón de guardar según haya cambios
    private fun updateSaveState() {
        val hasChanges =
            addressInput.text.toString().trim() != currentAddress.trim() ||
                nameInput.text.toString().trim() != currentName.trim()

        saveButton.isEnabled = hasChanges
        saveButton.alpha = if (hasChanges) 1f else 0.45f
    }
}
