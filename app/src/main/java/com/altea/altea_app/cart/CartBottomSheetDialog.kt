package com.altea.altea_app.cart

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.altea.altea_app.model.CartItem
import com.altea.altea_app.R
import com.altea.altea_app.home.AppBottomSheetDialog
import com.altea.altea_app.home.AppDialog

// Panel del carrito con las entradas seleccionadas y el total
class CartBottomSheetDialog(
    context: Context,
    cartItems: List<CartItem>,
    private val onQuantityChange: (CartItem, Int) -> Unit,
    private val onRemoveItem: (CartItem) -> Unit,
    private val onCheckout: () -> Unit
) : AppBottomSheetDialog(context) {

    // Copia mutable de las líneas: el panel la actualiza al cambiar cantidades o quitar entradas
    private val items = cartItems.toMutableList()

    private val totalPriceView: TextView
    private val emptyMessageView: TextView
    private val cartRecyclerView: RecyclerView
    private val checkoutButton: View
    private val cartAdapter: CartAdapter

    // Alto de una tarjeta, para ajustar la lista cuando cambia el número de líneas
    private var itemHeight = 0

    // Carga la lista del carrito y calcula el total
    init {
        setContentView(R.layout.bottom_sheet_cart)

        totalPriceView = findViewById(R.id.cart_total_price)!!
        emptyMessageView = findViewById(R.id.cart_empty_message)!!
        cartRecyclerView = findViewById(R.id.cart_recycler_view)!!
        checkoutButton = findViewById(R.id.cart_checkout_button)!!

        cartAdapter = CartAdapter(
            items,
            onQuantityChange = { item, quantity ->
                // Bajar la cantidad a 0 pide confirmación antes de quitar la entrada
                if (quantity <= 0) {
                    showRemoveConfirmation(item)
                } else {
                    updateQuantity(item, quantity)
                }
            },
            onRemove = { item -> showRemoveConfirmation(item) }
        )
        cartRecyclerView.adapter = cartAdapter

        refreshCart()

        findViewById<View>(R.id.close_cart_button)!!.setOnClickListener { dismiss() }

        // El botón de compra solo está activo si hay entradas en el carrito
        checkoutButton.setOnClickListener {
            dismiss()
            onCheckout()
        }
    }

    // Cambia la cantidad de una línea general y actualiza el carrito y sus totales
    private fun updateQuantity(item: CartItem, quantity: Int) {
        val index = indexOf(item)
        if (index < 0) return

        items[index] = item.copy(quantity = quantity)
        cartAdapter.notifyItemChanged(index)
        onQuantityChange(item, quantity)
        refreshCart()
    }

    // Pide confirmación antes de quitar una entrada de la compra
    private fun showRemoveConfirmation(item: CartItem) {
        val description = item.seatLabel?.let { seat ->
            context.getString(R.string.ticket_with_seat, item.ticketName, seat)
        } ?: item.ticketName

        AppDialog(
            context,
            title = context.getString(R.string.remove_ticket_title),
            message = context.getString(R.string.remove_ticket_message, description),
            primaryText = context.getString(R.string.remove_ticket_action),
            secondaryText = context.getString(R.string.cancel),
            destructive = true,
            onPrimary = { removeItem(item) }
        ).show()
    }

    // Quita la línea del panel y de la compra
    private fun removeItem(item: CartItem) {
        val index = indexOf(item)
        if (index < 0) return

        items.removeAt(index)
        cartAdapter.notifyItemRemoved(index)
        onRemoveItem(item)
        refreshCart()
    }

    // Localiza una línea por sus datos de compra, sin depender de la cantidad mostrada
    private fun indexOf(item: CartItem): Int {
        return items.indexOfFirst {
            it.eventoId == item.eventoId &&
                it.tipoEntradaId == item.tipoEntradaId &&
                it.asientoId == item.asientoId
        }
    }

    // Actualiza los totales, el mensaje de carrito vacío y el botón de compra
    private fun refreshCart() {
        totalPriceView.text = "%.2f€".format(items.sumOf { it.subtotal })
        emptyMessageView.isVisible = items.isEmpty()
        cartRecyclerView.isVisible = items.isNotEmpty()

        checkoutButton.isEnabled = items.isNotEmpty()
        checkoutButton.alpha = if (items.isNotEmpty()) 1f else 0.5f

        // La lista se encoge al quitar líneas
        if (itemHeight > 0) {
            fitListToItems(cartRecyclerView, itemHeight, items.size)
        }
    }

    // Ajusta el panel al contenido y aplica el estilo de la app
    override fun onStart() {
        super.onStart()
        applyAppSheetStyle()
        itemHeight = measureItemHeight(cartRecyclerView, R.layout.item_cart)
        fitListToItems(cartRecyclerView, itemHeight, items.size)
    }
}
