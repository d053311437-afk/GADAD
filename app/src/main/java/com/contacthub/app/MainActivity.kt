package com.contacthub.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

data class ContactItem(val name:String,val number:String)

class MainActivity : AppCompatActivity() {
    private lateinit var list: LinearLayout
    private lateinit var search: EditText
    private val contacts = mutableListOf<ContactItem>()
    private val prefs by lazy { getSharedPreferences("hub", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(25,118,210)

        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(24,28,24,16)
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.rgb(246,248,252))
        }
        root.addView(TextView(this).apply {
            text="מרכז אנשי קשר +"
            textSize=27f; gravity=Gravity.CENTER
            setTextColor(Color.rgb(20,60,110)); setPadding(0,12,0,12)
        })
        val top=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        search=EditText(this).apply {
            hint="חיפוש שם או מספר"; textDirection=View.TEXT_DIRECTION_RTL
            setSingleLine()
        }
        top.addView(search,LinearLayout.LayoutParams(0,WRAP_CONTENT,1f))
        top.addView(Button(this).apply { text="⚙"; setOnClickListener{showSettings()} })
        root.addView(top)

        val nav=HorizontalScrollView(this)
        val navRow=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        listOf("הכול","★ מועדפים","תגיות","משימות","גיבוי").forEach { label ->
            navRow.addView(Button(this).apply {
                text=label; isAllCaps=false
                setOnClickListener {
                    when(label){
                        "★ מועדפים"->render(search.text.toString(),true)
                        "תגיות"->toast("תגיות נשמרות לכל איש קשר")
                        "משימות"->showAllNotes()
                        "גיבוי"->showBackup()
                        else->render(search.text.toString(),false)
                    }
                }
            })
        }
        nav.addView(navRow); root.addView(nav)
        val scroll=ScrollView(this)
        list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        scroll.addView(list); root.addView(scroll,LinearLayout.LayoutParams(MATCH_PARENT,0,1f))
        setContentView(root)

