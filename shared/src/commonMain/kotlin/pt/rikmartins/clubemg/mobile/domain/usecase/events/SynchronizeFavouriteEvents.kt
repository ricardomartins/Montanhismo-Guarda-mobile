package pt.rikmartins.clubemg.mobile.domain.usecase.events

import co.touchlab.kermit.Logger
import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase

class SynchronizeFavouriteEvents(
    private val favouriteProvider: FavouriteProvider,
    private val eventsProvider: EventsProvider,
    private val notifier: Notifier,
    private val logger: Logger = Logger.withTag(SynchronizeFavouriteEvents::class.simpleName!!)
) : UseCase<Unit, Unit>() {

    override suspend fun execute(params: Unit) {
        logger.v { "Synchronizing favourite events" }
        val favouriteEventsIds = favouriteProvider.getAllFavouriteEventsIds()
        logger.d { "Favourite events are: $favouriteEventsIds" }
        val eventDiffs = eventsProvider.refreshEventsForDiff(favouriteEventsIds)
        logger.d { "Detected events changes: $eventDiffs" }
        if (eventDiffs.isNotEmpty()) {
            logger.v { "Notifying favourite events changes" }
            notifier.notifyFavouriteEventsChanged(eventDiffs)
        } else logger.v { "No notification of favourite events changes" }
    }

    interface FavouriteProvider {
        suspend fun getAllFavouriteEventsIds(): Collection<String>
    }

    interface EventsProvider {
        suspend fun refreshEventsForDiff(eventIds: Collection<String>): Collection<EventDiff>
    }

    interface Notifier {
        suspend fun notifyFavouriteEventsChanged(eventsDiffs: Collection<EventDiff>)
    }
}
