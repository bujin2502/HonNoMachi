package hr.foi.air.honnomachi.screen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import hr.foi.air.honnomachi.R

@Composable
fun SignupScreen(modifier: Modifier= Modifier) {
    Text(stringResource(R.string.signup_screen))
}