package com.makan.roadpulse.presentation.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.makan.roadpulse.domain.model.FriendRequest
import com.makan.roadpulse.domain.model.UserProfile
import com.makan.roadpulse.presentation.theme.TealPrimary

@Composable
fun FriendsScreen(viewModel: FriendsViewModel = hiltViewModel()) {
    val uiState         by viewModel.uiState.collectAsStateWithLifecycle()
    val query           by viewModel.searchQuery.collectAsStateWithLifecycle()
    val results         by viewModel.searchResults.collectAsStateWithLifecycle()
    val localPendingUids by viewModel.localPendingUids.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var removeFriendTarget by remember { mutableStateOf<UserProfile?>(null) }

    removeFriendTarget?.let { friend ->
        AlertDialog(
            onDismissRequest = { removeFriendTarget = null },
            title = { Text("Remove friend?") },
            text  = { Text("Remove ${friend.displayName} from your friends list?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeFriend(friend.uid)
                    removeFriendTarget = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { removeFriendTarget = null }) { Text("Cancel") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ── Search bar ─────────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search by display name…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = TealPrimary,
                    focusedLabelColor    = TealPrimary,
                    cursorColor          = TealPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        // ── Search results ─────────────────────────────────────────────────────
        if (query.isNotBlank() && results.isNotEmpty()) {
            item { Spacer(Modifier.height(8.dp)) }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column {
                        results.forEachIndexed { index, user ->
                            val alreadyFriend = uiState.friends.any { it.uid == user.uid }
                            val pendingOut = uiState.outgoingRequests.any { it.toUid == user.uid }
                                || user.uid in localPendingUids
                            if (!alreadyFriend) {
                                SearchResultRow(
                                    user = user,
                                    isPending = pendingOut,
                                    onAdd = { viewModel.sendRequest(user.uid) }
                                )
                                if (index < results.lastIndex) {
                                    androidx.compose.material3.HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Incoming requests ──────────────────────────────────────────────────
        if (uiState.incomingRequests.isNotEmpty()) {
            item { Spacer(Modifier.height(24.dp)) }
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Requests",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(8.dp))
                    Badge(containerColor = TealPrimary) {
                        Text(
                            uiState.incomingRequests.size.toString(),
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column {
                        uiState.incomingRequests.forEachIndexed { index, req ->
                            IncomingRequestRow(
                                request = req,
                                onAccept  = { viewModel.acceptRequest(req.id, req.fromUid) },
                                onDecline = { viewModel.declineRequest(req.id) }
                            )
                            if (index < uiState.incomingRequests.lastIndex) {
                                androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Friends list ───────────────────────────────────────────────────────
        item { Spacer(Modifier.height(24.dp)) }
        item {
            Text(
                "Friends (${uiState.friends.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
        }

        if (uiState.friends.isEmpty()) {
            item {
                Text(
                    "No friends yet — search to add some!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column {
                        uiState.friends.forEachIndexed { index, friend ->
                            FriendRow(
                                friend = friend,
                                onRemove = { removeFriendTarget = friend }
                            )
                            if (index < uiState.friends.lastIndex) {
                                androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    } // end LazyColumn
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
    } // end Box
}

// ── Row composables ────────────────────────────────────────────────────────────

@Composable
private fun SearchResultRow(user: UserProfile, isPending: Boolean, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(displayName = user.displayName, photoUrl = user.photoUrl, size = 36)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.displayName, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
            if (user.email.isNotBlank()) {
                Text(user.email, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (isPending) {
            Text("Pending", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Button(
                onClick = onAdd,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null,
                    modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun IncomingRequestRow(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            displayName = request.fromDisplayName,
            photoUrl    = request.fromPhotoUrl,
            size        = 36
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(request.fromDisplayName, fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
            Text("wants to be your friend", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onAccept, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Check, contentDescription = "Accept",
                tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onDecline, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Decline",
                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun FriendRow(friend: UserProfile, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(displayName = friend.displayName, photoUrl = friend.photoUrl, size = 36)
        Spacer(Modifier.width(12.dp))
        Text(
            friend.displayName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove friend",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun UserAvatar(displayName: String, photoUrl: String?, size: Int) {
    if (photoUrl != null) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = Modifier.size(size.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                fontSize = (size * 0.4f).sp,
                fontWeight = FontWeight.Bold,
                color = TealPrimary
            )
        }
    }
}
