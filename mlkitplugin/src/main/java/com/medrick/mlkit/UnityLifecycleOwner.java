package com.medrick.mlkit;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

/**
 * A custom implementation of LifecycleOwner for Unity
 * since UnityPlayerActivity doesn't implement this interface.
 */
public class UnityLifecycleOwner implements LifecycleOwner {
    private final LifecycleRegistry lifecycleRegistry;

    public UnityLifecycleOwner() {
        lifecycleRegistry = new LifecycleRegistry(this);
        // Initialize the lifecycle in RESUMED state since the Unity activity is already running
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    /**
     * Call this method when shutting down to properly clean up resources
     */
    public void destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
    }
}