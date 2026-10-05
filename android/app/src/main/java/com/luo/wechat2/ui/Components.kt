package com.luo.wechat2.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luo.wechat2.R
import com.luo.wechat2.data.ChatStorage

// MARK: - Clickable

fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = MutableInteractionSource(),
    indication = null,
    onClick = onClick
)

// MARK: - Top Bar

@Composable
fun AppTopBar(
    title: String,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    actionIcon: ImageVector? = null,
    onAction: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(GroupedBackground)
    ) {
        if (showBack) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(44.dp)
                    .clickableNoRipple(onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 56.dp)
        )

        if (actionIcon != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(44.dp)
                    .clickableNoRipple(onAction),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// MARK: - Search Bar

@Composable
fun SearchBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GroupedBackground)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(SystemGray5, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = SecondaryText,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "搜索",
                fontSize = 16.sp,
                color = SecondaryText
            )
        }
    }
}

// MARK: - Avatar

@Composable
fun ChatAvatar(avatar: String, size: Dp, corner: Dp) {
    // 对应 iOS 的 ChatAvatar：优先显示用户在“我 → 聊天头像”里上传的照片
    val storageVersion by ChatStorage.version.collectAsState()
    val rawAvatar = remember(avatar, storageVersion) { ChatStorage.avatarBase64(avatar) }
    val customBitmap = remember(rawAvatar) {
        rawAvatar?.let { encoded ->
            val bytes = try {
                Base64.decode(encoded, Base64.NO_WRAP)
            } catch (e: Exception) {
                null
            }
            bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
        }
    }

    if (customBitmap != null) {
        Image(
            bitmap = customBitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(corner))
        )
    } else if (avatar == "administerphoto") {
        Image(
            painter = painterResource(id = R.drawable.administerphoto),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(corner))
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(corner))
                .background(SystemGray5),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(size * 0.45f)
            )
        }
    }
}

// MARK: - Section Title

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = SecondaryText,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// MARK: - Unread Badge

@Composable
fun UnreadBadge(count: Int) {
    Box(
        modifier = Modifier
            .heightIn(min = 18.dp)
            .widthIn(min = 18.dp)
            .background(Color.Red, RoundedCornerShape(50))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

// MARK: - Section Card

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
    ) {
        content()
    }
}
