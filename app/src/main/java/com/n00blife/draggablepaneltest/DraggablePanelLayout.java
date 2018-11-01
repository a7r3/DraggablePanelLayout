package com.n00blife.draggablepaneltest;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

// Code Forked from
// https://github.com/TheHiddenDuck/draggable-panel-layout/blob/master/src/com/evilduck/animtest/DraggedPanelLayout.java
//
// Experimenting requires a separate arena you know ...
// Oh, and its buggy, might kill your Cat/Duck/Dog/Debugger
public class DraggablePanelLayout extends FrameLayout {

    private float parallaxFactor = 0.3f;
    // Tracks the velocity of a MotionEvent
    private VelocityTracker velocityTracker;
    // Obtaining the touch event's y co-ordinate
    // In order to scale down/up the slidingPanel's size, according to the drag direction
    private float interceptTouchY;
    // Is the slidingPanel being dragged ?
    private boolean isDragging = false;
    // Is the slidingPanel being touched ?
    private boolean isTouched = false;
    // Is the slidingPanel in its expanded form ?
    private boolean isExpanded = false;
    private boolean isOpened = false;
    private boolean isOpening = false;
    private boolean isHalfway = false;
    private View slidingPanel;
    private View bottomPanel;
    private int bottomPanelPeekHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources()
            .getDisplayMetrics());

    public DraggablePanelLayout(@NonNull Context context) {
        super(context);
    }

    public DraggablePanelLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DraggablePanelLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        bottomPanel = getChildAt(0);
        bottomPanel.layout(left, top, right, bottom - bottomPanelPeekHeight);

        slidingPanel = getChildAt(1);
        if (!isOpened) {
            int panelMeasuredHeight = slidingPanel.getMeasuredHeight();
            slidingPanel.layout(left, bottom - bottomPanelPeekHeight, right, bottom - bottomPanelPeekHeight
                    + panelMeasuredHeight);
        }
    }

    private void startDragEvent(MotionEvent event) {
        interceptTouchY = event.getY();
        isTouched = true;

        bottomPanel.setVisibility(View.VISIBLE);

        obtainVelocityTracker();
        velocityTracker.addMovement(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            interceptTouchY = ev.getY();
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            isDragging = true;
            startDragEvent(ev);
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
            isDragging = false;
        }
        return isDragging;
    }

    private void obtainVelocityTracker() {
        if(velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
    }

    // Returns a valid translation value, which would be within the bounds
    // So that the chances of a translation event beyond this range won't occur
    private float boundTranslation(float translation) {
        if (!isExpanded) {
            if (translation > 0) {
                translation = 0;
            } else if (Math.abs(translation) >= slidingPanel.getMeasuredHeight() - bottomPanelPeekHeight) {
                translation = -slidingPanel.getMeasuredHeight() + bottomPanelPeekHeight;
            }
        } else {
            if (translation < 0) {
                translation = 0;
            } else if (Math.abs(translation) >= slidingPanel.getMeasuredHeight() - bottomPanelPeekHeight) {
                translation = slidingPanel.getMeasuredHeight() - bottomPanelPeekHeight;
            }
        }
        return translation;
    }

    // Animates the sliding panel to either its expanded, or collapsed state
    // depending on the velocity of the movement (+/-)
    // and determining whether the user has flinged
    // If the user has flinged, go with the flow
    // If the user has not flinged, then go opposite to the flow
    private void animateToFinalPosition(float velocityY) {
        final boolean isFlinging = Math.abs(velocityY) > 0.5;

        float distY;
        long duration;

        distY = calculateDistance(isOpening);

        if(isFlinging) {
            isOpening = velocityY < 0;
            duration = Math.abs(Math.round(distY / velocityY));
        } else {
            isHalfway = Math.abs(slidingPanel.getTranslationY()) >= (getMeasuredHeight() - bottomPanelPeekHeight) / 2;
            // If the panel is opened already, then the opening process is finished
            // Don't respect the isHalfway in this case
            isOpening = isOpened != isHalfway;
            duration = Math.round(300 * Math.abs((double) slidingPanel.getTranslationY())
                    / (double) (getMeasuredHeight() - bottomPanelPeekHeight));
        }

        animatePanel(isOpening, distY, duration);

    }

    // Calculates the distance for which the animation has to be performed
    public float calculateDistance(boolean isOpening) {
        float distY;
        if (isOpened) {
            distY = isOpening
                    ? -slidingPanel.getTranslationY()
                    : getMeasuredHeight() - bottomPanelPeekHeight - slidingPanel.getTranslationY();
        } else {
            distY = isOpening
                    ? -(getMeasuredHeight() - bottomPanelPeekHeight + slidingPanel.getTranslationY())
                    : -slidingPanel.getTranslationY();
        }

        return distY;
    }

    // Animates the panels if the fling is left incomplete
    // Animates it back to the expanded / collapsed position depending
    //   on the nearest position
    public void animatePanel(final boolean isOpening, float distY, long duration) {
        ObjectAnimator slidingPanelAnimator = ObjectAnimator.ofFloat(slidingPanel, View.TRANSLATION_Y,
                slidingPanel.getTranslationY(), slidingPanel.getTranslationY() + distY);
        ObjectAnimator bottomPanelAnimator = ObjectAnimator.ofFloat(bottomPanel, View.TRANSLATION_Y,
                bottomPanel.getTranslationY(), bottomPanel.getTranslationY() + (float) (distY * parallaxFactor));

        AnimatorSet set = new AnimatorSet();
        set.playTogether(slidingPanelAnimator, bottomPanelAnimator);
        set.setDuration(duration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new Animator.AnimatorListener() {

            int slidingPanelLayerType;
            int bottomPanelLayerType;

            @Override
            public void onAnimationStart(Animator animator) {
                slidingPanelLayerType = slidingPanel.getLayerType();
                bottomPanelLayerType = bottomPanel.getLayerType();

                slidingPanel.setLayerType(LAYER_TYPE_HARDWARE, null);
                bottomPanel.setLayerType(LAYER_TYPE_HARDWARE, null);

                bottomPanel.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                bottomPanel.setVisibility(isOpening ? View.GONE : View.VISIBLE);

                slidingPanel.setLayerType(slidingPanelLayerType, null);
                bottomPanel.setLayerType(bottomPanelLayerType, null);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        set.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        obtainVelocityTracker();

        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            startDragEvent(event);
        } else if(event.getAction() == MotionEvent.ACTION_MOVE) {
            if(isTouched) {
                velocityTracker.addMovement(event);
                float translation = boundTranslation(event.getY() - interceptTouchY);

                slidingPanel.setTranslationY(translation);
                bottomPanel.setTranslationY(
                        isExpanded ?
                                (getMeasuredHeight() - bottomPanelPeekHeight - translation) * parallaxFactor
                                :
                                translation * parallaxFactor
                );
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            isDragging = false;
            isTouched = false;

            velocityTracker.addMovement(event);
            velocityTracker.computeCurrentVelocity(1);

            animateToFinalPosition(velocityTracker.getYVelocity());

            velocityTracker.recycle();
            velocityTracker = null;
        }

        // the touch event will always be consumed
        return true;
    }


}
