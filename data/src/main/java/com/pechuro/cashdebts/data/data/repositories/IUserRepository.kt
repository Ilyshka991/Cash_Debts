package com.pechuro.cashdebts.data.data.repositories

import com.pechuro.cashdebts.data.data.model.FirestoreUser
import com.pechuro.cashdebts.data.data.model.UserBaseInformation
import io.reactivex.Completable
import io.reactivex.Single

interface IUserRepository {
    val currentUserBaseInformation: UserBaseInformation

    fun get(uid: String = currentUserBaseInformation.uid): Single<FirestoreUser>

    fun isUserWithUidExist(uid: String = currentUserBaseInformation.uid): Single<Boolean>

    fun getUidByPhone(phoneNumber: String): Single<String>

    fun updateUser(
        user: FirestoreUser,
        uid: String = currentUserBaseInformation.uid
    ): Completable
}