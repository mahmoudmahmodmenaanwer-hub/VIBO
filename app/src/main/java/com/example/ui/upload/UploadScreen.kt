package com.example.ui.upload

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.VideoPlayerBox
import com.example.ui.theme.ViboCoral
import com.example.ui.theme.ViboCyan

@Composable
fun UploadScreen(
    viewModel: UploadViewModel,
    onVideoPublished: (String) -> Unit
) {
    val context = LocalContext.current
    val sourceTab by viewModel.sourceTab.collectAsStateWithLifecycle()
    val preparedUri by viewModel.preparedVideoUri.collectAsStateWithLifecycle()
    val directUrl by viewModel.directUrlInput.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val hashtagsInput by viewModel.hashtagsInput.collectAsStateWithLifecycle()
    val isPublic by viewModel.isPublic.collectAsStateWithLifecycle()
    val isPreparingFile by viewModel.isPreparingFile.collectAsStateWithLifecycle()
    val isPublishing by viewModel.isPublishing.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val publishedVideoId by viewModel.publishedVideoId.collectAsStateWithLifecycle()

    LaunchedEffect(publishedVideoId) {
        publishedVideoId?.let { id ->
            viewModel.clearPublishedEvent()
            onVideoPublished(id)
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onVideoPickedFromDevice(context, uri)
        }
    }

    val previewUri = when (sourceTab) {
        UploadSourceTab.PHONE_GALLERY -> preparedUri
        UploadSourceTab.DIRECT_URL -> directUrl.trim().takeIf {
            it.startsWith("http://") || it.startsWith("https://")
        }.orEmpty()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.upload_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = sourceTab == UploadSourceTab.PHONE_GALLERY,
                    onClick = { viewModel.selectSourceTab(UploadSourceTab.PHONE_GALLERY) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { Icon(Icons.Rounded.VideoLibrary, contentDescription = null) },
                    label = { Text(stringResource(R.string.upload_source_phone)) }
                )
                SegmentedButton(
                    selected = sourceTab == UploadSourceTab.DIRECT_URL,
                    onClick = { viewModel.selectSourceTab(UploadSourceTab.DIRECT_URL) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                    label = { Text(stringResource(R.string.upload_source_url)) }
                )
            }

            if (sourceTab == UploadSourceTab.PHONE_GALLERY) {
                if (preparedUri.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = 1.5.dp,
                                color = ViboCoral.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .clickable(enabled = !isPreparingFile) {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            }
                            .testTag("pick_video_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPreparingFile) {
                            CircularProgressIndicator(color = ViboCoral)
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudUpload,
                                    contentDescription = null,
                                    tint = ViboCoral,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = stringResource(R.string.upload_pick_video),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "MP4, MOV, WebM — مباشرة من معرض هاتفك",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(22.dp))
                    ) {
                        VideoPlayerBox(
                            videoUri = preparedUri,
                            isActive = true,
                            isMuted = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.upload_change_video))
                    }
                }
            } else {
                OutlinedTextField(
                    value = directUrl,
                    onValueChange = viewModel::updateDirectUrl,
                    label = { Text(stringResource(R.string.upload_source_url)) },
                    placeholder = { Text(stringResource(R.string.upload_url_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_url_input")
                )
                if (previewUri.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        VideoPlayerBox(
                            videoUri = previewUri,
                            isActive = true,
                            isMuted = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = viewModel::updateTitle,
                label = { Text(stringResource(R.string.upload_title_label)) },
                placeholder = { Text(stringResource(R.string.upload_title_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_title_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = viewModel::updateDescription,
                label = { Text(stringResource(R.string.upload_desc_label)) },
                placeholder = { Text(stringResource(R.string.upload_desc_hint)) },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_description_input")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.upload_category_label),
                    style = MaterialTheme.typography.titleSmall
                )
                LazyRow(
                    contentPadding = PaddingValues(0.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(UPLOAD_CATEGORIES) { cat ->
                        FilterChip(
                            selected = cat == category,
                            onClick = { viewModel.selectCategory(cat) },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ViboCyan.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            OutlinedTextField(
                value = hashtagsInput,
                onValueChange = viewModel::updateHashtags,
                label = { Text(stringResource(R.string.upload_hashtags_label)) },
                placeholder = { Text(stringResource(R.string.upload_hashtags_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.upload_visibility_label),
                    style = MaterialTheme.typography.titleSmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = isPublic,
                        onClick = { viewModel.setPublicVisibility(true) },
                        leadingIcon = { Icon(Icons.Rounded.Public, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = { Text(stringResource(R.string.upload_visibility_public)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ViboCoral,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = !isPublic,
                        onClick = { viewModel.setPublicVisibility(false) },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = { Text(stringResource(R.string.upload_visibility_private)) }
                    )
                }
            }

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = viewModel::publishVideo,
                enabled = !isPublishing && !isPreparingFile,
                colors = ButtonDefaults.buttonColors(containerColor = ViboCoral, contentColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("publish_video_button")
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(stringResource(R.string.upload_publishing), style = MaterialTheme.typography.labelLarge)
                } else {
                    Text(stringResource(R.string.upload_publish_button), style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
