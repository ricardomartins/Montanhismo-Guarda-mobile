package pt.rikmartins.clubemg.mobile.domain.usecase.events

import co.touchlab.kermit.Logger
import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase
import kotlin.time.Clock

class SynchronizeFavouriteEvents(
    private val bookmarkProvider: BookmarkProvider,
    private val eventsProvider: EventsProvider,
    private val notifier: Notifier,
    private val clock: Clock = Clock.System,
    private val logger: Logger = Logger.withTag(SynchronizeFavouriteEvents::class.simpleName!!)
) : UseCase.Action() {

    override suspend fun execute() {
        logger.v { "Synchronizing favourite events" }

        val favouriteEventsIds = bookmarkProvider.getAllBookmarkedEventsIds()
        logger.d { "Favourite events are: $favouriteEventsIds" }

        val eventDiffs = eventsProvider.refreshEventsForDiff(favouriteEventsIds)
        logger.v { "Detected ${eventDiffs.size} events changes" }

        val now = clock.now()
        val (diffsOfStaleEvent, diffsOfFreshEvents) = eventDiffs.partition { it.newEvent.endDate < now }

        logger.i { "Events $diffsOfStaleEvent are stale, and will be removed from the bookmarks" }
        diffsOfStaleEvent.forEach { staleDiff -> bookmarkProvider.removeBookmark(staleDiff.oldEvent.id) }

        logger.d { "${diffsOfFreshEvents.size} events are still fresh, and will be processed" }
        if (diffsOfFreshEvents.isNotEmpty()) diffsOfFreshEvents.filter { it.modifiedDateHasChanged() }
            .forEach { eventDiff ->
                // Relevant
                if (eventDiff.wasCanceled()) {
                    logger.v { "Notifying of cancelled event: ${eventDiff.oldEvent}" }
                    notifier.notifySingleEventCanceled(eventDiff)
                } else {
                    var haveNotified = false

                    if (eventDiff.wasPostponed()) {
                        logger.v { "Notifying of postponed event: ${eventDiff.oldEvent}" }
                        notifier.notifySingleEventPostponed(eventDiff)
                        haveNotified = true
                    }
                    if (eventDiff.startDateHasChanged() || eventDiff.endDateHasChanged()) {
                        logger.v { "Notifying of rescheduled event: ${eventDiff.oldEvent}" }
                        notifier.notifySingleEventRescheduled(eventDiff)
                        haveNotified = true
                    }
                    if (eventDiff.oldEvent.enrollmentUrl.isEmpty() && eventDiff.newEvent.enrollmentUrl.isNotEmpty()) {
                        logger.v { "Notifying of enrollment having started for event: ${eventDiff.oldEvent}" }
                        notifier.notifySingleEventEnrollmentStarted(eventDiff)
                        haveNotified = true
                    }

                    // Irrelevant
                    if (eventDiff.titleHasChanged()) {
                        logger.v { "Notifying of renamed event: ${eventDiff.oldEvent}" }
                        notifier.notifySingleEventRenamed(eventDiff)
                        haveNotified = true
                    }
                    if (!haveNotified) {
                        logger.v { "Notifying of general changes to event: ${eventDiff.oldEvent}" }
                        notifier.notifySingleEventOtherChanges(eventDiff)
                    }
                }
            } else logger.v { "No notification of bookmarked events changes" }
    }

    private fun EventDiff.wasCanceled(): Boolean = oldEvent.eventStatusType != EventStatusType.Cancelled &&
            oldEvent.eventStatusType != null && newEvent.eventStatusType == EventStatusType.Cancelled
    private fun EventDiff.wasPostponed(): Boolean = oldEvent.eventStatusType != EventStatusType.Rescheduled &&
            oldEvent.eventStatusType != null && newEvent.eventStatusType == EventStatusType.Rescheduled
    private fun EventDiff.startDateHasChanged(): Boolean = oldEvent.startDate != newEvent.startDate
    private fun EventDiff.endDateHasChanged(): Boolean = oldEvent.endDate != newEvent.endDate
    private fun EventDiff.modifiedDateHasChanged(): Boolean = oldEvent.modifiedDate != newEvent.modifiedDate
    private fun EventDiff.titleHasChanged(): Boolean = oldEvent.title != newEvent.title

    interface BookmarkProvider {
        suspend fun getAllBookmarkedEventsIds(): Collection<String>
        suspend fun removeBookmark(eventId: String)
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
