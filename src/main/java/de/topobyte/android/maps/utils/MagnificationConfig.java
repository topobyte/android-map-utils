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

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class MagnificationConfig
{

	public int base;
	public int min;
	public int max;

	public MagnificationConfig(int base, int min, int max)
	{
		this.base = base;
		this.min = min;
		this.max = max;
	}

	public static MagnificationConfig getMagnificationConfig(Activity context)
	{
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager windowManager = context.getWindowManager();
		Display display = windowManager.getDefaultDisplay();
		display.getMetrics(metrics);

		return getMagnificationConfig(metrics);
	}

	public static MagnificationConfig getMagnificationConfig(
			DisplayMetrics metrics)
	{
		float density = metrics.density;
		if (density < 1) {
			density = 1;
		}
		float factor = 1 + (density - 1) * 0.75f;
		return new MagnificationConfig(Math.round(factor * 100), 75, 150);
	}

}