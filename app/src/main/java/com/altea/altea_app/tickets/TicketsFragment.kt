package com.altea.altea_app.tickets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.altea.altea_app.R
import com.altea.altea_app.model.MockTickets

// Pantalla "Mis Entradas": lista las entradas compradas por el usuario
class TicketsFragment : Fragment() {

    // Infla el layout de la pantalla
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_tickets, container, false)
    }

    // Rellena la lista cuando la vista ya existe
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.tickets_recycler_view)
        recyclerView.adapter = OwnedTicketAdapter(MockTickets.create())
    }
}