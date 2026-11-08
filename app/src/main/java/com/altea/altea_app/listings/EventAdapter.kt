package com.altea.altea_app.listings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.altea.altea_app.model.Event
import com.altea.altea_app.R

// Adaptador de la cartelera: muestra cada evento de la lista
class EventAdapter(
    private val dataSet: MutableList<Event>,
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.ViewHolder>() {

    // Referencias a las vistas de una tarjeta de la cartelera
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView
        val dateTextView: TextView
        val priceTextView: TextView

        init {
            // Referencias a las vistas de la tarjeta
            titleTextView = view.findViewById(R.id.event_title)
            dateTextView = view.findViewById(R.id.event_date)
            priceTextView = view.findViewById(R.id.event_price)
        }
    }

    // Crea la vista de cada evento de la lista
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Crea la vista de la tarjeta con su layout
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_event, viewGroup, false)

        return ViewHolder(view)
    }

    // Rellena cada tarjeta con los datos del evento
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Obtiene el evento de esta posición de la lista
        val event = dataSet[position]

        // Nombre, fecha y precio "Desde" del evento
        viewHolder.titleTextView.text = event.name
        viewHolder.dateTextView.text = event.date
        viewHolder.priceTextView.text = "%.2f€".format(event.minPrice)

        // Al pulsar la tarjeta se abre el panel de entradas del evento
        viewHolder.itemView.setOnClickListener {
            onEventClick(event)
        }
    }

    fun updateEvents(newEvents: List<Event>) {
        dataSet.clear()
        dataSet.addAll(newEvents)
        notifyDataSetChanged()
    }

    // Número de eventos de la cartelera
    override fun getItemCount() = dataSet.size

}
