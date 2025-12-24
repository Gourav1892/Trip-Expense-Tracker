package com.example.tripexpensetracker.data.manager

import android.content.Context
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ContactManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun getLocalContacts(): List<ContactInfo> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<ContactInfo>()
        val contentResolver = context.contentResolver
        
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        
        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            
            while (it.moveToNext()) {
                val name = if (nameIndex >= 0) it.getString(nameIndex) else "Unknown"
                val rawNumber = if (numberIndex >= 0) it.getString(numberIndex) else ""
                
                // Normalize number: remove spaces, dashes, parentheses
                val normalizedNumber = rawNumber.replace(Regex("[^0-9+]"), "")
                
                if (normalizedNumber.isNotBlank()) {
                    contacts.add(ContactInfo(name, normalizedNumber))
                }
            }
        }
        // distinctBy number to avoid duplicates for same number
        contacts.distinctBy { it.phoneNumber }
    }
}

data class ContactInfo(
    val name: String,
    val phoneNumber: String
)
