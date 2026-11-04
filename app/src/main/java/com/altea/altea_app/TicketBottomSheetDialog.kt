package com.altea.altea_app

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class TicketBottomSheetDialog(context: Context, private val event: Event) :
    BottomSheetDialog(context) {

    // Número de tarjetas de entrada que se ven a la vez en la lista
    private val visibleTickets = 3

    private val totalPriceView: TextView
    private val ticketsRecyclerView: RecyclerView

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
        val ticketsAdapter = TicketAdapter(event.tickets) { updateTotalPrice() }

        ticketsRecyclerView = findViewById(R.id.tickets_recycler_view)!!
        ticketsRecyclerView.layoutManager = LinearLayoutManager(context)
        ticketsRecyclerView.adapter = ticketsAdapter

        updateTotalPrice()

        findViewById<View>(R.id.close_sheet_button)!!.setOnClickListener { dismiss() }

        findViewById<View>(R.id.add_to_cart_button)!!.setOnClickListener {
            val total = selectedTotal()
            val message = if (total <= 0.0) {
                context.getString(R.string.cart_empty)
            } else {
                context.getString(R.string.cart_added, "%.2f€".format(total))
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()

        // Oscurece la cartelera que queda detrás del panel
        window?.setDimAmount(0.5f)

        val bottomSheet: View =
            findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)!!

        // Mismo fondo y esquinas redondeadas que el resto de la app
        bottomSheet.setBackgroundResource(R.drawable.bg_bottom_sheet)

        // La lista deja ver como máximo tres tipos de entrada; el resto se consulta desplazando
        val listParams = ticketsRecyclerView.layoutParams as LinearLayout.LayoutParams
        listParams.height = ticketItemHeight() * visibleTickets
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
        // El RecyclerView tiene 20dp de padding a cada lado
        val width = context.resources.displayMetrics.widthPixels - (40 * density).toInt()

        item.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        // El margen de la tarjeta no se cuenta al medir, así que se suma aparte
        return item.measuredHeight + (8 * density).toInt()
    }

    private fun selectedTotal(): Double = event.tickets.sumOf { it.price * it.quantity }

    private fun updateTotalPrice() {
        totalPriceView.text = "%.2f€".format(selectedTotal())
    }
}
