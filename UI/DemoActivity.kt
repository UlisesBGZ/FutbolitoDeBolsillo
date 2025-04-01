package dev.ricknout.composesensors.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import dev.ricknout.composesensors.demo.model.Demo
import dev.ricknout.composesensors.demo.ui.DemoNavigation
import dev.ricknout.composesensors.demo.ui.DemoTheme

class DemoActivity : ComponentActivity() {

    private var demo by mutableStateOf(Demo.NONE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        demo = Demo.values()[savedInstanceState?.getInt(KEY_DEMO) ?: 0]
        setContent {
            DemoTheme {
                DemoNavigation(
                    demo = demo,
                    onItemClick = { demo = it },
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_DEMO, demo.ordinal)
    }

    companion object {
        private const val KEY_DEMO = "demo"
    }
}
