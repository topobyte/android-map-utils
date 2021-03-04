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

import java.util.List;

import com.slimjars.dist.gnu.trove.map.hash.TIntObjectHashMap;

import android.util.Log;
import android.view.View;
import de.topobyte.adt.geo.BBox;
import de.topobyte.android.maps.utils.HasSteplessMapWindow;

/**
 * This worker executes queries on the database and returns the results to the
 * LabelDrawer.
 */
public abstract class QueryWorker<T extends View & HasSteplessMapWindow>
		implements Runnable
{
	// Object to synchronize on
	private final Object syncObject = new Object();

	// Synchronized access to these variables
	private boolean queryInProcess = false;
	private boolean workAvailable = false;
	private DataRequest request = null;

	// Other variables
	protected LabelDrawer<?, ?, T> labelDrawer;

	private boolean running = true;

	public QueryWorker(LabelDrawer<?, ?, T> labelDrawer)
	{
		this.labelDrawer = labelDrawer;
	}

	public void post(DataRequest request)
	{
		Log.i("labels", "QueryWorker received request");
		synchronized (syncObject) {
			this.request = request;
			workAvailable = true;
			if (!queryInProcess) {
				syncObject.notify();
			}
		}
	}

	@Override
	public void run()
	{
		while (running) {
			synchronized (syncObject) {
				while (!workAvailable) {
					try {
						Log.i("labels",
								"QueryWorker: no work available, waiting");
						syncObject.wait();
					} catch (InterruptedException e) {
						continue;
					}
				}
				queryInProcess = true;
			}
			executeQuery();
			synchronized (syncObject) {
				queryInProcess = false;
			}
		}
	}

	private void executeQuery()
	{
		Log.i("labels", "QueryWorker: got some work, executing");
		DataRequest myRequest = null;
		synchronized (syncObject) {
			myRequest = request;
			workAvailable = false;
		}

		BBox bbox = myRequest.bbox;

		TIntObjectHashMap<List<Label>> labels = runQuery(bbox, myRequest.zoom);

		labelDrawer.report(myRequest, labels);
	}

	protected abstract TIntObjectHashMap<List<Label>> runQuery(BBox bbox,
			int zoom);

	public void destroy()
	{
		running = false;
		synchronized (syncObject) {
			syncObject.notify();
		}
	}
}
