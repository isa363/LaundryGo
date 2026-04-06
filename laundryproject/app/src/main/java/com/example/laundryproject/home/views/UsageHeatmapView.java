package com.example.laundryproject.home.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class UsageHeatmapView extends View {

    private final Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int[][] data = new int[7][24];

    public UsageHeatmapView(Context context) {
        super(context);
        init();
    }

    public UsageHeatmapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public UsageHeatmapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        labelPaint.setColor(Color.parseColor("#6B7280"));
        labelPaint.setTextSize(dp(11));

        valuePaint.setColor(Color.parseColor("#111827"));
        valuePaint.setTextSize(dp(11));
    }

    public void setData(int[][] data) {
        this.data = data != null ? data : new int[7][24];
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        float leftLabelWidth = dp(34);
        float topPadding = dp(16);
        float rightPadding = dp(8);
        float bottomPadding = dp(24);
        float gap = dp(4);

        float gridWidth = width - leftLabelWidth - rightPadding;
        float gridHeight = height - topPadding - bottomPadding;

        float cellWidth = (gridWidth - (23 * gap)) / 24f;
        float cellHeight = (gridHeight - (6 * gap)) / 7f;

        String[] days = {"S", "M", "T", "W", "T", "F", "S"};

        int max = 0;
        for (int row = 0; row < 7; row++) {
            for (int col = 0; col < 24; col++) {
                max = Math.max(max, data[row][col]);
            }
        }

        for (int row = 0; row < 7; row++) {
            float cy = topPadding + row * (cellHeight + gap) + cellHeight * 0.68f;
            canvas.drawText(days[row], 0, cy, labelPaint);

            for (int col = 0; col < 24; col++) {
                float left = leftLabelWidth + col * (cellWidth + gap);
                float top = topPadding + row * (cellHeight + gap);
                float right = left + cellWidth;
                float bottom = top + cellHeight;

                cellPaint.setColor(resolveHeatColor(data[row][col], max));
                canvas.drawRoundRect(new RectF(left, top, right, bottom), dp(6), dp(6), cellPaint);
            }
        }

        for (int hour = 0; hour < 24; hour += 4) {
            float x = leftLabelWidth + hour * (cellWidth + gap);
            canvas.drawText(shortHour(hour), x, height - dp(6), labelPaint);
        }
    }

    private int resolveHeatColor(int value, int max) {
        if (value <= 0 || max <= 0) return Color.parseColor("#E8EEF5");

        float ratio = value / (float) max;

        int r1 = 230, g1 = 238, b1 = 247;
        int r2 = 26,  g2 = 86,  b2 = 160;

        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return Color.rgb(r, g, b);
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