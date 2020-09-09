package tinkoff.testApplication

import android.content.Context
import android.graphics.drawable.Drawable
import android.media.Image
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.net.URL


class MainActivity : AppCompatActivity()
{
    val listGifs: MutableList<GifEl> = mutableListOf()

    var curNub = 0

    var errorF = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main)

        prevNotEnable()

        Glide.with(this).load(R.drawable.load_gif).into(load)

        GlobalScope.launch(Dispatchers.Main) {
            if (loadGif(this@MainActivity))
            {
                ++curNub
                setGif()
            }
        }

        btn_repeat.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                if (loadGif(this@MainActivity))
                {
                    ++curNub
                    setGif()
                    if (curNub > 1)
                        prevEnable()
                    ok()
                }
                else if (curNub > 0)
                    prevEnable()
            }
        }

        next.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                if (curNub == listGifs.size)
                {
                    if (loadGif(this@MainActivity))
                    {
                        ++curNub
                        setGif()
                        if (curNub > 1)
                            prevEnable()
                        ok()
                    }
                    else if (curNub > 0)
                        prevEnable()
                }
                else
                {
                    ++curNub
                    setGif()
                    if (curNub > 1)
                        prevEnable()
                }
            }
        }

        prev.setOnClickListener {
            if (curNub <= 2)
                prevNotEnable()
            if (errorF)
            {
                ok()
                return@setOnClickListener
            }
            --curNub
            setGif()
        }
    }

    private fun setGif() =
            with(listGifs[curNub - 1])
            {
                scr.listener(AnimationRequestListener(load)).into(gif)
                this@MainActivity.description.visibility = View.VISIBLE
                this@MainActivity.description.text = description
            }

    data class Post(var description: String, var gifURL: String)
    class GifEl(var scr: RequestBuilder<Drawable>, var description: String, var loaded: Boolean)

    suspend fun loadGif(context: Context): Boolean
    {
        load.visibility = View.VISIBLE
        var post: Post? = null
        var scr: RequestBuilder<Drawable>? = null
        val fn = GlobalScope.async(Dispatchers.IO) {
            val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetworkInfo
            var isConnected = activeNetwork?.isConnectedOrConnecting ?: false
            if (isConnected)
            {
                val jsonStr = URL("https://developerslife.ru/random?json=true").readText()
                post = Gson().fromJson(jsonStr, Post::class.java)
                post?.gifURL = "https" + post?.gifURL?.substring(4)
                val loaded = true to 0
                val errorListener = ErrorRequestListener()
                scr = Glide.with(context).load(post?.gifURL).listener(errorListener)
                isConnected = errorListener.loaded
            }
            else
            {
                load.visibility = View.GONE
                error()
            }
            isConnected
        }
        val correct = fn.await()
        if (correct)
            listGifs.add(GifEl(scr!!, post?.description ?: "", true))
        return correct
    }

    class ErrorRequestListener() : RequestListener<Drawable>
    {
        var loaded = true

        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?,
                                  isFirstResource: Boolean): Boolean
        {
            loaded = false
            return false
        }

        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?,
                                     dataSource: DataSource?, isFirstResource: Boolean): Boolean =
                false
    }

    class AnimationRequestListener(val load: ImageView) : RequestListener<Drawable>
    {
        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?,
                                  isFirstResource: Boolean): Boolean = false

        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?,
                                     dataSource: DataSource?, isFirstResource: Boolean): Boolean
        {
            load.visibility = View.GONE
            return false
        }
    }

    fun prevEnable()
    {
        prev.isEnabled = true
        prev.setBackgroundResource(R.drawable.btn_round_bg)
    }

    fun prevNotEnable()
    {
        prev.isEnabled = false
        prev.setBackgroundResource(R.drawable.btn_round_bg_not_enable)
    }

    fun error()
    {
        errorF = true
        GlobalScope.launch(Dispatchers.Main) {
            btn_repeat.visibility = View.VISIBLE
            tv_error.visibility = View.VISIBLE
            clode.visibility = View.VISIBLE
            gif.visibility = View.INVISIBLE
            description.visibility = View.INVISIBLE
        }
    }

    fun ok()
    {
        errorF = false
        btn_repeat.visibility = View.GONE
        tv_error.visibility = View.GONE
        clode.visibility = View.GONE
        gif.visibility = View.VISIBLE
        description.visibility = View.VISIBLE
    }
}