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
import de.topobyte.jeography.core.mapwindow.MapWindow;

public class MapViewOverlay<T extends HasMapWindow & PinchScalable> implements
		OnDrawListener<T>
{

	// values to support pinch scale
	private float dx, dy, pinchScale;

	@Override
	public void onDraw(T mapView, Canvas canvas)
	{
		MapWindow mapWindow = mapView.getMapWindow();

		// setup pinch scale stuff
		pinchScale = mapView.getPinchScaleFactor();
		int width = mapWindow.getWidth();
		int height = mapWindow.getHeight();
		if (pinchScale == 1.0f) {
			dx = 0;
			dy = 0;
		} else {
			dx = (width - pinchScale * width) / 2.0f;
			dy = (height - pinchScale * height) / 2.0f;
		}
	}

	protected float getX(float x)
	{
		return x * pinchScale + dx;
	}

	protected float getY(float y)
	{
		return y * pinchScale + dy;
	}

}
