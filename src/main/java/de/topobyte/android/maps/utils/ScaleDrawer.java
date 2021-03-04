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
import android.graphics.Paint.Cap;
import android.graphics.Paint.FontMetrics;
import android.view.View;
import de.topobyte.util.maps.MercatorUtil;
import de.topobyte.util.maps.scalebar.MapScaleBar;
import de.topobyte.util.maps.scalebar.MapScaleChecker;

public class ScaleDrawer<T extends View & HasMapWindow> implements
		OnDrawListener<T>
{
	private MapScaleChecker mapScaleChecker;

	private boolean enabled = true;

	private int maxWidth;
	private float offsetX;
	private float offsetY;

	private float innerLineWidth;
	private float outerLineWidth;
	private float heightBar;
	private float fontSize;

	private Paint paintBarOutline;
	private Paint paintBarInline;

	private Paint paintTextOutline;
	private Paint paintTextInline;
	private FontMetrics metrics;

	private int colorBackground = 0xFFFFFFFF;
	private int colorForeground = 0xFF000000;

	public ScaleDrawer(int maxWidth, float offsetX, float offsetY,
			float innerLineWidth, float outerLineWidth, float heightBar,
			float fontSize)
	{
		this.maxWidth = maxWidth;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.innerLineWidth = innerLineWidth;
		this.outerLineWidth = outerLineWidth;
		this.heightBar = heightBar;
		this.fontSize = fontSize;

		mapScaleChecker = new MapScaleChecker(maxWidth);

		initPaints();
	}

	public void setColors(int foreground, int background)
	{
		colorForeground = foreground;
		colorBackground = background;
		initPaints();
	}

	private void initPaints()
	{
		paintBarOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintBarOutline.setStyle(Paint.Style.STROKE);
		paintBarOutline.setStrokeCap(Cap.SQUARE);
		paintBarOutline.setStrokeWidth(outerLineWidth);
		paintBarOutline.setColor(colorBackground);

		paintBarInline = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintBarInline.setStyle(Paint.Style.STROKE);
		paintBarInline.setStrokeCap(Cap.SQUARE);
		paintBarInline.setStrokeWidth(innerLineWidth);
		paintBarInline.setColor(colorForeground);

		paintTextOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintTextOutline.setStyle(Paint.Style.STROKE);
		paintTextOutline.setStrokeCap(Cap.ROUND);
		paintTextOutline.setStrokeWidth(outerLineWidth);
		paintTextOutline.setColor(colorBackground);
		paintTextOutline.setTextSize(fontSize);

		paintTextInline = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintTextInline.setStyle(Paint.Style.FILL);
		paintTextInline.setColor(colorForeground);
		paintTextInline.setTextSize(fontSize);

		metrics = paintTextInline.getFontMetrics();
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	@Override
	public void onDraw(T view, Canvas canvas)
	{
		if (!enabled) {
			return;
		}

		double lat = view.getMapWindow().getCenterLat();
		double metersPerPixel = MercatorUtil.calculateGroundResolution(lat,
				view.getMapWindow().getWorldsizePixels());
		mapScaleChecker.setMaxPixels(maxWidth);
		MapScaleBar mapScaleBar = mapScaleChecker
				.getAppropriate(metersPerPixel);

		drawBar(view, canvas, mapScaleBar);
	}

	private void drawBar(T view, Canvas canvas, MapScaleBar mapScaleBar)
	{
		int height = view.getHeight();

		String text = createText(mapScaleBar);

		float y0 = height - offsetY - heightBar;
		float ym = height - offsetY - heightBar / 2;
		float y1 = height - offsetY;
		int w = mapScaleBar.getPixels();

		// outline
		canvas.drawLine(offsetX, ym, offsetX + w, ym, paintBarOutline);
		canvas.drawLine(offsetX, y0, offsetX, y1, paintBarOutline);
		canvas.drawLine(offsetX + w, y0, offsetX + w, y1, paintBarOutline);

		// inline
		canvas.drawLine(offsetX, ym, offsetX + w, ym, paintBarInline);
		canvas.drawLine(offsetX, y0, offsetX, y1, paintBarInline);
		canvas.drawLine(offsetX + w, y0, offsetX + w, y1, paintBarInline);

		// text
		float posX = offsetX + fontSize;
		float posY = ym - innerLineWidth - metrics.descent;
		canvas.drawText(text, posX, posY, paintTextOutline);
		canvas.drawText(text, posX, posY, paintTextInline);
	}

	private String createText(MapScaleBar mapScaleBar)
	{
		int meters = mapScaleBar.getMeters();
		if (meters > 1000) {
			return String.format("%d km", meters / 1000);
		}
		return String.format("%d m", meters);
	}

}
