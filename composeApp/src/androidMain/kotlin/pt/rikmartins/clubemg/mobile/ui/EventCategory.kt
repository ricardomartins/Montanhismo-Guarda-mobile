package pt.rikmartins.clubemg.mobile.ui

import androidx.annotation.StringRes
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventTaxonomy
import pt.rikmartins.clubemg.mobile.R
import pt.rikmartins.clubemg.mobile.domain.usecase.events.TaxonomyType
import pt.rikmartins.clubemg.mobile.ui.EventCategory.Match
import pt.rikmartins.clubemg.mobile.ui.EventCategory.NoMatch

sealed interface EventCategory : Comparable<EventCategory> {
    val order: Int

    sealed class Match(
        @param:StringRes val designationRes: Int,
        val slugMatch: String,
        override val order: Int,
    ) : EventCategory {

        data object Assembly : Match(designationRes = R.string.category_assembly, slugMatch = "assembleia", order = 10)
        data object Cycling : Match(designationRes = R.string.category_cycling, slugMatch = "ciclismo", order = 20)
        data object MountainBike : Match(
            designationRes = R.string.category_mountain_bike,
            slugMatch = "btt",
            order = 21
        )

        data object RoadBike : Match(designationRes = R.string.category_road, slugMatch = "estrada", order = 22)
        data object Nautical : Match(designationRes = R.string.category_sailing, slugMatch = "nautica", order = 30)
        data object Canoeing : Match(designationRes = R.string.category_canoe, slugMatch = "canoagem", order = 31)
        data object Competition : Match(
            designationRes = R.string.category_competition,
            slugMatch = "competicao",
            order = 40
        )

        data object MountainRunning : Match(
            designationRes = R.string.category_mountain_run,
            slugMatch = "corrida-de-montanha",
            order = 50
        )

        data object Climbing : Match(designationRes = R.string.category_climbing, slugMatch = "escalada", order = 60)
        data object Training : Match(designationRes = R.string.category_training, slugMatch = "formacao", order = 70)
        data object Mountain : Match(designationRes = R.string.category_mountain, slugMatch = "montanha", order = 80)
        data object Wintry : Match(designationRes = R.string.category_invernal, slugMatch = "invernal", order = 81)
        data object Multiactivity : Match(
            designationRes = R.string.category_multiactivity,
            slugMatch = "multiatividade",
            order = 90
        )

        data object Orienteering : Match(
            designationRes = R.string.category_orienteering,
            slugMatch = "orientacao",
            order = 100
        )

        data object Hiking : Match(
            designationRes = R.string.category_pedestrianism,
            slugMatch = "pedestrianismo",
            order = 110
        )

        data object Social : Match(designationRes = R.string.category_social, slugMatch = "social", order = 120)

        override fun compareTo(other: EventCategory): Int = order.compareTo(other.order)

        companion object {
            val entries = setOf(
                Assembly, Cycling, MountainBike, RoadBike, Nautical, Canoeing, Competition, MountainRunning, Climbing,
                Training, Mountain, Wintry, Multiactivity, Orienteering, Hiking, Social,
            )
        }
    }

    data class NoMatch(val designation: String) : EventCategory {
        override val order: Int = Int.MAX_VALUE

        override fun compareTo(other: EventCategory): Int = order.compareTo(other.order)
    }
}

internal fun EventTaxonomy.toEventCategory(): EventCategory =
    Match.entries.find { slug == it.slugMatch } ?: NoMatch(designation = name)

internal fun Collection<EventTaxonomy>.toEventCategories(): List<EventCategory> =
    filter { it.taxonomyType == TaxonomyType.CATEGORY }.map { it.toEventCategory() }.sorted()
