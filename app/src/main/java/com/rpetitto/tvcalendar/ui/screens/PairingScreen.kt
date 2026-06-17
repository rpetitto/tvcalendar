package com.rpetitto.tvcalendar.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rpetitto.tvcalendar.R
import com.rpetitto.tvcalendar.auth.DeviceCodeResponse
import com.rpetitto.tvcalendar.ui.components.rememberQrBitmap
import com.rpetitto.tvcalendar.ui.theme.AccentBlue
import com.rpetitto.tvcalendar.ui.theme.AmbientBackground
import com.rpetitto.tvcalendar.ui.theme.CoolWhite
import com.rpetitto.tvcalendar.ui.theme.MutedGray

/**
 * Full-screen pairing UI: big user code on the left, scannable QR of the
 * verification URL on the right, with a subtle polling indicator.
 */
@Composable
fun PairingScreen(
    deviceCode: DeviceCodeResponse,
    modifier: Modifier = Modifier,
) {
    val qr = rememberQrBitmap(deviceCode.verification)

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(AmbientBackground)
            .padding(64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(64.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.pairing_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = CoolWhite,
            )
            Text(
                text = deviceCode.userCode,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = AccentBlue,
            )
            Text(
                text = stringResource(R.string.pairing_instruction, deviceCode.verification),
                fontSize = 20.sp,
                color = MutedGray,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = AccentBlue,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.pairing_waiting),
                    fontSize = 16.sp,
                    color = MutedGray,
                )
            }
        }

        // QR rendered on a white card so cameras can read it on the dark UI.
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (qr != null) {
                Image(
                    bitmap = qr,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(text = deviceCode.verification, color = Color.Black, fontSize = 14.sp)
            }
        }
    }
}
