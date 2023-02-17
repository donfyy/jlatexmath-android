package org.sufficientlysecure.htmltextview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.TextPaint;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ReplacementSpan;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.scilab.forge.jlatexmath.core.AjLatexMath;
import org.scilab.forge.jlatexmath.core.Insets;
import org.scilab.forge.jlatexmath.core.TeXConstants;
import org.scilab.forge.jlatexmath.core.TeXFormula;
import org.scilab.forge.jlatexmath.core.TeXIcon;

import java.lang.ref.WeakReference;

public class LatexSpan extends ReplacementSpan {
    private WeakReference<TextView> mTextView;
    private String mLatex;
    private DynamicDrawableSpan mDynamicDrawableSpan;
    private DynamicDrawableSpan mPlaceholder;
    private boolean mIsLoaded;

    public LatexSpan(TextView textView) {
        mTextView = new WeakReference<>(textView);
        Log.i(LatexSpan.class.getSimpleName(), "init using placeholder");
        mPlaceholder = new DynamicDrawableSpan() {
            @Override
            public Drawable getDrawable() {
                Drawable ret;
                ret = new ColorDrawable(Color.TRANSPARENT);
                ret.setBounds(0, 0, 5, 5);
                return ret;
            }
        };
    }


    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
        mLatex = text.subSequence(start, end).toString();
        DynamicDrawableSpan s;
        if (mDynamicDrawableSpan != null) {
            s = mDynamicDrawableSpan;
            Log.i(LatexSpan.class.getSimpleName(), "getSize using source");
        } else {
            s = mPlaceholder;
            if (!mIsLoaded) {
                mIsLoaded = true;
                LatexAsyncTask latexAsyncTask = new LatexAsyncTask(mTextView.get(), this);
                latexAsyncTask.execute(mLatex);
                Log.i(LatexSpan.class.getSimpleName(), "getSize using placeholder");
            }
        }
        return s.getSize(paint, text, start, end, fm);
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        DynamicDrawableSpan s;
        if (mDynamicDrawableSpan != null) {
            s = mDynamicDrawableSpan;
            Log.i(LatexSpan.class.getSimpleName(), "draw using source");
        } else {
            s = mPlaceholder;
            Log.i(LatexSpan.class.getSimpleName(), "draw using placeholder");
        }
        s.draw(canvas, text, start, end, x, top, y, bottom, paint);
    }

    private static class LatexAsyncTask extends AsyncTask<String, Void, Drawable> {
        private final WeakReference<TextView> containerReference;
        private final WeakReference<LatexSpan> spanWeakReference;
        private String source;

        public LatexAsyncTask(TextView container, LatexSpan latexSpan) {
            this.containerReference = new WeakReference<>(container);
            spanWeakReference = new WeakReference<>(latexSpan);
        }

        @Override
        protected Drawable doInBackground(String... params) {
            source = params[0];
            AjLatexMath.init(containerReference.get().getContext());
            TeXFormula teXFormula = TeXFormula.getPartialTeXFormula(source);
            Bitmap bitmap = getBitmap(teXFormula);
            Log.i(LatexSpan.class.getSimpleName(), "finish latex generate");
            BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
            bitmapDrawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
            return bitmapDrawable;
        }

        private Bitmap getBitmap(TeXFormula formula) {
            TextPaint paint = containerReference.get().getPaint();
            TeXIcon icon = formula.new TeXIconBuilder()
                    .setStyle(TeXConstants.STYLE_DISPLAY)
                    .setSize(paint.getTextSize() / paint.density)
                    .setWidth(TeXConstants.UNIT_SP, paint.getTextSize() / paint.density, TeXConstants.ALIGN_LEFT)
                    .setIsMaxWidth(true)
                    .setInterLineSpacing(TeXConstants.UNIT_SP,
                            AjLatexMath.getLeading(paint.getTextSize() / paint.density))
                    .build();
            icon.setInsets(new Insets(5, 5, 5, 5));
            Bitmap image = Bitmap.createBitmap(icon.getIconWidth(), icon.getIconHeight(),
                    Bitmap.Config.ARGB_8888);
            System.out.println(" width=" + icon.getBox().getWidth() + " height=" + icon.getBox().getHeight() +
                    " iconwidth=" + icon.getIconWidth() + " iconheight=" + icon.getIconHeight());
            Canvas g2 = new Canvas(image);
            g2.drawColor(Color.TRANSPARENT);
            icon.paintIcon(g2, 0, 0);
            return image;
        }

        @Override
        protected void onPostExecute(Drawable result) {
            final TextView imageGetter = containerReference.get();
            if (imageGetter == null) {
                return;
            }
            LatexSpan latexSpan = spanWeakReference.get();
            if (latexSpan == null) {
                return;
            }
            latexSpan.mDynamicDrawableSpan = new DynamicDrawableSpan(DynamicDrawableSpan.ALIGN_BOTTOM) {
                @Override
                public Drawable getDrawable() {
                    return result;
                }
            };
            Log.i(LatexSpan.class.getSimpleName(), "finish latex, using source");
            imageGetter.invalidate();
            imageGetter.setText(imageGetter.getText());
        }
    }
}
