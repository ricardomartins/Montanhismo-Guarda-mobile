package pt.rikmartins.clubemg.mobile.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import pt.rikmartins.clubemg.mobile.R

@Composable
fun BookmarkToggleButton(
    isBookmarked: Boolean,
    setBookmark: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonShape: Shape
    @DrawableRes val buttonIconRes: Int
    @StringRes val buttonDescriptionRes: Int
    if (isBookmarked) {
        buttonShape = MaterialTheme.shapes.small
        buttonIconRes = R.drawable.ic_bookmark
        buttonDescriptionRes = R.string.unbookmark_activity_action_description
    } else {
        buttonShape = IconButtonDefaults.standardShape
        buttonIconRes = R.drawable.ic_bookmark_border
        buttonDescriptionRes = R.string.bookmark_activity_action_description
    }

    IconToggleButton(
        checked = isBookmarked,
        onCheckedChange = setBookmark,
        colors = IconButtonDefaults.iconToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier.wrapContentHeight(),
        shape = buttonShape,
    ) {
        Icon(
            painter = painterResource(buttonIconRes),
            contentDescription = stringResource(buttonDescriptionRes),
        )
    }
}