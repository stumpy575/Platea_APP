package com.altea.altea_app.seats

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.os.ResultReceiver
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.altea.altea_app.R
import com.altea.altea_app.data.ServerClient
import com.altea.altea_app.model.Event
import com.altea.altea_app.model.OwnedTicket
import com.altea.altea_app.model.PurchaseEntry
import com.altea.altea_app.model.Seat

// Pantalla de selección de asiento: dibuja los asientos que devuelve el servidor
class SeatSelectorActivity : AppCompatActivity() {

    private var serverClient: ServerClient? = null

    private lateinit var totalPriceView: TextView
    private lateinit var confirmButton: View
    private var ticketPrice = 0.0
    private val selectedSeats = mutableListOf<Seat>()
    private var initialSelectedSeats: List<String> = emptyList()
    private var blockedSeats: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_seat_selector)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.seat_selector_back_button).setOnClickListener {
            saveSelectionAndFinish()
        }

        // Guardar también la selección si se sale con el gesto o botón atrás
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveSelectionAndFinish()
            }
        })

        val seatsGrid: GridLayout = findViewById(R.id.seats_grid)
        val rowLabels: LinearLayout = findViewById(R.id.seats_row_labels)
        totalPriceView = findViewById(R.id.seat_total_price)
        confirmButton = findViewById(R.id.confirm_selection_button)
        confirmButton.setOnClickListener { saveSelectionAndFinish() }

        updateFooter()

        val address = intent.getStringExtra(EXTRA_SERVER_ADDRESS)
        val eventoId = intent.getStringExtra(EXTRA_EVENT_ID)
        val tipoEntradaId = intent.getStringExtra(EXTRA_TICKET_ID)
        ticketPrice = intent.getDoubleExtra(EXTRA_TICKET_PRICE, 0.0)
        initialSelectedSeats = intent.getStringArrayListExtra(EXTRA_SELECTED_SEATS).orEmpty()
        blockedSeats = intent.getStringArrayListExtra(EXTRA_BLOCKED_SEATS).orEmpty()

        if (address == null || eventoId == null || tipoEntradaId == null) {
            return
        }

        loadSeats(address, eventoId, tipoEntradaId, seatsGrid, rowLabels)
    }

    override fun onDestroy() {
        serverClient?.close()
        serverClient = null
        super.onDestroy()
    }

    // Conecta con el servidor y solicita los asientos del evento y tipo de entrada
    private fun loadSeats(
        address: String,
        eventoId: String,
        tipoEntradaId: String,
        seatsGrid: GridLayout,
        rowLabels: LinearLayout
    ) {
        serverClient = ServerClient(
            address = address,
            userMail = "",
            listener = object : ServerClient.Listener {

                override fun onEventsLoaded(events: List<Event>) = Unit

                override fun onOwnedTicketsLoaded(tickets: List<OwnedTicket>) = Unit

                override fun onPurchaseSuccess(entries: List<PurchaseEntry>) = Unit

                override fun onPurchaseError(message: String) = Unit

                override fun onConnectionError(message: String) = Unit

                override fun onConnectionClosed() = Unit

                override fun onSeatsLoaded(
                    seats: List<Seat>,
                    rows: Int,
                    seatsPerRow: Int
                ) {
                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread

                        drawSeats(seatsGrid, rowLabels, seats, rows, seatsPerRow)
                    }
                }
            },
            autoLoadCatalog = false
        )

        serverClient?.requestSeats(eventoId, tipoEntradaId)
        serverClient?.connect()
    }

    // Dibuja los asientos como cuadrados blancos en una cuadrícula
    private fun drawSeats(
        seatsGrid: GridLayout,
        rowLabels: LinearLayout,
        seats: List<Seat>,
        rows: Int,
        seatsPerRow: Int
    ) {
        seatsGrid.removeAllViews()
        rowLabels.removeAllViews()
        selectedSeats.clear()

        if (seatsPerRow <= 0) return

        val rowCount = if (rows > 0) rows else seats.maxOfOrNull { it.row } ?: 0

        seatsGrid.columnCount = seatsPerRow
        if (rowCount > 0) {
            seatsGrid.rowCount = rowCount
        }

        val density = resources.displayMetrics.density
        val seatWidth = (28 * density).toInt()
        val seatHeight = (34 * density).toInt()
        val seatGap = (4 * density).toInt()
        val rowGap = (8 * density).toInt()

        // Etiqueta de cada fila en el espacio izquierdo
        for (row in 1..rowCount) {
            val rowLabel = TextView(this).apply {
                text = "F$row"
                gravity = Gravity.CENTER
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }

            rowLabel.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                seatHeight + rowGap * 2
            )
            rowLabels.addView(rowLabel)
        }

        for (seat in seats) {
            if (seat.row <= 0 || seat.number <= 0) continue

            val params = GridLayout.LayoutParams(
                GridLayout.spec(seat.row - 1),
                GridLayout.spec(seat.number - 1)
            )
            params.width = seatWidth
            params.height = seatHeight
            params.setMargins(seatGap, rowGap, seatGap, rowGap)

            val seatView = TextView(this).apply {
                text = seat.number.toString()
                gravity = Gravity.CENTER
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
            seatView.setBackgroundResource(R.drawable.bg_seat)
            seatView.layoutParams = params

            val seatLabel = Seat.label(seat.row, seat.number)

            if (!seat.isAvailable) {
                // Las butacas ya vendidas se muestran, pero no se pueden elegir
                seatView.isEnabled = false
                seatView.alpha = 0.35f
            } else if (blockedSeats.contains(seatLabel)) {
                // Ya elegida para otro tipo de la misma zona: se ve seleccionada y bloqueada
                showBlockedSeat(seatView)
            } else {
                seatView.setOnClickListener {
                    setSeatSelected(seatView, seat, !seatView.isSelected)
                }

                if (initialSelectedSeats.contains(seatLabel)) {
                    setSeatSelected(seatView, seat, true)
                }
            }

            seatsGrid.addView(seatView)
        }

        updateFooter()
    }

    // Muestra una butaca elegida para otro tipo de entrada de la misma zona:
    // se dibuja como seleccionada y se le pone un filtro gris encima
    private fun showBlockedSeat(seatView: TextView) {
        seatView.isSelected = true
        seatView.setBackgroundResource(R.drawable.bg_seat_selected)
        seatView.setTextColor(ContextCompat.getColor(this, R.color.white))
        seatView.foreground = ContextCompat.getDrawable(this, R.drawable.bg_seat_blocked)
    }

    // Cambia el aspecto de un asiento según esté seleccionado o no
    private fun setSeatSelected(seatView: TextView, seat: Seat, selected: Boolean) {
        if (seatView.isSelected == selected) return

        seatView.isSelected = selected

        if (selected) {
            seatView.setBackgroundResource(R.drawable.bg_seat_selected)
            seatView.setTextColor(ContextCompat.getColor(this, R.color.white))
            selectedSeats.add(seat)
        } else {
            seatView.setBackgroundResource(R.drawable.bg_seat)
            seatView.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            selectedSeats.remove(seat)
        }

        updateFooter()
    }

    // Actualiza el total y el estado del botón de confirmación
    private fun updateFooter() {
        totalPriceView.text = "%.2f€".format(ticketPrice * selectedSeats.size)

        val enabled = selectedSeats.isNotEmpty()
        confirmButton.isEnabled = enabled
        confirmButton.alpha = if (enabled) 1f else 0.5f
    }

    // Devuelve los asientos seleccionados (etiquetas e ids) a la pantalla de entradas
    private fun saveSelectionAndFinish() {
        val sortedSeats = selectedSeats.sortedWith(compareBy({ it.row }, { it.number }))

        val resultReceiver = intent.getParcelableExtra(EXTRA_RESULT_RECEIVER) as? ResultReceiver
        resultReceiver?.send(
            Activity.RESULT_OK,
            Bundle().apply {
                putStringArrayList(
                    EXTRA_SELECTED_SEATS,
                    ArrayList(sortedSeats.map { Seat.label(it.row, it.number) })
                )
                putIntegerArrayList(
                    EXTRA_SELECTED_SEAT_IDS,
                    ArrayList(sortedSeats.map { it.id })
                )
            }
        )

        finish()
    }

    companion object {
        const val EXTRA_SERVER_ADDRESS = "com.altea.altea_app.SEAT_SERVER_ADDRESS"
        const val EXTRA_EVENT_ID = "com.altea.altea_app.SEAT_EVENT_ID"
        const val EXTRA_TICKET_ID = "com.altea.altea_app.SEAT_TICKET_ID"
        const val EXTRA_TICKET_PRICE = "com.altea.altea_app.SEAT_TICKET_PRICE"
        const val EXTRA_RESULT_RECEIVER = "com.altea.altea_app.SEAT_RESULT_RECEIVER"
        const val EXTRA_SELECTED_SEATS = "com.altea.altea_app.SEAT_SELECTED_SEATS"
        const val EXTRA_SELECTED_SEAT_IDS = "com.altea.altea_app.SEAT_SELECTED_SEAT_IDS"
        const val EXTRA_BLOCKED_SEATS = "com.altea.altea_app.SEAT_BLOCKED_SEATS"
    }
}
