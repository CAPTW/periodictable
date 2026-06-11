package com.chemtable.interactive.feature.minigame

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.internal.MainDispatcherFactory
import kotlin.coroutines.CoroutineContext

@OptIn(InternalCoroutinesApi::class)
class TestMainDispatcherFactory : MainDispatcherFactory {
    override val loadPriority: Int = Int.MAX_VALUE

    override fun createDispatcher(allFactories: List<MainDispatcherFactory>): MainCoroutineDispatcher {
        return TestMainDispatcher
    }
}

@OptIn(InternalCoroutinesApi::class)
object TestMainDispatcher : MainCoroutineDispatcher() {
    override val immediate: MainCoroutineDispatcher = this
    
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        block.run()
    }
    
    override fun isDispatchNeeded(context: CoroutineContext): Boolean = false
}
