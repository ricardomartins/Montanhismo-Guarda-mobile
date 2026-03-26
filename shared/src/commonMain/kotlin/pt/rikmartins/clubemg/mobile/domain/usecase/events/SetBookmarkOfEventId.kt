package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase

class SetBookmarkOfEventId(
    private val bookmarkProvider: BookmarkProvider,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): UseCase.BiConsumer<String, Boolean>(dispatcher) {

    override suspend fun execute(param1: String, param2: Boolean) {
        if (param2) {
            bookmarkProvider.addBookmark(param1)
        } else {
            bookmarkProvider.removeBookmark(param1)
        }
    }

    interface BookmarkProvider {
        suspend fun addBookmark(eventId: String)
        suspend fun removeBookmark(eventId: String)
    }
}