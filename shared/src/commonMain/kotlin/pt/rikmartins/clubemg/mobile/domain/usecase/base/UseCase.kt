package pt.rikmartins.clubemg.mobile.domain.usecase.base

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Base class for a UseCase that performs a suspendable operation.
 *
 * @param P The type of the parameter (Input). Use [Unit] if no input is required.
 * @param R The type of the result (Output). Use [Unit] if no output is returned.
 */
abstract class UseCase<in P, out R>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    protected abstract suspend fun execute(params: P): R

    suspend operator fun invoke(params: P): R = withContext(dispatcher) { execute(params) }
}
