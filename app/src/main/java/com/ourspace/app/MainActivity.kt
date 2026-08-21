package com.ourspace.app

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val exec = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("ourspace", Context.MODE_PRIVATE) }
    private var session: Session? = null
    private var role = "boy"
    private var coupleId: String? = null
    private var selectedImage: Uri? = null
    private lateinit var root: LinearLayout
    private lateinit var status: TextView

    private val blue = Color.rgb(7, 17, 31)
    private val blueCard = Color.rgb(13, 32, 56)
    private val blueAccent = Color.rgb(85, 183, 255)
    private val pink = Color.rgb(255, 240, 247)
    private val pinkCard = Color.rgb(255, 225, 239)
    private val pinkAccent = Color.rgb(240, 106, 171)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = prefs.getString("access", null)
        val uid = prefs.getString("uid", null)
        val refresh = prefs.getString("refresh", "") ?: ""
        if (!token.isNullOrBlank() && !uid.isNullOrBlank()) {
            session = Session(token, refresh, uid)
            role = prefs.getString("role", "boy") ?: "boy"
            showLoading()
            exec.execute {
                try {
                    val c = Api.getCouple(session!!)
                    runOnUiThread { if (c != null) { coupleId = c.id; showHome() } else showPair() }
                } catch (e: Exception) { runOnUiThread { showLogin(e.message ?: "Session expired") } }
            }
        } else showLogin(null)
    }

    private fun showLoading() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setBackgroundColor(blue) }
        val t = TextView(this).apply { text = "HEARTLY 💙\nLoading..."; textSize = 24f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        root.addView(t)
        setContentView(root)
    }

    private fun showLogin(error: String?) {
        root = baseRoot(blue)
        val title = title("HEARTLY 💙", Color.WHITE, 30f)
        root.addView(title)
        root.addView(sub("Your little world, just for two.", Color.LTGRAY))
        val email = input("Email", false)
        val pass = input("Password", true)
        val name = input("Display name (for new account)", false)
        val roleSpinner = Spinner(this)
        roleSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Boy • blue bat-cat theme", "Girl • pink cat theme"))
        root.addView(roleSpinner, lp())
        val login = button("Log in", blueAccent)
        val register = button("Create account", Color.rgb(40,70,100))
        root.addView(email, lp()); root.addView(pass, lp()); root.addView(name, lp())
        root.addView(login, lp()); root.addView(register, lp())
        status = sub(error ?: "Use Supabase email/password.", Color.LTGRAY); root.addView(status)
        login.setOnClickListener {
            busy(true, "Logging in...")
            exec.execute {
                try {
                    val s = Api.signIn(email.text.toString().trim(), pass.text.toString())
                    saveSession(s, if (roleSpinner.selectedItemPosition == 0) "boy" else "girl")
                    val r = Api.getProfileRole(s)
                    role = r
                    val c = Api.getCouple(s)
                    runOnUiThread { busy(false, ""); if (c != null) { coupleId=c.id; showHome() } else showPair() }
                } catch(e: Exception) { runOnUiThread { busy(false, e.message ?: "Login failed") } }
            }
        }
        register.setOnClickListener {
            busy(true, "Creating account...")
            exec.execute {
                try {
                    val chosen = if (roleSpinner.selectedItemPosition == 0) "boy" else "girl"
                    val s = Api.signUp(email.text.toString().trim(), pass.text.toString())
                        ?: throw Exception("Account created. If email confirmation is enabled, confirm the email, then log in.")
                    Api.createProfile(s, name.text.toString().trim(), chosen)
                    saveSession(s, chosen); role = chosen
                    runOnUiThread { busy(false, ""); showPair() }
                } catch(e: Exception) { runOnUiThread { busy(false, e.message ?: "Registration failed") } }
            }
        }
        setContentView(root)
    }

    private fun showPair() {
        root = baseRoot(if (role=="boy") blue else pink)
        val tc = if (role=="boy") Color.WHITE else Color.rgb(90,30,60)
        val accent = if (role=="boy") blueAccent else pinkAccent
        root.addView(title(if (role=="boy") "🐈‍⬛ OUR SPACE 💙" else "🎀 OUR SPACE 💗", tc, 28f))
        root.addView(sub("First, connect the two phones.", if(role=="boy") Color.LTGRAY else Color.DKGRAY))
        val create = button("Create a couple code", accent)
        val code = input("Enter 6-character code", false)
        val join = button("Join with code", accent)
        status = sub("", tc)
        root.addView(create, lp()); root.addView(sub("or", tc)); root.addView(code, lp()); root.addView(join, lp()); root.addView(status)
        create.setOnClickListener {
            exec.execute {
                try {
                    val result=Api.createCouple(session!!,role); prefs.edit().putString("couple",result.first).apply()
                    runOnUiThread { coupleId=result.first; status.text="Your code: ${result.second}\nGive this code to your partner."; status.textSize=22f; loadAfterPair() }
                } catch(e:Exception){runOnUiThread{status.text=e.message}}
            }
        }
        join.setOnClickListener {
            exec.execute {
                try {
                    val result=Api.joinCouple(session!!,role,code.text.toString()); prefs.edit().putString("couple",result.first).apply()
                    runOnUiThread { coupleId=result.first; showHome() }
                } catch(e:Exception){runOnUiThread{status.text=e.message}}
            }
        }
        setContentView(root)
    }

    private fun loadAfterPair() {
        status.append("\nWaiting for your partner... You can open Home when connected.")
        val refresh = button("Open our space", if(role=="boy") blueAccent else pinkAccent)
        root.addView(refresh, lp())
        refresh.setOnClickListener {
            exec.execute {
                try {
                    val c=Api.getCouple(session!!)
                    runOnUiThread { if(c!=null && c.boyId!=null && c.girlId!=null){coupleId=c.id;showHome()}else Toast.makeText(this,"Partner has not joined yet.",Toast.LENGTH_SHORT).show() }
                }catch(e:Exception){runOnUiThread{Toast.makeText(this,e.message,Toast.LENGTH_SHORT).show()}}
            }
        }
    }

    private fun showHome() {
        root=baseRoot(if(role=="boy") blue else pink)
        val tc=if(role=="boy") Color.WHITE else Color.rgb(70,25,50)
        val accent=if(role=="boy") blueAccent else pinkAccent
        val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        header.addView(title(if(role=="boy") "🐈‍⬛ Heartly 💙" else "🎀 Heartly 💗",tc,26f), LinearLayout.LayoutParams(0,WRAP,-1f))
        val logout=button("↪",accent); header.addView(logout, LinearLayout.LayoutParams(55,55))
        root.addView(header)
        root.addView(sub("Notes • Photos • Little moments", if (role == "boy") Color.LTGRAY else Color.DKGRAY))
        val addText=input("Write something for your partner...",false)
        val choose=button("📸 Choose photo",accent)
        val send=button("Send to our space",accent)
        root.addView(addText,lp()); root.addView(choose,lp()); root.addView(send,lp())
        status=sub("Loading shared notes...",tc); root.addView(status)
        val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        root.addView(list, LinearLayout.LayoutParams(-1,0,1f))
        choose.setOnClickListener{pickImage()}
        send.setOnClickListener{
            val text=addText.text.toString().trim()
            if(text.isBlank() && selectedImage==null){Toast.makeText(this,"Write a note or choose a photo.",Toast.LENGTH_SHORT).show();return@setOnClickListener}
            send.disabled(true)
            exec.execute {
                try{
                    var imageUrl:String?=null
                    selectedImage?.let{uri->val pair=Api.readBytes(uri,contentResolver);imageUrl=Api.uploadImage(session!!,coupleId!!,pair.first,pair.second)}
                    Api.addNote(session!!,coupleId!!,text,imageUrl)
                    selectedImage=null
                    runOnUiThread{addText.setText("");send.disabled(false);status.text="Sent ❤️";loadNotes(list)}
                }catch(e:Exception){runOnUiThread{send.disabled(false);status.text=e.message}}
            }
        }
        logout.setOnClickListener{prefs.edit().clear().apply();session=null;showLogin(null)}
        setContentView(root)
        loadNotes(list)
        // Refresh the home-screen widget immediately after opening the home.
        sendBroadcast(Intent(this,OurSpaceWidgetProvider::class.java).apply{action=AppWidgetManager.ACTION_APPWIDGET_UPDATE;putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,AppWidgetManager.getInstance(this@MainActivity).getAppWidgetIds(ComponentName(this@MainActivity,OurSpaceWidgetProvider::class.java)))})
    }

    private fun loadNotes(list:LinearLayout){
        exec.execute{
            try{
                val notes=Api.listNotes(session!!,coupleId!!)
                runOnUiThread{
                    list.removeAllViews()
                    notes.forEach{n->
                        val card=TextView(this).apply{
                            text=(if(n.userId==session!!.userId)"You 💙\n" else "Your love 💗\n")+(n.text ?: "📸 Photo shared")+"\n\n"+n.createdAt.replace("T"," ").take(16)
                          textSize=16f
setTextColor(if (role == "boy") Color.WHITE else Color.rgb(80,30,55))
setPadding(24,22,24,22)
background=rounded(if (role == "boy") blueCard else pinkCard)
                        }
                        list.addView(card,lp())
                        if(n.imageUrl!=null){
                            val img=ImageView(this);img.adjustViewBounds=true;img.scaleType=ImageView.ScaleType.CENTER_CROP
                            list.addView(img,lp())
                            exec.execute{val b=Api.downloadBitmap(n.imageUrl);runOnUiThread{if(b!=null)img.setImageBitmap(b)}}
                        }
                    }
                    status.text="Shared moments: ${notes.size}"
                }
            }catch(e:Exception){runOnUiThread{status.text=e.message}}
        }
    }

    private fun pickImage(){
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="image/*";addCategory(Intent.CATEGORY_OPENABLE)},42)
    }
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==42&&resultCode==RESULT_OK){selectedImage=data?.data;Toast.makeText(this,"Photo selected 📸",Toast.LENGTH_SHORT).show()}
    }

    private fun saveSession(s:Session,r:String){session=s;role=r;prefs.edit().putString("access",s.accessToken).putString("refresh",s.refreshToken).putString("uid",s.userId).putString("role",r).apply()}
    private fun baseRoot(bg:Int)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,30,24,24);setBackgroundColor(bg)}
    private fun title(t:String,c:Int,size:Float)=TextView(this).apply{text=t;textSize=size.toFloat();setTextColor(c);setTypeface(null,android.graphics.Typeface.BOLD);gravity=Gravity.CENTER}
    private fun sub(t:String,c:Int)=TextView(this).apply{text=t;textSize=14f;setTextColor(c);setPadding(4,10,4,14)}
    private fun input(h:String,pass:Boolean)=EditText(this).apply{hint=h;textSize=16f;setPadding(18,10,18,10);setSingleLine(false);if(pass)inputType=0x81}
    private fun button(t:String,c:Int)=Button(this).apply{text=t;textSize=15f;setTextColor(Color.WHITE);background=rounded(c);isAllCaps=false}
    private fun rounded(c:Int)=GradientDrawable().apply{setColor(c);cornerRadius=22f}
    private fun lp()=LinearLayout.LayoutParams(-1,LinearLayout.LayoutParams.WRAP_CONTENT).apply{setMargins(0,7,0,7)}
    private fun busy(on:Boolean,msg:String){runOnUiThread{status.text=msg}}
    private fun Button.disabled(v:Boolean){isEnabled=!v;alpha=if(v).5f else 1f}
    companion object{const val WRAP=LinearLayout.LayoutParams.WRAP_CONTENT}
}
