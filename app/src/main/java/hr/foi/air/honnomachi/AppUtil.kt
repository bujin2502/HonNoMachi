package hr.foi.air.honnomachi

import androidx.annotation.StringRes
import android.content.Context
import android.widget.Toast

object AppUtil {
    fun showToast(context: Context, @StringRes messageResId: Int) {
        Toast.makeText(context, context.getString(messageResId), Toast.LENGTH_LONG).show()
    }

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}