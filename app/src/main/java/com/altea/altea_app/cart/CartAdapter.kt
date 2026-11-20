package com.altea.altea_app.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.altea.altea_app.R
import com.altea.altea_app.model.CartItem

// Adaptador del carrito: muestra cada entrada seleccionada y sus controles
class CartAdapter(
    private val dataSet: List<CartItem>,
    private val onQuantityChange: (CartItem, Int) -> Unit,
    private val onRemove: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    // Referencias a las vistas de una tarjeta del carrito
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val eventNameTextView: TextView
        val ticketNameTextView: TextView
        val unitPriceTextView: TextView
        val quantitySelector: View
        val quantityTextView: TextView
        val minusButton: View
        val plusButton: View
        val removeButton: View

        init {
            // Referencias a las vistas de la tarjeta
            eventNameTextView = view.findViewById(R.id.cart_event_name)
            ticketNameTextView = view.findViewById(R.id.cart_ticket_name)
            unitPriceTextView = view.findViewById(R.id.cart_unit_price)
            quantitySelector = view.findViewById(R.id.cart_quantity_selector)
            quantityTextView = view.findViewById(R.id.cart_quantity_value)
            minusButton = view.findViewById(R.id.cart_quantity_minus)
            plusButton = view.findViewById(R.id.cart_quantity_plus)
            removeButton = view.findViewById(R.id.cart_remove_button)
        }
    }

    // Crea la vista de cada línea del carrito
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Crea la vista de la tarjeta con su layout
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_cart, viewGroup, false)

        return ViewHolder(view)
    }

    // Rellena cada tarjeta con el evento, la entrada y sus controles
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Obtiene la línea del carrito de esta posición
        val item = dataSet[position]

        // Evento y tipo de entrada (con la butaca si es una entrada numerada)
        viewHolder.eventNameTextView.text = item.eventName
        viewHolder.ticketNameTextView.text = item.seatLabel?.let { seat ->
            viewHolder.itemView.context.getString(
                R.string.ticket_with_seat,
                item.ticketName,
                seat
            )
        } ?: item.ticketName

        viewHolder.unitPriceTextView.text = "%.2f€".format(item.price)

        // Las butacas se quitan con la papelera; las generales, con el selector de cantidad
        val isNumbered = item.asientoId != null
        viewHolder.quantitySelector.visibility = if (isNumbered) View.GONE else View.VISIBLE
        viewHolder.removeButton.visibility = if (isNumbered) View.VISIBLE else View.GONE
        viewHolder.quantityTextView.text = item.quantity.toString()

        // Al pulsar - se quita una entrada (bajar a 0 pide confirmación)
        viewHolder.minusButton.setOnClickListener {
            onQuantityChange(item, item.quantity - 1)
        }

        // Al pulsar + se añade una entrada
        viewHolder.plusButton.setOnClickListener {
            onQuantityChange(item, item.quantity + 1)
        }

        // La papelera quita la butaca de la compra
        viewHolder.removeButton.setOnClickListener {
            onRemove(item)
        }
    }

    // Número de líneas del carrito
    override fun getItemCount() = dataSet.size

}
