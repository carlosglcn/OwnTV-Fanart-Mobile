package tv.own.owntv.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.own.owntv.mobile.R

/**
 * Shown while the database is upgraded to a new version's format ([tv.own.owntv.core.database.DatabaseUpgrade]):
 * on a large catalogue the first start after such an update takes a minute or more, and a blank screen
 * for that long looks like a hang. The screen stays on, so the phone does not lock mid-migration.
 */
@Composable
fun DatabaseMigrationScreen() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0D)), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 520.dp).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = Color(0xFF4FD1C5))
            Text(stringResource(R.string.database_migration_title), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Text(stringResource(R.string.database_migration_body), fontSize = 15.sp, color = Color(0xFFCFCFD6), textAlign = TextAlign.Center)
            Text(stringResource(R.string.database_migration_keep_mobile), fontSize = 14.sp, color = Color(0xFF8A8A94), textAlign = TextAlign.Center)
        }
    }
}
