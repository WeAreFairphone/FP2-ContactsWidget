/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fairphone.mycontacts.utils;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.TextUtils;
import android.util.Log;

import com.fairphone.mycontacts.R;

import junit.framework.Assert;

import org.w3c.dom.Text;

/**
 * A drawable that encapsulates all the functionality needed to display a letter tile to
 * represent a contact image.
 *
 * Adapted from AOSP ContactsCommon code
 *
 */
public class LetterTileDrawable extends Drawable {

	private static final String TAG = LetterTileDrawable.class.getSimpleName();

	private final Paint mPaint;

	/** Letter tile */
	private static TypedArray sColors;
	private static int sDefaultColor;
	private static int sTileFontColor;
	private static float sLetterToTileRatio;
	private static Bitmap DEFAULT_AVATAR;

	/** Reusable components to avoid new allocations */
	private static final Paint sPaint = new Paint();
	private static final Rect sRect = new Rect();
	private static final char[] sFirstChar = new char[1];


	private String mDisplayName;
	private float mScale = 1.0f;
	private float mOffset = 0.0f;
	private boolean mIsCircle = false;

	private Context mContext;

	private static LetterTileDrawable sGuttenberg;
	private String mIdentifier;

	public LetterTileDrawable(Context context) {
		mPaint = new Paint();
		mPaint.setFilterBitmap(true);
		mPaint.setDither(true);
		Resources res = context.getResources();

		mContext = context;

		if (sColors == null) {
			sColors = res.obtainTypedArray(R.array.letter_tile_colors_material);
			sDefaultColor = res.getColor(R.color.letter_tile_default_color);
			sTileFontColor = res.getColor(R.color.letter_tile_font_color);
			sLetterToTileRatio = res.getFraction(R.dimen.letter_to_tile_ratio, 1, 1);
			DEFAULT_AVATAR = BitmapFactory.decodeResource(res,
					R.drawable.ic_contact_no_photo);

			sPaint.setTypeface(Typeface.create(
					res.getString(R.string.letter_tile_letter_font_family), Typeface.NORMAL));
			sPaint.setTextAlign(Align.CENTER);
			sPaint.setAntiAlias(true);
		}
	}

	public static LetterTileDrawable getIntance(Context context){
		if (sGuttenberg == null || !sGuttenberg.mContext.equals(context)) {
			sGuttenberg = new LetterTileDrawable(context);
		}
		return sGuttenberg;
	}

