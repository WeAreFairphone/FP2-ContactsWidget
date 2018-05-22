package community.fairphone.mycontacts.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

import community.fairphone.mycontacts.R;

public class SquareImageView extends ImageView {
    private final boolean widthBasedSquare;

    public SquareImageView(Context context) {
        super(context);
        widthBasedSquare = true;
    }

    public SquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.SquareImageView,
                0, 0);
        widthBasedSquare = a.getInteger(R.styleable.SquareImageView_useDimension, 0) == 0;
    }

    public SquareImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.SquareImageView,
                0, 0);
        widthBasedSquare = a.getInteger(R.styleable.SquareImageView_useDimension, 0) == 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measurement = widthBasedSquare ? widthMeasureSpec : heightMeasureSpec;
        super.onMeasure(measurement, measurement);
    }
}
