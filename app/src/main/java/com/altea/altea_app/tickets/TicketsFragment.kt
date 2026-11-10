package com.altea.altea_app.tickets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.altea.altea_app.R
import com.altea.altea_app.model.OwnedTicket

// Pantalla "Mis Entradas": lista las entradas compradas por el usuario
class TicketsFragment : Fragment() {

    // El adaptador se crea con el fragmento y se conserva entre vistas.
    // Así las entradas pueden llegar antes de que la vista esté lista.
    private val ownedTicketAdapter = OwnedTicketAdapter(mutableListOf()) { ticket ->
        TicketCodeDialog(requireContext(), ticket).show()
    }

    private lateinit var emptyMessage: TextView

    // Infla el layout de la pantalla
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_tickets, container, false)
    }

    // Conecta la lista cuando la vista ya existe
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.tickets_recycler_view)
        recyclerView.adapter = ownedTicketAdapter
        emptyMessage = view.findViewById(R.id.tickets_empty_message)
        emptyMessage.isVisible = ownedTicketAdapter.itemCount == 0
    }

    // Llamado por HomeActivity cuando el servidor devuelve las entradas
    fun updateTickets(tickets: List<OwnedTicket>) {
        ownedTicketAdapter.updateTickets(tickets)
        // Si la respuesta llega antes de que exista la vista, el adaptador
        // guarda los datos y onViewCreated pinta el estado correcto.
        if (view != null) {
            emptyMessage.isVisible = tickets.isEmpty()
        }
    }
}