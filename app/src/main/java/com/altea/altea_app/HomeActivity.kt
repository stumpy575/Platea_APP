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

        // Generar eventos de prueba: cada evento tiene sus propios tipos de entrada
        repeat(5) {
            events.addAll(buildShows())
        }

        eventAdapter = EventAdapter(events) { event ->
            TicketBottomSheetDialog(this, event).show()
        }
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.adapter = eventAdapter
    }

    private fun buildShows(): List<Event> = listOf(
        Event(
            "El Rey León, El Musical",
            "Hoy, 20:30h",
            24.00,
            "",
            "El musical de Disney llega al escenario con música en directo, marionetas gigantes y una puesta en escena inolvidable para toda la familia.",
            listOf(
                Ticket(
                    "Platea General",
                    "Butaca en la planta principal con visión completa del escenario.",
                    24.00
                ),
                Ticket(
                    "Entrada Promocional",
                    "Tarifa reducida para estudiantes y menores de 25 años. Aforo limitado.",
                    18.50
                ),
                Ticket(
                    "Butaca de Palco",
                    "Butaca lateral en palco con una vista elevada del escenario.",
                    39.00
                ),
                Ticket(
                    "VIP Gold Experience",
                    "Primera fila, acceso preferente y programa firmado por el reparto.",
                    59.90
                ),
                Ticket(
                    "Palco Privado para cuatro",
                    "Palco exclusivo para cuatro personas con aperitivo incluido.",
                    149.00
                )
            )
        ),
        Event(
            "Jukebox: Tributo a los 80",
            "Mañana, 21:00h",
            19.50,
            "",
            "Un recorrido por los grandes éxitos de los ochenta con banda en directo, pantalla led y una pista que no deja de bailar.",
            listOf(
                Ticket(
                    "Entrada General",
                    "Acceso a la sala y zona de pista sin asiento asignado.",
                    19.50
                ),
                Ticket(
                    "Pista Premium",
                    "Zona delantera junto al escenario con acceso prioritario.",
                    29.00
                ),
                Ticket(
                    "Mesa VIP para dos",
                    "Mesa reservada con dos consumiciones incluidas y servicio de sala.",
                    65.00
                )
            )
        ),
        Event(
            "Circo Estelar Nocturno",
            "Vie, 19:00h",
            15.00,
            "",
            "Acróbatas, malabares y magia bajo una carpa de luces. Un espectáculo pensado para disfrutar en familia.",
            listOf(
                Ticket(
                    "Grada Infantil",
                    "Entrada para menores de 12 años en grada lateral.",
                    9.90
                ),
                Ticket(
                    "Grada General",
                    "Asiento numerado con visión frontal de la pista central.",
                    15.00
                ),
                Ticket(
                    "Palco Familiar",
                    "Palco privado para cuatro personas con palomitas incluidas.",
                    49.00
                )
            )
        ),
        Event(
            "Noches de Cuerda: Bandas Sonoras",
            "Sáb, 22:15h",
            22.00,
            "",
            "Un cuarteto de cuerda interpreta las bandas sonoras más conocidas del cine a la luz de cientos de velas.",
            listOf(
                Ticket(
                    "Entrada General",
                    "Asiento libre en las filas centrales de la sala.",
                    22.00
                ),
                Ticket(
                    "Zona Preferente",
                    "Filas delanteras con la mejor acústica del recinto.",
                    34.00
                ),
                Ticket(
                    "VIP Gold Experience",
                    "Asiento preferente, copa de bienvenida y programa del concierto.",
                    54.00
                )
            )
        )
    )
}
