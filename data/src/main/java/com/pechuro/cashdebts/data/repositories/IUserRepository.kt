package com.pechuro.cashdebts.data.repositories

import com.pechuro.cashdebts.data.model.FirestoreUser
import com.pechuro.cashdebts.data.model.UserBaseInformation
import io.reactivex.Completable
import io.reactivex.Single

interface IUserRepository {
    val currentUserBaseInformation: UserBaseInformation

    fun get(uid: String = currentUserBaseInformation.uid): Single<FirestoreUser>

    fun isUserExist(uid: String = currentUserBaseInformation.uid): Single<Boolean>

    fun setUser(user: FirestoreUser, uid: String = currentUserBaseInformation.uid): Completable
}