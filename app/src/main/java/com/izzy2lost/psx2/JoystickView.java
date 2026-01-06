package com.izzy2lost.psx2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.core.content.ContextCompat;

public class JoystickView extends View {
    public interface OnMoveListener {
        void onMove(float nx, float ny, int action);
    }

    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float centerX, centerY, radius, knobX, knobY, knobRadius;
    private boolean isDragging = false;
    private OnMoveListener listener;

    public JoystickView(Context ctx) { super(ctx); init(); }
    public JoystickView(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }
    public JoystickView(Context ctx, AttributeSet attrs, int defStyle) { super(ctx, attrs, defStyle); init(); }

    private void init() {
        basePaint.setAntiAlias(true);
        ringPaint.setAntiAlias(true);
        knobPaint.setAntiAlias(true);
        
        setClickable(true);
    }

    public void setOnMoveListener(OnMoveListener l) { this.listener = l; }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        radius = Math.min(w, h) * 0.35f;
        knobRadius = radius * 0.45f;
        resetKnob();
    }

    private void resetKnob() {
        knobX = centerX;
        knobY = centerY;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Base gradient
        android.graphics.RadialGradient baseGradient = new android.graphics.RadialGradient(
            centerX, centerY, radius,
            new int[]{0x44FFFFFF, 0x11000000},
            null, android.graphics.Shader.TileMode.CLAMP
        );
        basePaint.setShader(baseGradient);
        canvas.drawCircle(centerX, centerY, radius, basePaint);
        
        // Outer ring (Brand Primary)
        int brandPrimary = ContextCompat.getColor(getContext(), R.color.brand_primary);
        ringPaint.setShader(null);
        ringPaint.setColor(brandPrimary);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dp(1.5f));
        canvas.drawCircle(centerX, centerY, radius, ringPaint);
        
        // Knob gradient
        android.graphics.RadialGradient knobGradient = new android.graphics.RadialGradient(
            knobX, knobY, knobRadius,
            new int[]{brandPrimary, 0xFF003258}, // Primary to Deep Blue
            null, android.graphics.Shader.TileMode.CLAMP
        );
        knobPaint.setShader(knobGradient);
        knobPaint.setStyle(Paint.Style.FILL);
        
        // Knob shadow
        knobPaint.setShadowLayer(dp(4), 0, dp(2), 0x88000000);
        setLayerType(LAYER_TYPE_SOFTWARE, null); // Shadows need software layer usually
        
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint);
        
        // Subtle knob top highlight
        Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(0x33FFFFFF);
        highlightPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(knobX, knobY - knobRadius * 0.2f, knobRadius * 0.6f, highlightPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                isDragging = true;
                // fallthrough to move
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    float dx = event.getX() - centerX;
                    float dy = event.getY() - centerY;
                    // Clamp to circle
                    float dist = (float)Math.hypot(dx, dy);
                    if (dist > radius) {
                        float scale = radius / dist;
                        dx *= scale;
                        dy *= scale;
                    }
                    knobX = centerX + dx;
                    knobY = centerY + dy;
                    invalidate();
                    if (listener != null) {
                        // Normalize to [-1,1], invert Y so up is negative value (screen y grows down)
                        float nx = dx / radius;
                        float ny = dy / radius;
                        listener.onMove(clamp(nx), clamp(ny), MotionEvent.ACTION_MOVE);
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                resetKnob();
                if (listener != null) listener.onMove(0f, 0f, MotionEvent.ACTION_UP);
                return true;
        }
        return super.onTouchEvent(event);
    }

    private static float clamp(float v) { return Math.max(-1f, Math.min(1f, v)); }

    private float dp(float d) {
        return d * getResources().getDisplayMetrics().density;
    }
}
