package com.omixam.novabrowser

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*

class MainActivity : Activity() {
    private data class Tab(val web: WebView, var title: String = "Nueva pestaña", var url: String = HOME)
    private val tabs = mutableListOf<Tab>()
    private var active = 0
    private lateinit var root: LinearLayout
    private lateinit var address: EditText
    private lateinit var pageHost: FrameLayout
    private lateinit var tabCount: TextView
    private lateinit var progress: ProgressBar
    private val prefs by lazy { getSharedPreferences("nova_browser", MODE_PRIVATE) }
    private val bookmarks get() = prefs.getStringSet("bookmarks", emptySet())!!.toMutableSet()
    private val history get() = prefs.getStringSet("history", emptySet())!!.toMutableSet()

    companion object { private const val HOME = "nova://home" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(7, 11, 22)
        window.navigationBarColor = Color.rgb(7, 11, 22)
        buildUi()
        addTab(HOME)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun bg(color: Int, radius: Int = 14, stroke: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color); cornerRadius = dp(radius).toFloat()
            if (stroke != 0) setStroke(dp(1), stroke)
        }

    private fun label(text: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text; textSize = size; setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) setTypeface(null, Typeface.BOLD)
        }

    private fun button(text: String, action: () -> Unit): TextView =
        label(text, 17f, Color.WHITE, true).apply {
            background = bg(Color.rgb(24, 34, 57), 12, Color.rgb(44, 75, 112))
            setPadding(dp(12), dp(9), dp(12), dp(9))
            setOnClickListener { action() }
        }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(6))
            setBackgroundColor(Color.rgb(7, 11, 22))
        }
        val brandRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        val brand = TextView(this).apply {
            text = "NOVA"; textSize = 22f; setTextColor(Color.rgb(72, 203, 255))
            setTypeface(null, Typeface.BOLD); letterSpacing = .16f
        }
        val sub = label(" BROWSER", 10f, Color.rgb(150, 173, 205), true)
        brandRow.addView(brand)
        brandRow.addView(sub)
        brandRow.addView(Space(this), LinearLayout.LayoutParams(0, 1, 1f))
        tabCount = button("1") { showTabs() }
        brandRow.addView(tabCount, LinearLayout.LayoutParams(dp(42), dp(38)))
        root.addView(brandRow, LinearLayout.LayoutParams(-1, dp(42)))

        val addressRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = bg(Color.rgb(17, 25, 43), 16, Color.rgb(35, 65, 100))
            setPadding(dp(10), 0, dp(6), 0)
        }
        address = EditText(this).apply {
            hint = "Buscar o escribir una dirección"
            setHintTextColor(Color.rgb(125, 145, 175))
            setTextColor(Color.WHITE); textSize = 14f
            singleLine = true; setSelectAllOnFocus(true)
            setBackgroundColor(Color.TRANSPARENT)
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, action, event ->
                if (action == EditorInfo.IME_ACTION_GO || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                    navigateFromAddress(); true
                } else false
            }
        }
        addressRow.addView(TextView(this).apply {
            text = "⌕"; textSize = 22f; setTextColor(Color.rgb(65, 194, 255))
        }, LinearLayout.LayoutParams(dp(28), -2))
        addressRow.addView(address, LinearLayout.LayoutParams(0, dp(48), 1f))
        addressRow.addView(button("➜") { navigateFromAddress() }, LinearLayout.LayoutParams(dp(42), dp(38)))
        root.addView(addressRow, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8) })

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(46, 190, 255))
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(17, 25, 43))
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(2)))

        pageHost = FrameLayout(this).apply { setBackgroundColor(Color.rgb(7, 11, 22)) }
        root.addView(pageHost, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            setPadding(0, dp(7), 0, 0)
        }
        val items = listOf(
            "‹" to { goBack() }, "›" to { goForward() },
            "⌂" to { loadHome() }, "★" to { saveBookmark() },
            "↻" to { reload() }, "⋮" to { showMenu() }
        )
        items.forEach { (txt, fn) ->
            nav.addView(button(txt, fn), LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                marginEnd = dp(5)
            })
        }
        root.addView(nav, LinearLayout.LayoutParams(-1, dp(52)))
        setContentView(root)
    }

    private fun createWebView(): WebView {
        return WebView(this).apply {
            setBackgroundColor(Color.rgb(7, 11, 22))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return false
                    if (url.startsWith("http://") || url.startsWith("https://")) return false
                    return true
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    val tab = tabs.getOrNull(active) ?: return
                    tab.url = if (url == "https://nova.local/") HOME else (url ?: tab.url)
                    tab.title = view?.title?.takeIf { it.isNotBlank() } ?: tab.url
                    if (address.hasFocus().not()) address.setText(tab.url)
                    progress.progress = 0
                    if (tab.url.startsWith("http")) {
                        val h = history; h.add(tab.url)
                        prefs.edit().putStringSet("history", h).apply()
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progress.progress = newProgress
                    progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.INVISIBLE
                }
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    tabs.getOrNull(active)?.title = title ?: "NOVA"
                }
            }
        }
    }

    private fun addTab(url: String = HOME) {
        tabs.getOrNull(active)?.web?.visibility = View.GONE
        val web = createWebView()
        val tab = Tab(web)
        tabs.add(tab); active = tabs.lastIndex
        pageHost.addView(web, FrameLayout.LayoutParams(-1, -1))
        web.visibility = View.VISIBLE
        tabCount.text = tabs.size.toString()
        load(tab, url)
        updateAddress()
    }

    private fun load(tab: Tab, target: String) {
        if (target == HOME) {
            tab.url = HOME
            tab.web.loadDataWithBaseURL("https://nova.local/", homeHtml(), "text/html", "UTF-8", null)
        } else tab.web.loadUrl(target)
    }

    private fun homeHtml() = """
        <!doctype html><html><meta name="viewport" content="width=device-width,initial-scale=1">
        <style>
        *{box-sizing:border-box}body{margin:0;background:radial-gradient(ellipse at 50% 0%,#142e52,#080d1b 65%);color:#edf7ff;font-family:Arial,sans-serif;min-height:100vh;display:flex;align-items:center;justify-content:center;text-align:center}
        .wrap{width:90%;max-width:520px}.logo{font-size:54px;font-weight:900;letter-spacing:8px;color:#48caff;text-shadow:0 0 30px #168dff}.tag{color:#9ab5d5;letter-spacing:3px;font-size:11px;margin:8px 0 34px}
        .grid{display:grid;grid-template-columns:repeat(2,1fr);gap:12px;margin-top:28px}a{color:#dff5ff;text-decoration:none;background:#12223a;border:1px solid #23486d;border-radius:14px;padding:17px 8px;font-size:14px}
        .hint{color:#8099b8;font-size:12px;margin-top:30px}
        </style><div class="wrap"><div class="logo">NOVA</div><div class="tag">EXPLORE WITHOUT LIMITS</div>
        <div class="grid"><a href="https://www.google.com">Google</a><a href="https://www.youtube.com">YouTube</a><a href="https://www.wikipedia.org">Wikipedia</a><a href="https://www.github.com">GitHub</a></div>
        <div class="hint">Tu espacio. Tu internet. Tu navegador.</div></div></html>
    """.trimIndent()

    private fun normalize(raw: String): String {
        val value = raw.trim()
        if (value.isEmpty()) return HOME
        if (value.contains(" ") || (!value.contains(".") && !value.startsWith("http"))) {
            return "https://www.google.com/search?q=" + java.net.URLEncoder.encode(value, "UTF-8")
        }
        return if (value.startsWith("http://") || value.startsWith("https://")) value else "https://$value"
    }

    private fun navigateFromAddress() {
        val target = normalize(address.text.toString())
        tabs.getOrNull(active)?.let { load(it, target) }
        address.clearFocus()
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(address.windowToken, 0)
    }

    private fun updateAddress() {
        address.setText(tabs.getOrNull(active)?.url?.takeUnless { it == HOME } ?: "")
        tabCount.text = tabs.size.toString()
    }
    private fun loadHome() { tabs.getOrNull(active)?.let { load(it, HOME) }; address.setText("") }
    private fun goBack() { tabs.getOrNull(active)?.web?.let { if (it.canGoBack()) it.goBack() else loadHome() } }
    private fun goForward() { tabs.getOrNull(active)?.web?.let { if (it.canGoForward()) it.goForward() } }
    private fun reload() { tabs.getOrNull(active)?.web?.reload() }

    private fun switchTab(index: Int) {
        if (index !in tabs.indices) return
        tabs.getOrNull(active)?.web?.visibility = View.GONE
        active = index
        tabs[active].web.visibility = View.VISIBLE
        pageHost.bringChildToFront(tabs[active].web)
        updateAddress()
    }

    private fun showTabs() {
        val names = tabs.mapIndexed { i, t -> "${i + 1}. ${t.title.take(34)}" }.toMutableList()
        names.add("＋ Nueva pestaña")
        AlertDialog.Builder(this).setTitle("Pestañas (${tabs.size})")
            .setItems(names.toTypedArray()) { _, which ->
                if (which == tabs.size) addTab() else switchTab(which)
            }
            .setNeutralButton("Cerrar pestaña") { _, _ -> closeActiveTab() }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun closeActiveTab() {
        if (tabs.size <= 1) { loadHome(); return }
        val old = tabs.removeAt(active)
        pageHost.removeView(old.web); old.web.destroy()
        active = (active - 1).coerceAtLeast(0)
        tabs[active].web.visibility = View.VISIBLE
        pageHost.bringChildToFront(tabs[active].web)
        updateAddress()
    }

    private fun saveBookmark() {
        val url = tabs.getOrNull(active)?.url ?: return
        if (!url.startsWith("http")) { toast("Abre una página para guardarla"); return }
        val b = bookmarks; b.add(url); prefs.edit().putStringSet("bookmarks", b).apply()
        toast("Guardado en favoritos")
    }

    private fun showMenu() {
        val options = arrayOf("Nueva pestaña", "Favoritos", "Historial", "Compartir página", "Información")
        AlertDialog.Builder(this).setTitle("NOVA Browser").setItems(options) { _, which ->
            when (which) {
                0 -> addTab()
                1 -> showSaved("Favoritos", bookmarks)
                2 -> showSaved("Historial", history)
                3 -> {
                    val url = tabs.getOrNull(active)?.url ?: return@setItems
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, url)
                    }
                    startActivity(android.content.Intent.createChooser(intent, "Compartir con"))
                }
                4 -> AlertDialog.Builder(this).setTitle("NOVA Browser 1.0")
                    .setMessage("Navegador Android creado con WebView. Las páginas web pueden recopilar datos según sus propias políticas.")
                    .setPositiveButton("OK", null).show()
            }
        }.show()
    }

    private fun showSaved(title: String, values: Set<String>) {
        if (values.isEmpty()) { toast("Todavía no hay elementos"); return }
        val list = values.toList().reversed()
        AlertDialog.Builder(this).setTitle(title).setItems(list.toTypedArray()) { _, which ->
            tabs.getOrNull(active)?.let { load(it, list[which]) }
        }.setNegativeButton("Cerrar", null).show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() { goBack() }

    override fun onDestroy() {
        tabs.forEach { it.web.destroy() }
        tabs.clear()
        super.onDestroy()
    }
}
