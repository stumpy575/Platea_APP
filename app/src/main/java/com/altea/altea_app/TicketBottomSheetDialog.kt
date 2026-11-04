package com.altea.altea_app

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

// Panel de selección de entradas de un evento
class TicketBottomSheetDialog(
    context: Context,
    private val event: Event,
    private val onCartChanged: () -> Unit
) : BottomSheetDialog(context) {

    // Número de tarjetas de entrada que se ven a la vez en la lista
    private val visibleTickets = 3

    // Cantidades que había al abrir el panel, para deshacerlas si se cierra sin confirmar
    private val initialQuantities = event.tickets.map { it.quantity }
    private var confirmed = false

    private val totalPriceView: TextView
    private val ticketsRecyclerView: RecyclerView

    // Prepara los datos del evento, la lista de entradas y los botones
    init {
        setContentView(R.layout.bottom_sheet_tickets)

        val titleView: TextView = findViewById(R.id.event_sheet_title)!!
        val dateView: TextView = findViewById(R.id.event_sheet_date)!!
        val descriptionView: TextView = findViewById(R.id.event_sheet_description)!!

        titleView.text = event.name
        dateView.text = event.date
        descriptionView.text = event.description

        totalPriceView = findViewById(R.id.total_price)!!

        // El adaptador avisa al panel cada vez que cambia una cantidad
        val ticketsAdapter = TicketAdapter(event.tickets) {
            updateTotalPrice()
            onCartChanged()
        }

        ticketsRecyclerView = findViewById(R.id.tickets_recycler_view)!!
        ticketsRecyclerView.adapter = ticketsAdapter

        updateTotalPrice()

        findViewById<View>(R.id.close_sheet_button)!!.setOnClickListener { dismiss() }

        findViewById<View>(R.id.add_to_cart_button)!!.setOnClickListener {
            // Solo confirma y cierra: el contador ya se ha actualizado con los botones + y -
            confirmed = true
            dismiss()
        }
    }

    override fun dismiss() {
        // Al cerrar con la X, el gesto de arrastre o el botón atrás se deshacen los cambios
        if (!confirmed) {
            revertQuantities()
        }
        super.dismiss()
    }

    // Deshace las cantidades cambiadas y ajusta el contador del carrito
    private fun revertQuantities() {
        event.tickets.forEachIndexed { index, ticket ->
            ticket.quantity = initialQuantities[index]
        }

        onCartChanged()
    }

    // Ajusta el panel al contenido y aplica el estilo de la app
    override fun onStart() {
        super.onStart()

        // Oscurece la cartelera que queda detrás del panel
        window?.setDimAmount(0.5f)

        val bottomSheet: View =
            findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)!!

        // Mismo fondo y esquinas redondeadas que el resto de la app
        bottomSheet.setBackgroundResource(R.drawable.bg_bottom_sheet)

        // La lista muestra como máximo tres tipos de entrada; si el evento tiene menos, ocupa solo lo necesario
        val listParams = ticketsRecyclerView.layoutParams as LinearLayout.LayoutParams
        listParams.height = ticketItemHeight() * minOf(visibleTickets, event.tickets.size)
        listParams.weight = 0f
        ticketsRecyclerView.layoutParams = listParams

        // El panel se ajusta a su contenido en lugar de ocupar un porcentaje fijo de la pantalla
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    // Alto de una tarjeta de entrada, incluido su margen inferior
    private fun ticketItemHeight(): Int {
        val ticket = event.tickets.firstOrNull()
        val item = layoutInflater.inflate(R.layout.item_ticket, ticketsRecyclerView, false)

        item.findViewById<TextView>(R.id.ticket_name).text = ticket?.name.orEmpty()
        item.findViewById<TextView>(R.id.ticket_price).text = "%.2f€".format(ticket?.price ?: 0.0)

        val density = context.resources.displayMetrics.density
        // Descuenta el padding horizontal del RecyclerView para medir la tarjeta
        val width = context.resources.displayMetrics.widthPixels -
            ticketsRecyclerView.paddingLeft - ticketsRecyclerView.paddingRight

        item.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        // El margen de la tarjeta no se cuenta al medir, así que se suma aparte
        return item.measuredHeight + (8 * density).toInt()
    }

    // Suma el precio de las entradas seleccionadas
    private fun selectedTotal(): Double = event.tickets.sumOf { it.price * it.quantity }

    // Muestra el total seleccionado en el panel
    private fun updateTotalPrice() {
        totalPriceView.text = "%.2f€".format(selectedTotal())
    }
}
