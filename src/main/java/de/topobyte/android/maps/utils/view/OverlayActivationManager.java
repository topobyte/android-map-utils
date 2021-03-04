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

package de.topobyte.android.maps.utils.view;

import java.util.Timer;
import java.util.TimerTask;

import android.view.View;
import de.topobyte.jeography.core.mapwindow.MapWindowChangeListener;

/**
 * The OverlayActivationManager is an object that implements the
 * MapWindowChangeListener interface and may thus be added to a Viewer's
 * MapWindow instance. The object will deactivate the specified nodePainter as
 * soon as the mapWindows' properties change. After the delay, specified with
 * the timeout parameter, elapsed the nodePainter will be activated again and a
 * repaint of the viewer will be scheduled. If subsequent changes to the
 * mapWindow happen during the delay phase, the reactivation will be scheduled
 * to happen accordingly later such that the delay will be honored for the last
 * change that occurred after all.
 * 
 * @author Sebastian Kürten (sebastian.kuerten@fu-berlin.de)
 * 
 */
public class OverlayActivationManager implements MapWindowChangeListener
{

	private int timeout;

	private final View view;
	private final Disableable overlay;

	private Timer timer;
	private Task task = null;

	/**
	 * Creates a new OverlayActivationManager with the specified parameters.
	 * 
	 * @param iew
	 *            the viewer to refresh after the timeout elapsed.
	 * @param overlay
	 *            the overlay to enable / disable depending on the MapWindow's
	 *            activity.
	 * @param timeout
	 *            the timeout in milliseconds.
	 */
	public OverlayActivationManager(View view, Disableable overlay, int timeout)
	{
		this.timeout = timeout;
		this.view = view;
		this.overlay = overlay;

		timer = new Timer();
	}

	public int getTimeout()
	{
		return timeout;
	}

	public void setTimeout(int timeout)
	{
		this.timeout = timeout;
	}

	@Override
	public void changed()
	{
		if (task != null) {
			task.cancel();
		}
		overlay.setEnabled(false);
		task = new Task();
		timer.schedule(task, timeout);
	}

	private class Task extends TimerTask
	{

		@Override
		public void run()
		{
			overlay.setEnabled(true);
			view.postInvalidate();
		}

	}
}
