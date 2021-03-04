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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.graphics.Bitmap;
import android.util.Log;
import de.topobyte.android.maps.utils.MagnificationSupport;

/**
 * This worker renders labels into Bitmaps.
 */
public abstract class RenderWorker<LC extends MagnificationSupport> implements
		Runnable
{

	// Object to synchronize on
	private final Object syncObject = new Object();

	// Synchronized access to these variables
	private boolean working = false;
	private boolean workAvailable = false;
	// TODO: only accept a fixed number of requests to prevent memory leak
	private final Set<LabelRequest<LC>> requestsSet = new HashSet<>();
	private final List<LabelRequest<LC>> requests = new ArrayList<>();

	private boolean running = true;

	// Ohter variables
	private final LabelDrawer<?, LC, ?> labelDrawer;

	public RenderWorker(LabelDrawer<?, LC, ?> labelDrawer)
	{
		this.labelDrawer = labelDrawer;
	}

	public void post(LabelRequest<LC> request)
	{
		Log.i("labels", "Rendering request posted");
		synchronized (syncObject) {
			if (requestsSet.contains(request)) {
				Log.i("labels", "But we already have it in the queue");
				return;
			}
			requests.add(request);
			requestsSet.add(request);
			workAvailable = true;
			if (!working) {
				syncObject.notify();
			}
		}
	}

	public void cancelJobs()
	{
		synchronized (syncObject) {
			requests.clear();
			requestsSet.clear();
			workAvailable = false;
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
								"RenderWorker: no work available, waiting");
						syncObject.wait();
					} catch (InterruptedException e) {
						continue;
					}
				}
				working = true;
			}
			execute();
			synchronized (syncObject) {
				working = false;
			}
		}
	}

	private void execute()
	{
		LabelRequest<LC> myRequest = null;
		synchronized (syncObject) {
			myRequest = requests.remove(requests.size() - 1);
			requestsSet.remove(myRequest);
			workAvailable = requests.size() > 0;
		}
		Bitmap bitmap = createTextImage(myRequest.labelClass, myRequest.text);
		labelDrawer.report(myRequest, bitmap);
	}

	protected abstract Bitmap createTextImage(LC labelClass, String name);

	public void destroy()
	{
		running = false;
		synchronized (syncObject) {
			syncObject.notify();
		}
	}
}
