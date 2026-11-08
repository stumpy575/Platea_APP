package com.altea.altea_app.listings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.altea.altea_app.R
import com.altea.altea_app.home.HomeActivity
import com.altea.altea_app.model.Event

// Pantalla de cartelera: muestra los eventos que llegan del servidor
class ListingsFragment : Fragment() {

    // El adaptador se crea con el fragmento y se conserva entre vistas.
    // Así los eventos pueden llegar incluso antes de que la vista esté lista.
    private val eventAdapter = EventAdapter(mutableListOf()) { event ->
        TicketBottomSheetDialog(requireContext(), event) {
            (activity as? HomeActivity)?.updateCartBadge()
        }.show()
    }

    // Infla el layout de la cartelera
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_listings, container, false)
    }

    // Conecta la lista cuando la vista ya existe
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.listings_recycler_view)
        recyclerView.adapter = eventAdapter
    }

    // Llamado por HomeActivity cuando el servidor termina de cargar los eventos
    fun updateEvents(events: List<Event>) {
        eventAdapter.updateEvents(events)
    }
}