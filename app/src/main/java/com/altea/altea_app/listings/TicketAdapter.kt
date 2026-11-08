package com.altea.altea_app.listings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.altea.altea_app.R
import com.altea.altea_app.model.TicketType

// Adaptador del panel de entradas: muestra los tipos de entrada de un evento
class TicketAdapter(
    private val dataSet: List<TicketType>,
    private val onQuantityChange: () -> Unit
) : RecyclerView.Adapter<TicketAdapter.ViewHolder>() {

    // Referencias a las vistas de una tarjeta de entrada
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView
        val priceTextView: TextView
        val quantityTextView: TextView
        val minusButton: View
        val plusButton: View

        init {
            // Referencias a las vistas de la tarjeta
            nameTextView = view.findViewById(R.id.ticket_name)
            priceTextView = view.findViewById(R.id.ticket_price)
            quantityTextView = view.findViewById(R.id.quantity_value)
            minusButton = view.findViewById(R.id.quantity_minus)
            plusButton = view.findViewById(R.id.quantity_plus)
        }
    }

    // Crea la vista de cada tipo de entrada de la lista
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Crea la vista de la tarjeta con su layout
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_ticket, viewGroup, false)

        return ViewHolder(view)
    }

    // Rellena cada tarjeta y prepara los botones de cantidad
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Obtiene el tipo de entrada de esta posición de la lista
        val ticket = dataSet[position]

        // Nombre, precio y cantidad seleccionada
        viewHolder.nameTextView.text = ticket.name
        viewHolder.priceTextView.text = "%.2f€".format(ticket.price)
        viewHolder.quantityTextView.text = ticket.quantity.toString()

        // Al pulsar - se quita una entrada y se actualiza el carrito
        viewHolder.minusButton.setOnClickListener {
            // La cantidad nunca puede ser negativa
            if (ticket.quantity > 0) {
                ticket.quantity--
                viewHolder.quantityTextView.text = ticket.quantity.toString()
                onQuantityChange()
            }
        }

        // Al pulsar + se añade una entrada y se actualiza el carrito
        viewHolder.plusButton.setOnClickListener {
            ticket.quantity++
            viewHolder.quantityTextView.text = ticket.quantity.toString()
            onQuantityChange()
        }
    }

    // Número de tipos de entrada del evento
    override fun getItemCount() = dataSet.size

}
