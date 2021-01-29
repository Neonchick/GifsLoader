package tinkoff.testApplication.models

import android.graphics.drawable.Drawable
import com.bumptech.glide.RequestBuilder

data class GifEl(var scr: RequestBuilder<Drawable>, var description: String, var loaded: Boolean)