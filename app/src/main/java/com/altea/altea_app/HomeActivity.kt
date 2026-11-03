package com.altea.altea_app

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView

class HomeActivity : AppCompatActivity() {

    private val events = ArrayList<Event>()

    private lateinit var eventAdapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inflar la cartelera dentro del contenedor principal
        val contentContainer: FrameLayout = findViewById(R.id.content_container)
        layoutInflater.inflate(R.layout.view_listings, contentContainer, true)

        // Generar eventos de prueba
        repeat(20) {
            val event = Event(
                "El Rey León, El Musical",
                "Hoy, 20:30h",
                24.00,
                ""
            )
            events.add(event)
        }

        eventAdapter = EventAdapter(events)
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.adapter = eventAdapter
    }
}