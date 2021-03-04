// Copyright 2021 Sebastian Kuerten
//
// This file is part of android-map-utils.
//
// android-map-utils is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// android-map-utils is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with android-map-utils. If not, see <http://www.gnu.org/licenses/>.

package de.topobyte.android.maps.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Join;

public class TextOverlayDrawer
{

	private Paint paintTextBG = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint paintTextFG = new Paint(Paint.ANTI_ALIAS_FLAG);

	private int colorForeground = 0xFF000000;
	private int colorBackground = 0xFFFFFFFF;

	private float density;

	private float ascent;
	private float descent;
	private float lineHeight;

	public TextOverlayDrawer(float textSize, float strokeWidth, float density)
	{
		this.density = density;
		float size = textSize * density;
		float width = strokeWidth * density;

		paintTextBG.setColor(colorBackground);
		paintTextBG.setStrokeCap(Cap.ROUND);
		paintTextBG.setStrokeJoin(Join.ROUND);
		paintTextBG.setStyle(Paint.Style.STROKE);
		paintTextBG.setTextSize(size);
		paintTextBG.setStrokeWidth(width);

		paintTextFG.setColor(colorForeground);
		paintTextFG.setStyle(Paint.Style.FILL);
		paintTextFG.setTextSize(size);

		updateFontMetrics();
	}

	/*
	 * Methods for setting the appearance
	 */

	public void setBackgroundColor(int color)
	{
		paintTextBG.setColor(color);
	}

	public void setForegroundColor(int color)
	{
		paintTextFG.setColor(color);
	}

	public void setTextSize(float textSize)
	{
		float size = textSize * density;
		paintTextBG.setTextSize(size);
		paintTextFG.setTextSize(size);
		updateFontMetrics();
	}

	public void setStrokeWidth(float strokeWidth)
	{
		float width = strokeWidth * density;
		paintTextBG.setStrokeWidth(width);
		updateFontMetrics();
	}

	private void updateFontMetrics()
	{
		FontMetrics metrics = paintTextFG.getFontMetrics();
		ascent = metrics.ascent;
		descent = metrics.descent;
		lineHeight = metrics.bottom - metrics.top + metrics.leading;
	}

	/*
	 * Methods for drawing
	 */

	public void drawTopLeft(Canvas canvas, String text, float margin)
	{
		drawTopLeft(canvas, text, margin, 0);
	}

	public void drawTopLeft(Canvas canvas, String text, float margin, int line)
	{
		paintTextBG.setTextAlign(Align.LEFT);
		paintTextFG.setTextAlign(Align.LEFT);
		float dMargin = margin * density;
		float x = dMargin;
		float y = dMargin - ascent + line * lineHeight;
		canvas.drawText(text, x, y, paintTextBG);
		canvas.drawText(text, x, y, paintTextFG);
	}

	public void drawBottomLeft(Canvas canvas, String text, float margin,
			float height)
	{
		drawBottomLeft(canvas, text, margin, height, 0);
	}

	public void drawBottomLeft(Canvas canvas, String text, float margin,
			float height, int line)
	{
		paintTextBG.setTextAlign(Align.LEFT);
		paintTextFG.setTextAlign(Align.LEFT);
		float dMargin = margin * density;
		float offsetY = descent + dMargin;
		float x = dMargin;
		float y = height - offsetY - line * lineHeight;
		canvas.drawText(text, x, y, paintTextBG);
		canvas.drawText(text, x, y, paintTextFG);
	}

	public void drawTopRight(Canvas canvas, String text, float margin,
			float width)
	{
		drawTopRight(canvas, text, margin, width, 0);
	}

	public void drawTopRight(Canvas canvas, String text, float margin,
			float width, int line)
	{
		paintTextBG.setTextAlign(Align.RIGHT);
		paintTextFG.setTextAlign(Align.RIGHT);
		float dMargin = margin * density;
		float x = width - dMargin;
		float y = dMargin - ascent + line * lineHeight;
		canvas.drawText(text, x, y, paintTextBG);
		canvas.drawText(text, x, y, paintTextFG);
	}

	public void drawBottomRight(Canvas canvas, String text, float margin,
			float width, float height)
	{
		drawBottomRight(canvas, text, margin, width, height, 0);
	}

	public void drawBottomRight(Canvas canvas, String text, float margin,
			float width, float height, int line)
	{
		paintTextBG.setTextAlign(Align.RIGHT);
		paintTextFG.setTextAlign(Align.RIGHT);
		float dMargin = margin * density;
		float offsetY = descent + dMargin;
		float x = width - dMargin;
		float y = height - offsetY - line * lineHeight;
		canvas.drawText(text, x, y, paintTextBG);
		canvas.drawText(text, x, y, paintTextFG);
	}

}
