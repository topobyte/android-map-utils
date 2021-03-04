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

import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.rtree.RTree;
import com.slimjars.dist.gnu.trove.procedure.TIntProcedure;

public class RTreeIntersectionTester implements RectangleIntersectionTester
{

	int counter = 1;
	RTree tree = new RTree(1, 10);

	@Override
	public void add(Rectangle r, boolean clone)
	{
		if (clone) {
			r = new Rectangle(r.minX, r.minY, r.maxX, r.maxY);
		}
		tree.add(r, counter++);
	}

	boolean free = true;

	@Override
	public boolean isFree(Rectangle rectangle)
	{
		free = true;

		tree.intersects(rectangle, new TIntProcedure() {

			@Override
			public boolean execute(int id)
			{
				free = false;
				return false;
			}
		});

		return free;
	}

}
