package com.ourspace.app

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val exec = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("heartly", Context.MODE_PRIVATE) }
    private var session: Session? = null
    private var role = "boy"
    private var coupleId: String? = null
    private var selectedImage: Uri? = null
    private lateinit var root: LinearLayout
    private lateinit var status: TextView

    private val boyBg = Color.rgb(7, 17, 34)
    private val boyCard = Color.rgb(16, 39, 68)
    private val boyCard2 = Color.rgb(21, 51, 86)
    private val boyAccent = Color.rgb(93, 190, 255)
    private val boyGlow = Color.rgb(44, 119, 205)

    private val girlBg = Color.rgb(67, 25, 57)
    private val girlCard = Color.rgb(111, 45, 84)
    private val girlCard2 = Color.rgb(132, 55, 96)
    private val girlAccent = Color.rgb(255, 112, 181)
    private val girlGlow = Color.rgb(221, 80, 146)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = boyBg
        window.navigationBarColor = boyBg
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
        } else {
            showLogin(null)
        }
    }

    private fun showLoading() {
        val bg = if (role == "boy") boyBg else girlBg
        root = baseRoot(bg)
        root.gravity = Gravity.CENTER
        val mark = TextView(this).apply {
            text = if (role == "boy") "🦇💙" else "🎀💗"
            textSize = 52f
            gravity = Gravity.CENTER
        }
        root.addView(mark)
        root.addView(title("HEARTLY", Color.WHITE, 30f))
        root.addView(sub("Your little world, just for two.", Color.LTGRAY))
        setContentView(root)
    }

    private fun showLogin(error: String?) {
        applyTheme("boy")
        val scroll = ScrollView(this).apply { overScrollMode = View.OVER_SCROLL_NEVER }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 20, 18, 24)
            setBackgroundColor(boyBg)
        }
        scroll.addView(root)

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val icon = heroIcon("🦇", "💙", 78)
        top.addView(icon, LinearLayout.LayoutParams(-1, 90))

        top.addView(title("HEARTLY", Color.WHITE, 38f))
        top.addView(sub("Your little world, just for two.", Color.rgb(215, 225, 240)).apply {
            gravity = Gravity.CENTER
            textSize = 16f
        })

        val hero = card(boyCard, 24f).apply { setPadding(18, 14, 18, 14) }
        val heroText = TextView(this).apply {
            text = "🦇  A tiny space made for\n       two people who care.  💙"
            textSize = 17f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans", Typeface.BOLD)
        }
        hero.addView(heroText)
        root.addView(top, lp(0, 0))
        root.addView(hero, lp(0, 8))

        val roleBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(5, 5, 5, 5)
            background = roundedGradient(Color.rgb(24, 43, 67), Color.rgb(31, 57, 87), 28f)
        }
        val boy = segment("🦇  Boy", true)
        val girl = segment("🎀  Girl", false)
        roleBox.addView(boy, LinearLayout.LayoutParams(0, 58, 1f).apply { setMargins(2,2,2,2) })
        roleBox.addView(girl, LinearLayout.LayoutParams(0, 58, 1f).apply { setMargins(2,2,2,2) })
        root.addView(roleBox, lp(0, 12))

        val email = input("✉  Email", false, Color.WHITE, Color.rgb(210,220,235))
        val pass = input("🔒  Password", true, Color.WHITE, Color.rgb(210,220,235))
        val name = input("♙  Display name (for new account)", false, Color.WHITE, Color.rgb(210,220,235))
        root.addView(email, lp(0, 12))
        root.addView(pass, lp(0, 10))
        root.addView(name, lp(0, 10))

        val login = primaryButton("Log in  ✦", boyAccent, Color.WHITE)
        val register = outlineButton("Create account   ♡", boyAccent)
        root.addView(login, lp(0, 14))
        root.addView(register, lp(0, 10))

        status = sub(error ?: "Private by design • just for two. ♡", Color.rgb(205, 215, 230)).apply {
            gravity = Gravity.CENTER
            textSize = 13f
            setPadding(8, 18, 8, 8)
        }
        root.addView(status)

        boy.setOnClickListener { role = "boy"; updateLoginRole(roleBox, boy, girl, email, pass, name, login, register) }
        girl.setOnClickListener { role = "girl"; updateLoginRole(roleBox, boy, girl, email, pass, name, login, register) }

        login.setOnClickListener {
            busy(true, "Opening your little world…")
            exec.execute {
                try {
                    val s = Api.signIn(email.text.toString().trim(), pass.text.toString())
                    saveSession(s, role)
                    val r = Api.getProfileRole(s)
                    role = r
                    val c = Api.getCouple(s)
                    runOnUiThread { busy(false, ""); if (c != null) { coupleId = c.id; showHome() } else showPair() }
                } catch (e: Exception) {
                    runOnUiThread { busy(false, friendlyError(e.message ?: "Login failed")) }
                }
            }
        }

        register.setOnClickListener {
            busy(true, "Creating your space…")
            exec.execute {
                try {
                    val chosen = role
                    val s = Api.signUp(email.text.toString().trim(), pass.text.toString())
                        ?: throw Exception("Account created. Please confirm your email, then log in.")
                    Api.createProfile(s, name.text.toString().trim(), chosen)
                    saveSession(s, chosen)
                    runOnUiThread { busy(false, ""); showPair() }
                } catch (e: Exception) {
                    runOnUiThread { busy(false, friendlyError(e.message ?: "Registration failed")) }
                }
            }
        }

        setContentView(scroll)
    }

    private fun updateLoginRole(
        box: LinearLayout, boy: TextView, girl: TextView,
        email: EditText, pass: EditText, name: EditText,
        login: Button, register: Button
    ) {
        val isBoy = role == "boy"
        val bg = if (isBoy) boyBg else girlBg
        val accent = if (isBoy) boyAccent else girlAccent
        val card = if (isBoy) boyCard else girlCard
        root.setBackgroundColor(bg)
        box.background = roundedGradient(if (isBoy) Color.rgb(24,43,67) else Color.rgb(92,39,72), if (isBoy) Color.rgb(31,57,87) else Color.rgb(115,48,86), 28f)
        boy.background = if (isBoy) roundedGradient(boyAccent, boyGlow, 24f) else rounded(Color.TRANSPARENT, 24f)
        girl.background = if (!isBoy) roundedGradient(girlAccent, girlGlow, 24f) else rounded(Color.TRANSPARENT, 24f)
        login.background = roundedGradient(accent, if (isBoy) boyGlow else girlGlow, 28f)
        register.background = strokeRounded(accent, 28f)
        for (v in listOf(email, pass, name)) {
            v.background = strokeRounded(if (isBoy) Color.rgb(80,150,210) else Color.rgb(230,120,175), 24f)
            v.setHintTextColor(if (isBoy) Color.rgb(185,205,225) else Color.rgb(242,195,218))
        }
    }

    private fun showPair() {
        applyTheme(role)
        root = baseRoot(if (role == "boy") boyBg else girlBg)
        root.gravity = Gravity.CENTER_HORIZONTAL

        root.addView(heroIcon(if (role == "boy") "🦇" else "🐱", if (role == "boy") "💙" else "💗", 68))
        root.addView(title("Connect your two hearts", textColor(), 28f))
        root.addView(sub("One code • two phones • one little world.", muted(), 14f).apply { gravity = Gravity.CENTER })

        val create = primaryButton("Create our couple code  ✦", accent(), Color.WHITE)
        val code = input("Enter 6-character code", false, textColor(), muted())
        val join = outlineButton("Join with code   ♡", accent())
        status = sub("", textColor()).apply { gravity = Gravity.CENTER; textSize = 15f }

        root.addView(create, lp(0, 24))
        root.addView(sub("or", muted()).apply { gravity = Gravity.CENTER })
        root.addView(code, lp(0, 6))
        root.addView(join, lp(0, 10))
        root.addView(status, lp(0, 12))

        create.setOnClickListener {
            busy(true, "Making your private code…")
            exec.execute {
                try {
                    val result = Api.createCouple(session!!, role)
                    prefs.edit().putString("couple", result.first).apply()
                    runOnUiThread {
                        busy(false, "Your code: ${result.second}\nShare it with your partner ♡")
                        status.textSize = 20f
                        coupleId = result.first
                        loadAfterPair()
                    }
                } catch (e: Exception) { runOnUiThread { busy(false, friendlyError(e.message ?: "")) } }
            }
        }

        join.setOnClickListener {
            busy(true, "Connecting…")
            exec.execute {
                try {
                    val result = Api.joinCouple(session!!, role, code.text.toString())
                    prefs.edit().putString("couple", result.first).apply()
                    runOnUiThread { coupleId = result.first; showHome() }
                } catch (e: Exception) { runOnUiThread { busy(false, friendlyError(e.message ?: "")) } }
            }
        }
        setContentView(root)
    }

    private fun loadAfterPair() {
        status.append("\nWaiting for your partner…")
        val refresh = outlineButton("Open our space  →", accent())
        root.addView(refresh, lp(0, 10))
        refresh.setOnClickListener {
            exec.execute {
                try {
                    val c = Api.getCouple(session!!)
                    runOnUiThread {
                        if (c != null && c.boyId != null && c.girlId != null) {
                            coupleId = c.id
                            showHome()
                        } else Toast.makeText(this, "Your partner hasn't joined yet ♡", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) { runOnUiThread { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() } }
            }
        }
    }

    private fun showHome() {
        applyTheme(role)
        val scroll = ScrollView(this).apply { overScrollMode = View.OVER_SCROLL_NEVER }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 20)
            setBackgroundColor(if (role == "boy") boyBg else girlBg)
        }
        scroll.addView(root)

        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(heroIcon(if (role == "boy") "🦇" else "🐱", if (role == "boy") "💙" else "💗", 48),
            LinearLayout.LayoutParams(60, 58))
        val headText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        headText.addView(title(if (role == "boy") "Heartly 💙" else "Heartly 💗", textColor(), 24f))
        headText.addView(sub("You & me", muted(), 12f).apply { setPadding(0,0,0,0) })
        head.addView(headText, LinearLayout.LayoutParams(0, 58, 1f))
        val profile = TextView(this).apply {
            text = "♡"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(accent())
            background = strokeRounded(accent(), 18f)
        }
        head.addView(profile, LinearLayout.LayoutParams(54,54))
        root.addView(head)

        val hero = card(if (role == "boy") boyCard else girlCard, 26f).apply { setPadding(20, 20, 20, 18) }
        val heroTitle = TextView(this).apply {
            text = if (role == "boy") "A little world\nbuilt just for us two 💙" else "A little world\nbuilt just for us two 💗"
            textSize = 23f
            setTextColor(textColor())
            typeface = Typeface.DEFAULT_BOLD
        }
        val heroArt = TextView(this).apply {
            text = if (role == "boy") "      🦇\n  🐈‍⬛  💙  🏙️" else "      🎀\n  🐱  💗  🧸"
            textSize = 35f
            gravity = Gravity.CENTER
        }
        hero.addView(heroTitle)
        hero.addView(heroArt)
        root.addView(hero, lp(0, 16))

        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        stats.addView(statCard("📝", "Notes", "0"), LinearLayout.LayoutParams(0, 94, 1f).apply { setMargins(0,0,5,0) })
        stats.addView(statCard("📷", "Photos", "0"), LinearLayout.LayoutParams(0, 94, 1f).apply { setMargins(5,0,0,0) })
        root.addView(stats, lp(0, 10))

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(statCard("💬", "Chat", "Soon"), LinearLayout.LayoutParams(0, 94, 1f).apply { setMargins(0,0,5,0) })
        actions.addView(statCard("💗", "Memories", "♡"), LinearLayout.LayoutParams(0, 94, 1f).apply { setMargins(5,0,0,0) })
        root.addView(actions, lp(0, 8))

        val special = card(if (role == "boy") boyCard2 else girlCard2, 22f).apply { setPadding(18, 16, 18, 16) }
        special.addView(TextView(this).apply {
            text = "Special Days"
            textSize = 17f
            setTextColor(textColor())
            typeface = Typeface.DEFAULT_BOLD
        })
        special.addView(sub("Your little milestones live here ♡", muted(), 13f).apply { setPadding(0,5,0,0) })
        root.addView(special, lp(0, 10))

        val addText = input("♡  Write something for your partner…", false, textColor(), muted())
        val choose = outlineButton("📷  Add a photo", accent())
        val send = primaryButton("Send to our space  ✦", accent(), Color.WHITE)
        root.addView(addText, lp(0, 14))
        root.addView(choose, lp(0, 8))
        root.addView(send, lp(0, 8))

        status = sub("Shared moments", muted(), 13f)
        root.addView(status, lp(0, 8))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list, lp(0, 0))

        choose.setOnClickListener { pickImage() }
        send.setOnClickListener {
            val text = addText.text.toString().trim()
            if (text.isBlank() && selectedImage == null) {
                Toast.makeText(this, "Write a note or choose a photo ♡", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            send.disabled(true)
            exec.execute {
                try {
                    var imageUrl: String? = null
                    selectedImage?.let { uri ->
                        val pair = Api.readBytes(uri, contentResolver)
                        imageUrl = Api.uploadImage(session!!, coupleId!!, pair.first, pair.second)
                    }
                    Api.addNote(session!!, coupleId!!, text, imageUrl)
                    selectedImage = null
                    runOnUiThread {
                        addText.setText("")
                        send.disabled(false)
                        status.text = "Sent to your little world ♡"
                        loadNotes(list)
                    }
                } catch (e: Exception) {
                    runOnUiThread { send.disabled(false); status.text = friendlyError(e.message ?: "Couldn't send") }
                }
            }
        }

        val nav = bottomNav()
        root.addView(nav, lp(0, 18))
        setContentView(scroll)
        loadNotes(list)

        sendBroadcast(Intent(this, OurSpaceWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_IDS,
                AppWidgetManager.getInstance(this@MainActivity)
                    .getAppWidgetIds(ComponentName(this@MainActivity, OurSpaceWidgetProvider::class.java))
            )
        })
    }

    private fun loadNotes(list: LinearLayout) {
        exec.execute {
            try {
                val notes = Api.listNotes(session!!, coupleId!!)
                runOnUiThread {
                    list.removeAllViews()
                    notes.forEach { n ->
                        val mine = n.userId == session!!.userId
                        val card = card(if (role == "boy") boyCard else girlCard, 20f).apply {
                            setPadding(18, 14, 18, 14)
                        }
                        val who = TextView(this).apply {
                            text = if (mine) "♡  From me" else "♡  From your love"
                            textSize = 12f
                            setTextColor(accent())
                            typeface = Typeface.DEFAULT_BOLD
                        }
                        val body = TextView(this).apply {
                            text = (n.text ?: "📷 Photo shared")
                            textSize = 16f
                            setTextColor(textColor())
                            setPadding(0, 8, 0, 4)
                        }
                        val time = TextView(this).apply {
                            text = n.createdAt.replace("T"," ").take(16)
                            textSize = 11f
                            setTextColor(muted())
                        }
                        card.addView(who); card.addView(body); card.addView(time)
                        list.addView(card, lp(0, 8))
                        if (n.imageUrl != null) {
                            val img = ImageView(this).apply {
                                adjustViewBounds = true
                                scaleType = ImageView.ScaleType.CENTER_CROP
                                background = rounded(if (role == "boy") boyCard2 else girlCard2, 20f)
                            }
                            list.addView(img, lp(0, 8))
                            exec.execute {
                                val b = Api.downloadBitmap(n.imageUrl)
                                runOnUiThread { if (b != null) img.setImageBitmap(b) }
                            }
                        }
                    }
                    status.text = "Shared moments • ${notes.size}"
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = friendlyError(e.message ?: "Couldn't load moments") }
            }
        }
    }

    private fun bottomNav(): LinearLayout {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(8, 7, 8, 7)
            background = rounded(if (role == "boy") Color.rgb(12,29,50) else Color.rgb(102,40,76), 28f)
        }
        listOf("⌂\nHome", "▤\nNotes", "＋\nAdd", "♡\nChat", "♙\nProfile").forEachIndexed { i, label ->
            val item = TextView(this).apply {
                text = label
                textSize = if (i == 2) 17f else 11f
                gravity = Gravity.CENTER
                setTextColor(if (i == 0) accent() else muted())
                typeface = if (i == 0) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            nav.addView(item, LinearLayout.LayoutParams(0, 58, 1f))
            item.setOnClickListener {
                when (i) {
                    0 -> showHome()
                    1 -> Toast.makeText(this, "Notes are shown on Home for now ♡", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(this, "Use the note box above to share ♡", Toast.LENGTH_SHORT).show()
                    3 -> Toast.makeText(this, "Private chat is coming next.", Toast.LENGTH_SHORT).show()
                    4 -> {
                        prefs.edit().clear().apply()
                        session = null
                        showLogin(null)
                    }
                }
            }
        }
        return nav
    }

    private fun statCard(icon: String, name: String, value: String): LinearLayout {
        val c = card(if (role == "boy") boyCard else girlCard, 20f).apply {
            setPadding(10, 10, 10, 8)
            gravity = Gravity.CENTER
        }
        c.addView(TextView(this).apply { text = icon; textSize = 23f; gravity = Gravity.CENTER })
        c.addView(TextView(this).apply {
            text = name
            textSize = 12f
            setTextColor(textColor())
            gravity = Gravity.CENTER
        })
        c.addView(TextView(this).apply {
            text = value
            textSize = 11f
            setTextColor(accent())
            gravity = Gravity.CENTER
        })
        return c
    }

    private fun segment(text: String, selected: Boolean): TextView = TextView(this).apply {
        this.text = text
        textSize = 14f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        background = if (selected) roundedGradient(boyAccent, boyGlow, 24f) else rounded(Color.TRANSPARENT, 24f)
    }

    private fun heroIcon(main: String, heart: String, size: Int): TextView = TextView(this).apply {
        text = "$main  $heart"
        textSize = (size / 2.0).toFloat()
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
    }

    private fun applyTheme(r: String) {
        role = r
        val bg = if (r == "boy") boyBg else girlBg
        window.statusBarColor = bg
        window.navigationBarColor = bg
    }

    private fun baseRoot(bg: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(22, 28, 22, 24)
        setBackgroundColor(bg)
    }

    private fun input(h: String, password: Boolean, text: Int, hint: Int) = EditText(this).apply {
        this.hint = h
        textSize = 15f
        setTextColor(text)
        setHintTextColor(hint)
        setSingleLine(true)
        setPadding(18, 0, 18, 0)
        background = strokeRounded(if (role == "boy") Color.rgb(67,128,178) else Color.rgb(207,101,155), 24f)
        if (password) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }

    private fun primaryButton(t: String, c: Int, textC: Int) = Button(this).apply {
        text = t
        textSize = 15f
        setTextColor(textC)
        typeface = Typeface.DEFAULT_BOLD
        isAllCaps = false
        background = roundedGradient(c, if (role == "boy") boyGlow else girlGlow, 28f)
        minHeight = 58
        stateListAnimator = null
    }

    private fun outlineButton(t: String, c: Int) = Button(this).apply {
        text = t
        textSize = 15f
        setTextColor(c)
        isAllCaps = false
        background = strokeRounded(c, 28f)
        minHeight = 56
        stateListAnimator = null
    }

    private fun title(t: String, c: Int, size: Float) = TextView(this).apply {
        text = t
        textSize = size
        setTextColor(c)
        typeface = Typeface.create("sans", Typeface.BOLD)
        gravity = Gravity.CENTER
    }

    private fun sub(t: String, c: Int, size: Float = 14f) = TextView(this).apply {
        text = t
        textSize = size
        setTextColor(c)
        setPadding(4, 8, 4, 10)
    }

    private fun card(c: Int, radius: Float) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(c, radius)
    }

    private fun rounded(c: Int, radius: Float) = GradientDrawable().apply {
        setColor(c)
        cornerRadius = radius
    }

    private fun strokeRounded(c: Int, radius: Float) = GradientDrawable().apply {
        setColor(Color.TRANSPARENT)
        setStroke(2, c)
        cornerRadius = radius
    }

    private fun roundedGradient(start: Int, end: Int, radius: Float) = GradientDrawable(
        GradientDrawable.Orientation.TL_BR, intArrayOf(start, end)
    ).apply { cornerRadius = radius }

    private fun lp(top: Int = 7, bottom: Int = 7) = LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
        setMargins(0, top, 0, bottom)
    }

    private fun textColor() = if (role == "boy") Color.WHITE else Color.rgb(255, 239, 247)
    private fun muted() = if (role == "boy") Color.rgb(178, 201, 225) else Color.rgb(244, 198, 221)
    private fun accent() = if (role == "boy") boyAccent else girlAccent

    private fun busy(on: Boolean, msg: String) {
        if (::status.isInitialized) {
            status.text = msg
            status.alpha = if (on) 0.75f else 1f
        }
    }

    private fun friendlyError(msg: String): String {
        return if (msg.contains("YOUR_PROJECT_ID", true) || msg.contains("your_project_id", true)) {
            "Supabase is not connected yet. Add your project URL and publishable key first."
        } else msg
    }

    private fun Button.disabled(v: Boolean) {
        isEnabled = !v
        alpha = if (v) .55f else 1f
    }

    private fun saveSession(s: Session, r: String) {
        session = s
        role = r
        prefs.edit().putString("access", s.accessToken).putString("refresh", s.refreshToken)
            .putString("uid", s.userId).putString("role", r).apply()
    }

    private fun pickImage() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }, 42)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42 && resultCode == RESULT_OK) {
            selectedImage = data?.data
            Toast.makeText(this, "Photo selected ♡", Toast.LENGTH_SHORT).show()
        }
    }
}
