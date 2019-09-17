package me.li2.androidform

import android.content.Context
import android.util.Patterns
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

fun TextInputEditText.errorCheckingTextChanges(
    textInputLayout: TextInputLayout,
    @StringRes errorMessageId: Int,
    isValid: (String) -> Boolean
): Observable<String> {
    return textChanges().mapToString().doOnNext { input ->
        if (input.isNotEmpty()) {
            textInputLayout.error =
                if (isValid(input)) null else textInputLayout.context.getString(errorMessageId)
        }
    }
}

fun Observable<CharSequence>.mapToString(): Observable<String> = this.map { it.toString() }

fun <T> Observable<T>.throttleFirstShort() = this.throttleFirst(500L, TimeUnit.MILLISECONDS)!!

fun Boolean?.orFalse() = this ?: false

fun Fragment.hideKeyboard() = activity?.run {
    currentFocus?.let {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(it.windowToken, 0)
    }
}

object Validation {
    fun isValidEmail(email: String) = Patterns.EMAIL_ADDRESS.matcher(email).matches()
    fun isValidPassword(password: String) = password.length >= 6
}
