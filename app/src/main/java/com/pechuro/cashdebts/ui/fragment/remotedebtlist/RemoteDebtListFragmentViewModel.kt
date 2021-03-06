package com.pechuro.cashdebts.ui.fragment.remotedebtlist

import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import com.pechuro.cashdebts.R
import com.pechuro.cashdebts.data.data.model.DebtDeleteStatus
import com.pechuro.cashdebts.data.data.model.DebtRole
import com.pechuro.cashdebts.data.data.model.FirestoreDebtStatus
import com.pechuro.cashdebts.data.data.model.FirestoreDebtStatus.Companion.COMPLETE
import com.pechuro.cashdebts.data.data.model.FirestoreDebtStatus.Companion.COMPLETION_REJECTED_BY_CREDITOR
import com.pechuro.cashdebts.data.data.model.FirestoreDebtStatus.Companion.COMPLETION_REJECTED_BY_DEBTOR
import com.pechuro.cashdebts.data.data.model.FirestoreDebtStatus.Companion.CONFIRMATION_REJECTED
import com.pechuro.cashdebts.data.data.model.FirestoreDebtStatus.Companion.EDIT_CONFIRMATION_REJECTED_BY_CREDITOR
import com.pechuro.cashdebts.data.data.model.FirestoreDebtStatus.Companion.EDIT_CONFIRMATION_REJECTED_BY_DEBTOR
import com.pechuro.cashdebts.data.data.model.FirestoreDebtStatus.Companion.IN_PROGRESS
import com.pechuro.cashdebts.data.data.model.FirestoreDebtStatus.Companion.WAIT_FOR_COMPLETE_FROM_CREDITOR
import com.pechuro.cashdebts.data.data.model.FirestoreDebtStatus.Companion.WAIT_FOR_COMPLETE_FROM_DEBTOR
import com.pechuro.cashdebts.data.data.model.FirestoreDebtStatus.Companion.WAIT_FOR_CONFIRMATION
import com.pechuro.cashdebts.data.data.model.FirestoreDebtStatus.Companion.WAIT_FOR_EDIT_CONFIRMATION_FROM_CREDITOR
import com.pechuro.cashdebts.data.data.model.FirestoreDebtStatus.Companion.WAIT_FOR_EDIT_CONFIRMATION_FROM_DEBTOR
import com.pechuro.cashdebts.data.data.model.FirestoreRemoteDebt
import com.pechuro.cashdebts.data.data.repositories.IRemoteDebtRepository
import com.pechuro.cashdebts.data.data.repositories.IUserRepository
import com.pechuro.cashdebts.model.connectivity.ConnectivityListener
import com.pechuro.cashdebts.model.entity.CountryData
import com.pechuro.cashdebts.model.entity.DiffResult
import com.pechuro.cashdebts.model.notification.NotificationCreateData
import com.pechuro.cashdebts.model.notification.NotificationManager
import com.pechuro.cashdebts.model.prefs.PrefsManager
import com.pechuro.cashdebts.ui.base.BaseViewModel
import com.pechuro.cashdebts.ui.fragment.remotedebtlist.adapter.RemoteDebtListAdapter
import com.pechuro.cashdebts.ui.fragment.remotedebtlist.data.RemoteDebt
import com.pechuro.cashdebts.ui.fragment.remotedebtlist.data.RemoteDebtDiffCallback
import com.pechuro.cashdebts.ui.fragment.remotedebtlist.data.RemoteDebtsUiInfo
import com.pechuro.cashdebts.ui.utils.extensions.getFormattedNumber
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observables.ConnectableObservable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class RemoteDebtListFragmentViewModel @Inject constructor(
    private val debtRepository: IRemoteDebtRepository,
    private val userRepository: IUserRepository,
    private val diffCallback: RemoteDebtDiffCallback,
    private val prefsManager: PrefsManager,
    private val countryList: List<CountryData>,
    private val notificationManager: NotificationManager,
    connectivityListener: ConnectivityListener
) : BaseViewModel() {
    val command = PublishSubject.create<Command>()

    val isConnectionAvailable = BehaviorSubject.create<Boolean>()

    val debtSource: ConnectableObservable<RemoteDebtsUiInfo> = debtRepository.getSource()
        .subscribeOn(Schedulers.io())
        .concatMapSingle { map ->
            Observable.fromIterable(map.toList())
                .flatMapSingle { originData ->
                    val (id, firestoreDebt) = originData

                    val anotherPersonUid: String
                    val isCurrentUserCreditor: Boolean
                    if (firestoreDebt.creditorUid == userRepository.currentUserBaseInformation.uid) {
                        anotherPersonUid = firestoreDebt.debtorUid
                        isCurrentUserCreditor = true
                    } else {
                        anotherPersonUid = firestoreDebt.creditorUid
                        isCurrentUserCreditor = false
                    }

                    userRepository.getSingle(anotherPersonUid)
                        .map {
                            RemoteDebt.User(
                                anotherPersonUid,
                                it.firstName,
                                it.lastName,
                                it.phoneNumber.getFormattedNumber(countryList),
                                it.photoUrl
                            )
                        }
                        .map {
                            RemoteDebt(
                                id,
                                it,
                                firestoreDebt.value,
                                firestoreDebt.description,
                                firestoreDebt.date,
                                firestoreDebt.status,
                                if (isCurrentUserCreditor) DebtRole.CREDITOR else DebtRole.DEBTOR,
                                firestoreDebt.initPersonUid == userRepository.currentUserBaseInformation.uid,
                                false,
                                firestoreDebt.deleteStatus != DebtDeleteStatus.NOT_DELETED,
                                false,
                                firestoreDebt.isFirstTimeAdded
                            )
                        }
                }.toList()
        }
        .observeOn(Schedulers.computation())
        .scan { first: List<RemoteDebt>, second: List<RemoteDebt> ->
            val mergedList = first.filter { it in second } + second
            mergedList.toSet().toList()
        }
        .map {
            if (prefsManager.filterNotShowCompleted) {
                it.filter { debt -> debt.status != COMPLETE }
            } else {
                it
            }
        }
        .map { debts ->
            if (prefsManager.filterUnitePersons) {
                val notGroupingItems = debts.filter { it.status != IN_PROGRESS }
                val singleItems = (debts - notGroupingItems)
                    .groupBy { it.user }
                    .filter { it.value.size == 1 }
                    .toList()
                    .map { it.second[0] }
                val groupedItems = (debts - notGroupingItems - singleItems)
                    .groupingBy { it.user }
                    .aggregate { _: RemoteDebt.User, accumulator: RemoteDebt?, element: RemoteDebt, first: Boolean ->
                        if (first) {
                            element.apply {
                                if (role == DebtRole.DEBTOR) {
                                    value = -value
                                }
                                isUnited = true
                            }
                        } else {
                            accumulator!!.apply {
                                value += if (element.role == DebtRole.CREDITOR) element.value else -element.value
                            }
                        }
                    }.toList().map {
                        it.second!!.apply {
                            if (value < 0) {
                                value = -value
                                role = DebtRole.DEBTOR
                            } else {
                                role = DebtRole.CREDITOR
                            }
                        }
                    }
                notGroupingItems + groupedItems + singleItems
            } else {
                debts
            }
        }
        .map {
            val resultList = mutableListOf<RemoteDebt>()
            if (it.isEmpty()) {
                resultList += RemoteDebt.EMPTY
            } else {
                resultList += it
            }
            resultList
        }
        .filter { diffCallback.oldList != it }
        .map { list ->
            list.sortedByDescending { it.date }
        }
        .map {
            diffCallback.newList = it
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            diffCallback.oldList = it
            DiffResult(diffResult, it)
        }.map { diffResult ->
            val totalValue = diffResult.dataList
                .filter { it.status == IN_PROGRESS }
                .fold(0.0) { acc, debt ->
                    acc + if (debt.role == DebtRole.CREDITOR) debt.value else -debt.value
                }
            RemoteDebtsUiInfo(diffResult, totalValue)
        }
        .observeOn(AndroidSchedulers.mainThread())
        .replay(1)

    private var debtSourceConnection: Disposable? = null

    private var previousDeletedDebt: RemoteDebt? = null

    init {
        initSource()
        connectivityListener.listen(isConnectionAvailable::onNext).addTo(compositeDisposable)
    }

    fun initSource() {
        debtSourceConnection?.dispose()
        debtSourceConnection = debtSource.connect()
    }

    fun complete(debt: RemoteDebt) {
        val status =
            if (debt.role == DebtRole.DEBTOR) WAIT_FOR_COMPLETE_FROM_CREDITOR else WAIT_FOR_COMPLETE_FROM_DEBTOR
        val firestoreDebt = debt.toFirestoreDebt(status)
        debtRepository.update(debt.id, firestoreDebt).subscribe({
            command.onNext(Command.ShowMessage(R.string.snackbar_msg_info_sent))
        }, {
            command.onNext(Command.ShowMessage(R.string.common_error_load))
        }).addTo(compositeDisposable)
    }

    fun update(info: Pair<RemoteDebtListAdapter.Actions, RemoteDebt>) {
        when (val action = getDebtAction(info.first, info.second.status)) {
            is DebtAction.OneSideDelete -> with(info.second) {
                if (isLocal) fullDelete(this) else oneSideDelete(this)
            }
            is DebtAction.AddReject -> rejectDebtAdd(info.second)
            is DebtAction.AddAccept -> acceptDebtAdd(info.second)
            is DebtAction.FullDelete -> fullDelete(info.second)
            is DebtAction.EditReject -> rejectDebtEdit(info.second.id)
            is DebtAction.EditAccept -> acceptDebtEdit(info.second)
            is DebtAction.Update -> updateDebt(info.second, action.status)
            is DebtAction.Resend -> resendDebt(info.second)
        }
    }

    private fun updateDebt(
        debt: RemoteDebt,
        status: Int
    ) {
        val firestoreDebt = debt.toFirestoreDebt(status = status)
        debtRepository.update(debt.id, firestoreDebt).subscribe({
            command.onNext(Command.ShowMessage(R.string.snackbar_msg_updated))
        }, {
            command.onNext(Command.ShowMessage(R.string.common_error_load))
        }).addTo(compositeDisposable)
    }

    fun restoreDebt() {
        val debt = previousDeletedDebt ?: return
        debtRepository.update(debt.id, debt.toFirestoreDebt())
            .onErrorComplete()
            .subscribe()
            .addTo(compositeDisposable)
    }

    private fun resendDebt(debt: RemoteDebt) {
        val firestoreDebt =
            debt.toFirestoreDebt(
                status = FirestoreDebtStatus.WAIT_FOR_CONFIRMATION,
                isFirstTimeAdded = false
            )
        debtRepository.update(debt.id, firestoreDebt).subscribe({
            command.onNext(Command.ShowMessage(R.string.snackbar_msg_resent))
        }, {
            command.onNext(Command.ShowMessage(R.string.common_error_load))
        }).addTo(compositeDisposable)
    }

    private fun acceptDebtEdit(debt: RemoteDebt) {
        val firestoreDebt = debt.toFirestoreDebt(status = IN_PROGRESS)
        debtRepository.update(debt.id, firestoreDebt)
            .mergeWith(debtRepository.delete("${debt.id}_tmp"))
            .subscribe({
                command.onNext(Command.ShowMessage(R.string.snackbar_msg_accepted))
            }, {
                command.onNext(Command.ShowMessage(R.string.common_error_load))
            }).addTo(compositeDisposable)
    }

    private fun rejectDebtEdit(id: String) {
        debtRepository.getSingle("${id}_tmp")
            .flatMapCompletable {
                debtRepository.update(
                    id, FirestoreRemoteDebt(
                        it.creditorUid,
                        it.debtorUid,
                        it.value,
                        it.description,
                        it.date,
                        it.status,
                        it.initPersonUid,
                        DebtDeleteStatus.NOT_DELETED,
                        it.isFirstTimeAdded,
                        userRepository.currentUserBaseInformation.uid
                    )
                )
            }.andThen(debtRepository.delete("${id}_tmp"))
            .subscribe({
                command.onNext(Command.ShowMessage(R.string.snackbar_msg_restored))
            }, {
                command.onNext(Command.ShowMessage(R.string.common_error_load))
            }).addTo(compositeDisposable)
    }

    private fun acceptDebtAdd(debt: RemoteDebt) {
        val firestoreDebt = debt.toFirestoreDebt(status = IN_PROGRESS)
        debtRepository.update(debt.id, firestoreDebt)
            .subscribe({
                dismissNotification(debt)
                command.onNext(Command.ShowMessage(R.string.snackbar_msg_accepted))
            }, {
                command.onNext(Command.ShowMessage(R.string.common_error_load))
            }).addTo(compositeDisposable)
    }

    private fun rejectDebtAdd(debt: RemoteDebt) {
        val firestoreDebt = debt.toFirestoreDebt(status = CONFIRMATION_REJECTED)
        debtRepository.update(debt.id, firestoreDebt)
            .subscribe({
                dismissNotification(debt)
                command.onNext(Command.ShowMessage(R.string.snackbar_msg_rejected))
            }, {
                command.onNext(Command.ShowMessage(R.string.common_error_load))
            }).addTo(compositeDisposable)
    }

    private fun oneSideDelete(debt: RemoteDebt) {
        previousDeletedDebt = debt
        val deleteStatus = if (debt.role == DebtRole.DEBTOR) {
            DebtDeleteStatus.DELETED_FROM_DEBTOR
        } else {
            DebtDeleteStatus.DELETED_FROM_CREDITOR
        }
        val firestoreDebt = debt.toFirestoreDebt(deleteStatus = deleteStatus)
        debtRepository.update(debt.id, firestoreDebt).subscribe({
            command.onNext(Command.ShowUndoDeletionSnackbar)
        }, {
            command.onNext(Command.ShowMessage(R.string.common_error_load))
        }).addTo(compositeDisposable)
    }

    private fun fullDelete(debt: RemoteDebt) {
        previousDeletedDebt = debt
        debtRepository.delete(debt.id).subscribe({
            command.onNext(Command.ShowUndoDeletionSnackbar)
        }, {
            command.onNext(Command.ShowMessage(R.string.common_error_load))
        }).addTo(compositeDisposable)
    }

    private fun RemoteDebt.toFirestoreDebt(
        status: Int = this.status,
        deleteStatus: Int = when {
            !isLocal -> DebtDeleteStatus.NOT_DELETED
            role == DebtRole.CREDITOR -> DebtDeleteStatus.DELETED_FROM_DEBTOR
            role == DebtRole.DEBTOR -> DebtDeleteStatus.DELETED_FROM_CREDITOR
            else -> throw IllegalArgumentException()
        },
        isFirstTimeAdded: Boolean = this.isFirstTimeAdded
    ) = FirestoreRemoteDebt(
        if (role == DebtRole.CREDITOR) userRepository.currentUserBaseInformation.uid else user.uid,
        if (role == DebtRole.DEBTOR) userRepository.currentUserBaseInformation.uid else user.uid,
        value,
        description,
        date,
        status,
        if (isCurrentUserInit) userRepository.currentUserBaseInformation.uid else user.uid,
        deleteStatus,
        isFirstTimeAdded,
        userRepository.currentUserBaseInformation.uid
    )

    private fun getDebtAction(action: RemoteDebtListAdapter.Actions, currentStatus: Int) =
        when (action) {
            RemoteDebtListAdapter.Actions.ACCEPT -> when (currentStatus) {
                WAIT_FOR_CONFIRMATION -> DebtAction.AddAccept
                WAIT_FOR_COMPLETE_FROM_CREDITOR, WAIT_FOR_COMPLETE_FROM_DEBTOR ->
                    DebtAction.Update(COMPLETE)
                WAIT_FOR_EDIT_CONFIRMATION_FROM_CREDITOR, WAIT_FOR_EDIT_CONFIRMATION_FROM_DEBTOR ->
                    DebtAction.EditAccept
                else -> throw IllegalArgumentException()
            }
            RemoteDebtListAdapter.Actions.REJECT -> when (currentStatus) {
                WAIT_FOR_CONFIRMATION -> DebtAction.AddReject
                WAIT_FOR_COMPLETE_FROM_CREDITOR -> DebtAction.Update(COMPLETION_REJECTED_BY_CREDITOR)
                WAIT_FOR_COMPLETE_FROM_DEBTOR -> DebtAction.Update(COMPLETION_REJECTED_BY_DEBTOR)
                WAIT_FOR_EDIT_CONFIRMATION_FROM_CREDITOR ->
                    DebtAction.Update(EDIT_CONFIRMATION_REJECTED_BY_CREDITOR)
                WAIT_FOR_EDIT_CONFIRMATION_FROM_DEBTOR -> DebtAction.Update(
                    EDIT_CONFIRMATION_REJECTED_BY_DEBTOR
                )
                else -> throw IllegalArgumentException()
            }
            RemoteDebtListAdapter.Actions.OK -> when (currentStatus) {
                CONFIRMATION_REJECTED -> DebtAction.FullDelete
                EDIT_CONFIRMATION_REJECTED_BY_CREDITOR, EDIT_CONFIRMATION_REJECTED_BY_DEBTOR ->
                    DebtAction.EditReject
                else -> DebtAction.Update(IN_PROGRESS)
            }
            RemoteDebtListAdapter.Actions.DELETE -> DebtAction.OneSideDelete
            RemoteDebtListAdapter.Actions.RESEND -> DebtAction.Resend
        }

    private fun dismissNotification(debt: RemoteDebt) {
        val personName = with(debt.user) { "$firstName $lastName" }
        val value = if (debt.role == DebtRole.CREDITOR) debt.value else -debt.value
        NotificationCreateData(debt.id, personName, value).run {
            notificationManager.dismiss(hashCode())
        }
    }

    private sealed class DebtAction {
        class Update(val status: Int) : DebtAction()
        object AddReject : DebtAction()
        object AddAccept : DebtAction()
        object OneSideDelete : DebtAction()
        object FullDelete : DebtAction()
        object EditReject : DebtAction()
        object EditAccept : DebtAction()
        object Resend : DebtAction()
    }

    sealed class Command {
        class ShowMessage(@StringRes val msgId: Int) : Command()
        object ShowUndoDeletionSnackbar : Command()
    }
}