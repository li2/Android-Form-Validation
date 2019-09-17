package me.li2.androidform

import me.li2.androidform.Validation.isValidEmail
import me.li2.androidform.Validation.isValidPassword

data class SignUpForm(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String) {

    fun isValid() = firstName.isNotBlank()
            && lastName.isNotBlank()
            && isValidEmail(email)
            && isValidPassword(password)
}
