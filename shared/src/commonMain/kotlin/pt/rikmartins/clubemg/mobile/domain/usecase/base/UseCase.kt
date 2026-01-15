package pt.rikmartins.clubemg.mobile.domain.usecase.base

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Base class for a UseCase that performs a suspendable operation.
 */
sealed class UseCase(protected val dispatcher: CoroutineDispatcher) {

    abstract class Action(dispatcher: CoroutineDispatcher = Dispatchers.Default) : UseCase(dispatcher) {

        protected abstract suspend fun execute()

        suspend operator fun invoke() = withContext(dispatcher) { execute() }
    }

    abstract class Consumer<in P>(dispatcher: CoroutineDispatcher = Dispatchers.Default) : UseCase(dispatcher) {

        protected abstract suspend fun execute(param: P)

        suspend operator fun invoke(param: P) = withContext(dispatcher) { execute(param) }
    }

    abstract class BiConsumer<in P1, in P2>(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ) : UseCase(dispatcher) {

        protected abstract suspend fun execute(param1: P1, param2: P2)

        suspend operator fun invoke(param1: P1, param2: P2) =
            withContext(dispatcher) { execute(param1, param2) }
    }

    abstract class Supplier<out R>(dispatcher: CoroutineDispatcher = Dispatchers.Default) : UseCase(dispatcher) {

        protected abstract suspend fun execute(): R

        suspend operator fun invoke(): R = withContext(dispatcher) { execute() }
    }

    abstract class Function<in P, out R>(dispatcher: CoroutineDispatcher = Dispatchers.Default) : UseCase(dispatcher) {

        protected abstract suspend fun execute(param: P): R

        suspend operator fun invoke(param: P): R = withContext(dispatcher) { execute(param) }
    }

    abstract class BiFunction<in P1, in P2, out R>(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ) : UseCase(dispatcher) {

        protected abstract suspend fun execute(param1: P1, param2: P2): R

        suspend operator fun invoke(param1: P1, param2: P2): R =
            withContext(dispatcher) { execute(param1, param2) }
    }
}