        search.addTextChangedListener(object:android.text.TextWatcher{
            override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){}
            override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int){render(s?.toString()?:"",false)}
            override fun afterTextChanged(s:android.text.Editable?){}
        })
        requestAndLoad()
    }

    private fun requestAndLoad(){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.READ_CONTACTS,Manifest.permission.READ_CALL_LOG),100)
        else loadContacts()
    }
    override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){
        super.onRequestPermissionsResult(r,p,g)
        if(r==100 && ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED) loadContacts()
    }
    private fun loadContacts(){
        contacts.clear()
        val c=contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER),
            null,null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC")
        c?.use{
            val ni=it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val pi=it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while(it.moveToNext()) contacts.add(ContactItem(it.getString(ni)?:"ללא שם",it.getString(pi)?:""))
        }
        render("",false)
    }
    private fun key(n:String)=n.filter{it.isDigit()}.takeLast(10)
    private fun render(q:String,favs:Boolean){
        list.removeAllViews()
        val filtered=contacts.distinctBy{key(it.number)}.filter{
            (!favs || prefs.getBoolean("fav_"+key(it.number),false)) &&
            (q.isBlank() || it.name.contains(q,true) || it.number.contains(q))
        }
        if(filtered.isEmpty()) list.addView(TextView(this).apply{text="לא נמצאו אנשי קשר";gravity=Gravity.CENTER;textSize=18f;setPadding(0,60,0,0)})
        filtered.forEach{ct->
            val box=LinearLayout(this).apply{
                orientation=LinearLayout.VERTICAL; setPadding(18,14,18,14)
                setBackgroundColor(Color.WHITE)
            }
            val fav=prefs.getBoolean("fav_"+key(ct.number),false)
            box.addView(TextView(this).apply{
                text=(if(fav)"★ " else "☆ ")+ct.name+"\n"+ct.number
                textSize=18f; setTextColor(Color.rgb(25,55,95))
                setOnClickListener{toggleFav(ct)}
            })
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
            fun b(t:String, action:()->Unit)=Button(this).apply{text=t;isAllCaps=false;setOnClickListener{action()}}
            row.addView(b("☎"){startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+ct.number)))},LinearLayout.LayoutParams(0,WRAP_CONTENT,1f))
            row.addView(b("פרטים"){showContact(ct)},LinearLayout.LayoutParams(0,WRAP_CONTENT,1f))
            row.addView(b("פתק"){editNote(ct)},LinearLayout.LayoutParams(0,WRAP_CONTENT,1f))
            box.addView(row)
            list.addView(box,LinearLayout.LayoutParams(MATCH_PARENT,WRAP_CONTENT).apply{setMargins(0,7,0,7)})
        }
    }
    private fun toggleFav(c:ContactItem){
        val k="fav_"+key(c.number); prefs.edit().putBoolean(k,!prefs.getBoolean(k,false)).apply(); render(search.text.toString(),false)
    }
    private fun editNote(c:ContactItem){
        val input=EditText(this).apply{
            hint="פתק, משימה, תזכורת או תגית"
            setText(prefs.getString("note_"+key(c.number),""))
        }
        AlertDialog.Builder(this).setTitle(c.name+" — פתק אישי").setView(input)
            .setPositiveButton("שמור"){_,_->prefs.edit().putString("note_"+key(c.number),input.text.toString()).apply()}
            .setNegativeButton("ביטול",null).show()
    }
    private fun showContact(c:ContactItem){
        val note=prefs.getString("note_"+key(c.number),"")?:""
        AlertDialog.Builder(this).setTitle(c.name).setMessage(
            "מספר: "+c.number+"\n\nפתק/משימה: "+(if(note.isBlank())"אין" else note)+
            "\n\nכלים: חיוג • מועדף • פתק • תגית • שיתוף • פתיחה באנשי הקשר")
            .setPositiveButton("פתח באנשי קשר"){_,_->
                startActivity(Intent(Intent.ACTION_VIEW,Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,Uri.encode(c.number))))
            }.setNeutralButton("שתף"){_,_->startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,c.name+" "+c.number)},"שיתוף"))}
            .setNegativeButton("סגור",null).show()
    }
    private fun showAllNotes(){
        val s=contacts.mapNotNull{c->prefs.getString("note_"+key(c.number),"")?.takeIf{it.isNotBlank()}?.let{c.name+": "+it}}.joinToString("\n\n")
        AlertDialog.Builder(this).setTitle("פתקים ומשימות").setMessage(if(s.isBlank())"עדיין אין פתקים" else s).setPositiveButton("סגור",null).show()
    }
    private fun showBackup(){
        val data=contacts.filter{prefs.getBoolean("fav_"+key(it.number),false)}.joinToString("\n"){it.name+" | "+it.number}
        AlertDialog.Builder(this).setTitle("גיבוי וייצוא").setMessage("ייצוא מהיר של המועדפים. פתקים נשמרים מקומית במכשיר.")
            .setPositiveButton("שתף גיבוי"){_,_->startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,data)},"גיבוי"))}
            .setNegativeButton("סגור",null).show()
    }
    private fun showSettings(){
        val options=arrayOf("מצב כהה/בהיר לפי המכשיר","גודל טקסט","הרשאות אנשי קשר ושיחות","פרטיות — הנתונים נשמרים מקומית","איפוס פתקים ומועדפים")
        AlertDialog.Builder(this).setTitle("הגדרות").setItems(options){_,which->
            if(which==2) startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")))
            if(which==4) AlertDialog.Builder(this).setTitle("איפוס?").setPositiveButton("אפס"){_,_->prefs.edit().clear().apply();render("",false)}.setNegativeButton("ביטול",null).show()
            if(which!=2 && which!=4) toast(options[which])
        }.setNegativeButton("סגור",null).show()
    }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
