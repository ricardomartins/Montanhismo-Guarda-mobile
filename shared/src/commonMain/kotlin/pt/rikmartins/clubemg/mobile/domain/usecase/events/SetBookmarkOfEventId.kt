package pt.rikmartins.clubemg.mobile.domain.usecase.events

import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase

class SetBookmarkOfEventId(private val bookmarkProvider: BookmarkProvider): UseCase.BiConsumer<String, Boolean>() {

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