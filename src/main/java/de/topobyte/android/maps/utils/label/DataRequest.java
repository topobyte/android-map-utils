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

import de.topobyte.adt.geo.BBox;

public class DataRequest
{

	int configurationId;
	BBox bbox;
	int zoom;

	public DataRequest(int configurationId, BBox bbox, int zoom)
	{
		this.configurationId = configurationId;
		this.bbox = bbox;
		this.zoom = zoom;
	}

	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof DataRequest)) {
			return false;
		}
		DataRequest otherRequest = (DataRequest) other;
		return otherRequest.zoom == zoom && otherRequest.bbox.equals(bbox)
				&& otherRequest.configurationId == configurationId;
	}

}
