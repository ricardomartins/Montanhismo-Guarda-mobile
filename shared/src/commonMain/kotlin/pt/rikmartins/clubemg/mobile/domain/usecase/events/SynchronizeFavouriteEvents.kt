package pt.rikmartins.clubemg.mobile.domain.usecase.events

import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase

class SynchronizeFavouriteEvents(
    private val favouriteProvider: FavouriteProvider,
    private val eventsProvider: EventsProvider,
    private val notifier: Notifier,
) : UseCase<Unit, Unit>() {

    override suspend fun execute(params: Unit) {
        val favouriteEventsIds = favouriteProvider.getAllFavouriteEventsIds()
        val eventDiffs = eventsProvider.refreshEventsForDiff(favouriteEventsIds)
        if (eventDiffs.isNotEmpty()) notifier.notifyFavouriteEventsChanged(eventDiffs)
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
