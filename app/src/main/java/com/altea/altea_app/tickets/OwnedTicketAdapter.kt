package com.altea.altea_app.tickets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.altea.altea_app.R
import com.altea.altea_app.model.OwnedTicket

// Adaptador de "Mis Entradas": muestra cada entrada comprada
class OwnedTicketAdapter(
    private val dataSet: List<OwnedTicket>,
    private val onCodeClick: (OwnedTicket) -> Unit
) : RecyclerView.Adapter<OwnedTicketAdapter.ViewHolder>() {

    // Referencias a las vistas de una tarjeta de entrada comprada
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.event_title)
        val dateTextView: TextView = view.findViewById(R.id.event_date)
        val typeTextView: TextView = view.findViewById(R.id.ticket_type)
        val codeButton: View = view.findViewById(R.id.ticket_code_button)
    }

    // Crea la vista de cada entrada
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_owned_ticket, parent, false)

        return ViewHolder(view)
    }

    // Rellena la tarjeta con los datos de la entrada
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val ticket = dataSet[position]

        viewHolder.titleTextView.text = ticket.eventName
        viewHolder.dateTextView.text = ticket.eventDate
        viewHolder.typeTextView.text = ticket.ticketName

        viewHolder.codeButton.setOnClickListener {
            onCodeClick(ticket)
        }
    }

    // Número de entradas compradas
    override fun getItemCount() = dataSet.size
}