package com.pechuro.cashdebts.data.data.exception

abstract class FirebaseStorageExceptions(msg: String? = null) : Exception(msg)

class FirebaseStorageCommonException(msg: String? = null) : FirebaseStorageExceptions(msg)

