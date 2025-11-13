package pt.rikmartins.clubemg.mobile.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Base class for a UseCase that returns a [Flow] for observing data changes.
 *
 * @param RQ1 The type of the first request.
 * @param RQ2 The type of the second request.
 * @param RSP The type of the response flow's emissions.
 */
sealed class WatchCase<in RQ1, in RQ2, out RSP> {

    @Throws(Throwable::class)
    abstract fun execute(request1: RQ1, request2: RQ2): Flow<RSP>

    @Throws(Throwable::class)
    operator fun <RQ1_DT, RQ2_DT, RSP_DT> invoke(
        request1Dto: RQ1_DT,
        request2Dto: RQ2_DT,
        request1Converter: (RQ1_DT) -> RQ1,
        request2Converter: (RQ2_DT) -> RQ2,
        responseConverter: (RSP) -> RSP_DT
    ): Flow<RSP_DT> {
        val request1 = request1Converter(request1Dto)
        val request2 = request2Converter(request2Dto)
        return execute(request1, request2).map { responseConverter(it) }
    }

    /**
     * An operation that supplies a flow of values without any input.
     * Renamed from FlowSupplier for consistency.
     */
    abstract class Supplier<out RSP> : WatchCase<NoRequest, NoRequest, RSP>() {

        @Throws(Throwable::class)
        abstract fun get(): Flow<RSP>

        final override fun execute(request1: NoRequest, request2: NoRequest): Flow<RSP> = get()

        @Throws(Throwable::class)
        operator fun invoke(): Flow<RSP> =
            super.invoke(Unit, Unit, ::itFun, ::itFun, ::itFun)

        @Throws(Throwable::class)
        operator fun <RSP_DT> invoke(responseConverter: (RSP) -> RSP_DT): Flow<RSP_DT> =
            super.invoke(Unit, Unit, ::itFun, ::itFun, responseConverter)
    }

    /**
     * A function that takes one input and returns a flow of values.
     * Renamed from FlowFunction for consistency.
     */
    abstract class Function<in RQ, out RSP> : WatchCase<RQ, NoRequest, RSP>() {

        @Throws(Throwable::class)
        abstract fun call(request: RQ): Flow<RSP>

        final override fun execute(request1: RQ, request2: NoRequest): Flow<RSP> = call(request1)

        @Throws(Throwable::class)
        operator fun invoke(request: RQ): Flow<RSP> =
            super.invoke(request, Unit, ::itFun, ::itFun, ::itFun)

        @Throws(Throwable::class)
        operator fun <RQ_DT, RSP_DT> invoke(
            requestDto: RQ_DT,
            requestConverter: (RQ_DT) -> RQ,
            responseConverter: (RSP) -> RSP_DT
        ): Flow<RSP_DT> = super.invoke(requestDto, Unit, requestConverter, ::itFun, responseConverter)
    }

    /**
     * A function that takes two inputs and returns a flow of values.
     * Renamed from FlowBiFunction for consistency.
     */
    abstract class BiFunction<in RQ1, in RQ2, out RSP> : WatchCase<RQ1, RQ2, RSP>() {

        @Throws(Throwable::class)
        abstract fun call(request1: RQ1, request2: RQ2): Flow<RSP>

        final override fun execute(request1: RQ1, request2: RQ2): Flow<RSP> = call(request1, request2)

        @Throws(Throwable::class)
        operator fun invoke(request1: RQ1, request2: RQ2): Flow<RSP> =
            super.invoke(request1, request2, ::itFun, ::itFun, ::itFun)
    }
}
