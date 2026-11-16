package com.altea.altea_app.data

import com.altea.altea_app.model.Event
import com.altea.altea_app.model.OwnedTicket
import com.altea.altea_app.model.PurchaseEntry
import com.altea.altea_app.model.Seat
import com.altea.altea_app.model.Venue
import com.altea.altea_app.model.TicketType
import com.altea.altea_app.model.Zone
import org.json.JSONArray

// Un object es como un objecto que se instancia solo
// y del cual solo existe una unica instancia.
// Se utiliza para almacenar metodos utilitarios, como una clase utilitaria
// en java de metodos static. Es mas o menos un equivalente a eso.
object ServerMessageParser {

    // JSONArray de tickets -> Lista de objetos Ticket
    fun parseTickets(array: JSONArray): List<TicketType> {
        val result = mutableListOf<TicketType>()
        for (i in 0 until array.length()) {
            val json = array.optJSONObject(i) ?: continue

            val id = json.optString("id")
            val name = json.optString("nom")
            val price = json.optDouble("preu", 0.0)
            val zone = json.optString("zona")
            if (id.isBlank() || name.isBlank()) continue

            result += TicketType(
                id = id,
                name = name,
                price = price,
                zone = zone
            )
        }
        return result
    }

    // JSONArray de eventos -> Lista de objetos Event
    fun parseEvents(array: JSONArray): List<Event> {
        val result = mutableListOf<Event>()
        for (i in 0 until array.length()) {
            val json = array.optJSONObject(i) ?: continue
            val id = json.optString("id")
            val name = json.optString("nom")
            if (id.isBlank() || name.isBlank()) continue
            result += Event(
                id = id,
                venueId = json.optString("recinte"),
                name = name,
                date = json.optString("dataHora"),
                img = json.optString("imatge"),
                description = json.optString("descripcio")
            )
        }
        return result
    }

    // JSONArray de venues -> Lista de objetos Venue con sus zonas
    fun parseVenues(array: JSONArray): List<Venue> {
        val result = mutableListOf<Venue>()

        for (i in 0 until array.length()) {
            val json = array.optJSONObject(i) ?: continue

            val id = json.optString("id")
            val name = json.optString("nombre")
            if (id.isBlank() || name.isBlank()) continue

            val zones = mutableListOf<Zone>()
            val zonesArray = json.optJSONArray("zonas") ?: JSONArray()

            for (j in 0 until zonesArray.length()) {
                val zoneJson = zonesArray.optJSONObject(j) ?: continue

                val zoneId = zoneJson.optString("id")
                val zoneName = zoneJson.optString("nombre")
                val type = zoneJson.optString("tipo")
                if (zoneId.isBlank() || zoneName.isBlank()) continue

                // Las zonas generales no tienen filas ni asientos por fila
                val rows = zoneJson.optInt("filas", 0).takeIf { it > 0 }
                val seatsPerRow = zoneJson.optInt("asientosPorFila", 0).takeIf { it > 0 }

                zones += Zone(
                    id = zoneId,
                    name = zoneName,
                    type = type,
                    rows = rows,
                    seatsPerRow = seatsPerRow
                )
            }

            result += Venue(
                id = id,
                name = name,
                zones = zones
            )
        }

        return result
    }

    // JSONArray de asientos -> Lista de objetos Seat
    fun parseSeats(array: JSONArray): List<Seat> {
        val result = mutableListOf<Seat>()

        for (i in 0 until array.length()) {
            val json = array.optJSONObject(i) ?: continue

            val id = json.optInt("id", 0)
            val row = json.optInt("fila", 0)
            val number = json.optInt("numero", 0)
            if (id <= 0 || row <= 0 || number <= 0) continue

            result += Seat(
                id = id,
                row = row,
                number = number,
                state = json.optString("estado")
            )
        }

        return result
    }

    fun parsePurchaseEntries(array: JSONArray): List<PurchaseEntry> {
        val result = mutableListOf<PurchaseEntry>()
        for (i in 0 until array.length()) {
            val json = array.optJSONObject(i) ?: continue
            val eventoId = json.optString("eventoId")
            val tipoEntradaId = json.optString("tipoEntradaId")
            val localizador = json.optString("localizador")
            if (localizador.isEmpty()) continue
            result += PurchaseEntry(
                eventoId = eventoId,
                tipoEntradaId = tipoEntradaId,
                localizador = localizador
            )
        }
        return result
    }

    // JSONArray de entradas compradas -> Lista de objetos OwnedTicket
    fun parseOwnedTickets(array: JSONArray): List<OwnedTicket> {
        val result = mutableListOf<OwnedTicket>()

        for (i in 0 until array.length()) {
            val json = array.optJSONObject(i) ?: continue

            val locator = json.optString("localizador")
            val eventName = json.optString("eventoNom")

            // Sin localizador o sin nombre no hay tarjeta que mostrar
            if (locator.isBlank() || eventName.isBlank()) continue

            result += OwnedTicket(
                locator = locator,
                eventName = eventName,
                eventDate = json.optString("dataHora"),
                ticketName = json.optString("tipoEntradaNom")
            )
        }

        return result
    }

}