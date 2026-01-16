package pt.rikmartins.clubemg.mobile.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveAllEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SetBookmarkOfEventId
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SynchronizeFavouriteEvents

internal class BookmarkRepository(
    private val dataStore: DataStore<Preferences>,
) : SynchronizeFavouriteEvents.BookmarkProvider, SetBookmarkOfEventId.BookmarkProvider,
    ObserveAllEvents.BookmarkProvider {


    override suspend fun getAllBookmarkedEventsIds(): Collection<String> =
        dataStore.data.first()[BOOKMARKED_EVENT_IDS] ?: emptySet()

    override suspend fun addBookmark(eventId: String) {
        dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[BOOKMARKED_EVENT_IDS] = (preferences[BOOKMARKED_EVENT_IDS] ?: emptySet()) + eventId
            }
        }
    }

    override suspend fun removeBookmark(eventId: String) {
        dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[BOOKMARKED_EVENT_IDS] = (preferences[BOOKMARKED_EVENT_IDS] ?: emptySet()) - eventId
            }
        }
    }

    override val favouriteEventsIds: Flow<Collection<String>>
        get() = dataStore.data.map { it[BOOKMARKED_EVENT_IDS] ?: emptySet() }

    private companion object {
        val BOOKMARKED_EVENT_IDS = stringSetPreferencesKey("bookmarked_event_ids")
    }
}