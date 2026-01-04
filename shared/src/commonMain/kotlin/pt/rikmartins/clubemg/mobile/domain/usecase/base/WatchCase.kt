package pt.rikmartins.clubemg.mobile.domain.usecase.base

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

/**
 * Base class for a UseCase that returns a [Flow] for observing data changes.
 *
 * @param R The type of the response flow's emissions.
 */
sealed class WatchCase<out R>(
    protected val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    abstract class Supplier<out R>(dispatcher: CoroutineDispatcher = Dispatchers.Default) : WatchCase<R>(dispatcher) {

        protected abstract fun execute(): Flow<R>

        operator fun invoke(): Flow<R> = execute().flowOn(dispatcher)
    }

    abstract class Function<in P, out R>(dispatcher: CoroutineDispatcher = Dispatchers.Default) : WatchCase<R>(dispatcher) {
        protected abstract fun execute(param: P): Flow<R>

        operator fun invoke(param: P): Flow<R> = execute(param).flowOn(dispatcher)
    }

    abstract class BiFunction<in P1, in P2, out R>(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ) : WatchCase<R>(dispatcher) {

        protected abstract fun execute(param1: P1, param2: P2): Flow<R>

        operator fun invoke(param1: P1, param2: P2): Flow<R> = execute(param1, param2).flowOn(dispatcher)
    }
}

