package com.altea.altea_app.home

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.altea.altea_app.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

// Base común de los paneles inferiores: estilo y ajuste de altura de la lista
abstract class AppBottomSheetDialog(context: Context) : BottomSheetDialog(context) {

    companion object {
        // Número máximo de tarjetas visibles a la vez en los listados
        private const val MAX_VISIBLE_ITEMS = 3
    }

    // Aplica el estilo de la app y deja el panel expandido
    protected fun applyAppSheetStyle() {
        window?.setDimAmount(0.5f)

        val bottomSheet: View =
            findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)!!

        bottomSheet.setBackgroundResource(R.drawable.bg_bottom_sheet)
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    // Mide una tarjeta del listado, incluido su margen inferior de 8dp
    protected fun measureItemHeight(
        recyclerView: RecyclerView,
        itemLayout: Int,
        bind: (View) -> Unit = {}
    ): Int {
        val item = layoutInflater.inflate(itemLayout, recyclerView, false)
        bind(item)

        val density = context.resources.displayMetrics.density
        // Descuenta el padding horizontal del RecyclerView para medir la tarjeta
        val width = context.resources.displayMetrics.widthPixels -
                recyclerView.paddingLeft - recyclerView.paddingRight

        item.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        // El margen de la tarjeta no se cuenta al medir, así que se suma aparte
        return item.measuredHeight + (8 * density).toInt()
    }

    // Ajusta la lista para mostrar como máximo MAX_VISIBLE_ITEMS tarjetas
    protected fun fitListToItems(
        recyclerView: RecyclerView,
        itemHeight: Int,
        itemCount: Int
    ) {
        val params = recyclerView.layoutParams as LinearLayout.LayoutParams
        params.height = itemHeight * minOf(MAX_VISIBLE_ITEMS, itemCount) +
                recyclerView.paddingTop
        params.weight = 0f
        recyclerView.layoutParams = params
    }
}