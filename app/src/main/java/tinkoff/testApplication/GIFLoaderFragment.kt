package tinkoff.testApplication

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_layout.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import tinkoff.testApplication.models.GifEl
import tinkoff.testApplication.models.ListPost
import tinkoff.testApplication.models.Post
import tinkoff.testApplication.requestListener.AnimationRequestListener
import tinkoff.testApplication.requestListener.ErrorRequestListener
import java.net.URL

class GIFLoaderFragment() : Fragment() {

    private val listGIFs: MutableList<GifEl> = mutableListOf()
    private var curNub = 0
    private var errorFlag = false
    private val listLink: MutableList<Post> = mutableListOf()
    private var lastLink = 0
    private var lastPage = 0
    private lateinit var categoryLink: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            categoryTextView.text = it.getString("name")
            categoryLink = it.getString("link") ?: ""
        }
        setPreviousImageButtonNotEnable()
        Glide.with(this).load(R.drawable.load_gif).into(loadImageView)
        GlobalScope.launch(Dispatchers.Main) {
            if (loadGif(context!!)) {
                ++curNub
                setGif()
            }
        }
        repeatImageButton.setOnClickListener { repeatImageButtonClick() }
        nextImageButton.setOnClickListener { nextImageButtonClick() }
        previousImageButton.setOnClickListener { previousImageButtonClick() }
    }

    private fun repeatImageButtonClick() {
        GlobalScope.launch(Dispatchers.Main) {
            if (loadGif(context!!)) {
                ++curNub
                setGif()
                if (curNub > 1)
                    setPreviousImageButtonEnable()
                setNormalVisibility()
            } else if (curNub > 0)
                setPreviousImageButtonEnable()
        }
    }

    private fun nextImageButtonClick() {
        GlobalScope.launch(Dispatchers.Main) {
            if (curNub == listGIFs.size) {
                if (loadGif(context!!)) {
                    ++curNub
                    setGif()
                    if (curNub > 1)
                        setPreviousImageButtonEnable()
                    setNormalVisibility()
                } else if (curNub > 0)
                    setPreviousImageButtonEnable()
            } else {
                ++curNub
                setGif()
                if (curNub > 1)
                    setPreviousImageButtonEnable()
            }
        }
    }

    private fun previousImageButtonClick() {
        if (curNub <= 2)
            setPreviousImageButtonNotEnable()
        if (errorFlag) {
            setNormalVisibility()
            return
        }
        --curNub
        setGif()
    }

    private fun setGif() {
        with(listGIFs[curNub - 1]) {
            scr.listener(AnimationRequestListener(loadImageView)).into(gifImageView)
            titleTextView.visibility = View.VISIBLE
            titleTextView.text = description
        }
    }

    private suspend fun loadGif(context: Context): Boolean {
        loadImageView.visibility = View.VISIBLE
        var post: Post? = null
        var scr: RequestBuilder<Drawable>? = null
        val fn = GlobalScope.async(Dispatchers.IO) {
            val cm =
                context.getSystemService(AppCompatActivity.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetworkInfo
            var isConnected = activeNetwork?.isConnectedOrConnecting ?: false
            if (isConnected) {
                post = getPost()
                post?.gifURL = "https" + post?.gifURL?.substring(4)
                val errorListener = ErrorRequestListener()
                scr = Glide.with(context).load(post?.gifURL).listener(errorListener)
                isConnected = errorListener.loaded
            } else {
                loadImageView.visibility = View.GONE
                setErrorVisibility()
            }
            isConnected
        }
        val correct = fn.await()
        if (correct)
            listGIFs.add(GifEl(scr!!, post?.description ?: "", true))
        return correct
    }

    private fun getPost(): Post {
        if (lastLink >= listLink.size) {
            if (categoryLink == "random") {
                val jsonStr =
                    URL("https://developerslife.ru/random?json=true").readText()
                val post = Gson().fromJson(jsonStr, Post::class.java)
                listLink += post
            } else {
                val jsonStr =
                    URL("https://developerslife.ru/${categoryLink}/${lastPage++}?json=true").readText()
                val listPost = Gson().fromJson(jsonStr, ListPost::class.java)
                listLink += listPost.result
            }
        }
        return listLink[lastLink++]
    }

    private fun setPreviousImageButtonEnable() {
        previousImageButton.isEnabled = true
        previousImageButton.setBackgroundResource(R.drawable.btn_round_bg)
    }

    private fun setPreviousImageButtonNotEnable() {
        previousImageButton.isEnabled = false
        previousImageButton.setBackgroundResource(R.drawable.btn_round_bg_not_enable)
    }

    private fun setErrorVisibility() {
        errorFlag = true
        GlobalScope.launch(Dispatchers.Main) {
            repeatImageButton.visibility = View.VISIBLE
            errorTextView.visibility = View.VISIBLE
            cloudImageView.visibility = View.VISIBLE
            gifImageView.visibility = View.INVISIBLE
            titleTextView.visibility = View.INVISIBLE
        }
    }

    private fun setNormalVisibility() {
        errorFlag = false
        repeatImageButton.visibility = View.GONE
        errorTextView.visibility = View.GONE
        cloudImageView.visibility = View.GONE
        gifImageView.visibility = View.VISIBLE
        titleTextView.visibility = View.VISIBLE
    }
}