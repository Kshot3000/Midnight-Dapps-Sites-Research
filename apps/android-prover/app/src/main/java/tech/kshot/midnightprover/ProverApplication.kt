package tech.kshot.midnightprover

import android.app.Application

/** Holds the app-scoped prove engine so jobs outlive any one screen. */
open class ProverApplication : Application() {
    lateinit var store: ProverStore
        private set
    lateinit var engine: ProveEngine
        private set

    override fun onCreate() {
        super.onCreate()
        store = ProverStore(this)
        engine = ProveEngine(store)
        ProveService.createChannel(this)
    }
}
