package com.altea.altea_app.data

import com.altea.altea_app.model.Event
import com.altea.altea_app.model.OwnedTicket
import com.altea.altea_app.model.PurchaseEntry
import com.altea.altea_app.model.TicketType
import org.json.JSONArray

// Un object es como un objecto que se instancia solo
// y del cual solo existe una unica instancia.
// Se utiliza para almacenar metodos utilitarios, como una clase utilitaria
// en java de metodos static. Es mas o menos un equivalente a eso.
object ServerMessageParser {

    // JSONArray de tickets -> Lista de objetos Ticket
    fun parseTickets(array: JSONArray, zone: String? = null): List<TicketType> {
        val result = mutableListOf<TicketType>()
        for (i in 0 until array.length()) {
            val json = array.optJSONObject(i) ?: continue
            // Se utiliza para testing, excluir zonas.
            // Como solo hay entradas generales de una zona hago la guarrindongada
            // de excluir la zona, porque no tenemos una peticion para pedir
            // los recintos y sus zonas y no quiero molestar al ivan xd
            if (zone != null && json.optString("zona") != zone) continue

            val id = json.optString("id")
            val name = json.optString("nom")
            val price = json.optDouble("preu", 0.0)
            if (id.isBlank() || name.isBlank()) continue

            result += TicketType(
                id = id,
                name = name,
                price = price
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
                name = name,
                date = json.optString("dataHora"),
                img = json.optString("imatge"),
                description = json.optString("descripcio")
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