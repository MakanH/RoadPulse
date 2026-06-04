package com.makan.roadpulse.presentation.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makan.roadpulse.data.service.NotificationService
import com.makan.roadpulse.domain.model.AppNotification
import com.makan.roadpulse.presentation.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Bell button ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationBellButton(
    hasUnread: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(40.dp)) {
        Surface(
            onClick  = onClick,
            modifier = Modifier.size(40.dp),
            shape    = CircleShape,
            color    = Color.Black.copy(alpha = 0.45f),
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector        = Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    tint               = Color.White,
                    modifier           = Modifier.size(20.dp)
                )
            }
        }
        if (hasUnread) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(TealPrimary)
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
            )
        }
    }
}

// ── Notification bottom sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSheet(
    notifications: List<AppNotification>,
    onDismiss: () -> Unit,
    onMarkAllRead: () -> Unit,
    onNotificationTap: (AppNotification) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Notifications",
                fontSize   = 16.sp,
                fontWeight = FontWeight.W500,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            if (notifications.any { !it.isRead }) {
                TextButton(onClick = onMarkAllRead) {
                    Text(
                        "Mark all read",
                        fontSize = 13.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No notifications yet",
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    NotificationRow(notif = notif, onTap = { onNotificationTap(notif) })
                }
            }
        }
    }
}

// ── Notification row ──────────────────────────────────────────────────────────

@Composable
private fun NotificationRow(
    notif: AppNotification,
    onTap: () -> Unit
) {
    val bgColor = if (notif.isRead)
        MaterialTheme.colorScheme.surface
    else
        TealPrimary.copy(alpha = 0.06f)

    Surface(
        onClick = onTap,
        color   = bgColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NotifIcon(type = notif.type)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = notif.body,
                    fontSize   = 13.sp,
                    fontWeight = if (notif.isRead) FontWeight.Normal else FontWeight.Medium,
                    color      = if (notif.isRead)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines   = 2
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = formatNotifTime(notif.createdAt),
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun NotifIcon(type: String) {
    val (icon, tint) = when (type) {
        NotificationService.TYPE_FRIEND_REQUEST    -> Icons.Filled.PersonAdd to TealPrimary
        NotificationService.TYPE_FRIEND_ACCEPTED   -> Icons.Filled.Check     to Color(0xFF4CAF50)
        NotificationService.TYPE_GROUP_INVITE      -> Icons.Filled.Group     to Color(0xFF378ADD)
        NotificationService.TYPE_GROUP_DRIVE_STARTED -> Icons.Filled.Navigation to Color(0xFFD85A30)
        NotificationService.TYPE_ACHIEVEMENT       -> Icons.Filled.EmojiEvents to Color(0xFFF39C12)
        else                                       -> Icons.Filled.Notifications to TealPrimary
    }
    NotifCircle(icon = icon, tint = tint)
}

@Composable
private fun NotifCircle(icon: ImageVector, tint: Color) {
    Box(
        modifier          = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment  = Alignment.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = tint,
            modifier           = Modifier.size(18.dp)
        )
    }
}

// ── Time formatting ───────────────────────────────────────────────────────────

private fun formatNotifTime(createdAt: Long): String {
    if (createdAt == 0L) return ""
    val diffMs    = System.currentTimeMillis() - createdAt
    val diffMin   = diffMs / 60_000
    val diffHours = diffMs / 3_600_000
    val diffDays  = diffMs / 86_400_000
    return when {
        diffMin  < 1  -> "Just now"
        diffMin  < 60 -> "${diffMin}m ago"
        diffHours < 24 -> "${diffHours}h ago"
        diffDays == 1L -> "Yesterday"
        else           -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(createdAt))
    }
}
