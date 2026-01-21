package pt.rikmartins.clubemg.mobile.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import pt.rikmartins.clubemg.mobile.ClubeMGApp
import pt.rikmartins.clubemg.mobile.MainActivity
import pt.rikmartins.clubemg.mobile.R
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventDiff
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SynchronizeFavouriteEvents
import java.sql.Date
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toJavaInstant

@OptIn(ExperimentalTime::class)
class AndroidNotifier(private val context: Context) : SynchronizeFavouriteEvents.Notifier {

    private val notificationManager by lazy { NotificationManagerCompat.from(context) }

    override suspend fun notifySingleEventCanceled(eventDiff: EventDiff) {
        ClubeMGNotification.EventCanceled.notifyWith(eventDiff)
    }

    override suspend fun notifySingleEventPostponed(eventDiff: EventDiff) {
        ClubeMGNotification.EventPostponed.notifyWith(eventDiff)
    }

    override suspend fun notifySingleEventRescheduled(eventDiff: EventDiff) {
        ClubeMGNotification.EventRescheduled.notifyWith(eventDiff)
    }

    override suspend fun notifySingleEventEnrollmentStarted(eventDiff: EventDiff) {
        ClubeMGNotification.EventEnrollmentStarted.notifyWith(eventDiff)
    }

    override suspend fun notifySingleEventRenamed(eventDiff: EventDiff) {
        ClubeMGNotification.SingleEventRenamed.notifyWith(eventDiff)
    }

    override suspend fun notifySingleEventOtherChanges(eventDiff: EventDiff) {
        ClubeMGNotification.SingleEventOtherChanges.notifyWith(eventDiff)
    }

    private fun ClubeMGNotification.notifyWith(eventDiff: EventDiff) {
        val notification = asNotification(context, eventDiff)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) notificationManager.notify(notification.hashCode(), notification)
    }
}

fun ClubeMGApp.setupNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channels = Channel.entries.map { it.asNotificationChannel(this) }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannels(channels)
    }
}

private enum class Channel(
    val id: String,
    val importance: Int,
    val compatPriority: Int,
    @param:StringRes
    val displayName: Int,
    @param:StringRes
    val displayDescription: Int,
) {
    FAVOURITE_RELEVANT(
        id = "favourite_relevant",
        importance = NotificationManager.IMPORTANCE_DEFAULT,
        compatPriority = NotificationCompat.PRIORITY_DEFAULT,
        displayName = R.string.favourite_relevant_channel_name,
        displayDescription = R.string.favourite_relevant_channel_description,
    ),
    FAVOURITE_NOT_RELEVANT(
        id = "favourite_not_relevant",
        importance = NotificationManager.IMPORTANCE_LOW,
        compatPriority = NotificationCompat.PRIORITY_LOW,
        displayName = R.string.favourite_not_relevant_channel_name,
        displayDescription = R.string.favourite_not_relevant_channel_description,
    ),
    ;

    @RequiresApi(Build.VERSION_CODES.O)
    fun asNotificationChannel(context: Context): NotificationChannel = NotificationChannel(
        /* id = */ id,
        /* name = */ context.getString(displayName),
        /* importance = */ importance
    ).apply {
        description = context.getString(this@Channel.displayDescription)
    }
}

@OptIn(ExperimentalTime::class)
private sealed class ClubeMGNotification(val channel: Channel) {

    object EventCanceled : ClubeMGNotification(channel = Channel.FAVOURITE_RELEVANT) {

        override fun Context.getTitle(eventDiff: EventDiff): String = getString(R.string.event_cancelled_title)

        override fun Context.getText(eventDiff: EventDiff): String =
            getString(R.string.event_cancelled_text, eventDiff.oldEvent.title)

        override fun Context.getBigText(eventDiff: EventDiff): String = getString(
            R.string.event_cancelled_big_text,
            eventDiff.oldEvent.title,
            eventDiff.oldEvent.startDate.asDate()
        )
    }

    object EventPostponed : ClubeMGNotification(channel = Channel.FAVOURITE_RELEVANT) {

        override fun Context.getTitle(eventDiff: EventDiff): String = getString(R.string.event_postponed_title)

        override fun Context.getText(eventDiff: EventDiff): String =
            getString(R.string.event_postponed_text, eventDiff.oldEvent.title)

        override fun Context.getBigText(eventDiff: EventDiff): String = getString(
            R.string.event_postponed_big_text,
            eventDiff.oldEvent.title,
            eventDiff.oldEvent.startDate.asDate()
        )
    }

    object EventRescheduled : ClubeMGNotification(channel = Channel.FAVOURITE_RELEVANT) {

        override fun Context.getTitle(eventDiff: EventDiff): String = getString(R.string.event_rescheduled_title)

        override fun Context.getText(eventDiff: EventDiff): String = getString(
            R.string.event_rescheduled_text,
            eventDiff.oldEvent.title,
            eventDiff.oldEvent.startDate.asDate()
        )

        override fun Context.getBigText(eventDiff: EventDiff): String = getString(
            R.string.event_rescheduled_big_text,
            eventDiff.oldEvent.title,
            eventDiff.oldEvent.startDate.asDate(),
            eventDiff.newEvent.startDate.asDate(),
        )
    }

    fun Instant.asDate(): java.util.Date = Date.from(this.toJavaInstant())

    object EventEnrollmentStarted : ClubeMGNotification(channel = Channel.FAVOURITE_RELEVANT) {

        override fun Context.getTitle(eventDiff: EventDiff): String = getString(R.string.event_enrollment_started_title)

        override fun Context.getText(eventDiff: EventDiff): String =
            getString(R.string.event_enrollment_started_text, eventDiff.oldEvent.title)

        override fun Context.getBigText(eventDiff: EventDiff): String =
            getString(R.string.event_enrollment_started_big_text, eventDiff.oldEvent.title)
    }

    object SingleEventRenamed : ClubeMGNotification(channel = Channel.FAVOURITE_NOT_RELEVANT) {

        override fun Context.getTitle(eventDiff: EventDiff): String = getString(R.string.single_event_renamed_title)


        override fun Context.getText(eventDiff: EventDiff): String =
            getString(R.string.single_event_renamed_text, eventDiff.oldEvent.title)

        override fun Context.getBigText(eventDiff: EventDiff): String =
            getString(R.string.single_event_renamed_big_text, eventDiff.oldEvent.title, eventDiff.newEvent.title)
    }

    object SingleEventOtherChanges : ClubeMGNotification(channel = Channel.FAVOURITE_NOT_RELEVANT) {

        override fun Context.getTitle(eventDiff: EventDiff): String =
            getString(R.string.single_event_other_changes_title)

        override fun Context.getText(eventDiff: EventDiff): String =
            getString(R.string.single_event_other_changes_text, eventDiff.oldEvent.title)

        override fun Context.getBigText(eventDiff: EventDiff): String? = null
    }

    protected abstract fun Context.getTitle(eventDiff: EventDiff): String
    protected abstract fun Context.getText(eventDiff: EventDiff): String
    protected abstract fun Context.getBigText(eventDiff: EventDiff): String?

    fun asNotification(context: Context, eventDiff: EventDiff): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat
            .Builder(context, channel.id)
            .setPriority(channel.compatPriority)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setCategory(Notification.CATEGORY_EVENT)
            .setContentTitle(context.getTitle(eventDiff))
            .setContentText(context.getText(eventDiff))
            .run {
                context.getBigText(eventDiff)
                    ?.let { setStyle(NotificationCompat.BigTextStyle().bigText(it)) }
                    ?: this
            }
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }
}