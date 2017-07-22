package com.common.library.llj.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListAdapter;

import com.common.library.llj.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class SwipeLayout extends FrameLayout {

    private ViewDragHelper mDragHelper;

    private int mDragDistance = 0;
    private DragEdge mDragEdge;
    private ShowMode mShowMode;

    private float mHorizontalSwipeOffset;
    private float mVerticalSwipeOffset;

    private List<SwipeListener>                    mSwipeListeners  = new ArrayList<SwipeListener>();
    private List<SwipeDenier>                      mSwipeDeniers    = new ArrayList<SwipeDenier>();
    private Map<View, ArrayList<OnRevealListener>> mRevealListeners = new HashMap<View, ArrayList<OnRevealListener>>();
    private Map<View, Boolean>                     mShowEntirely    = new HashMap<View, Boolean>();

    private DoubleClickListener mDoubleClickListener;

    private boolean mSwipeEnabled = true;

    public static enum DragEdge {
        Left, Right, Top, Bottom;
    }

    public static enum ShowMode {
        LayDown, PullOut
    }

    public SwipeLayout(Context context) {
        this(context, null);
    }

    public SwipeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mDragHelper = ViewDragHelper.create(this, 2.0f, mDragHelperCallback);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SwipeLayout);
        int ordinal = a.getInt(R.styleable.SwipeLayout_drag_edge, DragEdge.Right.ordinal());
        mHorizontalSwipeOffset = a.getDimension(R.styleable.SwipeLayout_horizontalSwipeOffset, 0);
        mVerticalSwipeOffset = a.getDimension(R.styleable.SwipeLayout_verticalSwipeOffset, 0);
        mDragEdge = DragEdge.values()[ordinal];
        ordinal = a.getInt(R.styleable.SwipeLayout_show_mode, ShowMode.PullOut.ordinal());
        mShowMode = ShowMode.values()[ordinal];
    }

    /**
     * 菜单按钮的一系列监听
     *
     * @author liulj
     */
    public interface SwipeListener {
        // 刚开始打开，只回掉一次，且最先回调
        public void onStartOpen(SwipeLayout layout);

        // 完全打开
        public void onOpen(SwipeLayout layout);

        // 刚开始关闭，只回掉一次，且最先回调
        public void onStartClose(SwipeLayout layout);

        // 完全关闭
        public void onClose(SwipeLayout layout);

        /**
         * 位置变化时回调
         *
         * @param layout
         * @param leftOffset 拖动的view左边的left距离原始位置的距离
         * @param topOffset  拖动的view顶部的top距离原始位置的距离
         */
        public void onUpdate(SwipeLayout layout, int leftOffset, int topOffset);

        /**
         * 释放的时候回调用，就是onViewReleased中回调的速率
         *
         * @param layout
         * @param xvel   向右似乎为正数，向左似乎为负数
         * @param yvel   向下似乎为正数，向上似乎为负数
         */
        public void onHandRelease(SwipeLayout layout, float xvel, float yvel);
    }

    public void addSwipeListener(SwipeListener l) {
        mSwipeListeners.add(l);
    }

    public void removeSwipeListener(SwipeListener l) {
        mSwipeListeners.remove(l);
    }

    public void clearSwipeListener() {
        if (mSwipeListeners != null) {
            mSwipeListeners.clear();
        }
    }

    public static interface SwipeDenier {
        /*
         * Called in onInterceptTouchEvent Determines if this swipe event should
         * be denied Implement this interface if you are using views with swipe
         * gestures As a child of SwipeLayout
         *
         * @return true deny false allow
         */
        public boolean shouldDenySwipe(MotionEvent ev);
    }

    public void addSwipeDenier(SwipeDenier denier) {
        mSwipeDeniers.add(denier);
    }

    public void removeSwipeDenier(SwipeDenier denier) {
        mSwipeDeniers.remove(denier);
    }

    public void removeAllSwipeDeniers() {
        mSwipeDeniers.clear();
    }

    public interface OnRevealListener {
        public void onReveal(View child, DragEdge edge, float fraction, int distance);
    }

    /**
     * bind a view with a specific
     * {@link com.daimajia.swipe.SwipeLayout.OnRevealListener}
     *
     * @param childId the view id.
     * @param l       the target
     *                {@link com.daimajia.swipe.SwipeLayout.OnRevealListener}
     */
    public void addRevealListener(int childId, OnRevealListener l) {
        View child = findViewById(childId);
        if (child == null) {
            throw new IllegalArgumentException("Child does not belong to SwipeListener.");
        }

        if (!mShowEntirely.containsKey(child)) {
            mShowEntirely.put(child, false);
        }
        if (mRevealListeners.get(child) == null)
            mRevealListeners.put(child, new ArrayList<OnRevealListener>());

        mRevealListeners.get(child).add(l);
    }

    /**
     * bind multiple views with an
     * {@link com.daimajia.swipe.SwipeLayout.OnRevealListener}.
     *
     * @param childIds the view id.
     * @param l        the {@link com.daimajia.swipe.SwipeLayout.OnRevealListener}
     */
    public void addRevealListener(int[] childIds, OnRevealListener l) {
        for (int i : childIds)
            addRevealListener(i, l);
    }

    public void removeRevealListener(int childId, OnRevealListener l) {
        View child = findViewById(childId);

        if (child == null)
            return;

        mShowEntirely.remove(child);
        if (mRevealListeners.containsKey(child))
            mRevealListeners.get(child).remove(l);
    }

    public void removeAllRevealListeners(int childId) {
        View child = findViewById(childId);
        if (child != null) {
            mRevealListeners.remove(child);
            mShowEntirely.remove(child);
        }
    }

    private ViewDragHelper.Callback mDragHelperCallback = new ViewDragHelper.Callback() {
        // 水平拖动的位置移动
        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            // 通过getChildAt(1)获得需要拖动的view，水平拖动只要关注Left和Right的实现
            if (child == getSurfaceView()) {
                switch (mDragEdge) {
                    case Top:
                    case Bottom:
                        return getPaddingLeft();
                    case Left:
                        // 如果left小于padding值(就是被拉出左边界了)，就设置拖动后的位置仍然是getPaddingLeft的位置，相当于没有拖动(设置左边最小位置)
                        if (left < getPaddingLeft())
                            return getPaddingLeft();
                        // 如果拖动的位置大于设置的可以拖动的距离，还是返回设置好后拖动的最大位置(设置右边最大位置)
                        if (left > getPaddingLeft() + mDragDistance)
                            return getPaddingLeft() + mDragDistance;
                        break;
                    case Right:
                        // 如果向右边拖动了则保持原位置不动
                        if (left > getPaddingLeft())
                            return getPaddingLeft();
                        // 设置左边可拖出的最大距离
                        if (left < getPaddingLeft() - mDragDistance)
                            return getPaddingLeft() - mDragDistance;
                        break;
                }
            } else if (child == getBottomView()) {

                switch (mDragEdge) {
                    case Top:
                    case Bottom:
                        return getPaddingLeft();
                    case Left:
                        if (mShowMode == ShowMode.PullOut) {
                            if (left > getPaddingLeft())
                                return getPaddingLeft();
                        }
                        break;
                    case Right:
                        if (mShowMode == ShowMode.PullOut) {
                            if (left < getMeasuredWidth() - mDragDistance) {
                                return getMeasuredWidth() - mDragDistance;
                            }
                        }
                        break;
                }
            }
            return left;
        }

        // 上下拖动
        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            if (child == getSurfaceView()) {
                switch (mDragEdge) {
                    case Left:
                    case Right:
                        // 左右拖动位置不变
                        return getPaddingTop();
                    // 向下拖
                    case Top:
                        // 如果向上拖出原来的top，则位置不变
                        if (top < getPaddingTop())
                            return getPaddingTop();
                        // 设置向下可以拖动后的最大top
                        if (top > getPaddingTop() + mDragDistance)
                            return getPaddingTop() + mDragDistance;
                        break;
                    // 向上拖
                    case Bottom:
                        // 设置向上可以拖动的最大top位置
                        if (top < getPaddingTop() - mDragDistance) {
                            return getPaddingTop() - mDragDistance;
                        }
                        // 如果往下拖则保持位置不变
                        if (top > getPaddingTop()) {
                            return getPaddingTop();
                        }
                }
            } else {
                switch (mDragEdge) {
                    case Left:
                    case Right:
                        return getPaddingTop();
                    case Top:
                        if (mShowMode == ShowMode.PullOut) {
                            if (top > getPaddingTop())
                                return getPaddingTop();
                        } else {
                            if (getSurfaceView().getTop() + dy < getPaddingTop())
                                return getPaddingTop();
                            if (getSurfaceView().getTop() + dy > getPaddingTop() + mDragDistance)
                                return getPaddingTop() + mDragDistance;
                        }
                        break;
                    case Bottom:
                        if (mShowMode == ShowMode.PullOut) {
                            if (top < getMeasuredHeight() - mDragDistance)
                                return getMeasuredHeight() - mDragDistance;
                        } else {
                            if (getSurfaceView().getTop() + dy >= getPaddingTop())
                                return getPaddingTop();
                            if (getSurfaceView().getTop() + dy <= getPaddingTop() - mDragDistance)
                                return getPaddingTop() - mDragDistance;
                        }
                }
            }
            return top;
        }

        // 判断需要拖动的view
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == getSurfaceView() || child == getBottomView();
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return mDragDistance;
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mDragDistance;
        }

        // 手指释放时候的回调，ViewDragHelper内部会回传x，y的速率
        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            mIsBeingDragged = false;
            for (SwipeListener l : mSwipeListeners)
                l.onHandRelease(SwipeLayout.this, xvel, yvel);
            if (releasedChild == getSurfaceView()) {
                // 判断手指释放后view应该回到哪个位置
                processSurfaceRelease(xvel, yvel);
            } else if (releasedChild == getBottomView()) {
                if (getShowMode() == ShowMode.PullOut) {
                    processBottomPullOutRelease(xvel, yvel);
                } else if (getShowMode() == ShowMode.LayDown) {
                    processBottomLayDownMode(xvel, yvel);
                }
            }

            invalidate();
        }

        // 当拖动的view的位置改变的时候，菜单的view做相应的位置改变，dx和dy为偏移的距离
        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            mIsBeingDragged = true;
            int evLeft = getSurfaceView().getLeft(), evRight = getSurfaceView().getRight(), evTop = getSurfaceView().getTop(), evBottom = getSurfaceView().getBottom();
            if (changedView == getSurfaceView()) {
                // 设置另一个没有被拖动的view的位置变化
                if (mShowMode == ShowMode.PullOut) {
                    if (mDragEdge == DragEdge.Left || mDragEdge == DragEdge.Right)
                        // 设置相应的偏移的距离
                        getBottomView().offsetLeftAndRight(dx);
                    else
                        getBottomView().offsetTopAndBottom(dy);
                }

            } else if (changedView == getBottomView()) {

                if (mShowMode == ShowMode.PullOut) {
                    getSurfaceView().offsetLeftAndRight(dx);
                    getSurfaceView().offsetTopAndBottom(dy);
                } else {
                    Rect rect = computeBottomLayDown(mDragEdge);
                    getBottomView().layout(rect.left, rect.top, rect.right, rect.bottom);

                    int newLeft = getSurfaceView().getLeft() + dx, newTop = getSurfaceView().getTop() + dy;

                    if (mDragEdge == DragEdge.Left && newLeft < getPaddingLeft())
                        newLeft = getPaddingLeft();
                    else if (mDragEdge == DragEdge.Right && newLeft > getPaddingLeft())
                        newLeft = getPaddingLeft();
                    else if (mDragEdge == DragEdge.Top && newTop < getPaddingTop())
                        newTop = getPaddingTop();
                    else if (mDragEdge == DragEdge.Bottom && newTop > getPaddingTop())
                        newTop = getPaddingTop();

                    getSurfaceView().layout(newLeft, newTop, newLeft + getMeasuredWidth(), newTop + getMeasuredHeight());
                }
            }
            // 分发OnRevealListener监听回调
            dispatchRevealEvent(evLeft, evTop, evRight, evBottom);
            // 分发菜单按钮的一系列事件
            dispatchSwipeEvent(evLeft, evTop, dx, dy);
            // 重新绘制view
            invalidate();
        }
    };

    /**
     * the dispatchRevealEvent method may not always get accurate position, it
     * makes the view may not always get the event when the view is totally
     * show( fraction = 1), so , we need to calculate every time.
     *
     * @param child
     * @param relativePosition
     * @param edge
     * @param surfaceLeft
     * @param surfaceTop
     * @param surfaceRight
     * @param surfaceBottom
     * @return
     */
    protected boolean isViewTotallyFirstShowed(View child, Rect relativePosition, DragEdge edge, int surfaceLeft, int surfaceTop, int surfaceRight, int surfaceBottom) {
        if (mShowEntirely.get(child))
            return false;
        int childLeft = relativePosition.left;
        int childRight = relativePosition.right;
        int childTop = relativePosition.top;
        int childBottom = relativePosition.bottom;
        boolean r = false;
        if (getShowMode() == ShowMode.LayDown) {
            if ((edge == DragEdge.Right && surfaceRight <= childLeft) || (edge == DragEdge.Left && surfaceLeft >= childRight) || (edge == DragEdge.Top && surfaceTop >= childBottom) || (edge == DragEdge.Bottom && surfaceBottom <= childTop))
                r = true;
        } else if (getShowMode() == ShowMode.PullOut) {
            if ((edge == DragEdge.Right && childRight <= getWidth()) || (edge == DragEdge.Left && childLeft >= getPaddingLeft()) || (edge == DragEdge.Top && childTop >= getPaddingTop()) || (edge == DragEdge.Bottom && childBottom <= getHeight()))
                r = true;
        }
        return r;
    }

    protected boolean isViewShowing(View child, Rect relativePosition, DragEdge availableEdge, int surfaceLeft, int surfaceTop, int surfaceRight, int surfaceBottom) {
        int childLeft = relativePosition.left;
        int childRight = relativePosition.right;
        int childTop = relativePosition.top;
        int childBottom = relativePosition.bottom;
        if (getShowMode() == ShowMode.LayDown) {
            switch (availableEdge) {
                case Right:
                    if (surfaceRight > childLeft && surfaceRight <= childRight) {
                        return true;
                    }
                    break;
                case Left:
                    if (surfaceLeft < childRight && surfaceLeft >= childLeft) {
                        return true;
                    }
                    break;
                case Top:
                    if (surfaceTop >= childTop && surfaceTop < childBottom) {
                        return true;
                    }
                    break;
                case Bottom:
                    if (surfaceBottom > childTop && surfaceBottom <= childBottom) {
                        return true;
                    }
                    break;
            }
        } else if (getShowMode() == ShowMode.PullOut) {
            switch (availableEdge) {
                case Right:
                    if (childLeft <= getWidth() && childRight > getWidth())
                        return true;
                    break;
                case Left:
                    if (childRight >= getPaddingLeft() && childLeft < getPaddingLeft())
                        return true;
                    break;
                case Top:
                    if (childTop < getPaddingTop() && childBottom >= getPaddingTop())
                        return true;
                    break;
                case Bottom:
                    if (childTop < getHeight() && childTop >= getPaddingTop())
                        return true;
                    break;
            }
        }
        return false;
    }

    protected Rect getRelativePosition(View child) {
        View t = child;
        Rect r = new Rect(t.getLeft(), t.getTop(), 0, 0);
        while (t.getParent() != null && t != getRootView()) {
            t = (View) t.getParent();
            if (t == this)
                break;
            r.left += t.getLeft();
            r.top += t.getTop();
        }
        r.right = r.left + child.getMeasuredWidth();
        r.bottom = r.top + child.getMeasuredHeight();
        return r;
    }

    private int mEventCounter = 0;

    protected void dispatchSwipeEvent(int surfaceLeft, int surfaceTop, int dx, int dy) {
        DragEdge edge = getDragEdge();
        boolean open = true;
        if (edge == DragEdge.Left) {
            if (dx < 0)
                open = false;
        } else if (edge == DragEdge.Right) {
            if (dx > 0)
                open = false;
        } else if (edge == DragEdge.Top) {
            if (dy < 0)
                open = false;
        } else if (edge == DragEdge.Bottom) {
            if (dy > 0)
                open = false;
        }

        dispatchSwipeEvent(surfaceLeft, surfaceTop, open);
    }

    protected void dispatchSwipeEvent(int surfaceLeft, int surfaceTop, boolean open) {
        safeBottomView();
        // 判断菜单的状态，关闭，打开，或者middle状态
        Status status = getOpenStatus();

        if (!mSwipeListeners.isEmpty()) {
            mEventCounter++;
            for (SwipeListener l : mSwipeListeners) {
                if (mEventCounter == 1) {
                    if (open) {
                        l.onStartOpen(this);
                    } else {
                        l.onStartClose(this);
                    }
                }
                l.onUpdate(SwipeLayout.this, surfaceLeft - getPaddingLeft(), surfaceTop - getPaddingTop());
            }

            if (status == Status.Close) {
                for (SwipeListener l : mSwipeListeners) {
                    l.onClose(SwipeLayout.this);
                }
                mEventCounter = 0;
            }

            if (status == Status.Open) {
                getBottomView().setEnabled(true);
                for (SwipeListener l : mSwipeListeners) {
                    l.onOpen(SwipeLayout.this);
                }
                mEventCounter = 0;
            }
        }
    }

    /**
     * prevent bottom view get any touch event. Especially in LayDown mode.
     */
    private void safeBottomView() {
        Status status = getOpenStatus();
        ViewGroup bottom = getBottomView();

        if (status == Status.Close) {
            if (bottom.getVisibility() != INVISIBLE)
                bottom.setVisibility(INVISIBLE);
        } else {
            if (bottom.getVisibility() != VISIBLE)
                bottom.setVisibility(VISIBLE);
        }
    }

    protected void dispatchRevealEvent(final int surfaceLeft, final int surfaceTop, final int surfaceRight, final int surfaceBottom) {
        if (mRevealListeners.isEmpty())
            return;
        for (Map.Entry<View, ArrayList<OnRevealListener>> entry : mRevealListeners.entrySet()) {
            View child = entry.getKey();
            Rect rect = getRelativePosition(child);
            if (isViewShowing(child, rect, mDragEdge, surfaceLeft, surfaceTop, surfaceRight, surfaceBottom)) {
                mShowEntirely.put(child, false);
                int distance = 0;
                float fraction = 0f;
                if (getShowMode() == ShowMode.LayDown) {
                    switch (mDragEdge) {
                        case Left:
                            distance = rect.left - surfaceLeft;
                            fraction = distance / (float) child.getWidth();
                            break;
                        case Right:
                            distance = rect.right - surfaceRight;
                            fraction = distance / (float) child.getWidth();
                            break;
                        case Top:
                            distance = rect.top - surfaceTop;
                            fraction = distance / (float) child.getHeight();
                            break;
                        case Bottom:
                            distance = rect.bottom - surfaceBottom;
                            fraction = distance / (float) child.getHeight();
                            break;
                    }
                } else if (getShowMode() == ShowMode.PullOut) {
                    switch (mDragEdge) {
                        case Left:
                            distance = rect.right - getPaddingLeft();
                            fraction = distance / (float) child.getWidth();
                            break;
                        case Right:
                            distance = rect.left - getWidth();
                            fraction = distance / (float) child.getWidth();
                            break;
                        case Top:
                            distance = rect.bottom - getPaddingTop();
                            fraction = distance / (float) child.getHeight();
                            break;
                        case Bottom:
                            distance = rect.top - getHeight();
                            fraction = distance / (float) child.getHeight();
                            break;
                    }
                }

                for (OnRevealListener l : entry.getValue()) {
                    l.onReveal(child, mDragEdge, Math.abs(fraction), distance);
                    if (Math.abs(fraction) == 1) {
                        mShowEntirely.put(child, true);
                    }
                }
            }

            if (isViewTotallyFirstShowed(child, rect, mDragEdge, surfaceLeft, surfaceTop, surfaceRight, surfaceBottom)) {
                mShowEntirely.put(child, true);
                for (OnRevealListener l : entry.getValue()) {
                    if (mDragEdge == DragEdge.Left || mDragEdge == DragEdge.Right)
                        l.onReveal(child, mDragEdge, 1, child.getWidth());
                    else
                        l.onReveal(child, mDragEdge, 1, child.getHeight());
                }
            }

        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * {@link android.view.View.OnLayoutChangeListener} added in API 11. I need
     * to support it from API 8.
     */
    public interface OnLayout {
        public void onLayout(SwipeLayout v);
    }

    private List<OnLayout> mOnLayoutListeners;

    public void addOnLayoutListener(OnLayout l) {
        if (mOnLayoutListeners == null)
            mOnLayoutListeners = new ArrayList<OnLayout>();
        mOnLayoutListeners.add(l);
    }

    public void removeOnLayoutListener(OnLayout l) {
        if (mOnLayoutListeners != null)
            mOnLayoutListeners.remove(l);
    }

    @SuppressLint("WrongCall")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        if (childCount != 2) {
            throw new IllegalStateException("You need 2  views in SwipeLayout");
        }
        if (!(getChildAt(0) instanceof ViewGroup) || !(getChildAt(1) instanceof ViewGroup)) {
            throw new IllegalArgumentException("The 2 children in SwipeLayout must be an instance of ViewGroup");
        }

        if (mShowMode == ShowMode.PullOut)
            layoutPullOut();
        else if (mShowMode == ShowMode.LayDown)
            layoutLayDown();

        safeBottomView();

        if (mOnLayoutListeners != null)
            for (int i = 0; i < mOnLayoutListeners.size(); i++) {
                mOnLayoutListeners.get(i).onLayout(this);
            }

    }

    void layoutPullOut() {
        Rect rect = computeSurfaceLayoutArea(false);
        getSurfaceView().layout(rect.left, rect.top, rect.right, rect.bottom);
        rect = computeBottomLayoutAreaViaSurface(ShowMode.PullOut, rect);
        getBottomView().layout(rect.left, rect.top, rect.right, rect.bottom);
        bringChildToFront(getSurfaceView());
    }

    void layoutLayDown() {
        Rect rect = computeSurfaceLayoutArea(false);
        getSurfaceView().layout(rect.left, rect.top, rect.right, rect.bottom);
        rect = computeBottomLayoutAreaViaSurface(ShowMode.LayDown, rect);
        getBottomView().layout(rect.left, rect.top, rect.right, rect.bottom);
        bringChildToFront(getSurfaceView());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mDragEdge == DragEdge.Left || mDragEdge == DragEdge.Right)
            mDragDistance = getBottomView().getMeasuredWidth() - dp2px(mHorizontalSwipeOffset);
        else
            mDragDistance = getBottomView().getMeasuredHeight() - dp2px(mVerticalSwipeOffset);
    }

    private boolean mTouchConsumedByChild = false;
    private boolean mIsReachTouchSlop     = false;

    private float mInitialDownX;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if (!isEnabled() || !isEnabledInAdapterView()) {
            return true;
        }

        if (!isSwipeEnabled()) {
            return false;
        }

        for (SwipeDenier denier : mSwipeDeniers) {
            if (denier != null && denier.shouldDenySwipe(ev)) {
                return false;
            }
        }
        //
        // if a child wants to handle the touch event,
        // then let it do it.
        //
        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mInitialDownX = ev.getX();
                Status status = getOpenStatus();
                if (status == Status.Close) {
                    mTouchConsumedByChild = childNeedHandleTouchEvent(getSurfaceView(), ev) != null;
                } else if (status == Status.Open) {
                    mTouchConsumedByChild = childNeedHandleTouchEvent(getBottomView(), ev) != null;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                initDragStatus(ev.getX());
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTouchConsumedByChild = false;
                break;
        }

        if (mTouchConsumedByChild && mIsReachTouchSlop)
            return false;
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    private void initDragStatus(float activeMoveX) {
        float diff = Math.abs(activeMoveX - mInitialDownX);

        if (diff > mDragHelper.getTouchSlop()) {
            mIsReachTouchSlop = true;
        } else {
            mIsReachTouchSlop = false;
        }
    }

    /**
     * if the ViewGroup children want to handle this event.
     *
     * @param v
     * @param event
     * @return
     */
    private View childNeedHandleTouchEvent(ViewGroup v, MotionEvent event) {
        if (v == null)
            return null;
        if (v.onTouchEvent(event))
            return v;

        int childCount = v.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = v.getChildAt(i);
            if (child instanceof ViewGroup) {
                View grandChild = childNeedHandleTouchEvent((ViewGroup) child, event);
                if (grandChild != null)
                    return grandChild;
            } else {
                if (childNeedHandleTouchEvent(v.getChildAt(i), event))
                    return v.getChildAt(i);
            }
        }
        return null;
    }

    /**
     * if the view (v) wants to handle this event.
     *
     * @param v
     * @param event
     * @return
     */
    private boolean childNeedHandleTouchEvent(View v, MotionEvent event) {
        if (v == null)
            return false;

        int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        int left = loc[0], top = loc[1];

        if (event.getRawX() > left && event.getRawX() < left + v.getWidth() && event.getRawY() > top && event.getRawY() < top + v.getHeight()) {
            return v.onTouchEvent(event);
        }

        return false;
    }

    private float sX = -1, sY = -1;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabledInAdapterView() || !isEnabled())
            return true;

        if (!isSwipeEnabled())
            return super.onTouchEvent(event);

        int action = event.getActionMasked();
        ViewParent parent = getParent();

        gestureDetector.onTouchEvent(event);
        Status status = getOpenStatus();
        ViewGroup touching = null;
        if (status == Status.Close) {
            touching = getSurfaceView();
        } else if (status == Status.Open) {
            touching = getBottomView();
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDragHelper.processTouchEvent(event);
                parent.requestDisallowInterceptTouchEvent(true);

                sX = event.getRawX();
                sY = event.getRawY();

                if (touching != null)
                    touching.setPressed(true);

                return true;
            case MotionEvent.ACTION_MOVE: {
                if (sX == -1 || sY == -1) {
                    // Trick:
                    // When in nested mode, we need to send a constructed
                    // ACTION_DOWN MotionEvent to mDragHelper, to help
                    // it initialize itself.
                    event.setAction(MotionEvent.ACTION_DOWN);
                    mDragHelper.processTouchEvent(event);
                    parent.requestDisallowInterceptTouchEvent(true);
                    sX = event.getRawX();
                    sY = event.getRawY();
                    return true;
                }

                float distanceX = event.getRawX() - sX;
                float distanceY = event.getRawY() - sY;
                float angle = Math.abs(distanceY / distanceX);
                angle = (float) Math.toDegrees(Math.atan(angle));

                boolean doNothing = false;
                if (mDragEdge == DragEdge.Right) {
                    boolean suitable = (status == Status.Open && distanceX > 0) || (status == Status.Close && distanceX < 0);
                    suitable = suitable || (status == Status.Middle);

                    if (angle > 30 || !suitable) {
                        doNothing = true;
                    }
                }

                if (mDragEdge == DragEdge.Left) {
                    boolean suitable = (status == Status.Open && distanceX < 0) || (status == Status.Close && distanceX > 0);
                    suitable = suitable || status == Status.Middle;

                    if (angle > 30 || !suitable) {
                        doNothing = true;
                    }
                }

                if (mDragEdge == DragEdge.Top) {
                    boolean suitable = (status == Status.Open && distanceY < 0) || (status == Status.Close && distanceY > 0);
                    suitable = suitable || status == Status.Middle;

                    if (angle < 60 || !suitable) {
                        doNothing = true;
                    }
                }

                if (mDragEdge == DragEdge.Bottom) {
                    boolean suitable = (status == Status.Open && distanceY > 0) || (status == Status.Close && distanceY < 0);
                    suitable = suitable || status == Status.Middle;

                    if (angle < 60 || !suitable) {
                        doNothing = true;
                    }
                }

                if (doNothing) {
                    parent.requestDisallowInterceptTouchEvent(false);
                    return false;
                } else {
                    if (touching != null) {
                        touching.setPressed(false);
                    }
                    parent.requestDisallowInterceptTouchEvent(true);
                    mDragHelper.processTouchEvent(event);
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                sX = -1;
                sY = -1;
                if (touching != null) {
                    touching.setPressed(false);
                }
            }
            default:
                parent.requestDisallowInterceptTouchEvent(true);
                mDragHelper.processTouchEvent(event);
        }

        return true;
    }

    /**
     * if working in {@link android.widget.AdapterView}, we should response
     * {@link android.widget.Adapter} isEnable(int position).
     *
     * @return true when item is enabled, else disabled.
     */
    private boolean isEnabledInAdapterView() {
        AdapterView adapterView = getAdapterView();
        boolean enable = true;
        if (adapterView != null) {
            Adapter adapter = adapterView.getAdapter();
            if (adapter != null) {
                int p = adapterView.getPositionForView(SwipeLayout.this);
                if (adapter instanceof BaseAdapter) {
                    enable = ((BaseAdapter) adapter).isEnabled(p);
                } else if (adapter instanceof ListAdapter) {
                    enable = ((ListAdapter) adapter).isEnabled(p);
                }
            }
        }
        return enable;
    }

    public void setSwipeEnabled(boolean enabled) {
        mSwipeEnabled = enabled;
    }

    public boolean isSwipeEnabled() {
        return mSwipeEnabled;
    }

    private boolean insideAdapterView() {
        return getAdapterView() != null;
    }

    private AdapterView getAdapterView() {
        ViewParent t = getParent();
        while (t != null) {
            if (t instanceof AdapterView) {
                return (AdapterView) t;
            }
            t = t.getParent();
        }
        return null;
    }

    private void performAdapterViewItemClick(MotionEvent e) {
        ViewParent t = getParent();
        while (t != null) {
            if (t instanceof AdapterView) {
                AdapterView view = (AdapterView) t;
                int p = view.getPositionForView(SwipeLayout.this);
                if (p != AdapterView.INVALID_POSITION && view.performItemClick(view.getChildAt(p - view.getFirstVisiblePosition()), p, view.getAdapter().getItemId(p)))
                    return;
            } else {
                if (t instanceof View && ((View) t).performClick())
                    return;
            }
            t = t.getParent();
        }
    }

    private GestureDetector gestureDetector = new GestureDetector(getContext(), new SwipeDetector());

    class SwipeDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        /**
         * Simulate the touch event lifecycle. If you use SwipeLayout in
         * {@link android.widget.AdapterView} ({@link android.widget.ListView},
         * {@link android.widget.GridView} etc.) It will manually call
         * {@link android.widget.AdapterView}.performItemClick,
         * performItemLongClick.
         *
         * @param e
         * @return
         */
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (mDoubleClickListener == null) {
                performAdapterViewItemClick(e);
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (mDoubleClickListener != null) {
                performAdapterViewItemClick(e);
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            performLongClick();
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mDoubleClickListener != null) {
                View target;
                ViewGroup bottom = getBottomView();
                ViewGroup surface = getSurfaceView();
                if (e.getX() > bottom.getLeft() && e.getX() < bottom.getRight() && e.getY() > bottom.getTop() && e.getY() < bottom.getBottom()) {
                    target = bottom;
                } else {
                    target = surface;
                }
                mDoubleClickListener.onDoubleClick(SwipeLayout.this, target == surface);
            }
            return true;
        }
    }

    public void setDragEdge(DragEdge dragEdge) {
        mDragEdge = dragEdge;
        requestLayout();
    }

    /**
     * set the drag distance, it will force set the bottom view's width or
     * height via this value.
     *
     * @param max
     */
    public void setDragDistance(int max) {
        if (max < 0)
            throw new IllegalArgumentException("Drag distance can not be < 0");
        mDragDistance = dp2px(max);
        requestLayout();
    }

    /**
     * There are 2 diffirent show mode.
     * {@link com.daimajia.swipe.SwipeLayout.ShowMode}.PullOut and
     * {@link com.daimajia.swipe.SwipeLayout.ShowMode}.LayDown.
     *
     * @param mode
     */
    public void setShowMode(ShowMode mode) {
        mShowMode = mode;
        requestLayout();
    }

    public DragEdge getDragEdge() {
        return mDragEdge;
    }

    public int getDragDistance() {
        return mDragDistance;
    }

    public ShowMode getShowMode() {
        return mShowMode;
    }

    public ViewGroup getSurfaceView() {
        return (ViewGroup) getChildAt(1);
    }

    public ViewGroup getBottomView() {
        return (ViewGroup) getChildAt(0);
    }

    public enum Status {
        Middle, Open, Close
    }

    /**
     * get the open status.
     *
     * @return {@link com.daimajia.swipe.SwipeLayout.Status} Open , Close or
     * Middle.
     */
