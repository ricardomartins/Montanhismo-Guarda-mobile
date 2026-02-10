package pt.rikmartins.clubemg.mobile.data.bookmark

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveAllEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveAllFavouriteEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SetBookmarkOfEventId
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SynchronizeFavouriteEvents

internal class BookmarkRepository(
    private val dataStore: DataStore<Preferences>,
    private val logger: Logger = Logger.withTag(BookmarkRepository::class.simpleName!!)
) : SynchronizeFavouriteEvents.BookmarkProvider, SetBookmarkOfEventId.BookmarkProvider,
    ObserveAllEvents.BookmarkProvider, ObserveAllFavouriteEvents.BookmarkProvider {


    override suspend fun getAllBookmarkedEventsIds(): Collection<String> =
        dataStore.data.first()[BOOKMARKED_EVENT_IDS] ?: emptySet()

    override suspend fun addBookmark(eventId: String) {
        logger.v("Adding bookmark for event $eventId")
        dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[BOOKMARKED_EVENT_IDS] = (preferences[BOOKMARKED_EVENT_IDS] ?: emptySet()) + eventId
                logger.d("Bookmarks are now ${preferences[BOOKMARKED_EVENT_IDS]}, after adding $eventId")
            }
        }
        logger.v("Finished adding bookmark for event $eventId")
    }

    override suspend fun removeBookmark(eventId: String) {
        logger.v("Removing bookmark for event $eventId")
        dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[BOOKMARKED_EVENT_IDS] = (preferences[BOOKMARKED_EVENT_IDS] ?: emptySet()) - eventId
                logger.d("Bookmarks are now ${preferences[BOOKMARKED_EVENT_IDS]}, after removing $eventId")
            }
        }
        logger.v("Finished removing bookmark for event $eventId")
    }

    override val favouriteEventsIds: Flow<Collection<String>>
        get() = dataStore.data.map { it[BOOKMARKED_EVENT_IDS] ?: emptySet() }

    private companion object {
        val BOOKMARKED_EVENT_IDS = stringSetPreferencesKey("bookmarked_event_ids")
    }
}