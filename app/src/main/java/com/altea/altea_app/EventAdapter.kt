package com.altea.altea_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventAdapter(private val dataSet: List<Event>) :
    RecyclerView.Adapter<EventAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView
        val dateTextView: TextView
        val priceTextView: TextView

        init {
            // Define click listener for the ViewHolder's View
            titleTextView = view.findViewById(R.id.event_title)
            dateTextView = view.findViewById(R.id.event_date)
            priceTextView = view.findViewById(R.id.event_price)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_event, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val event = dataSet[position]

        viewHolder.titleTextView.text = event.name
        viewHolder.dateTextView.text = event.date
        viewHolder.priceTextView.text = "%.2f€".format(event.price)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}