//    public Status getOpenStatus() {
//        int surfaceLeft = getSurfaceView().getLeft();
//        int surfaceTop = getSurfaceView().getTop();
//        if (surfaceLeft == getPaddingLeft() && surfaceTop == getPaddingTop())
//            return Status.Close;
//
//        if (surfaceLeft == (getPaddingLeft() - mDragDistance) || surfaceLeft == (getPaddingLeft() + mDragDistance) || surfaceTop == (getPaddingTop() - mDragDistance) || surfaceTop == (getPaddingTop() + mDragDistance))
//            return Status.Open;
//
//        return Status.Middle;
//    }
    private boolean mIsBeingDragged;

    public Status getOpenStatus() {
        View surfaceView = getSurfaceView();
        if (surfaceView == null) {
            return Status.Close;
        }
        int surfaceLeft = surfaceView.getLeft();
        int surfaceTop = surfaceView.getTop();
        if (surfaceLeft == getPaddingLeft() && surfaceTop == getPaddingTop() && mIsBeingDragged == false)
            return Status.Close;

        if (surfaceLeft == (getPaddingLeft() - mDragDistance) || surfaceLeft == (getPaddingLeft() + mDragDistance)
                || surfaceTop == (getPaddingTop() - mDragDistance) || surfaceTop == (getPaddingTop() + mDragDistance))
            return Status.Open;

        return Status.Middle;
    }

    /**
     * Process the surface release event.
     *
     * @param xvel
     * @param yvel
     */
    private void processSurfaceRelease(float xvel, float yvel) {
        // 如果onViewReleased回传的xvel为0，说明没有速率，则关闭按钮，就是回到初始位置
        if (xvel == 0 && getOpenStatus() == Status.Middle)
            close();

        if (mDragEdge == DragEdge.Left || mDragEdge == DragEdge.Right) {
            // 向右滑动速率会大于0，速率大于0Left则打开，否则关闭
            if (xvel > 0) {
                if (mDragEdge == DragEdge.Left)
                    open();
                else
                    close();
            }
            // 向左滑动速率会小于0，速率小于0Left则关闭，否则打开
            if (xvel < 0) {
                if (mDragEdge == DragEdge.Left)
                    close();
                else
                    open();
            }
        } else {
            // 速率大于0则打开，否则关闭
            if (yvel > 0) {
                if (mDragEdge == DragEdge.Top)
                    open();
                else
                    close();
            }
            if (yvel < 0) {
                if (mDragEdge == DragEdge.Top)
                    close();
                else
                    open();
            }
        }
    }

    /**
     * process bottom (PullOut mode) hand release event.
     *
     * @param xvel
     * @param yvel
     */
    private void processBottomPullOutRelease(float xvel, float yvel) {

        if (xvel == 0 && getOpenStatus() == Status.Middle)
            close();

        if (mDragEdge == DragEdge.Left || mDragEdge == DragEdge.Right) {
            if (xvel > 0) {
                if (mDragEdge == DragEdge.Left)
                    open();
                else
                    close();
            }
            if (xvel < 0) {
                if (mDragEdge == DragEdge.Left)
                    close();
                else
                    open();
            }
        } else {
            if (yvel > 0) {
                if (mDragEdge == DragEdge.Top)
                    open();
                else
                    close();
            }

            if (yvel < 0) {
                if (mDragEdge == DragEdge.Top)
                    close();
                else
                    open();
            }
        }
    }

    /**
     * process bottom (LayDown mode) hand release event.
     *
     * @param xvel
     * @param yvel
     */
    private void processBottomLayDownMode(float xvel, float yvel) {

        if (xvel == 0 && getOpenStatus() == Status.Middle)
            close();

        int l = getPaddingLeft(), t = getPaddingTop();

        if (xvel < 0 && mDragEdge == DragEdge.Right)
            l -= mDragDistance;
        if (xvel > 0 && mDragEdge == DragEdge.Left)
            l += mDragDistance;

        if (yvel > 0 && mDragEdge == DragEdge.Top)
            t += mDragDistance;
        if (yvel < 0 && mDragEdge == DragEdge.Bottom)
            t -= mDragDistance;

        mDragHelper.smoothSlideViewTo(getSurfaceView(), l, t);
        invalidate();
    }

    /**
     * smoothly open surface.
     */
    public void open() {
        open(true, true);
    }

    public void open(boolean smooth) {
        open(smooth, true);
    }

    public void open(boolean smooth, boolean notify) {
        ViewGroup surface = getSurfaceView(), bottom = getBottomView();
        int dx, dy;
        Rect rect = computeSurfaceLayoutArea(true);
        if (smooth) {
            mDragHelper.smoothSlideViewTo(getSurfaceView(), rect.left, rect.top);
        } else {
            dx = rect.left - surface.getLeft();
            dy = rect.top - surface.getTop();
            surface.layout(rect.left, rect.top, rect.right, rect.bottom);
            if (getShowMode() == ShowMode.PullOut) {
                Rect bRect = computeBottomLayoutAreaViaSurface(ShowMode.PullOut, rect);
                bottom.layout(bRect.left, bRect.top, bRect.right, bRect.bottom);
            }
            if (notify) {
                dispatchRevealEvent(rect.left, rect.top, rect.right, rect.bottom);
                dispatchSwipeEvent(rect.left, rect.top, dx, dy);
            } else {
                safeBottomView();
            }
        }
        invalidate();
    }

    /**
     * smoothly close surface.
     */
    public void close() {
        close(true, true);
    }

    public void close(boolean smooth) {
        close(smooth, true);
    }

    /**
     * close surface
     *
     * @param smooth smoothly or not.
     * @param notify if notify all the listeners.
     */
    public void close(boolean smooth, boolean notify) {
        ViewGroup surface = getSurfaceView();
        int dx, dy;
        if (smooth)
            mDragHelper.smoothSlideViewTo(getSurfaceView(), getPaddingLeft(), getPaddingTop());
        else {
            Rect rect = computeSurfaceLayoutArea(false);
            dx = rect.left - surface.getLeft();
            dy = rect.top - surface.getTop();
            surface.layout(rect.left, rect.top, rect.right, rect.bottom);
            if (notify) {
                dispatchRevealEvent(rect.left, rect.top, rect.right, rect.bottom);
                dispatchSwipeEvent(rect.left, rect.top, dx, dy);
            } else {
                safeBottomView();
            }
        }
        invalidate();
    }

    public void toggle() {
        toggle(true);
    }

    public void toggle(boolean smooth) {
        if (getOpenStatus() == Status.Open)
            close(smooth);
        else if (getOpenStatus() == Status.Close)
            open(smooth);
    }

    /**
     * a helper function to compute the Rect area that surface will hold in.
     *
     * @param open open status or close status.
     * @return
     */
    private Rect computeSurfaceLayoutArea(boolean open) {
        int l = getPaddingLeft(), t = getPaddingTop();
        if (open) {
            if (mDragEdge == DragEdge.Left)
                l = getPaddingLeft() + mDragDistance;
            else if (mDragEdge == DragEdge.Right)
                l = getPaddingLeft() - mDragDistance;
            else if (mDragEdge == DragEdge.Top)
                t = getPaddingTop() + mDragDistance;
            else
                t = getPaddingTop() - mDragDistance;
        }
        return new Rect(l, t, l + getMeasuredWidth(), t + getMeasuredHeight());
    }

    private Rect computeBottomLayoutAreaViaSurface(ShowMode mode, Rect surfaceArea) {
        Rect rect = surfaceArea;

        int bl = rect.left, bt = rect.top, br = rect.right, bb = rect.bottom;
        if (mode == ShowMode.PullOut) {
            if (mDragEdge == DragEdge.Left)
                bl = rect.left - mDragDistance;
            else if (mDragEdge == DragEdge.Right)
                bl = rect.right;
            else if (mDragEdge == DragEdge.Top)
                bt = rect.top - mDragDistance;
            else
                bt = rect.bottom;

            if (mDragEdge == DragEdge.Left || mDragEdge == DragEdge.Right) {
                bb = rect.bottom;
                br = bl + getBottomView().getMeasuredWidth();
            } else {
                bb = bt + getBottomView().getMeasuredHeight();
                br = rect.right;
            }
        } else if (mode == ShowMode.LayDown) {
            if (mDragEdge == DragEdge.Left)
                br = bl + mDragDistance;
            else if (mDragEdge == DragEdge.Right)
                bl = br - mDragDistance;
            else if (mDragEdge == DragEdge.Top)
                bb = bt + mDragDistance;
            else
                bt = bb - mDragDistance;

        }
        return new Rect(bl, bt, br, bb);

    }

    private Rect computeBottomLayDown(DragEdge dragEdge) {
        int bl = getPaddingLeft(), bt = getPaddingTop();
        int br, bb;
        if (dragEdge == DragEdge.Right) {
            bl = getMeasuredWidth() - mDragDistance;
        } else if (dragEdge == DragEdge.Bottom) {
            bt = getMeasuredHeight() - mDragDistance;
        }
        if (dragEdge == DragEdge.Left || dragEdge == DragEdge.Right) {
            br = bl + mDragDistance;
            bb = bt + getMeasuredHeight();
        } else {
            br = bl + getMeasuredWidth();
            bb = bt + mDragDistance;
        }
        return new Rect(bl, bt, br, bb);
    }

    public void setOnDoubleClickListener(DoubleClickListener doubleClickListener) {
        mDoubleClickListener = doubleClickListener;
    }

    public interface DoubleClickListener {
        public void onDoubleClick(SwipeLayout layout, boolean surface);
    }

    private int dp2px(float dp) {
        return (int) (dp * getContext().getResources().getDisplayMetrics().density + 0.5f);
    }
}
