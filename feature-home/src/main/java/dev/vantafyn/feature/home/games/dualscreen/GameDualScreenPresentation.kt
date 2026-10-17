package dev.vantafyn.feature.home.games.dualscreen

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.flow.StateFlow

/**
 * Android Presentation displaying the Vantafyn 3DS-style Top Screen on a secondary display.
 *
 * Implements [LifecycleOwner], [SavedStateRegistryOwner], and [ViewModelStoreOwner] so that
 * embedded Jetpack Compose views properly bind to the presentation window without throwing
 * IllegalStateException ("ViewTreeLifecycleOwner not found").
 */
class GameDualScreenPresentation(
    context: Context,
    display: Display,
    private val contentState: StateFlow<DualScreenTopContent>,
) : Presentation(context, display), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val appViewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedStateRegistryController.performRestore(savedInstanceState)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        setCancelable(false)
        setCanceledOnTouchOutside(false)

        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        val composeView = ComposeView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                val state by contentState.collectAsState()
                GameDualScreenTopScreen(state = state)
            }
        }

        // Attach owners to composeView and decorView before and after attaching to window
        bindViewTreeOwners(composeView)
        setContentView(composeView)

        window?.decorView?.let { decor ->
            bindViewTreeOwners(decor)
        }
    }

    override fun onBackPressed() {
        // Presentation is an ambient secondary display screen and should not be dismissed on back press
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        // Never consume key events so input stays on the primary display/Activity
        return false
    }

    override fun onStart() {
        super.onStart()
        if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }
    }

    override fun onStop() {
        if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }
        super.onStop()
    }

    override fun dismiss() {
        try {
            super.dismiss()
        } catch (_: Exception) {}
        if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            try {
                lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
                appViewModelStore.clear()
            } catch (_: Exception) {}
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            try {
                lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
                appViewModelStore.clear()
            } catch (_: Exception) {}
        }
    }

    override fun onSaveInstanceState(): Bundle {
        val bundle = super.onSaveInstanceState()
        savedStateRegistryController.performSave(bundle)
        return bundle
    }

    private fun bindViewTreeOwners(view: View) {
        // 1. ViewTreeLifecycleOwner
        try {
            val clazz = Class.forName("androidx.lifecycle.ViewTreeLifecycleOwner")
            val method = clazz.getMethod("set", View::class.java, LifecycleOwner::class.java)
            method.invoke(null, view, this)
        } catch (_: Throwable) {
            try {
                val rClass = Class.forName("androidx.lifecycle.runtime.R\$id")
                val idField = rClass.getField("view_tree_lifecycle_owner")
                val id = idField.getInt(null)
                view.setTag(id, this)
            } catch (_: Throwable) {}
        }

        // 2. ViewTreeSavedStateRegistryOwner
        try {
            val clazz = Class.forName("androidx.savedstate.ViewTreeSavedStateRegistryOwner")
            val method = clazz.getMethod("set", View::class.java, SavedStateRegistryOwner::class.java)
            method.invoke(null, view, this)
        } catch (_: Throwable) {
            try {
                val rClass = Class.forName("androidx.savedstate.R\$id")
                val idField = rClass.getField("view_tree_saved_state_registry_owner")
                val id = idField.getInt(null)
                view.setTag(id, this)
            } catch (_: Throwable) {}
        }

        // 3. ViewTreeViewModelStoreOwner
        try {
            val clazz = Class.forName("androidx.lifecycle.ViewTreeViewModelStoreOwner")
            val method = clazz.getMethod("set", View::class.java, ViewModelStoreOwner::class.java)
            method.invoke(null, view, this)
        } catch (_: Throwable) {
            try {
                val rClass = Class.forName("androidx.lifecycle.viewmodel.R\$id")
                val idField = rClass.getField("view_tree_view_model_store_owner")
                val id = idField.getInt(null)
                view.setTag(id, this)
            } catch (_: Throwable) {}
        }
    }
}
