package pt.rikmartins.clubemg.mobile.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

fun createDataStore(producePathWithFileName: (String) -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath { producePathWithFileName(dataStoreFileName).toPath() }

private const val dataStoreFileName = "dice.preferences_pb"