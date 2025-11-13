package pt.rikmartins.clubemg.mobile.domain.usecase

// Typealias for use cases that don't require an input parameter.
typealias NoRequest = Unit

// Helper function to pass arguments without transformation.
internal fun <T> itFun(it: T): T = it