package com.example.tripexpensetracker.ui.common

import android.content.Context
import android.provider.ContactsContract

data class ContactData(val name: String, val phoneNumber: String)

object ContactUtils {
    fun getAllContacts(context: Context): List<ContactData> {
        val contacts = mutableListOf<ContactData>()
        val contentResolver = context.contentResolver
        
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                if (nameIndex >= 0 && numberIndex >= 0) {
                    val name = it.getString(nameIndex)
                    val number = it.getString(numberIndex)
                    // Basic normalization
                    val normalizedNumber = number?.replace(Regex("[^0-9+]"), "")
                    if (!normalizedNumber.isNullOrBlank()) {
                        contacts.add(ContactData(name ?: "Unknown", normalizedNumber))
                    }
                }
            }
        }
        return contacts.distinctBy { it.phoneNumber } // rudimentary dedup
    }
}
