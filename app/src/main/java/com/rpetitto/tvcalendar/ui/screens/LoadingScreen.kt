package com.rpetitto.tvcalendar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rpetitto.tvcalendar.R
import com.rpetitto.tvcalendar.ui.theme.AccentBlue
import com.rpetitto.tvcalendar.ui.theme.AmbientBackground
import com.rpetitto.tvcalendar.ui.theme.MutedGray
import com.rpetitto.tvcalendar.ui.theme.TvType

/** Brief transition state shown between auth and the calendar. */
@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmbientBackground),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.loading),
            style = TvType.dateLabel,
            color = MutedGray,
        )
    }
}
