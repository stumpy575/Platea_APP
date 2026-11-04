package com.altea.altea_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TicketAdapter(
    private val dataSet: List<Ticket>,
    private val onQuantityChange: () -> Unit
) : RecyclerView.Adapter<TicketAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView
        val priceTextView: TextView
        val quantityTextView: TextView
        val minusButton: View
        val plusButton: View

        init {
            nameTextView = view.findViewById(R.id.ticket_name)
            priceTextView = view.findViewById(R.id.ticket_price)
            quantityTextView = view.findViewById(R.id.quantity_value)
            minusButton = view.findViewById(R.id.quantity_minus)
            plusButton = view.findViewById(R.id.quantity_plus)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_ticket, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val ticket = dataSet[position]

        viewHolder.nameTextView.text = ticket.name
        viewHolder.priceTextView.text = "%.2f€".format(ticket.price)
        viewHolder.quantityTextView.text = ticket.quantity.toString()

        viewHolder.minusButton.setOnClickListener {
            // La cantidad nunca puede ser negativa
            if (ticket.quantity > 0) {
                ticket.quantity--
                viewHolder.quantityTextView.text = ticket.quantity.toString()
                onQuantityChange()
            }
        }

        viewHolder.plusButton.setOnClickListener {
            ticket.quantity++
            viewHolder.quantityTextView.text = ticket.quantity.toString()
            onQuantityChange()
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}
