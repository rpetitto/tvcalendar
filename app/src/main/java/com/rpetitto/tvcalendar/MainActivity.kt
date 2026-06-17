package com.rpetitto.tvcalendar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rpetitto.tvcalendar.ui.screens.CalendarScreen
import com.rpetitto.tvcalendar.ui.screens.LoadingScreen
import com.rpetitto.tvcalendar.ui.screens.PairingScreen
import com.rpetitto.tvcalendar.ui.theme.AmbientBackground
import com.rpetitto.tvcalendar.ui.theme.CoolWhite
import com.rpetitto.tvcalendar.ui.theme.MutedGray
import com.rpetitto.tvcalendar.ui.theme.TVCalendarTheme
import com.rpetitto.tvcalendar.ui.theme.TvType

class MainActivity : ComponentActivity() {

    private var viewModel: MainViewModel? = null

    /** Fires once a minute (system broadcast) to nudge a day-rollover check. */
    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_TIME_TICK) {
                viewModel?.onMinuteTick()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Always-on ambient display: keep the screen awake and on.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        setContent {
            TVCalendarTheme {
                val vm: MainViewModel = viewModel()
                viewModel = vm
                TVCalendarApp(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // ACTION_TIME_TICK can't be declared in the manifest; register at runtime.
        registerReceiver(timeTickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
        viewModel?.onResumed()
    }

    override fun onPause() {
        super.onPause()
        runCatching { unregisterReceiver(timeTickReceiver) }
        viewModel?.onPaused()
    }
}

@Composable
fun TVCalendarApp(viewModel: MainViewModel) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    when (val state = authState) {
        is MainViewModel.AuthState.NeedsAuth -> LoadingScreen()
        is MainViewModel.AuthState.Pairing -> PairingScreen(deviceCode = state.deviceCode)
        is MainViewModel.AuthState.Authenticated -> {
            val calendarState by viewModel.calendarState.collectAsStateWithLifecycle()
            if (calendarState.isLoading && calendarState.dayEvents.isEmpty()) {
                LoadingScreen()
            } else {
                CalendarScreen(
                    state = calendarState,
                    onViewSelected = viewModel::setView,
                    onFocusDate = viewModel::setFocusedDate,
                    onActivateDate = viewModel::openAgenda,
                    onBack = viewModel::onBackPressed,
                )
            }
        }
        is MainViewModel.AuthState.Error -> ErrorScreen(state.message)
    }
}

@Composable
private fun ErrorScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmbientBackground)
            .padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(R.string.error_generic), style = TvType.dayHeader, color = CoolWhite)
        Text(text = message, style = TvType.eventTime, color = MutedGray)
    }
}
