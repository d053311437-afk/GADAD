package com.contacthub.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 35, 28, 20)
            setBackgroundColor(Color.rgb(245, 247, 252))
        }

        val title = TextView(this).apply {
            text = "מרכז אנשי הקשר"
            textSize = 28f
            setTextColor(Color.rgb(35, 45, 80))
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 25)
        }

        val search = EditText(this).apply {
            hint = "חיפוש אנשי קשר..."
            textDirection = android.view.View.TEXT_DIRECTION_RTL
        }

        val features = TextView(this).apply {
            text = "אנשי קשר • שיחות • פתקים • משימות • תמונות • קבצים"
            gravity = Gravity.CENTER
            textSize = 14f
            setPadding(0, 20, 0, 20)
        }

        val scroll = ScrollView(this)

        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scroll.addView(list)

        main.addView(title)
        main.addView(search)
        main.addView(features)
        main.addView(scroll)

        setContentView(main)

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS,
                    Manifest.permission.READ_CALL_LOG
                ),
                100
            )
        } else {
            loadContacts()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (
            requestCode == 100 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            loadContacts()
        }
    }

    private fun loadContacts() {

        list.removeAllViews()

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

            val nameIndex =
                it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)

            val numberIndex =
                it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {

                val name = it.getString(nameIndex) ?: "ללא שם"
                val number = it.getString(numberIndex) ?: ""

                val card = Button(this).apply {
                    text = "👤 $name`n$number`nשיחות | פתקים | תמונות | משימות | הגדרות"
                    textSize = 16f
                    isAllCaps = false
                    gravity = Gravity.RIGHT
                    setPadding(25, 20, 25, 20)
                }

                list.addView(card)
            }
        }
    }
}
