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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.infomatiq.jsi.Rectangle;
import com.slimjars.dist.gnu.trove.map.hash.TIntObjectHashMap;
import com.slimjars.dist.gnu.trove.map.hash.TObjectIntHashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;
import de.topobyte.adt.geo.BBox;
import de.topobyte.android.maps.utils.HasSteplessMapWindow;
import de.topobyte.android.maps.utils.MagnificationSupport;
import de.topobyte.android.maps.utils.OnDrawListener;
import de.topobyte.android.maps.utils.view.Disableable;
import de.topobyte.android.misc.utils.AndroidTimeUtil;
import de.topobyte.jeography.core.mapwindow.SteplessMapWindow;

public abstract class LabelDrawer<B, LC extends MagnificationSupport, T extends View & HasSteplessMapWindow>
		implements Disableable, OnDrawListener<T>
{
	private final static String LOG = "labels";
	private final static String LOG_TIMES = "labels-time";

	protected Context context;
	private final View view;
	protected float density;

	protected float magnification = 1;

	// Every LabelClass is identified by an integer id. These Maps connect
	// LabelClasses with their ids and allow lookup in either direction.
	protected TIntObjectHashMap<LC> labelClasses = new TIntObjectHashMap<>();
	protected TObjectIntHashMap<LC> labelClassToId = new TObjectIntHashMap<>();
	// This map stores all currently known label candidates. It maps from some
	// label-class identifier to the set of candidates of that type.
	protected TIntObjectHashMap<Set<Label>> candidates = new TIntObjectHashMap<>();
	// This map stores a cache for label bitmaps for each LabelClass.
	protected TIntObjectHashMap<Map<String, Bitmap>> bitmaps = new TIntObjectHashMap<>();

	// TODO: implement a mechanism for making sure that the bitmaps cache only
	// contains relevant items in every multi-threaded scenario

	private boolean enabled = true;
	protected boolean enabledInternally = false;

	protected QueryWorker<T> queryWorker;
	protected RenderWorker<LC> renderWorker;

	private boolean drawDebugFrame = false;
	private boolean drawLabelBoxes = false;

	private static Paint paintFrame = new Paint();
	static {
		paintFrame.setStyle(Style.STROKE);
		paintFrame.setColor(0xFFFF0000);
	}

	private static Paint paintBoxes = new Paint();
	static {
		paintBoxes.setStyle(Style.STROKE);
		paintBoxes.setColor(0xFFFF0000);
	}

	private final Object lockBitmapCache = new Object();
	private final Object lockCandidates = new Object();
	private int configurationId = 0;

	protected Map<B, List<LabelBox>> renderedLabels = new HashMap<>();

	public LabelDrawer(Context context, View view, float density)
			throws IOException
	{
		this.context = context;
		this.view = view;
		this.density = density;
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public Map<B, List<LabelBox>> getRenderedLabels()
	{
		return renderedLabels;
	}

	public TIntObjectHashMap<LC> getLabelClasses()
	{
		return labelClasses;
	}

	public boolean setMagnification(float magnification)
	{
		boolean update = this.magnification != magnification;
		if (!update) {
			return false;
		}

		this.magnification = magnification;
		for (int key : labelClasses.keys()) {
			LC labelClass = labelClasses.get(key);
			labelClass.setMagnification(magnification);
		}

		for (int key : candidates.keys()) {
			Set<Label> labels = candidates.get(key);
			for (Label label : labels) {
				label.width = -1;
			}
		}

		if (renderWorker != null) {
			renderWorker.cancelJobs();
		}

		synchronized (lockBitmapCache) {
			configurationId++;
			clearCache();
		}
		return true;
	}

	public float getMagnification()
	{
		return magnification;
	}

	public void layersChanged()
	{
		Log.i(LOG, "layersChanged()");

		// Reset these variables to force a reload of data on the next onDraw()
		lastIssuedRequest = null;
		lastFinishedRequest = null;
	}

	protected void clearCache()
	{
		// This is happening on the UI-thread, so it won't interfere with
		// onDraw() having references that could be used after recycle().
		for (int id : bitmaps.keys()) {
			Map<String, Bitmap> bm = bitmaps.get(id);
			for (Bitmap bitmap : bm.values()) {
				bitmap.recycle();
			}
		}
		bitmaps.clear();
	}

	/**
	 * These variables store the parameters of the area we last queried / last
	 * got results for. We use this to optimize the number of queries by not
	 * issuing duplicate queries with the same parameters.
	 */
	private DataRequest lastIssuedRequest = null;
	private DataRequest lastFinishedRequest = null;

	/**
	 * Called by the QueryWorker to update the internal structure of candidate
	 * labels after retrieving some data from the database.
	 */
	void report(DataRequest request, TIntObjectHashMap<List<Label>> labelMap)
	{
		Log.i(LOG, "LabelDrawer: received some results from the QueryWorker");
		AndroidTimeUtil.time("report data");
		synchronized (lockCandidates) {
			if (request.configurationId != configurationId) {
				Log.i(LOG, "Dismissing outdated data results");
				return;
			}
			lastFinishedRequest = request;
			for (int key : labelMap.keys()) {
				List<Label> labels = labelMap.get(key);
				Set<Label> classCandidates = candidates.get(key);
				if (classCandidates == null) {
					classCandidates = new TreeSet<>();
					candidates.put(key, classCandidates);
				}
				Log.i(LOG, "For class " + key + ": " + labels.size());
				for (Label label : labels) {
					classCandidates.add(label);
				}
			}
		}
		AndroidTimeUtil.time("report data", LOG_TIMES,
				"time for integrating data results: %d");
		view.postInvalidate();
	}

	/**
	 * Called by the RenderWorker to notify about a new readily usable
	 * Label-Bitmap.
	 */
	void report(LabelRequest<LC> request, Bitmap bitmap)
	{
		synchronized (lockBitmapCache) {
			if (request.configurationId != configurationId) {
				bitmap.recycle();
				return;
			}
			LC labelClass = request.labelClass;
			int id = labelClassToId.get(labelClass);
			Log.i(LOG, "Received label for class " + id + ": '" + request.text
					+ "'");
			Map<String, Bitmap> bm = bitmaps.get(id);

			if (bm == null) {
				bm = new HashMap<>();
				bitmaps.put(id, bm);
			}

			bm.put(request.text, bitmap);
		}
		view.postInvalidate();
	}

	@Override
	public void onDraw(T mapView, Canvas canvas)
	{
		if (!enabled || !enabledInternally) {
			return;
		}
		Log.i(LOG, "onDraw()");

		if (drawDebugFrame) {
			// draw a red rectangle around everything
			Rect rect = new Rect(10, 10, mapView.getWidth() - 10,
					mapView.getHeight() - 10);
			canvas.drawRect(rect, paintFrame);
		}

		SteplessMapWindow mapWindow = mapView.getSteplessMapWindow();

		// Post a new request to the QueryWorker
		BBox bbox = mapWindow.getBoundingBox();
		double zoom = mapWindow.getZoom();
		int izoom = (int) Math.round(zoom);

		DataRequest request = new DataRequest(configurationId, bbox, izoom);
		if (lastIssuedRequest != null && request.equals(lastIssuedRequest)) {
			Log.i(LOG,
					"Not making another query, it's the same as the last issued one");
		} else if (lastFinishedRequest != null
				&& request.equals(lastFinishedRequest)) {
			Log.i(LOG,
					"Not making another query, it's the same as the last successful one");
		} else {
			lastIssuedRequest = request;
			Log.i(LOG, "Posting request to the QueryWorker");
			queryWorker.post(request);
		}

		// Do the actual rendering of what we already have in place
		RectangleIntersectionTester tester = new RTreeIntersectionTester();

		synchronized (lockBitmapCache) {
			synchronized (lockCandidates) {
				render(mapWindow, bbox, canvas, tester);

				int size = 0;
				for (int key : bitmaps.keys()) {
					Map<String, Bitmap> bm = bitmaps.get(key);
					if (bm == null) {
						continue;
					}
					size += bm.size();
				}
				Log.i(LOG, "Size of bitmap cache: " + size);
			}
		}
	}

	protected abstract void render(SteplessMapWindow mapWindow, BBox bbox,
			Canvas canvas, RectangleIntersectionTester tester);

	protected void use(int id, Label label, LC labelClass, Canvas canvas,
			RectangleIntersectionTester tester, Map<String, Bitmap> bm,
			Rectangle r, List<LabelBox> basket)
	{
		tester.add(r, true);
		basket.add(new LabelBox(label, r.copy()));

		Bitmap bitmap = bm.get(label.text);

		if (bitmap == null) {
			renderWorker.post(new LabelRequest<>(configurationId, id,
					labelClass, label.text));
			return;
		}

		canvas.drawBitmap(bitmap, r.minX, r.minY, null);
		if (drawLabelBoxes) {
			canvas.drawRect(new RectF(r.minX, r.minY, r.maxX, r.maxY),
					paintBoxes);
		}
	}

	public void setDrawDebugFrame(boolean drawFrame)
	{
		this.drawDebugFrame = drawFrame;
	}

	public void setDrawDebugBoxes(boolean drawBoxes)
	{
		this.drawLabelBoxes = drawBoxes;
	}

	public void clearBitmaps()
	{
		synchronized (lockBitmapCache) {
			configurationId++;
			clearCache();
		}
	}

	public void clearBitmapsAndCandidates()
	{
		synchronized (lockBitmapCache) {
			synchronized (lockCandidates) {
				configurationId++;
				clearCache();
				candidates.clear();
			}
		}
	}

	public void forceNewQuery()
	{
		lastIssuedRequest = null;
		lastFinishedRequest = null;
	}

	public void destroy()
	{
		enabledInternally = false;
		clearBitmapsAndCandidates();
		queryWorker.destroy();
		renderWorker.destroy();
	}
}
