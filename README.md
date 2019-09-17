# Android Practice - Form Validation in RxJava2 and RxBinding

As an Android developer, you may have worked on screens which allow the user to input and submit the changes to server, such as login, sign up, address, update profile, etc. Let's call these screens as form in this article. Normally the form is not allowed to submit until all mandatory fields are filled correctly. For a better user experience, an error message should be shown below the mandatory field if it's not valid, the submit button should be disabled as long as one mandatory field is not valid.

![android-form-validation](https://user-images.githubusercontent.com/6058601/65045075-7eb0f880-d9a1-11e9-9f06-d5518891d305.gif)

## `combineLatest` One subscription chain

This article demonstrates how to handle the form validation and submit in a centralized and concise approach. Let's start with a sign up screen, which contains first name, last name, email, password and sign up button, `combineLatest` is used to combine all the UI observables into one subscription chain which allows us to valid and submit form in one place.

    compositeDisposable += Observable.combineLatest(
            firstNameChanges,
            lastNameChanges,
            emailChanges,
            passwordChanges,
            Function4 { firstName: String, lastName: String, email: String, password: String ->
                SignUpForm(firstName, lastName, email, password)
            })
            .switchMap { form ->
                binding.isValid = form.isValid()
                btn_signup.clicks().throttleFirstShort().map { form }
            }
            .subscribe { form ->
                hideKeyboard()
                // TODO call function in ViewModel, something like this:
                // viewModel.signup(...)
            }

## EditText validation and error message

`EditText` is most the often used widget to input name, email, password, card number, search keywords, etc, and it's often used as child of `TextInputLayout` to provide hint and error message. The validation can be handled in `textChanges().doOnNext { }`,  an extension function is created to abstract this logic, the main reason doing this is to make `combineLatest` chain clear without a bunch of doOnNext.

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

then the `emailChanges` and `passwordChanges` can be written like this:

    private val emailChanges
        get() = et_email.errorCheckingTextChanges(til_email, R.string.invalid_email) {
            isValidEmail(it)
        }
    
    private val passwordChanges
        get() = et_password.errorCheckingTextChanges(til_password, R.string.invalid_password) {
            isValidPassword(it)
        }

## RxBinding: handle UI events in Rx

The form can be composed by a bunch of widgets, not only EditText by also CheckBox, SwitchButton, RadioButton, Spinner, DateTime Picker, Location Picker, Button, etc, which can also be combined through RxBinding:

    TextView.textChanges(): InitialValueObservable<CharSequence>
    
    SeekBar.userChanges(): InitialValueObservable<Int>
    
    // CheckBox, RadioButton, ToggleButton, Switch, etc.
    CompoundButton.checkedChanges(): InitialValueObservable<Boolean>
    
    // Spinner, ListView, etc.
    AdapterView<T>.itemSelections(): InitialValueObservable<Int>

## How to combine more then 10 observables?

RxJava provide operators to combine at most 9 observables, from `BiFunction` to `Function9`, how can we handle the scenario if the form contains more than 10 observables?  It can also takes a collection of observables:

    val obsList = arrayOf(
            firstNameObs,
            lastNameObs,
            mobileNumberObs,
            companyObs,
            streetAddressLine1Obs,
            streetAddressLine2Obs,
            cityObs,
            stateObs,
            postcodeObs,
            countryObs
    )
    
    Observable.combineLatest(obsList) {
        AddressUI(firstName = it[0] as String,
                lastName = it[1] as String,
                phoneNumber = it[2] as String,
                company = it[3] as String?,
                street = it[4] as String,
                street2 = it[5] as String?,
                city = it[6] as String,
                state = it[7] as String,
                postcode = it[8] as String,
                country = it[9] as String)
    }.subscribe { addressForm -> 
        
    }

## Combine your customized observable

`combineLatest` operator requires all source Observables to emit at least one value. That means the subscription won't be triggered if one observable never emit.  This is not an issue for RxBinding as you can see all the returned value are `InitialValueObservable` which emits an initial value when subscribe.

However if you wanna combine your customized observable, please make sure call `.startWith` for your observable in the chain.

## Submit by ActionBar MenuItem?

Say if the screen contains a submit button on the bottom of the form and also a submit menu item on ActionBar, how should we handle the menu item click event in this case? Is it possible to handle it in the same `combineLatest` subscription chain? The answer is yes. The solution is `PublishSubject` and `Observable.merge`:

    private val signUpMenuClicksPublish = PublishSubject.create<Unit>()
    private val signUpMenuClicks = signUpMenuClicksPublish.toFlowable(BackpressureStrategy.LATEST).toObservable()
    private lateinit var signUpMenuItem: MenuItem
    
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
    
    compositeDisposable += Observable.combineLatest(
        ...})
        .switchMap { form ->
                val isValid = form.isValid()
                binding.isValid = isValid
                if (this::signUpMenuItem.isInitialized) {
                    signUpMenuItem.isEnabled = isValid
                }
                Observable.merge(signUpMenuClicks, btn_signup.clicks().throttleFirstShort()).map { form }
            }

That's it, you can also check codes on GitHub:

[](https://github.com/li2/Android-Form-Validation)