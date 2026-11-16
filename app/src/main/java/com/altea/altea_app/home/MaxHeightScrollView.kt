package com.altea.altea_app.home

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

// ScrollView con una altura máxima. Se usa en los diálogos para que un texto
// muy largo pueda desplazarse sin empujar los botones fuera de la pantalla.
class MaxHeightScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    // Altura máxima en píxeles; 0 significa sin límite
    var maxHeight: Int = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val cappedSpec = if (maxHeight > 0) {
            MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        } else {
            heightMeasureSpec
        }

        super.onMeasure(widthMeasureSpec, cappedSpec)
    }
}
