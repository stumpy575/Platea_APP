package com.altea.altea_app.listings

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.altea.altea_app.model.Event
import com.altea.altea_app.R
import com.altea.altea_app.home.AppBottomSheetDialog
import com.altea.altea_app.home.HomeActivity
import com.altea.altea_app.seats.SeatSelectorActivity

// Panel de selección de entradas de un evento
class TicketBottomSheetDialog(
    context: Context,
    private val event: Event,
    private val onCartChanged: () -> Unit
) : AppBottomSheetDialog(context) {

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
        val ticketsAdapter = TicketAdapter(
            dataSet = event.tickets,
            venueName = event.venue?.name.orEmpty(),
            zones = event.zones,
            onQuantityChange = {
                updateTotalPrice()
                onCartChanged()
            },
            onSelectSeat = { ticket ->
                val address = (context as? HomeActivity)
                    ?.intent
                    ?.getStringExtra(HomeActivity.EXTRA_SERVER_ADDRESS)

                if (address != null) {
                    context.startActivity(
                        Intent(context, SeatSelectorActivity::class.java)
                            .putExtra(SeatSelectorActivity.EXTRA_SERVER_ADDRESS, address)
                            .putExtra(SeatSelectorActivity.EXTRA_EVENT_ID, event.id)
                            .putExtra(SeatSelectorActivity.EXTRA_TICKET_ID, ticket.id)
                    )
                }
            }
        )

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
        applyAppSheetStyle()
        fitListToItems(
            ticketsRecyclerView,
            measureItemHeight(ticketsRecyclerView, R.layout.item_ticket) { item ->
                val ticket = event.tickets.firstOrNull()
                if (ticket != null) {
                    item.findViewById<TextView>(R.id.ticket_name).text = ticket.name
                    item.findViewById<TextView>(R.id.ticket_price).text =
                        "%.2f€".format(ticket.price)
                }
            },
            event.tickets.size
        )
    }

    // Suma el precio de las entradas seleccionadas
    private fun selectedTotal(): Double = event.tickets.sumOf { it.price * it.quantity }

    // Muestra el total seleccionado en el panel
    private fun updateTotalPrice() {
        totalPriceView.text = "%.2f€".format(selectedTotal())
    }
}
