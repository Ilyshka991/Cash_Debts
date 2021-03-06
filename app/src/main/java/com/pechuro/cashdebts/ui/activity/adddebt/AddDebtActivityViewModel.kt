package com.pechuro.cashdebts.ui.activity.adddebt

import androidx.annotation.StringRes
import com.pechuro.cashdebts.R
import com.pechuro.cashdebts.calculator.Calculator
import com.pechuro.cashdebts.calculator.Result
import com.pechuro.cashdebts.data.data.exception.FirestoreUserNotFoundException
import com.pechuro.cashdebts.data.data.model.*
import com.pechuro.cashdebts.data.data.model.DebtRole.Companion.CREDITOR
import com.pechuro.cashdebts.data.data.model.DebtRole.Companion.DEBTOR
import com.pechuro.cashdebts.data.data.repositories.ILocalDebtRepository
import com.pechuro.cashdebts.data.data.repositories.IRemoteDebtRepository
import com.pechuro.cashdebts.data.data.repositories.IUserRepository
import com.pechuro.cashdebts.model.connectivity.ConnectivityListener
import com.pechuro.cashdebts.ui.activity.adddebt.model.BaseDebtInfo
import com.pechuro.cashdebts.ui.activity.adddebt.model.impl.LocalDebtInfo
import com.pechuro.cashdebts.ui.activity.adddebt.model.impl.RemoteDebtInfo
import com.pechuro.cashdebts.ui.base.BaseViewModel
import com.pechuro.cashdebts.ui.utils.extensions.requireValue
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class AddDebtActivityViewModel @Inject constructor(
    private val localDebtRepository: ILocalDebtRepository,
    private val remoteDebtRepository: IRemoteDebtRepository,
    private val userRepository: IUserRepository,
    private val connectivityListener: ConnectivityListener,
    private val calculator: Calculator
) : BaseViewModel() {
    val command = PublishSubject.create<Events>()

    val isConnectionAvailable = BehaviorSubject.create<Boolean>()
    val mathExpression = BehaviorSubject.createDefault("")

    val loadingState = BehaviorSubject.createDefault<LoadingState>(LoadingState.OnStop)

    val debtValue: Observable<Pair<Boolean, Result>> by lazy {
        mathExpression
            .distinctUntilChanged()
            .map { expr ->
                val builder = StringBuilder(expr)

                when {
                    expr.isEmpty() -> builder.append(0)
                    expr.length > 2 && expr[expr.lastIndex] == '-' && expr[expr.lastIndex - 1] in "+-" ->
                        builder.append(0)
                    expr.length > 2 && expr[expr.lastIndex] == '-' && expr[expr.lastIndex - 1] in "*/" ->
                        builder.append(1)
                    expr.length > 1 && expr[expr.lastIndex] in ".+-" -> builder.append(0)
                    expr.length > 1 && expr[expr.lastIndex] in "*/" -> builder.append(1)
                    expr.length > 2 && expr[expr.lastIndex] == '(' && expr[expr.lastIndex - 1] in "+-/*"
                    -> builder.delete(builder.length - 2, builder.length)
                }


                val openStapleCount = builder.count { it == '(' }
                val closeStapleCount = builder.count { it == ')' }
                if (openStapleCount > closeStapleCount) {
                    repeat(openStapleCount - closeStapleCount) {
                        builder.append(')')
                    }
                }

                builder.toString()
            }
            .map { isNotMathExpression(it) to calculator.evaluate(it) }
            .subscribeOn(Schedulers.computation())
            .also { observable ->
                observable
                    .map {
                        when (val result = it.second) {
                            is Result.Success -> result.result
                            is Result.Error -> 0.0
                        }
                    }
                    .subscribe(debt.value)
            }
            .observeOn(AndroidSchedulers.mainThread())
    }

    lateinit var debt: BaseDebtInfo
    private var isDebtAlreadyLoaded = false

    init {
        setConnectivityListener()
    }

    fun setInitialData(isLocalDebt: Boolean) {
        if (!::debt.isInitialized) {
            debt = if (isLocalDebt) LocalDebtInfo() else RemoteDebtInfo()
        }
    }

    fun loadExistingDebt(id: String) {
        if (isDebtAlreadyLoaded) return

        val debtInfo = debt
        val source = when (debtInfo) {
            is LocalDebtInfo -> localDebtRepository.getSingle(id)
            is RemoteDebtInfo -> remoteDebtRepository.getSingle(id)
            else -> throw  IllegalArgumentException()
        }

        debtInfo.id = id

        source.subscribe({
            onDebtLoaded(it)
            isDebtAlreadyLoaded = true
        }, {
            it.printStackTrace()
        }).addTo(compositeDisposable)
    }

    fun setPhoneData(phoneNumber: String) {
        (debt as RemoteDebtInfo).phone.onNext(phoneNumber)
    }

    fun validatePersonInfo() {
        when (val data = debt) {
            is LocalDebtInfo -> {
                if (data.isValid()) {
                    command.onNext(Events.OpenInfo(false))
                } else {
                    command.onNext(Events.OnError(R.string.fragment_add_debt_local_error_invalid_name))
                }
            }
            is RemoteDebtInfo -> {
                if (data.isValid()) {
                    if (data.phone.requireValue.isEqualsCurrentNumber()) {
                        command.onNext(Events.OnError(R.string.fragment_add_debt_remote_error_number_equal_current))
                    } else {
                        checkUserExist(data)
                    }
                } else {
                    command.onNext(Events.OnError(R.string.fragment_add_debt_remote_error_invalid_phone))
                }
            }
        }
    }

    fun save() {
        if (!debt.isInfoValid()) {
            command.onNext(Events.OnError(R.string.fragment_add_debt_info_error_invalid_info))
            return
        }
        loadingState.onNext(LoadingState.OnStart)
        when (val debt = debt) {
            is LocalDebtInfo -> addLocalDebt(debt)
            is RemoteDebtInfo -> addRemoteDebt(debt)
        }
    }

    private fun onDebtLoaded(firestoreDebt: FirestoreBaseDebt) {
        when (firestoreDebt) {
            is FirestoreLocalDebt -> {
                val localDebt = debt as LocalDebtInfo
                localDebt.name.onNext(firestoreDebt.name)
                localDebt.debtRole.onNext(firestoreDebt.role)
            }
            is FirestoreRemoteDebt -> {
                val localDebt = debt as RemoteDebtInfo

                localDebt.isPersonChangeEnabled.onNext(false)

                if (userRepository.currentUserBaseInformation.uid == firestoreDebt.creditorUid) {
                    localDebt.personUid.onNext(firestoreDebt.debtorUid)
                    localDebt.debtRole.onNext(CREDITOR)
                } else {
                    localDebt.personUid.onNext(firestoreDebt.creditorUid)
                    localDebt.debtRole.onNext(DEBTOR)
                }

                userRepository.getSingle(localDebt.personUid.requireValue, true).subscribe({
                    localDebt.phone.onNext(it.phoneNumber)
                }, {
                    it.printStackTrace()
                })
            }
        }
        with(debt) {
            mathExpression.onNext(firestoreDebt.value.toString())
            description.onNext(firestoreDebt.description)
            date.onNext(firestoreDebt.date)
        }
    }

    private fun setConnectivityListener() {
        connectivityListener.listen(isConnectionAvailable)
    }

    private fun addRemoteDebt(debt: RemoteDebtInfo) {
        val id = debt.id

        val creditorUid: String
        val debtorUid: String
        val status: Int
        when (debt.debtRole.value) {
            null -> {
                command.onNext(Events.OnError(R.string.common_error))
                return
            }
            CREDITOR -> {
                creditorUid = userRepository.currentUserBaseInformation.uid
                debtorUid = debt.personUid.requireValue
                status = if (id == null) {
                    FirestoreDebtStatus.WAIT_FOR_CONFIRMATION
                } else {
                    FirestoreDebtStatus.WAIT_FOR_EDIT_CONFIRMATION_FROM_DEBTOR
                }
            }
            DEBTOR -> {
                debtorUid = userRepository.currentUserBaseInformation.uid
                creditorUid = debt.personUid.requireValue
                status = if (id == null) {
                    FirestoreDebtStatus.WAIT_FOR_CONFIRMATION
                } else {
                    FirestoreDebtStatus.WAIT_FOR_EDIT_CONFIRMATION_FROM_CREDITOR
                }
            }
            else -> throw IllegalArgumentException()
        }

        val sendingDebt = FirestoreRemoteDebt(
            creditorUid,
            debtorUid,
            debt.value.requireValue,
            debt.description.requireValue,
            debt.date.requireValue,
            status,
            userRepository.currentUserBaseInformation.uid,
            DebtDeleteStatus.NOT_DELETED,
            true,
            userRepository.currentUserBaseInformation.uid
        )

        val operation = if (id == null) {
            remoteDebtRepository.add(sendingDebt)
        } else {
            remoteDebtRepository.getSingle(id).flatMapCompletable {
                remoteDebtRepository.update(
                    "${id}_tmp", FirestoreRemoteDebt(
                        it.creditorUid,
                        it.debtorUid,
                        it.value,
                        it.description,
                        it.date,
                        it.status,
                        it.initPersonUid,
                        DebtDeleteStatus.CACHED,
                        it.isFirstTimeAdded,
                        it.lastChangePersonUid
                    )
                )
            }.andThen(remoteDebtRepository.update(id, sendingDebt))
        }

        operation.subscribe({
            loadingState.onNext(LoadingState.OnStop)
            command.onNext(Events.OnSaved)
        }, {
            loadingState.onNext(LoadingState.OnStop)
        }).addTo(compositeDisposable)
    }

    private fun addLocalDebt(debt: LocalDebtInfo) {
        val sendingDebt = with(debt) {
            FirestoreLocalDebt(
                userRepository.currentUserBaseInformation.uid,
                name.requireValue,
                value.requireValue,
                description.requireValue,
                date.requireValue,
                debtRole.requireValue
            )
        }
        val id = debt.id
        val operation = if (id == null) {
            localDebtRepository.add(sendingDebt)
        } else {
            localDebtRepository.update(id, sendingDebt)
        }
        operation.subscribe({
            loadingState.onNext(LoadingState.OnStop)
            command.onNext(Events.OnSaved)
        }, {
            loadingState.onNext(LoadingState.OnStop)
        }).addTo(compositeDisposable)
    }

    private fun checkUserExist(data: RemoteDebtInfo) {
        if (data.personUid.requireValue.isNotEmpty()) {
            command.onNext(Events.OpenInfo(true))
            return
        }

        loadingState.onNext(LoadingState.OnStart)
        userRepository.getUidByPhone(data.phone.requireValue).subscribe({
            data.personUid.onNext(it)
            loadingState.onNext(LoadingState.OnStop)
            command.onNext(Events.OpenInfo(true))
        }, {
            loadingState.onNext(LoadingState.OnStop)
            when (it) {
                is FirestoreUserNotFoundException -> command.onNext(Events.OnUserNotExist)
                else -> command.onNext(Events.OnError(R.string.common_error))
            }
        }).addTo(compositeDisposable)
    }

    private fun isNotMathExpression(expr: String) = expr.matches(NUMBER_REGEX)

    private fun String.isEqualsCurrentNumber() =
        this == userRepository.currentUserBaseInformation.phoneNumber

    sealed class Events {
        object OnSaved : Events()
        class OpenInfo(val isInternetRequired: Boolean) : Events()
        class OnError(@StringRes val id: Int) : Events()
        class SetOptionsMenuEnabled(val isEnabled: Boolean) : Events()
        object OnUserNotExist : Events()
    }

    sealed class LoadingState {
        object OnStart : LoadingState()
        object OnStop : LoadingState()
    }

    companion object {
        private val NUMBER_REGEX = "-?\\d+(\\.\\d+)?".toRegex()
    }
}