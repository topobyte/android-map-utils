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

public class PlaceStyle
{

	public int fillDot;
	public int fillCaption;
	public int strokeCaption;

	public PlaceStyle(int fillDot, int fillCaption, int strokeCaption)
	{
		this.fillDot = fillDot;
		this.fillCaption = fillCaption;
		this.strokeCaption = strokeCaption;
	}

}
