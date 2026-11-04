package com.altea.altea_app

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

// Panel del carrito con las entradas seleccionadas y el total
class CartBottomSheetDialog(
    context: Context,
    private val cartItems: List<CartItem>
) : BottomSheetDialog(context) {

    // Número de tarjetas del carrito que se ven a la vez en la lista
    private val visibleItems = 3

    private val totalPriceView: TextView
    private val cartRecyclerView: RecyclerView
    private val emptyMessageView: TextView

    // Carga la lista del carrito y calcula el total
    init {
        setContentView(R.layout.bottom_sheet_cart)

        totalPriceView = findViewById(R.id.cart_total_price)!!
        emptyMessageView = findViewById(R.id.cart_empty_message)!!
        cartRecyclerView = findViewById(R.id.cart_recycler_view)!!

        cartRecyclerView.adapter = CartAdapter(cartItems)

        // El total se calcula sumando los subtotales de cada línea del carrito
        totalPriceView.text = "%.2f€".format(cartItems.sumOf { it.subtotal })
        emptyMessageView.isVisible = cartItems.isEmpty()
        cartRecyclerView.isVisible = cartItems.isNotEmpty()

        findViewById<View>(R.id.close_cart_button)!!.setOnClickListener { dismiss() }
    }

    // Ajusta el panel al contenido y aplica el estilo de la app
    override fun onStart() {
        super.onStart()

        // Oscurece la cartelera que queda detrás del panel
        window?.setDimAmount(0.5f)

        val bottomSheet: View =
            findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)!!

        // Mismo fondo y esquinas redondeadas que el resto de la app
        bottomSheet.setBackgroundResource(R.drawable.bg_bottom_sheet)

        // La lista muestra como máximo tres entradas; si hay más, se consulta desplazando
        val listParams = cartRecyclerView.layoutParams as LinearLayout.LayoutParams
        listParams.height = cartItemHeight() * minOf(visibleItems, cartItems.size) +
            cartRecyclerView.paddingTop
        cartRecyclerView.layoutParams = listParams

        // El panel se ajusta a su contenido en lugar de ocupar un porcentaje fijo de la pantalla
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    // Alto de una tarjeta del carrito, incluido su margen inferior
    private fun cartItemHeight(): Int {
        val item = layoutInflater.inflate(R.layout.item_cart, cartRecyclerView, false)

        val density = context.resources.displayMetrics.density
        // Descuenta el padding horizontal del RecyclerView para medir la tarjeta
        val width = context.resources.displayMetrics.widthPixels -
            cartRecyclerView.paddingLeft - cartRecyclerView.paddingRight

        item.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        // El margen de la tarjeta no se cuenta al medir, así que se suma aparte
        return item.measuredHeight + (8 * density).toInt()
    }
}
