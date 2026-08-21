package com.ourspace.app

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private val exec = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("ourspace", Context.MODE_PRIVATE) }
    private var session: Session? = null
    private var role = "boy"
    private var coupleId: String? = null
    private var selectedImage: Uri? = null
    private lateinit var root: FrameLayout
    private lateinit var status: TextView

    private val boyBg = Color.rgb(18, 8, 24)
    private val boyBg2 = Color.rgb(30, 11, 39)
    private val girlBg = Color.rgb(48, 13, 38)
    private val girlBg2 = Color.rgb(77, 20, 58)
    private val glass = Color.argb(82, 255, 255, 255)
    private val glassStrong = Color.argb(112, 255, 255, 255)
    private val pinkAccent = Color.rgb(255, 105, 177)
    private val pinkBright = Color.rgb(255, 169, 211)
    private val blueAccent = Color.rgb(88, 174, 255)
    private val white = Color.rgb(255, 247, 252)
    private val muted = Color.rgb(220, 190, 211)
    private val errorColor = Color.rgb(255, 151, 180)
    private val notificationChannel = "heartly_moments"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.rgb(14, 7, 18)
        createNotificationChannel()
        requestNotificationPermissionIfNeeded()

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
                } catch (e: Exception) {
                    runOnUiThread { showLogin(e.message ?: "Session expired") }
                }
            }
        } else showLogin(null)
    }

    override fun onDestroy() {
        exec.shutdownNow()
        super.onDestroy()
    }

    private fun showLoading() {
        root = FrameLayout(this)
        root.background = pageBackground(boyBg, boyBg2)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        val logo = image(R.drawable.heartly_art, 190, 190)
        box.addView(logo, lp(190, 190, 0, 0))
        val t = text("HEARTLY", 30f, white, Typeface.DEFAULT_BOLD, Gravity.CENTER)
        box.addView(t, lp(-1, WRAP, 0, 8))
        box.addView(text("Your little world, just for two.", 15f, muted, Typeface.DEFAULT, Gravity.CENTER), lp(-1, WRAP))
        root.addView(box, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        animateIn(box)
    }

    private fun showLogin(error: String?) {
        role = if (role == "girl") "girl" else "boy"
        root = FrameLayout(this)
        root.background = pageBackground(if (role == "boy") boyBg else girlBg, if (role == "boy") boyBg2 else girlBg2)
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            setClipToPadding(false)
            setPadding(dp(10), dp(8), dp(10), dp(18))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(12), dp(8), dp(12), dp(12))
        }
        scroll.addView(content)
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        addFloatingHearts(root)

        val hero = image(R.drawable.heartly_art, -1, 0)
        hero.scaleType = ImageView.ScaleType.CENTER_CROP
        hero.background = roundedStroke(Color.argb(90, 255, 153, 208), Color.argb(55, 255, 255, 255), 34f, 2)
        hero.clipToOutline = true
        hero.outlineProvider = RoundedOutline(34f)
        content.addView(hero, lp(-1, dp(300), 0, 0))

        val title = text("HEARTLY", 42f, white, Typeface.DEFAULT_BOLD, Gravity.CENTER)
        title.letterSpacing = 0.04f
        content.addView(title, lp(-1, WRAP, 0, 2))
        content.addView(text("Your little world, just for two.", 17f, Color.rgb(245, 220, 236), Typeface.DEFAULT, Gravity.CENTER), lp(-1, WRAP, 0, 12))

        val selector = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedStroke(Color.argb(50, 255, 255, 255), Color.argb(125, 255, 113, 179), 34f, 1)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        val boyCard = roleCard("boy", R.drawable.boy_art, "Boy", "blue bat-cat theme")
        val girlCard = roleCard("girl", R.drawable.girl_art, "Girl", "pink kitty theme")
        selector.addView(boyCard, LinearLayout.LayoutParams(0, dp(82), 1f))
        selector.addView(girlCard, LinearLayout.LayoutParams(0, dp(82), 1f).apply { marginStart = dp(2) })
        content.addView(selector, lp(-1, dp(90), 0, 12))

        val email = field(R.drawable.ic_mail, "Email", false)
        val pass = field(R.drawable.ic_lock, "Password", true)
        val name = field(R.drawable.ic_person, "Display name (for new account)", false)
        content.addView(email, lp(-1, dp(62), 0, 9))
        content.addView(pass, lp(-1, dp(62), 0, 9))
        content.addView(name, lp(-1, dp(62), 0, 12))

        val login = primaryButton("Log in", R.drawable.ic_heart)
        val register = outlineButton("Create account", R.drawable.ic_heart)
        content.addView(login, lp(-1, dp(66), 0, 10))
        content.addView(register, lp(-1, dp(64), 0, 10))

        status = text(error ?: "Private by design • just for two.", 14f, if (error == null) muted else errorColor, Typeface.DEFAULT, Gravity.CENTER)
        status.setPadding(dp(8), dp(8), dp(8), dp(10))
        content.addView(status, lp(-1, WRAP))
        content.addView(text("Having trouble?\nWe're here to help", 15f, Color.rgb(236, 204, 226), Typeface.DEFAULT, Gravity.CENTER), lp(-1, WRAP, 0, 8))

        updateRoleCards(boyCard, girlCard)
        boyCard.setOnClickListener { play(R.raw.sfx_select); role = "boy"; updateRoleCards(boyCard, girlCard); animateTheme(root); }
        girlCard.setOnClickListener { play(R.raw.sfx_select); role = "girl"; updateRoleCards(boyCard, girlCard); animateTheme(root); }

        login.setOnClickListener {
            play(R.raw.sfx_tap)
            if (email.text().isBlank() || pass.text().isBlank()) { play(R.raw.sfx_error); status.text = "Enter your email and password."; status.setTextColor(errorColor); shake(login); return@setOnClickListener }
            busy(true, "Opening your little world…")
            exec.execute {
                try {
                    val s = Api.signIn(email.text(), pass.text())
                    saveSession(s, role)
                    role = Api.getProfileRole(s)
                    val c = Api.getCouple(s)
                    runOnUiThread {
                        play(R.raw.sfx_success)
                        busy(false, "")
                        if (c != null) { coupleId = c.id; notifyUser("Welcome back", "Your Heartly space is ready."); showHome() } else showPair()
                    }
                } catch (e: Exception) {
                    runOnUiThread { play(R.raw.sfx_error); busy(false, e.message ?: "Login failed"); status.setTextColor(errorColor); shake(login) }
                }
            }
        }

        register.setOnClickListener {
            play(R.raw.sfx_tap)
            if (email.text().isBlank() || pass.text().length < 6) { play(R.raw.sfx_error); status.text = "Use a valid email and a password of at least 6 characters."; status.setTextColor(errorColor); shake(register); return@setOnClickListener }
            busy(true, "Creating your little world…")
            exec.execute {
                try {
                    val chosen = role
                    val s = Api.signUp(email.text(), pass.text())
                        ?: throw Exception("Account created. If email confirmation is enabled, confirm your email, then log in.")
                    Api.createProfile(s, name.text(), chosen)
                    saveSession(s, chosen)
                    runOnUiThread { play(R.raw.sfx_success); busy(false, ""); notifyUser("Heartly created", "Now connect the two phones."); showPair() }
                } catch (e: Exception) {
                    runOnUiThread { play(R.raw.sfx_error); busy(false, e.message ?: "Registration failed"); status.setTextColor(errorColor); shake(register) }
                }
            }
        }

        setContentView(root)
        animateIn(content)
        pulse(title)
    }

    private fun roleCard(roleName: String, art: Int, heading: String, caption: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(4), dp(8), dp(4))
        }
        val img = ImageView(this).apply { setImageResource(art); scaleType = ImageView.ScaleType.CENTER_CROP; clipToOutline = true }
        card.addView(img, LinearLayout.LayoutParams(dp(66), dp(72)))
        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(7), 0, 0, 0) }
        labels.addView(text(heading, 17f, white, Typeface.DEFAULT_BOLD, Gravity.START))
        labels.addView(text(caption, 12f, Color.rgb(220, 195, 216), Typeface.DEFAULT, Gravity.START))
        card.addView(labels, LinearLayout.LayoutParams(0, -1, 1f))
        card.tag = roleName
        return card
    }

    private fun updateRoleCards(boy: View, girl: View) {
        val selectedBoy = role == "boy"
        boy.background = if (selectedBoy) roundedStroke(Color.argb(120, 75, 165, 255), Color.rgb(87, 165, 255), 28f, 2) else roundedFill(Color.argb(30, 255,255,255), 28f)
        girl.background = if (!selectedBoy) roundedStroke(Color.argb(120, 255, 90, 172), Color.rgb(255, 111, 179), 28f, 2) else roundedFill(Color.argb(30, 255,255,255), 28f)
        boy.scaleX = if (selectedBoy) 1f else .97f; boy.scaleY = boy.scaleX
        girl.scaleX = if (!selectedBoy) 1f else .97f; girl.scaleY = girl.scaleX
    }

    private fun showPair() {
        root = FrameLayout(this)
        root.background = pageBackground(if (role == "boy") boyBg else girlBg, if (role == "boy") boyBg2 else girlBg2)
        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(dp(22), dp(28), dp(22), dp(30)) }
        scroll.addView(content); root.addView(scroll, FrameLayout.LayoutParams(-1,-1)); addFloatingHearts(root)
        content.addView(image(R.drawable.heartly_art, -1, 0).apply { scaleType = ImageView.ScaleType.CENTER_CROP }, lp(-1, dp(250), 0, 14))
        content.addView(text("Connect your two hearts", 29f, white, Typeface.DEFAULT_BOLD, Gravity.CENTER), lp(-1, WRAP, 0, 4))
        content.addView(text("One code • two phones • one little world.", 15f, muted, Typeface.DEFAULT, Gravity.CENTER), lp(-1, WRAP, 0, 22))
        val create = primaryButton("Create our couple code", R.drawable.ic_heart)
        val code = field(R.drawable.ic_lock, "Enter 6-character code", false)
        val join = outlineButton("Join with code", R.drawable.ic_arrow)
        status = text("", 15f, white, Typeface.DEFAULT, Gravity.CENTER)
        content.addView(create, lp(-1, dp(64), 0, 10)); content.addView(text("or", 13f, muted, Typeface.DEFAULT, Gravity.CENTER), lp(-1, WRAP, 0, 6)); content.addView(code, lp(-1, dp(62), 0, 8)); content.addView(join, lp(-1, dp(62), 0, 10)); content.addView(status, lp(-1, WRAP, 0, 8))

        create.setOnClickListener {
            play(R.raw.sfx_tap); busy(true, "Making your private code…")
            exec.execute {
                try {
                    val result = Api.createCouple(session!!, role)
                    prefs.edit().putString("couple", result.first).apply()
                    runOnUiThread { play(R.raw.sfx_success); coupleId = result.first; status.text = "Your code\n${result.second}\n\nShare it with your partner."; status.textSize = 20f; loadAfterPair(); notifyUser("Couple code created", "Share your Heartly code with your partner.") }
                } catch (e: Exception) { runOnUiThread { play(R.raw.sfx_error); status.text = e.message ?: "Could not create couple." } }
            }
        }
        join.setOnClickListener {
            play(R.raw.sfx_tap); busy(true, "Connecting…")
            exec.execute {
                try {
                    val result = Api.joinCouple(session!!, role, code.text())
                    prefs.edit().putString("couple", result.first).apply()
                    runOnUiThread { play(R.raw.sfx_success); coupleId = result.first; notifyUser("Heartly connected", "Your shared space is ready."); showHome() }
                } catch (e: Exception) { runOnUiThread { play(R.raw.sfx_error); status.text = e.message ?: "Could not join couple." } }
            }
        }
        setContentView(root); animateIn(content)
    }

    private fun loadAfterPair() {
        status.append("\nWaiting for your partner…")
        val refresh = outlineButton("Open our space", R.drawable.ic_arrow)
        (root.getChildAt(0) as? ScrollView)?.let { (it.getChildAt(0) as LinearLayout).addView(refresh, lp(-1, dp(62), 0, 10)) }
        refresh.setOnClickListener {
            play(R.raw.sfx_tap)
            exec.execute {
                try {
                    val c = Api.getCouple(session!!)
                    runOnUiThread { if (c != null && c.boyId != null && c.girlId != null) { play(R.raw.sfx_success); coupleId = c.id; showHome() } else Toast.makeText(this, "Your partner has not joined yet.", Toast.LENGTH_SHORT).show() }
                } catch (e: Exception) { runOnUiThread { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() } }
            }
        }
    }

    private fun showHome() {
        root = FrameLayout(this)
        root.background = pageBackground(if (role == "boy") boyBg else girlBg, if (role == "boy") boyBg2 else girlBg2)
        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(24)) }
        scroll.addView(content); root.addView(scroll, FrameLayout.LayoutParams(-1,-1)); addFloatingHearts(root)

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(image(if (role == "boy") R.drawable.boy_art else R.drawable.girl_art, dp(62), dp(62)), LinearLayout.LayoutParams(dp(62), dp(62)))
        val headText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10),0,0,0) }
        headText.addView(text("HEARTLY", 24f, white, Typeface.DEFAULT_BOLD, Gravity.START))
        headText.addView(text("Notes • photos • little moments", 12f, muted, Typeface.DEFAULT, Gravity.START))
        header.addView(headText, LinearLayout.LayoutParams(0, WRAP, 1f))
        val logout = iconButton(R.drawable.ic_logout, accent())
        header.addView(logout, LinearLayout.LayoutParams(dp(52), dp(52)))
        content.addView(header, lp(-1, dp(62), 0, 12))

        val hero = ImageView(this).apply { setImageResource(R.drawable.heartly_art); scaleType = ImageView.ScaleType.CENTER_CROP; background = roundedStroke(Color.argb(90,255,130,190), Color.argb(50,255,255,255), 30f, 2); clipToOutline = true; outlineProvider = RoundedOutline(30f) }
        content.addView(hero, lp(-1, dp(250), 0, 12))

        val addText = field(R.drawable.ic_heart, "Write something for your partner…", false)
        val choose = outlineButton("Add a photo", R.drawable.ic_camera)
        val send = primaryButton("Send to our space", R.drawable.ic_heart)
        content.addView(addText, lp(-1, dp(62), 0, 8)); content.addView(choose, lp(-1, dp(60), 0, 8)); content.addView(send, lp(-1, dp(64), 0, 10))
        status = text("Loading shared moments…", 13f, muted, Typeface.DEFAULT, Gravity.CENTER); content.addView(status, lp(-1, WRAP, 0, 6))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(list, lp(-1, WRAP, 0, 0))

        choose.setOnClickListener { play(R.raw.sfx_tap); pickImage() }
        send.setOnClickListener {
            play(R.raw.sfx_tap)
            val text = addText.text()
            if (text.isBlank() && selectedImage == null) { play(R.raw.sfx_error); Toast.makeText(this, "Write a note or choose a photo.", Toast.LENGTH_SHORT).show(); shake(send); return@setOnClickListener }
            send.disabled(true); busy(true, "Sending to your little world…")
            exec.execute {
                try {
                    var imageUrl: String? = null
                    selectedImage?.let { uri -> val pair = Api.readBytes(uri, contentResolver); imageUrl = Api.uploadImage(session!!, coupleId!!, pair.first, pair.second) }
                    Api.addNote(session!!, coupleId!!, text, imageUrl)
                    selectedImage = null
                    runOnUiThread { play(R.raw.sfx_send); addText.setText(""); send.disabled(false); status.text = "Sent to your little world"; notifyUser("Moment shared", "Your new moment is now in Heartly."); loadNotes(list); pop(send) }
                } catch (e: Exception) { runOnUiThread { play(R.raw.sfx_error); send.disabled(false); status.text = e.message ?: "Couldn't send" } }
            }
        }
        logout.setOnClickListener { play(R.raw.sfx_tap); prefs.edit().clear().apply(); session = null; coupleId = null; showLogin(null) }
        setContentView(root); animateIn(content); loadNotes(list)
        sendBroadcast(Intent(this, OurSpaceWidgetProvider::class.java).apply { action = AppWidgetManager.ACTION_APPWIDGET_UPDATE; putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(this@MainActivity).getAppWidgetIds(ComponentName(this@MainActivity, OurSpaceWidgetProvider::class.java))) })
    }

    private fun loadNotes(list: LinearLayout) {
        exec.execute {
            try {
                val notes = Api.listNotes(session!!, coupleId!!)
                runOnUiThread {
                    list.removeAllViews()
                    notes.forEach { n ->
                        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = roundedStroke(Color.argb(35,255,255,255), accent(), 24f, 1); setPadding(dp(18), dp(15), dp(18), dp(15)) }
                        card.addView(text(if (n.userId == session!!.userId) "From you" else "From your love", 12f, accent(), Typeface.DEFAULT_BOLD, Gravity.START))
                        card.addView(text(n.text ?: "Photo shared", 16f, white, Typeface.DEFAULT, Gravity.START).apply { setPadding(0, dp(7), 0, dp(5)) })
                        card.addView(text(n.createdAt.replace("T", " ").take(16), 11f, muted, Typeface.DEFAULT, Gravity.START))
                        list.addView(card, lp(-1, WRAP, 0, 8))
                        if (n.imageUrl != null) {
                            val img = ImageView(this).apply { adjustViewBounds = true; scaleType = ImageView.ScaleType.CENTER_CROP; background = roundedFill(Color.argb(40,255,255,255), 22f); clipToOutline = true; outlineProvider = RoundedOutline(22f) }
                            list.addView(img, lp(-1, dp(230), 0, 8))
                            exec.execute { val b = Api.downloadBitmap(n.imageUrl); runOnUiThread { if (b != null) { img.setImageBitmap(b); pop(img) } } }
                        }
                    }
                    status.text = "Shared moments • ${notes.size}"
                }
            } catch (e: Exception) { runOnUiThread { status.text = e.message ?: "Couldn't load moments" } }
        }
    }

    private fun pickImage() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }, 42)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42 && resultCode == RESULT_OK) {
            selectedImage = data?.data
            play(R.raw.sfx_select)
            Toast.makeText(this, "Photo selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun field(icon: Int, hint: String, password: Boolean): LinearLayout {
        val edit = EditText(this).apply {
            this.hint = hint
            textSize = 15f
            setTextColor(white)
            setHintTextColor(Color.rgb(210, 178, 201))
            setSingleLine(true)
            setPadding(0, 0, dp(12), 0)
            background = null
            inputType = if (password) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT
        }
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedStroke(Color.argb(45,255,255,255), if (role == "boy") Color.rgb(89,142,201) else Color.rgb(224,104,166), 31f, 1)
            setPadding(dp(15), 0, dp(7), 0)
        }
        val iconView = ImageView(this).apply { setImageResource(icon); alpha = .78f }
        wrap.addView(iconView, LinearLayout.LayoutParams(dp(28), dp(28)).apply { marginEnd = dp(10) })
        wrap.addView(edit, LinearLayout.LayoutParams(0, -1, 1f))
        if (password) {
            val eye = iconButton(R.drawable.ic_eye, Color.TRANSPARENT)
            eye.setPadding(0,0,0,0)
            eye.background = null
            eye.setOnClickListener {
                val hidden = (edit.inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD) == InputType.TYPE_TEXT_VARIATION_PASSWORD
                edit.inputType = if (hidden) InputType.TYPE_CLASS_TEXT else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                edit.setSelection(edit.text.length)
                eye.setImageResource(if (hidden) R.drawable.ic_eye_off else R.drawable.ic_eye)
                play(R.raw.sfx_tap)
            }
            wrap.addView(eye, LinearLayout.LayoutParams(dp(46), dp(52)))
        }
        wrap.tag = edit
        return wrap
    }

    private fun LinearLayout.text(): String = (tag as? EditText)?.text?.toString() ?: ""

    private fun LinearLayout.setText(value: String) {
        (tag as? EditText)?.setText(value)
    }

    private fun LinearLayout.disabled(value: Boolean) {
        isEnabled = !value
        alpha = if (value) 0.55f else 1f
    }

    private fun primaryButton(label: String, icon: Int): LinearLayout = actionButton(label, icon, true)
    private fun outlineButton(label: String, icon: Int): LinearLayout = actionButton(label, icon, false)

    private fun actionButton(label: String, icon: Int, filled: Boolean): LinearLayout {
        val box = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(16),0,dp(16),0) }
        box.background = if (filled) roundedGradient(pinkAccent, Color.rgb(222,62,137), 32f) else roundedStroke(Color.argb(50,255,255,255), pinkAccent, 32f, 2)
        val iconView = ImageView(this).apply { setImageResource(icon); setColorFilter(white); alpha = .92f }
        box.addView(iconView, LinearLayout.LayoutParams(dp(23), dp(23)).apply { marginEnd = dp(10) })
        box.addView(text(label, 16f, white, Typeface.DEFAULT_BOLD, Gravity.CENTER), LinearLayout.LayoutParams(WRAP, WRAP))
        box.setOnClickListener { play(R.raw.sfx_tap) }
        return box
    }

    private fun iconButton(icon: Int, bg: Int): ImageButton = ImageButton(this).apply {
        setImageResource(icon); setColorFilter(white); scaleType = ImageView.ScaleType.CENTER; setBackgroundColor(bg); contentDescription = null
    }

    private fun accent() = if (role == "boy") blueAccent else pinkAccent

    private fun saveSession(s: Session, r: String) { session = s; role = r; prefs.edit().putString("access", s.accessToken).putString("refresh", s.refreshToken).putString("uid", s.userId).putString("role", r).apply() }

    private fun busy(on: Boolean, msg: String) { if (::status.isInitialized) { status.text = msg; status.alpha = if (on) .72f else 1f } }

    private fun addFloatingHearts(parent: FrameLayout) {
        repeat(7) { i ->
            val h = ImageView(this).apply { setImageResource(R.drawable.ic_heart); setColorFilter(if (i % 2 == 0) pinkBright else pinkAccent); alpha = .12f + (i % 3) * .06f }
            val size = dp(12 + (i % 3) * 5)
            val p = FrameLayout.LayoutParams(size, size).apply { leftMargin = dp(20 + (i * 53) % 320); topMargin = dp(80 + (i * 97) % 650) }
            parent.addView(h, p)
            h.animate().translationY(-dp(30 + i * 6).toFloat()).alpha(.28f).setDuration(2600L + i * 260).setStartDelay(i * 180L).withEndAction { h.animate().translationY(0f).alpha(.12f).setDuration(2200L).start() }.start()
        }
    }

    private fun animateIn(v: View) { v.alpha = 0f; v.translationY = dp(18).toFloat(); v.animate().alpha(1f).translationY(0f).setDuration(520).setInterpolator(AccelerateDecelerateInterpolator()).start() }
    private fun animateTheme(v: View) { v.animate().alpha(.55f).setDuration(90).withEndAction { v.animate().alpha(1f).setDuration(220).start() }.start() }
    private fun pulse(v: View) { v.animate().scaleX(1.025f).scaleY(1.025f).setDuration(900).withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(900).withEndAction { pulse(v) }.start() }.start() }
    private fun pop(v: View) { v.scaleX=.94f;v.scaleY=.94f;v.alpha=.65f;v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(300).start() }
    private fun shake(v: View) { v.animate().translationX(dp(8).toFloat()).setDuration(50).withEndAction { v.animate().translationX(-dp(8).toFloat()).setDuration(50).withEndAction { v.animate().translationX(0f).setDuration(50).start() }.start() }.start() }

    private fun play(res: Int) { try { MediaPlayer.create(this, res)?.apply { setOnCompletionListener { it.release() }; start() } } catch (_: Exception) {} }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(notificationChannel, "Heartly moments", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Heartly activity notifications" })
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 77)
    }

    private fun notifyUser(title: String, message: String) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = getSystemService(NotificationManager::class.java)
        val intent = Intent(this, MainActivity::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        val pending = PendingIntent.getActivity(this, 101, intent, flags)
        val notification = android.app.Notification.Builder(this, notificationChannel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    private fun pageBackground(top: Int, bottom: Int) = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(top, Color.rgb(64, 17, 50), bottom))
    private fun roundedFill(c: Int, radius: Float) = GradientDrawable().apply { setColor(c); cornerRadius = radius }
    private fun roundedStroke(fill: Int, stroke: Int, radius: Float, width: Int) = GradientDrawable().apply { setColor(fill); setStroke(width, stroke); cornerRadius = radius }
    private fun roundedGradient(start: Int, end: Int, radius: Float) = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(start, end)).apply { cornerRadius = radius }
    private fun text(t: String, size: Float, c: Int, typeface: Typeface, gravity: Int) = TextView(this).apply { text=t;textSize=size;setTextColor(c);setTypeface(typeface);this.gravity=gravity }
    private fun image(res: Int, w: Int, h: Int) = ImageView(this).apply { setImageResource(res); scaleType=ImageView.ScaleType.CENTER_CROP }
    private fun lp(w: Int = -1, h: Int = WRAP, top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(w, h).apply { setMargins(0, top, 0, bottom) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
    private class RoundedOutline(private val r: Float) : android.view.ViewOutlineProvider() { override fun getOutline(view: View, outline: android.graphics.Outline) { outline.setRoundRect(0,0,view.width,view.height,r) } }
    companion object { const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT }
}
