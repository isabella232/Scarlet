/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle.android

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.ShutdownReason
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

internal class ApplicationResumedLifecycle(
    application: Application,
    private val lifecycleRegistry: LifecycleRegistry
) : Lifecycle by lifecycleRegistry {
    private val job = SupervisorJob()
    private var scope = CoroutineScope(job + Dispatchers.IO)

    init {
        application.registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks())
    }

    private inner class ActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
        var isResumed: Boolean = false
        var job: Job? = null

        override fun onActivityPaused(activity: Activity) {
            isResumed = false

            job = scope.launch {
                supervisorScope {
                    delay(30000)
                    if (!isResumed) {
                        lifecycleRegistry.onNext(Lifecycle.State.Stopped.WithReason(ShutdownReason(1000, "App is paused")))
                        job = null
                    }
                }
            }
        }

        override fun onActivityResumed(activity: Activity) {
            isResumed = true

            if (job?.isActive == true) {
                job?.cancel()
                job = null
            }

            lifecycleRegistry.onNext(Lifecycle.State.Started)
        }

        override fun onActivityStarted(activity: Activity) {}

        override fun onActivityDestroyed(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    }
}
