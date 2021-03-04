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

public class LabelRequest<LC> implements Comparable<LabelRequest<LC>>
{

	int configurationId;
	int classId;
	LC labelClass;
	String text;

	public LabelRequest(int configurationId, int classId, LC labelClass,
			String text)
	{
		this.configurationId = configurationId;
		this.classId = classId;
		this.labelClass = labelClass;
		this.text = text;
	}

	@Override
	public int hashCode()
	{
		return configurationId + classId + text.hashCode();
	}

	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof LabelRequest)) {
			return false;
		}
		LabelRequest<?> or = (LabelRequest<?>) other;
		return or.configurationId == configurationId && or.classId == classId
				&& or.text.equals(text);
	}

	@Override
	public int compareTo(LabelRequest<LC> other)
	{
		if (other.configurationId != configurationId) {
			return configurationId - other.configurationId;
		}
		if (other.classId != classId) {
			return classId - other.classId;
		}
		return text.compareTo(other.text);
	}
}
