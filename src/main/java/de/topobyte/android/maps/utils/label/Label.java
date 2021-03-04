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

public class Label implements Comparable<Label>
{

	public int x;
	public int y;
	public String text;
	public int width = -1;
	int placeType;
	int id;

	public Label(int x, int y, String text, int placeType, int id)
	{
		this.x = x;
		this.y = y;
		this.text = text;
		this.placeType = placeType;
		this.id = id;
	}

	@Override
	public int hashCode()
	{
		return x + y;
	}

	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof Label)) {
			return false;
		}
		Label otherLabel = (Label) other;
		return otherLabel.x == x && otherLabel.y == y
				&& equalsSafe(otherLabel.text, text);
	}

	private boolean equalsSafe(String t1, String t2)
	{
		if (t1 == null && t2 == null) {
			return true;
		}
		if ((t1 == null) ^ (t2 == null)) {
			return false;
		}
		return t1.equals(t2);
	}

	@Override
	public int compareTo(Label otherLabel)
	{
		if (x < otherLabel.x) {
			return -1;
		}
		if (x > otherLabel.x) {
			return 1;
		}
		if (y < otherLabel.y) {
			return -1;
		}
		if (y > otherLabel.y) {
			return 1;
		}
		return compareSafe(text, otherLabel.text);
	}

	private int compareSafe(String t1, String t2)
	{
		if (t1 == null && t2 == null) {
			return 0;
		}
		if (t1 == null) {
			return -1;
		}
		if (t2 == null) {
			return 1;
		}
		return t1.compareTo(t2);
	}

	public int getId()
	{
		return id;
	}

	public String getText()
	{
		return text;
	}

}
