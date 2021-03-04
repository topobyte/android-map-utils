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

package de.topobyte.android.maps.utils.label;

import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import de.topobyte.android.maps.utils.MagnificationSupport;

public class LabelClass implements MagnificationSupport
{

	private int textSize;
	private float strokeWidth;
	private float magnification;
	private float dotSizeNonScaled;

	public Paint paintTextFill;
	public Paint paintTextStroke;
	public Paint paintDotFill;
	public boolean hasDot;
	public int dotSize;
	public LabelBoxConfig labelBoxConfig;
	public boolean trySecondary;
	public boolean tryReallyHard;

	public LabelClass(int textSize, float strokeWidth, boolean hasDot,
			float dotSize, PlaceStyle placeStyle, Typeface typeface, int style,
			float magnification, boolean trySecondary, boolean tryReallyHard)
	{
		this.textSize = textSize;
		this.strokeWidth = strokeWidth;
		this.magnification = magnification;
		this.hasDot = hasDot;
		this.dotSizeNonScaled = dotSize;
		this.trySecondary = trySecondary;
		this.tryReallyHard = tryReallyHard;

		Typeface font = Typeface.create(typeface, style);

		paintTextFill = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintTextFill.setStyle(Style.FILL);
		paintTextFill.setTypeface(font);
		paintTextStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintTextStroke.setStyle(Style.STROKE);
		paintTextStroke.setStrokeCap(Cap.ROUND);
		paintTextStroke.setStrokeJoin(Join.ROUND);
		paintTextStroke.setTypeface(font);
		paintDotFill = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintDotFill.setStyle(Style.FILL);

		setPlaceStyle(placeStyle);

		update();
	}

	@Override
	public void setMagnification(float magnification)
	{
		this.magnification = magnification;
		update();
	}

	private void update()
	{
		int textSize = (int) Math.ceil(this.textSize * magnification);
		int strokeWidth = (int) Math.ceil(this.strokeWidth * magnification);
		int border = (int) Math.ceil(this.strokeWidth * magnification / 2);
		dotSize = (int) Math.ceil(this.dotSizeNonScaled * magnification);

		paintTextFill.setTextSize(textSize);
		paintTextStroke.setTextSize(textSize);
		paintTextStroke.setStrokeWidth(strokeWidth);

		labelBoxConfig = new LabelBoxConfig(textSize, border);
	}

	public int getBoxWidth(String name)
	{
		float textLength = paintTextFill.measureText(name);
		return (int) Math.ceil(textLength + 2 * labelBoxConfig.border);
	}

	public void setPlaceStyle(PlaceStyle placeStyle)
	{
		paintTextFill.setColor(placeStyle.fillCaption);
		paintTextStroke.setColor(placeStyle.strokeCaption);
		paintDotFill.setColor(placeStyle.fillDot);
	}

}
