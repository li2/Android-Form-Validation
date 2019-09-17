package me.li2.androidform

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function4
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_signup.*
import me.li2.androidform.Validation.isValidEmail
import me.li2.androidform.Validation.isValidPassword
import me.li2.androidform.databinding.FragmentSignupBinding

class SignUpFragment : Fragment() {

    private val compositeDisposable = CompositeDisposable()

    private val firstNameChanges
        get() = et_first_name.textChanges().mapToString()

    private val lastNameChanges
        get() = et_last_name.textChanges().mapToString()

    private val emailChanges
        get() = et_email.errorCheckingTextChanges(til_email, R.string.invalid_email) {
            isValidEmail(it)
        }

    private val passwordChanges
        get() = et_password.errorCheckingTextChanges(til_password, R.string.invalid_password) {
            isValidPassword(it)
        }

    private val signUpMenuClicksPublish = PublishSubject.create<Unit>()
    private val signUpMenuClicks = signUpMenuClicksPublish.toFlowable(BackpressureStrategy.LATEST).toObservable()
    private lateinit var signUpMenuItem: MenuItem

    private lateinit var binding: FragmentSignupBinding

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_signup, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        compositeDisposable += Observable.combineLatest(
                firstNameChanges,
                lastNameChanges,
                emailChanges,
                passwordChanges,
                Function4 { firstName: String, lastName: String, email: String, password: String ->
                    SignUpForm(firstName, lastName, email, password)
                })
                .switchMap { form ->
                    val isValid = form.isValid()
                    binding.isValid = isValid
                    if (this::signUpMenuItem.isInitialized) {
                        signUpMenuItem.isEnabled = isValid
                    }
                    Observable.merge(signUpMenuClicks, btn_signup.clicks().throttleFirstShort()).map { form }
                }
                .subscribe { form ->
                    hideKeyboard()
                    // TODO call function in ViewModel, something like this:
                    // viewModel.signup(...)
                }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.signup_menu, menu)
        signUpMenuItem = menu.findItem(R.id.signup)
        signUpMenuItem.isEnabled = false
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.signup -> {
                signUpMenuClicksPublish.onNext(Unit)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
