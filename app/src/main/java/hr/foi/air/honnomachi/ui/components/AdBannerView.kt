package hr.foi.air.honnomachi.ui.components

import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdView
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.ads.AdBannerState
import hr.foi.air.honnomachi.ads.AdManager

@Composable
fun AdBannerView(
    adManager: AdManager,
    adState: AdBannerState,
    onAdLoaded: () -> Unit,
    onAdFailed: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (adState) {
        is AdBannerState.Capped -> return
        is AdBannerState.Loading,
        is AdBannerState.Loaded,
        is AdBannerState.Failed,
        -> Unit
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("ad_banner"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.ad_label),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp),
        )

        if (adState is AdBannerState.Failed) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 16.dp)
                        .testTag("ad_fallback"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.ad_fallback_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val context = LocalContext.current
            var adView by remember { mutableStateOf<AdView?>(null) }

            DisposableEffect(Unit) {
                onDispose {
                    adView?.destroy()
                }
            }

            AndroidView(
                factory = {
                    adManager.createBannerAdView(context, onAdLoaded, onAdFailed).also { view ->
                        adView = view
                        view.layoutParams =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                            )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
