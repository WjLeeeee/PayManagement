package com.woojin.paymanagement.android.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.woojin.paymanagement.android.shared.FirebaseSharedRoomRepository
import com.woojin.paymanagement.domain.repository.SharedRoomRepository
import com.woojin.paymanagement.utils.PreferencesManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val sharedRoomModule = module {
    single<SharedRoomRepository> {
        FirebaseSharedRoomRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance(),
            preferencesManager = get<PreferencesManager>()
        )
    }
}
