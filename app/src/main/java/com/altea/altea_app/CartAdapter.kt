package com.altea.altea_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adaptador del carrito: muestra cada entrada seleccionada con su subtotal
class CartAdapter(
    private val dataSet: List<CartItem>
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    // Referencias a las vistas de una tarjeta del carrito
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val eventNameTextView: TextView
        val ticketNameTextView: TextView
        val unitPriceTextView: TextView
        val subtotalTextView: TextView

        init {
            // Referencias a las vistas de la tarjeta
            eventNameTextView = view.findViewById(R.id.cart_event_name)
            ticketNameTextView = view.findViewById(R.id.cart_ticket_name)
            unitPriceTextView = view.findViewById(R.id.cart_unit_price)
            subtotalTextView = view.findViewById(R.id.cart_subtotal)
        }
    }

    // Crea la vista de cada línea del carrito
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Crea la vista de la tarjeta con su layout
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_cart, viewGroup, false)

        return ViewHolder(view)
    }

    // Rellena cada tarjeta con el evento, la entrada y los precios
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Obtiene la línea del carrito de esta posición
        val item = dataSet[position]

        // Evento y tipo de entrada
        viewHolder.eventNameTextView.text = item.eventName
        viewHolder.ticketNameTextView.text = item.ticketName

        // Precio unitario por cantidad y subtotal de la línea
        viewHolder.unitPriceTextView.text = "%.2f€ x %d".format(item.price, item.quantity)
        viewHolder.subtotalTextView.text = "%.2f€".format(item.subtotal)
    }

    // Número de líneas del carrito
    override fun getItemCount() = dataSet.size

}
