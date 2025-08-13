package com.example.opensource.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class OverlayView extends View {
    private float[] mappedXs = null;
    private float[] mappedYs = null;
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

    public OverlayView(Context c, AttributeSet a) {
        super(c, a);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(6f);
        stroke.setColor(0xFFFF3B30); // 빨강
    }

    public void setMappedContour(float[] xs, float[] ys) {
        this.mappedXs = xs;
        this.mappedYs = ys;
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mappedXs == null || mappedYs == null || mappedXs.length < 4) return;
        Path p = new Path();
        p.moveTo(mappedXs[0], mappedYs[0]);
        for (int i = 1; i < mappedXs.length; i++) p.lineTo(mappedXs[i], mappedYs[i]);
        p.close();
        canvas.drawPath(p, stroke);
    }

    public void debugDrawTestBox() {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        float[] xs = new float[]{50, w-50, w-50, 50};
        float[] ys = new float[]{50, 50, h-50, h-50};
        setMappedContour(xs, ys);
    }

}
