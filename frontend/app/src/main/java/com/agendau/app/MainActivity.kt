package com.agendau.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.agendau.app.ui.CalendarioFragment
import com.agendau.app.ui.InicioFragment
import com.agendau.app.ui.MateriasFragment
import com.agendau.app.ui.TareasFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // La app siempre se muestra en español, sin importar el idioma del celular
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es-CO"))
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val menu = findViewById<BottomNavigationView>(R.id.bottomNav)

        if (savedInstanceState == null) {
            mostrar(InicioFragment())
        }

        menu.setOnItemSelectedListener { opcion ->
            when (opcion.itemId) {
                R.id.nav_inicio -> mostrar(InicioFragment())
                R.id.nav_tareas -> mostrar(TareasFragment())
                R.id.nav_calendario -> mostrar(CalendarioFragment())
                R.id.nav_materias -> mostrar(MateriasFragment())
            }
            true
        }
    }

    private fun mostrar(fragmento: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor, fragmento)
            .commit()
    }
}
