package com.streamil.tv

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Android / Android TV lean UI; no account, analytics or trackers. */
class MainActivity : Activity() {
    data class Station(val name:String,val url:String,val group:String,val kind:String)
    private val prefs by lazy { getSharedPreferences("streamil",MODE_PRIVATE) }
    private val executor = Executors.newSingleThreadExecutor()
    private val stations = mutableListOf<Station>()
    private val favorites = mutableSetOf<String>()
    private var filter = "TV"
    private var query = ""
    private var player:ExoPlayer? = null
    private lateinit var root:LinearLayout
    private lateinit var list:LinearLayout
    private lateinit var status:TextView
    private lateinit var playerView:PlayerView
    private val bg=Color.rgb(13,19,33)
    private val accent=Color.rgb(25,115,210)

    override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState)
        window.decorView.layoutDirection=View.LAYOUT_DIRECTION_RTL
        favorites.addAll(prefs.getStringSet("favorites", emptySet()) ?: emptySet())
        restore(); makeUi()
        if(stations.isEmpty()) loadStarter()
        else render()
    }
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun text(s:String,size:Float=18f):TextView = TextView(this).apply {
        this.text=s; textSize=size; setTextColor(Color.WHITE); setPadding(dp(12),dp(10),dp(12),dp(10)); gravity=Gravity.CENTER_VERTICAL
    }
    private fun button(s:String, action:()->Unit):Button = Button(this).apply {
        text=s; textSize=16f; isAllCaps=false; setTextColor(Color.WHITE); setBackgroundColor(accent)
        isFocusable=true; isFocusableInTouchMode=true; setOnClickListener{action()}
    }
    private fun makeUi(){
        root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(dp(10),dp(8),dp(10),dp(8)) }
        setContentView(root)
        root.addView(text("StreamIL TV  •  טלוויזיה | רדיו | VOD",23f).apply{setTypeface(null,Typeface.BOLD)})
        val tabs=HorizontalScrollView(this).apply {isHorizontalScrollBarEnabled=false}
        val bar=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        listOf("TV" to "טלוויזיה", "RADIO" to "רדיו", "VOD" to "VOD", "FAV" to "★ מועדפים").forEach { (k,label)->
            bar.addView(button(label){filter=k;render()},LinearLayout.LayoutParams(0,dp(48),1f))
        }
        tabs.addView(bar,HorizontalScrollView.LayoutParams(-1,-2));root.addView(tabs)
        val tools=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        tools.addView(button("+ הוספת M3U"){askPlaylist()},LinearLayout.LayoutParams(0,dp(50),1f))
        tools.addView(button("רענון"){loadStarter()},LinearLayout.LayoutParams(0,dp(50),1f))
        root.addView(tools)
        val search=EditText(this).apply{hint="חיפוש ערוץ, תחנה או סרט";setSingleLine(true);setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY);setPadding(dp(12),0,dp(12),0)}
        root.addView(search,LinearLayout.LayoutParams(-1,dp(50)))
        search.addTextChangedListener(object:android.text.TextWatcher{
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){}
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){query=s.toString();render()}
            override fun afterTextChanged(s:android.text.Editable?){}
        })
        playerView=PlayerView(this).apply{useController=true;visibility=View.GONE}
        root.addView(playerView,LinearLayout.LayoutParams(-1,dp(210)))
        status=text("טוענת תחנות…",14f);root.addView(status)
        val scroll=ScrollView(this)
        list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        scroll.addView(list);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
    }
    private fun render(){if(!::list.isInitialized)return
        list.removeAllViews()
        val visible=stations.filter { s ->
            (filter=="FAV" && favorites.contains(s.url) || filter==s.kind) &&
            (query.isBlank() || s.name.contains(query,true) || s.group.contains(query,true))
        }
        status.text="${visible.size} פריטים • ${stations.size} בספרייה"
        if(visible.isEmpty())list.addView(text("אין פריטים להצגה. ניתן להוסיף רשימת M3U מורשית או לרענן."))
        visible.take(1500).forEach { s ->
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
            val title=button("▶  ${s.name}  ·  ${s.group}"){play(s)}
            row.addView(title,LinearLayout.LayoutParams(0,dp(62),1f))
            row.addView(button(if(favorites.contains(s.url))"★" else "☆"){
                if(!favorites.add(s.url))favorites.remove(s.url)
                prefs.edit().putStringSet("favorites",favorites.toSet()).apply();render()
            },LinearLayout.LayoutParams(dp(58),dp(62)))
            list.addView(row)
        }
    }
    private fun play(s:Station){try{
        if(player==null)player=ExoPlayer.Builder(this).build()
        playerView.player=player
        playerView.visibility=View.VISIBLE
        player?.setMediaItem(MediaItem.fromUri(s.url));player?.prepare();player?.playWhenReady=true
        status.text="מנגן: ${s.name} • אם השידור אינו זמין, נסו תחנה אחרת"
    }catch(e:Exception){status.text="שגיאת ניגון: ${e.localizedMessage}"}}
    private fun askPlaylist(){val field=EditText(this).apply{hint="https://example.org/list.m3u";inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI;setSingleLine(true)}
        AlertDialog.Builder(this).setTitle("הוספת רשימת M3U / M3U8").setMessage("יש להזין קישור לרשימה שיש לך הרשאה לצפות בה.")
            .setView(field).setPositiveButton("ייבוא"){_,_->val url=field.text.toString().trim();if(url.startsWith("https://")||url.startsWith("http://"))fetchM3u(url) else status.text="יש להזין קישור HTTP או HTTPS תקין"}
            .setNegativeButton("ביטול",null).show()
    }
    private fun fetchM3u(url:String){status.text="מורידה רשימה…"
        executor.execute { try{val body=download(url,4_000_000);val items=parseM3u(body)
            runOnUiThread {merge(items);status.text="יובאו ${items.size} פריטים"}
        }catch(e:Exception){runOnUiThread{status.text="ייבוא נכשל: ${e.message}"}} }
    }
    private fun loadStarter(){status.text="טוענת ערוצים ותחנות רדיו ציבוריים…"
        executor.execute {
            var tv=emptyList<Station>();var radio=emptyList<Station>()
            try {tv=parseM3u(download("https://iptv-org.github.io/iptv/countries/il.m3u",4_000_000))}catch(_:Exception){}
            try {val data=JSONArray(download("https://de1.api.radio-browser.info/json/stations/bycountrycodeexact/IL?hidebroken=true&limit=300",2_000_000))
                radio=(0 until data.length()).mapNotNull{ i->val o=data.getJSONObject(i);val u=o.optString("url_resolved");if(u.startsWith("http"))Station(o.optString("name","רדיו"),u,o.optString("tags","רדיו"),"RADIO")else null}
            }catch(_:Exception){}
            runOnUiThread{merge(tv+radio);status.text="נטענו ${tv.size} ערוצי טלוויזיה ו־${radio.size} תחנות רדיו (זמינות שידור אינה מובטחת)"}
        }
    }
    private fun merge(items:List<Station>){val known=stations.map{it.url}.toHashSet();items.forEach{if(known.add(it.url))stations.add(it)};save();render()}
    private fun parseM3u(body:String):List<Station>{val result=mutableListOf<Station>();var name="";var group="";var kind="TV"
        val groupRx=Regex("group-title=\"([^\"]*)\"",RegexOption.IGNORE_CASE)
        val titleRx=Regex("tvg-name=\"([^\"]*)\"",RegexOption.IGNORE_CASE)
        for(line in body.lineSequence().map{it.trim()}){
            if(line.startsWith("#EXTINF",true)){
                group=groupRx.find(line)?.groupValues?.get(1)?:"כללי"
                name=titleRx.find(line)?.groupValues?.get(1)?.takeIf{it.isNotBlank()}?:line.substringAfterLast(',',"ערוץ")
                val low=(line+" "+group).lowercase()
                kind=when { listOf("radio","רדיו").any{it in low}->"RADIO";listOf("vod","movie","series","סרט","סדרה").any{it in low}->"VOD";else->"TV" }
            }else if(line.startsWith("http://")||line.startsWith("https://")){
                if(result.size>=10000)break
                result.add(Station(name.ifBlank{"שידור ${result.size+1}"},line,group,kind));name="";group="";kind="TV"
            }
        }
        return result
    }
    private fun download(url:String,maxBytes:Int):String{
        val conn=(URL(url).openConnection() as HttpURLConnection).apply{connectTimeout=12000;readTimeout=15000;instanceFollowRedirects=true;setRequestProperty("User-Agent","StreamILTV/1.0")}
        try{if(conn.responseCode !in 200..299)error("HTTP ${conn.responseCode}")
            conn.inputStream.use{stream->val out=java.io.ByteArrayOutputStream();val buf=ByteArray(8192)
                while(true){val n=stream.read(buf);if(n<0)break;if(out.size()+n>maxBytes)error("הרשימה גדולה מדי");out.write(buf,0,n)}
                return out.toString(Charsets.UTF_8.name())
            }
        }finally{conn.disconnect()}
    }
    private fun save(){val a=JSONArray();stations.take(10000).forEach{a.put(JSONObject().put("name",it.name).put("url",it.url).put("group",it.group).put("kind",it.kind))};prefs.edit().putString("stations",a.toString()).apply()}
    private fun restore(){try{val a=JSONArray(prefs.getString("stations","[]"));for(i in 0 until a.length()){val o=a.getJSONObject(i);stations.add(Station(o.getString("name"),o.getString("url"),o.optString("group"),o.optString("kind","TV")))}}catch(_:Exception){}}
    override fun onStop(){super.onStop();player?.pause()}
    override fun onDestroy(){player?.release();player=null;executor.shutdownNow();super.onDestroy()}
}
