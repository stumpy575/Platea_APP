package com.altea.altea_app

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Prueba instrumentada de ejemplo que se ejecuta en un dispositivo Android.
 *
 * Consulta la [documentación de pruebas](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Contexto de la aplicación que se está probando
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.altea.altea_app", appContext.packageName)
    }
}