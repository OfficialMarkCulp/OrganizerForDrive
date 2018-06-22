package net.nut.photosorganizer;

import android.content.Context;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

@com.bumptech.glide.annotation.GlideModule
public final class GlideModule extends AppGlideModule
{
    @Override
    public void applyOptions(Context context, GlideBuilder builder)
    {
        builder.setDefaultRequestOptions(new RequestOptions().format(DecodeFormat.PREFER_ARGB_8888));
    }
}