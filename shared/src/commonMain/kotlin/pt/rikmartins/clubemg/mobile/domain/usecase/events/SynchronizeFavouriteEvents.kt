package pt.rikmartins.clubemg.mobile.domain.usecase.events

import co.touchlab.kermit.Logger
import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase

class SynchronizeFavouriteEvents(
    private val bookmarkProvider: BookmarkProvider,
    private val eventsProvider: EventsProvider,
    private val notifier: Notifier,
    private val logger: Logger = Logger.withTag(SynchronizeFavouriteEvents::class.simpleName!!)
) : UseCase.Action() {

    override suspend fun execute() {
        logger.v { "Synchronizing favourite events" }
        val favouriteEventsIds = bookmarkProvider.getAllBookmarkedEventsIds()
        logger.d { "Favourite events are: $favouriteEventsIds" }
        val eventDiffs = eventsProvider.refreshEventsForDiff(favouriteEventsIds)
        logger.d { "Detected events changes: $eventDiffs" }
        if (eventDiffs.isNotEmpty()) {
            logger.v { "Notifying favourite events changes" }

            eventDiffs.forEach { eventDiff ->
                // Relevant
                if (false) { // TODO: Add event cancellation logic
                    notifier.notifySingleEventCanceled(eventDiff)
                } else {
                    var haveNotified = false

                    if (false) { // TODO: Add event postpone logic
                        notifier.notifySingleEventPostponed(eventDiff)
                        haveNotified = true
                    }
                    if (eventDiff.startDateHasChanged() || eventDiff.endDateHasChanged()) {
                        notifier.notifySingleEventRescheduled(eventDiff)
                        haveNotified = true
                    }

                    if (eventDiff.oldEvent.enrollmentUrl.isEmpty() && eventDiff.newEvent.enrollmentUrl.isNotEmpty()) {
                        notifier.notifySingleEventEnrollmentStarted(eventDiff)
                        haveNotified = true
                    }

                    // Irrelevant
                    if (eventDiff.titleHasChanged()) {
                        notifier.notifySingleEventRenamed(eventDiff)
                        haveNotified = true
                    }
                    if (eventDiff.modifiedDateHasChanged() && !haveNotified)
                        notifier.notifySingleEventOtherChanges(eventDiff)
                }

            }
        } else logger.v { "No notification of bookmarked events changes" }
    }

    private fun EventDiff.startDateHasChanged(): Boolean = oldEvent.startDate != newEvent.startDate
    private fun EventDiff.endDateHasChanged(): Boolean = oldEvent.endDate != newEvent.endDate
    private fun EventDiff.modifiedDateHasChanged(): Boolean = oldEvent.modifiedDate != newEvent.modifiedDate
    private fun EventDiff.titleHasChanged(): Boolean = oldEvent.title != newEvent.title

    interface BookmarkProvider {
        suspend fun getAllBookmarkedEventsIds(): Collection<String>
    }

    interface EventsProvider {
        suspend fun refreshEventsForDiff(eventIds: Collection<String>): Collection<EventDiff>
    }

    interface Notifier {
        // Relevant
        suspend fun notifySingleEventCanceled(eventDiff: EventDiff)
        suspend fun notifySingleEventPostponed(eventDiff: EventDiff)
        suspend fun notifySingleEventRescheduled(eventDiff: EventDiff)
        suspend fun notifySingleEventEnrollmentStarted(eventDiff: EventDiff)

        // Irrelevant
        suspend fun notifySingleEventRenamed(eventDiff: EventDiff)
        suspend fun notifySingleEventOtherChanges(eventDiff: EventDiff)
    }
}
