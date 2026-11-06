package com.altea.altea_app

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

}