package com.altea.altea_app.home

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.altea.altea_app.model.CartItem
import com.altea.altea_app.R

// Panel del carrito con las entradas seleccionadas y el total
class CartBottomSheetDialog(
    context: Context,
    private val cartItems: List<CartItem>,
    private val onCheckout: () -> Unit
) : AppBottomSheetDialog(context) {

    private val cartRecyclerView: RecyclerView

    // Carga la lista del carrito y calcula el total
    init {
        setContentView(R.layout.bottom_sheet_cart)

        val totalPriceView: TextView = findViewById(R.id.cart_total_price)!!
        val emptyMessageView: TextView = findViewById(R.id.cart_empty_message)!!
        cartRecyclerView = findViewById(R.id.cart_recycler_view)!!

        cartRecyclerView.adapter = CartAdapter(cartItems)

        // El total se calcula sumando los subtotales de cada línea del carrito
        totalPriceView.text = "%.2f€".format(cartItems.sumOf { it.subtotal })
        emptyMessageView.isVisible = cartItems.isEmpty()
        cartRecyclerView.isVisible = cartItems.isNotEmpty()

        findViewById<View>(R.id.close_cart_button)!!.setOnClickListener { dismiss() }

        // El botón de compra solo está activo si hay entradas en el carrito
        val checkoutButton = findViewById<View>(R.id.cart_checkout_button)!!
        checkoutButton.isEnabled = cartItems.isNotEmpty()
        checkoutButton.alpha = if (cartItems.isNotEmpty()) 1f else 0.5f
        checkoutButton.setOnClickListener {
            dismiss()
            onCheckout()
        }
    }

    // Ajusta el panel al contenido y aplica el estilo de la app
    override fun onStart() {
        super.onStart()
        applyAppSheetStyle()
        fitListToItems(
            cartRecyclerView,
            measureItemHeight(cartRecyclerView, R.layout.item_cart),
            cartItems.size
        )
    }
}
