package com.altea.altea_app.home

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.view.isVisible
import com.altea.altea_app.R

// Diálogo de aviso o de confirmación con el estilo de la app.
// El botón principal ejecuta una acción; el secundario (opcional) siempre cancela.
class AppDialog(
    context: Context,
    private val title: String,
    private val message: String,
    private val primaryText: String = context.getString(R.string.accept),
    private val secondaryText: String? = null,
    private val destructive: Boolean = false,
    private val detailLabel: String? = null,
    private val detail: String? = null,
    private val onPrimary: (() -> Unit)? = null
) : Dialog(context) {

    init {
        setContentView(R.layout.dialog_app)
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

        bindContent()
    }

    // Rellena el diálogo con el aviso y sus botones
    private fun bindContent() {
        findViewById<TextView>(R.id.dialog_title).text = title

        // El mensaje puede faltar si el diálogo solo muestra un detalle
        val messageView = findViewById<TextView>(R.id.dialog_message)
        val hasMessage = message.isNotBlank()
        messageView.text = message
        messageView.isVisible = hasMessage
        findViewById<View>(R.id.dialog_message_scroll).isVisible = hasMessage

        applyScrollLimits(hasMessage, !detail.isNullOrBlank())

        bindDetail()
        bindPrimaryButton()
        bindSecondaryButton()
    }

    // Limita el alto de las zonas desplazables para que los botones nunca queden fuera
    private fun applyScrollLimits(hasMessage: Boolean, hasDetail: Boolean) {
        val metrics = getContext().resources.displayMetrics

        // Espacio que ya ocupan el relleno de la tarjeta, el título, la etiqueta y los botones
        val reserved = (230 * metrics.density).toInt()
        val available = ((metrics.heightPixels * 0.86f) - reserved)
            .toInt()
            .coerceAtLeast((metrics.heightPixels * 0.35f).toInt())

        val messageScroll = findViewById<MaxHeightScrollView>(R.id.dialog_message_scroll)
        val detailScroll = findViewById<MaxHeightScrollView>(R.id.dialog_detail_scroll)

        // Si solo hay un bloque, puede ocupar todo el alto disponible
        messageScroll.maxHeight = when {
            !hasMessage -> 0
            hasDetail -> (available * 0.30f).toInt()
            else -> available
        }

        detailScroll.maxHeight = when {
            !hasDetail -> 0
            hasMessage -> (available * 0.70f).toInt()
            else -> available
        }
    }

    // Muestra el bloque de detalle destacado (por ejemplo, los localizadores)
    private fun bindDetail() {
        if (detail.isNullOrBlank()) return

        findViewById<View>(R.id.dialog_detail_scroll).isVisible = true
        findViewById<TextView>(R.id.dialog_detail).text = detail

        val labelView = findViewById<TextView>(R.id.dialog_detail_label)
        labelView.isVisible = !detailLabel.isNullOrBlank()
        labelView.text = detailLabel.orEmpty()
    }

    // El botón principal cierra el diálogo y ejecuta la acción recibida
    private fun bindPrimaryButton() {
        val primaryButton = findViewById<View>(R.id.dialog_primary_button)
        findViewById<TextView>(R.id.dialog_primary_text).text = primaryText

        // Las acciones destructivas (quitar una entrada) usan el rojo de la app
        if (destructive) {
            primaryButton.setBackgroundResource(R.drawable.ripple_danger_button)
        }

        primaryButton.setOnClickListener {
            dismiss()
            onPrimary?.invoke()
        }
    }

    // El botón secundario solo aparece si se pide y siempre cancela
    private fun bindSecondaryButton() {
        if (secondaryText == null) return

        val secondaryButton = findViewById<View>(R.id.dialog_secondary_button)
        secondaryButton.isVisible = true
        findViewById<TextView>(R.id.dialog_secondary_text).text = secondaryText
        secondaryButton.setOnClickListener { dismiss() }
    }
}
