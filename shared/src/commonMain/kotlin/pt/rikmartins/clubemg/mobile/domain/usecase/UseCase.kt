package pt.rikmartins.clubemg.mobile.domain.usecase

/**
 * Base class for a UseCase.
 * This class is designed to be the single source of truth for a business logic operation.
 *
 * @param RQ1 The type of the first request.
 * @param RQ2 The type of the second request.
 * @param RSP The type of the response.
 */
sealed class UseCase<in RQ1, in RQ2, out RSP> {

    // Abstract method to be implemented by subclasses with the core business logic.
    @Throws(Throwable::class)
    abstract suspend fun execute(request1: RQ1, request2: RQ2): RSP

    // Generic invoke operator to handle data transfer object (DTO) conversions.
    @Throws(Throwable::class)
    suspend operator fun <RQ1_DT, RQ2_DT, RSP_DT> invoke(
        firstRequestDto: RQ1_DT,
        secondRequestDto: RQ2_DT,
        firstRequestConverter: (RQ1_DT) -> RQ1,
        secondRequestConverter: (RQ2_DT) -> RQ2,
        responseConverter: (RSP) -> RSP_DT
    ): RSP_DT {
        val request1 = firstRequestConverter(firstRequestDto)
        val request2 = secondRequestConverter(secondRequestDto)
        val response = execute(request1, request2)
        return responseConverter(response)
    }


    // Represents an action that takes no input and returns no value.
    abstract class Action : UseCase<NoRequest, NoRequest, Unit>() {
        @Throws(Throwable::class)
        abstract suspend fun run()

        final override suspend fun execute(request1: NoRequest, request2: NoRequest) = run()

        @Throws(Throwable::class)
        suspend operator fun invoke() = execute(Unit, Unit)
    }

    // Represents an operation that consumes one input and returns no value.
    abstract class Consumer<in RQ> : UseCase<RQ, NoRequest, Unit>() {
        @Throws(Throwable::class)
        abstract suspend fun accept(request: RQ)

        final override suspend fun execute(request1: RQ, request2: NoRequest) = accept(request1)

        @Throws(Throwable::class)
        suspend operator fun invoke(request: RQ) = execute(request, Unit)
    }

    // Represents an operation that supplies a value without any input.
    abstract class Supplier<out RSP> : UseCase<NoRequest, NoRequest, RSP>() {
        @Throws(Throwable::class)
        abstract suspend fun get(): RSP

        final override suspend fun execute(request1: NoRequest, request2: NoRequest): RSP = get()

        @Throws(Throwable::class)
        suspend operator fun invoke(): RSP = super.invoke(Unit, Unit, ::itFun, ::itFun, ::itFun)

        @Throws(Throwable::class)
        suspend operator fun <RSP_DT> invoke(responseConverter: (RSP) -> RSP_DT): RSP_DT =
            super.invoke(Unit, Unit, ::itFun, ::itFun, responseConverter)
    }

    // Represents a function that takes one input and returns a value.
    abstract class Function<in RQ, out RSP> : UseCase<RQ, NoRequest, RSP>() {
        @Throws(Throwable::class)
        abstract suspend fun call(request: RQ): RSP

        final override suspend fun execute(request1: RQ, request2: NoRequest): RSP = call(request1)

        @Throws(Throwable::class)
        suspend operator fun invoke(request: RQ): RSP = super.invoke(request, Unit, ::itFun, ::itFun, ::itFun)
    }

    // Represents a function that takes two inputs and returns a value.
    abstract class BiFunction<in RQ1, in RQ2, out RSP> : UseCase<RQ1, RQ2, RSP>() {
        @Throws(Throwable::class)
        abstract suspend fun call(request1: RQ1, request2: RQ2): RSP

        final override suspend fun execute(request1: RQ1, request2: RQ2): RSP = call(request1, request2)

        @Throws(Throwable::class)
        suspend operator fun invoke(request1: RQ1, request2: RQ2): RSP =
            super.invoke(request1, request2, ::itFun, ::itFun, ::itFun)
    }
}
