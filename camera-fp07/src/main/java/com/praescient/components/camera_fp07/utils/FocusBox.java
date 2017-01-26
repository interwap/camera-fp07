package com.praescient.components.camera_fp07.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.praescient.components.camera_fp07.R;

public class FocusBox extends View {

    private final Paint paint;
    private final int frameColor;
    private final int cornerColor;
    private static final String TAG = "ish";

    private Rect box;

    public FocusBox(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();

        frameColor = resources.getColor(R.color.focus_box_frame);
        cornerColor = resources.getColor(R.color.focus_box_corner);
    }

    private Rect getBox(){

        if(box == null){

            Point point = FocusUtil.getScreenResolution(getContext());
            int sides = 384; //Original 192
            int width = point.x;
            int height = point.y;
            int left = (width - sides)/2;
            int top = (height - sides)/2;
            int right = left + sides;
            int bottom = top + sides;
            box = new Rect(left, top, right, bottom);

        }

        return box;
    }

    public Rect getTrueBox() {
        return box;
    }

    @Override
    public void onDraw(Canvas canvas) {

        //Large Focus Box
        Rect frame = getBox();
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(frameColor);
        canvas.drawRect(frame.left, frame.top,frame.right,frame.bottom, paint);

        //Small Box
        Rect smallBox = new Rect();
        Paint smallPaint = new Paint();
        int smallSides = 288; //Original 154
        int sleft = (width - smallSides)/2;
        int stop = (height - smallSides)/2;
        int sright = sleft + smallSides;
        int sbottom = stop + smallSides;
        smallBox.set(sleft, stop, sright, sbottom);
        smallPaint.setStyle(Paint.Style.STROKE);
        smallPaint.setStrokeWidth(5);
        smallPaint.setColor(cornerColor);
        //canvas.drawRect(smallBox, smallPaint);
        canvas.drawCircle(width/2, height/2, 144, smallPaint);

    }
}