	public static Bitmap getLetterTileBitmap(Context context, String displayName, String identifier, int imgSize) {
		LetterTileDrawable guttenberg = getIntance(context);

		Bitmap result = Bitmap.createBitmap(imgSize, imgSize, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(result);
		guttenberg.getBounds().set(0, 0, imgSize, imgSize);
		guttenberg.setIsCircular(true);
		if (!TextUtils.isEmpty(identifier)) {
			guttenberg.setContactDetails(displayName, identifier);
			guttenberg.draw(canvas);
		} else {
			guttenberg.drawDefault(canvas, displayName);
		}

		return result;
	}

	@Override
	public void draw(final Canvas canvas) {
		final Rect bounds = getBounds();
		if (!isVisible() || bounds.isEmpty()) {
			return;
		}
		// Draw letter tile.
		drawLetterTile(canvas);
	}

	/**
	 * Draw the bitmap onto the canvas at the current bounds taking into account the current scale.
	 */
	private void drawBitmap(final Bitmap bitmap, final int width, final int height,
	                        final Canvas canvas) {
		// The bitmap should be drawn in the middle of the canvas without changing its width to
		// height ratio.
		final Rect destRect = copyBounds();

		// Crop the destination bounds into a square, scaled and offset as appropriate
		final int halfLength = (int) (mScale * Math.min(destRect.width(), destRect.height()) / 2);

		destRect.set(destRect.centerX() - halfLength,
				(int) (destRect.centerY() - halfLength + mOffset * destRect.height()),
				destRect.centerX() + halfLength,
				(int) (destRect.centerY() + halfLength + mOffset * destRect.height()));

		// Source rectangle remains the entire bounds of the source bitmap.
		sRect.set(0, 0, width, height);

		canvas.drawBitmap(bitmap, sRect, destRect, mPaint);
	}

	private void drawLetterTile(final Canvas canvas) {
		// Draw background color.
		sPaint.setColor(pickColor(mIdentifier));

		sPaint.setAlpha(mPaint.getAlpha());
		final Rect bounds = getBounds();

		final int minDimension = Math.min(bounds.width(), bounds.height());

		if (mIsCircle) {
			canvas.drawCircle(bounds.centerX(), bounds.centerY(), minDimension / 2, sPaint);
		} else {
			canvas.drawRect(bounds, sPaint);
		}

		// Draw letter/digit only if the first character is an english letter
		if (mDisplayName != null
				&& isEnglishLetter(mDisplayName.charAt(0))) {
			// Draw letter or digit.
			sFirstChar[0] = Character.toUpperCase(mDisplayName.charAt(0));

			// Scale text by canvas bounds and user selected scaling factor
			sPaint.setTextSize(mScale * sLetterToTileRatio * minDimension);
			//sPaint.setTextSize(sTileLetterFontSize);
			sPaint.getTextBounds(sFirstChar, 0, 1, sRect);
			sPaint.setColor(sTileFontColor);

			// Draw the letter in the canvas, vertically shifted up or down by the user-defined
			// offset
			canvas.drawText(sFirstChar, 0, 1, bounds.centerX(),
					bounds.centerY() + mOffset * bounds.height() + sRect.height() / 2,
					sPaint);
		} else if (mIdentifier != null) {
			drawDefault(canvas, mIdentifier);
		} else {
			drawDefault(canvas, mDisplayName);
		}
	}

	private void drawDefault(final Canvas canvas, final String id) {
		int color = sDefaultColor;
		if (!TextUtils.isEmpty(id)) {
			color = pickColor(id);
		}
		sPaint.setColor(color);
		sPaint.setAlpha(mPaint.getAlpha());
		final Rect bounds = getBounds();

		final int minDimension = Math.min(bounds.width(), bounds.height());

		if (mIsCircle) {
			canvas.drawCircle(bounds.centerX(), bounds.centerY(), minDimension / 2, sPaint);
		} else {
			canvas.drawRect(bounds, sPaint);
		}
		canvas.drawBitmap(
				DEFAULT_AVATAR,
				new Rect(0, 0, DEFAULT_AVATAR.getWidth(), DEFAULT_AVATAR.getHeight()),
				new Rect(0, 0, bounds.width(), bounds.height()),
				new Paint());
	}

	public int getColor() {
		return pickColor(mIdentifier);
	}

	/**
	 * Returns a deterministic color based on the provided contact identifier string.
	 */
	public int pickColor(final String identifier) {
		if (TextUtils.isEmpty(identifier)) {
			return sDefaultColor;
		}
		// String.hashCode() implementation is not supposed to change across java versions, so
		// this should guarantee the same email address always maps to the same color.
		// The email should already have been normalized by the ContactRequest.
		final int color = Math.abs(identifier.hashCode()) % sColors.length();
		return sColors.getColor(color, sDefaultColor);
	}

	private static boolean isEnglishLetter(final char c) {
		return ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
	}

	@Override
	public void setAlpha(final int alpha) {
		mPaint.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(final ColorFilter cf) {
		mPaint.setColorFilter(cf);
	}

	@Override
	public int getOpacity() {
		return android.graphics.PixelFormat.OPAQUE;
	}

	/**
	 * Scale the drawn letter tile to a ratio of its default size
	 *
	 * @param scale The ratio the letter tile should be scaled to as a percentage of its default
	 * size, from a scale of 0 to 2.0f. The default is 1.0f.
	 */
	public void setScale(float scale) {
		mScale = scale;
	}

	/**
	 * Assigns the vertical offset of the position of the letter tile to the ContactDrawable
	 *
	 * @param offset The provided offset must be within the range of -0.5f to 0.5f.
	 * If set to -0.5f, the letter will be shifted upwards by 0.5 times the height of the canvas
	 * it is being drawn on, which means it will be drawn with the center of the letter starting
	 * at the top edge of the canvas.
	 * If set to 0.5f, the letter will be shifted downwards by 0.5 times the height of the canvas
	 * it is being drawn on, which means it will be drawn with the center of the letter starting
	 * at the bottom edge of the canvas.
	 * The default is 0.0f.
	 */
	public void setOffset(float offset) {
		Assert.assertTrue(offset >= -0.5f && offset <= 0.5f);
		mOffset = offset;
	}

	public void setContactDetails(final String displayName, final String identifier) {
		mDisplayName = displayName;
		mIdentifier = identifier;
	}

	public void setIsCircular(boolean isCircle) {
		mIsCircle = isCircle;
	}
}
