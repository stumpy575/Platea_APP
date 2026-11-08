package com.altea.altea_app.tickets

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.altea.altea_app.R
import com.altea.altea_app.model.OwnedTicket

// Diálogo con el resumen y el código de una entrada comprada
class TicketCodeDialog(
    context: Context,
    private val ticket: OwnedTicket
) : Dialog(context) {

    init {
        setContentView(R.layout.dialog_ticket_code)
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

        bindTicket()
    }

    // Rellena el diálogo con los datos de la entrada
    private fun bindTicket() {
        findViewById<TextView>(R.id.code_dialog_title).text = ticket.eventName
        findViewById<TextView>(R.id.code_dialog_date).text =
            getContext().getString(R.string.ticket_code_date, ticket.eventDate)
        findViewById<TextView>(R.id.code_dialog_type).text =
            getContext().getString(R.string.ticket_code_type, ticket.ticketName)
        findViewById<TextView>(R.id.code_dialog_value).text = ticket.locator

        findViewById<View>(R.id.code_dialog_close_button)
            .setOnClickListener { dismiss() }
    }
}