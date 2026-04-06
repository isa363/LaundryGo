package com.example.laundryproject.home.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class HourlyUsageChartView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float[] data = new float[24];

    public HourlyUsageChartView(Context context) {
        super(context);
        init();
    }

    public HourlyUsageChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HourlyUsageChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barPaint.setColor(Color.parseColor("#1A56A0"));

        axisPaint.setColor(Color.parseColor("#D6DEE8"));
        axisPaint.setStrokeWidth(dp(1));

        labelPaint.setColor(Color.parseColor("#6B7280"));
        labelPaint.setTextSize(dp(11));
    }

    public void setData(float[] data) {
        this.data = data != null ? data : new float[24];
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float leftPadding = dp(8);
        float rightPadding = dp(8);
        float topPadding = dp(10);
        float bottomPadding = dp(26);

        float chartWidth = getWidth() - leftPadding - rightPadding;
        float chartHeight = getHeight() - topPadding - bottomPadding;

        float slotWidth = chartWidth / 24f;
        float barWidth = slotWidth * 0.62f;

        float max = 0f;
        for (float v : data) {
            if (v > max) max = v;
        }
        if (max <= 0f) max = 1f;

        canvas.drawLine(leftPadding, topPadding + chartHeight, getWidth() - rightPadding,
                topPadding + chartHeight, axisPaint);

        for (int hour = 0; hour < 24; hour++) {
            float value = data[hour];
            float barHeight = (value / max) * chartHeight;

            float left = leftPadding + hour * slotWidth + (slotWidth - barWidth) / 2f;
            float top = topPadding + chartHeight - barHeight;
            float right = left + barWidth;
            float bottom = topPadding + chartHeight;

            canvas.drawRoundRect(new RectF(left, top, right, bottom), dp(6), dp(6), barPaint);

            if (hour % 4 == 0) {
                canvas.drawText(shortHour(hour), left - dp(2), getHeight() - dp(6), labelPaint);
            }
        }
    }

    private String shortHour(int hour) {
        if (hour == 0) return "12A";
        if (hour < 12) return hour + "A";
        if (hour == 12) return "12P";
        return (hour - 12) + "P";
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}