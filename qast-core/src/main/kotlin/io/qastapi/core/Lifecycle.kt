package io.qastapi.core

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

enum class LifecycleState {
    UNINITIALIZED,
    INITIALIZING,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED
}

class ApplicationLifecycle {
    private val stateRef = AtomicReference(LifecycleState.UNINITIALIZED)
    private val onStartHooks = CopyOnWriteArrayList<suspend () -> Unit>()
    private val onStopHooks = CopyOnWriteArrayList<suspend () -> Unit>()

    val state: LifecycleState
        get() = stateRef.get()

    fun onStart(hook: suspend () -> Unit) {
        onStartHooks.add(hook)
    }

    fun onStop(hook: suspend () -> Unit) {
        onStopHooks.add(hook)
    }

    suspend fun start(action: suspend () -> Unit) {
        if (!stateRef.compareAndSet(LifecycleState.UNINITIALIZED, LifecycleState.INITIALIZING) &&
            !stateRef.compareAndSet(LifecycleState.STOPPED, LifecycleState.INITIALIZING)
        ) {
            throw IllegalStateException("Cannot start application from state: $state")
        }

        stateRef.set(LifecycleState.STARTING)
        for (hook in onStartHooks) {
            hook()
        }

        action()
        stateRef.set(LifecycleState.RUNNING)
    }

    suspend fun stop(action: suspend () -> Unit) {
        if (!stateRef.compareAndSet(LifecycleState.RUNNING, LifecycleState.STOPPING)) {
            return
        }

        for (hook in onStopHooks) {
            try {
                hook()
            } catch (e: Exception) {
                // Log and continue cleanup
            }
        }

        try {
            action()
        } finally {
            stateRef.set(LifecycleState.STOPPED)
        }
    }
}
