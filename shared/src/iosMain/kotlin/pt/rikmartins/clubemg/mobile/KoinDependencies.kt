package pt.rikmartins.clubemg.mobile

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pt.rikmartins.clubemg.mobile.data.MuseumRepository

class KoinDependencies : KoinComponent {
    val museumRepository: MuseumRepository by inject()
}