package com.altea.altea_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CartAdapter(
    private val dataSet: List<CartItem>
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val eventNameTextView: TextView
        val ticketNameTextView: TextView
        val unitPriceTextView: TextView
        val subtotalTextView: TextView

        init {
            eventNameTextView = view.findViewById(R.id.cart_event_name)
            ticketNameTextView = view.findViewById(R.id.cart_ticket_name)
            unitPriceTextView = view.findViewById(R.id.cart_unit_price)
            subtotalTextView = view.findViewById(R.id.cart_subtotal)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_cart, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = dataSet[position]

        viewHolder.eventNameTextView.text = item.eventName
        viewHolder.ticketNameTextView.text = item.ticketName
        viewHolder.unitPriceTextView.text = "%.2f€ x %d".format(item.price, item.quantity)
        viewHolder.subtotalTextView.text = "%.2f€".format(item.subtotal)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}
