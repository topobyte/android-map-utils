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

package de.topobyte.android.maps.utils.map;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import de.topobyte.adt.geo.BBox;
import de.topobyte.android.maps.utils.HasMapWindow;
import de.topobyte.android.maps.utils.HasSteplessMapWindow;
import de.topobyte.android.maps.utils.OnDrawListener;
import de.topobyte.android.maps.utils.events.EventManager;
import de.topobyte.android.maps.utils.events.EventManagerManaged;
import de.topobyte.android.maps.utils.events.Vector2;
import de.topobyte.android.mapview.ImageManagerSourceRam;
import de.topobyte.android.mapview.ReferenceCountedBitmap;
import de.topobyte.interactiveview.Zoomable;
import de.topobyte.jeography.core.Tile;
import de.topobyte.jeography.core.TileOnWindow;
import de.topobyte.jeography.core.mapwindow.MapWindow;
import de.topobyte.jeography.core.mapwindow.MapWindowChangeListener;
import de.topobyte.jeography.core.mapwindow.SteplessMapWindow;
import de.topobyte.jeography.core.mapwindow.SteppedMapWindow;
import de.topobyte.jeography.tiles.LoadListener;

public class BaseMapView extends View implements
		LoadListener<Tile, ReferenceCountedBitmap>, EventManagerManaged,
		Zoomable, HasMapWindow, HasSteplessMapWindow, MapWindowChangeListener
{

	/*
	 * Constructors
	 */

	public BaseMapView(Context context)
	{
		super(context);
		init();
	}

	public BaseMapView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init();
	}

	public BaseMapView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init();
	}

	/*
	 * Fields
	 */

	private int displayHeight;
	private int displayWidth;
	protected float density;

	protected SteplessMapWindow mapWindow;
	protected SteppedMapWindow tileMapWindow;
	protected double tileScale;

	protected ImageManagerSourceRam<Tile, ReferenceCountedBitmap> imageManager;

	private Paint paintImages = new Paint();

	private boolean drawGrid = false;
	private float moveSpeed = 1.0f;

	protected float magnification = 1;
	protected float tileScaleFactor = 1;
	protected float userScale = 1;

	// initialization, called on construction

	private void init()
	{
		paintImages.setAntiAlias(true);
		paintImages.setFilterBitmap(true);
		paintImages.setDither(true);

		/* determine screen size to find sane value for the cache size */
		WindowManager windowManager = (WindowManager) getContext()
				.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics displaymetrics = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(displaymetrics);
		displayHeight = displaymetrics.heightPixels;
		displayWidth = displaymetrics.widthPixels;
		density = displaymetrics.density;
		Log.i("display-metrics", "height: " + displayHeight);
		Log.i("display-metrics", "width: " + displayWidth);
		Log.i("display-metrics", "density: " + density);
		if (density < 1) {
			density = 1;
		}
	}

	// initialization, called on construction by subclass

	protected void init(SteplessMapWindow mapWindow)
	{
		this.mapWindow = mapWindow;
		setupTileMapWindow();
		mapWindow.addChangeListener(this);
	}

	protected void init(
			ImageManagerSourceRam<Tile, ReferenceCountedBitmap> imageManager)
	{
		this.imageManager = imageManager;
		imageManager.addLoadListener(this);
	}

	public void destroy()
	{
		imageManager.removeLoadListener(this);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);
		mapWindow.resize(w, h);
		postInvalidate();
	}

	@Override
	public void changed()
	{
		setupTileMapWindow();
	}

	protected void setupTileMapWindow()
	{
		BBox bbox = mapWindow.getBoundingBox();
		double realZoom = mapWindow.getZoom();
		int tileZoom = (int) Math.round(realZoom);

		tileScale = Math.pow(2, realZoom - tileZoom);

		int tileWindowWidth = (int) Math.ceil(mapWindow.getWidth() / tileScale);
		int tileWindowHeight = (int) Math.ceil(mapWindow.getHeight()
				/ tileScale);
		tileMapWindow = new SteppedMapWindow(tileWindowWidth, tileWindowHeight,
				tileZoom, mapWindow.getCenterLon(), mapWindow.getCenterLat());
		tileMapWindow.setTileSize(mapWindow.getWorldScale());

		Log.i("mapwindow", "bbox: " + bbox);
		Log.i("mapwindow", String.format("width: %d, height: %d",
				mapWindow.getWidth(), mapWindow.getHeight()));
		Log.i("mapwindow", String.format(
				"real zoom: %f, tile zoom: %d, scale factor: %f", realZoom,
				tileZoom, tileScale));
		Log.i("mapwindow",
				String.format("tiles numx: %d, numy: %d",
						tileMapWindow.getNumTilesX(),
						tileMapWindow.getNumTilesY()));
	}

	/*
	 * Information
	 */

	@Override
	public MapWindow getMapWindow()
	{
		return mapWindow;
	}

	@Override
	public SteplessMapWindow getSteplessMapWindow()
	{
		return mapWindow;
	}

	/*
	 * Draw listeners
	 */

	private final List<OnDrawListener<BaseMapView>> onDrawListeners = new ArrayList<>();

	public void addOnDrawListener(OnDrawListener<BaseMapView> listener)
	{
		onDrawListeners.add(listener);
	}

	public void removeOnDrawListener(OnDrawListener<BaseMapView> listener)
	{
		onDrawListeners.remove(listener);
	}

	/*
	 * Drawing
	 */

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		// first cancel pending jobs
		imageManager.cancelJobs();

		// renew current tiles' cache status
		for (TileOnWindow tile : tileMapWindow) {
			imageManager.willNeed(tile);
		}

		int tw = mapWindow.getWorldScale();
		int th = mapWindow.getWorldScale();

		// draw all the tiles
		for (TileOnWindow tile : tileMapWindow) {
			// calculate priority
			int priority = calculatePriority(tile, tileMapWindow);

			// request
			ReferenceCountedBitmap rbitmap = imageManager.get(tile, priority);

			if (rbitmap != null) {
				drawTile(canvas, tile, rbitmap, tw, th);
			} else {
				boolean done = drawUpperTile(canvas, tile, tw, th);
				if (!done) {
					drawLowerTiles(canvas, tile, tw, th);
				}
			}
		}

		// draw the grid
		if (drawGrid) {
			Paint paint = new Paint();
			paint.setColor(0xff000000);
			paint.setStyle(Paint.Style.STROKE);
			for (TileOnWindow tile : tileMapWindow) {
				double ddx = tile.getDX() * tileScale;
				double ddy = tile.getDY() * tileScale;

				RectF rect = new RectF((float) ddx, (float) ddy,
						(float) (ddx + tw * tileScale), (float) (ddy + th
								* tileScale));
				canvas.drawRect(rect, paint);
			}
		}

		// activate onDrawListeners
		for (OnDrawListener<BaseMapView> listener : onDrawListeners) {
			listener.onDraw(this, canvas);
		}
	}

	private Rect src = new Rect();
	private Rect dst = new Rect();

	private void drawTile(Canvas canvas, TileOnWindow tile,
			ReferenceCountedBitmap rcBitmap, int tw, int th)
	{
		rcBitmap.increment();
		Bitmap bitmap = rcBitmap.getBitmap();
		bitmap.setDensity(canvas.getDensity());

		double ddx = tile.getDX() * tileScale;
		double ddy = tile.getDY() * tileScale;

		int left = (int) Math.round(ddx);
		int top = (int) Math.round(ddy);
		int right = (int) Math.round(ddx + tileScale * tw);
		int bottom = (int) Math.round(ddy + tileScale * th);
		dst.set(left, top, right, bottom);

		canvas.drawBitmap(bitmap, null, dst, paintImages);
		rcBitmap.decrement();
	}

	private boolean drawUpperTile(Canvas canvas, TileOnWindow tile, int tw,
			int th)
	{
		int zoom = tile.getZoom() - 1;
		if (zoom < 1) {
			return false;
		}
		int ty;
		if (tile.getTy() >= 0) {
			ty = tile.getTy() / 2;
		} else {
			ty = -((-tile.getTy() + 1) / 2);
		}
		int tx = tile.getTx() / 2;
		int txa = tile.getTx() % 2;
		int tya = tile.getTy() % 2;
		Tile t = new Tile(zoom, tx, ty);
		ReferenceCountedBitmap rc = imageManager.getButDoNotProduce(t);
		if (rc == null) {
			return false;
		}

		rc.increment();
		Bitmap bitmap = rc.getBitmap();
		bitmap.setDensity(canvas.getDensity());

		double ddx = tile.getDX() * tileScale;
		double ddy = tile.getDY() * tileScale;

		int baseX = txa == 0 ? 0 : tw / 2;
		int baseY = tya == 0 ? 0 : th / 2;

		src.set(baseX, baseY, baseX + tw / 2, baseY + th / 2);

		int left = (int) Math.round(ddx);
		int top = (int) Math.round(ddy);
		int right = (int) Math.round(ddx + tileScale * tw);
		int bottom = (int) Math.round(ddy + tileScale * th);
		dst.set(left, top, right, bottom);

		canvas.drawBitmap(bitmap, src, dst, paintImages);
		rc.decrement();
		return true;
	}

	private void drawLowerTiles(Canvas canvas, TileOnWindow tile, int tw, int th)
	{
		double ddx = tile.getDX() * tileScale;
		double ddy = tile.getDY() * tileScale;

		int zoom = tile.getZoom() + 1;
		for (int i = 0; i < 2; i++) {
			int tx = tile.getTx() * 2 + i;
			for (int k = 0; k < 2; k++) {
				int ty = tile.getTy() * 2 + k;
				Tile t = new Tile(zoom, tx, ty);
				ReferenceCountedBitmap rc = imageManager.getButDoNotProduce(t);
				if (rc == null) {
					continue;
				}

				rc.increment();
				Bitmap bitmap = rc.getBitmap();
				bitmap.setDensity(canvas.getDensity());

				double dstLeft = ddx + i * tw * tileScale / 2;
				double dstTop = ddy + k * th * tileScale / 2;

				int left = (int) Math.round(dstLeft);
				int top = (int) Math.round(dstTop);
				int right = (int) Math.round(dstLeft + tileScale / 2 * tw);
				int bottom = (int) Math.round(dstTop + tileScale / 2 * th);
				dst.set(left, top, right, bottom);

				canvas.drawBitmap(bitmap, null, dst, paintImages);
				rc.decrement();
			}
		}
	}

	/*
	 * Calculate the priority of a tile within the rendering queue. The priority
	 * is lower the nearer a tile's center is to the center of the current
	 * screen. Tiles with a lower priority get rendered first.
	 */
	private int calculatePriority(TileOnWindow tile, MapWindow mapWindow)
	{
		int width = mapWindow.getWidth();
		int height = mapWindow.getHeight();
		int midX = width / 2;
		int midY = height / 2;
		int tX = tile.getDX() + Tile.SIZE / 2;
		int tY = tile.getDY() + Tile.SIZE / 2;
		int dX = tX - midX;
		int dY = tY - midY;
		int dist = dX * dX + dY * dY;
		return dist;
	}

	protected int calculateCacheSize()
	{
		// Calculate based on tile map window size and tile size
		int tw = tileMapWindow.getTileWidth();
		int th = tileMapWindow.getTileHeight();

		int wtX = (tileMapWindow.getWidth() + tw - 1) / tw;
		int wtY = (tileMapWindow.getHeight() + th - 1) / th;

		// Calculate based on display size and scaled tile size
		double stw = tw * tileScale;
		double sth = th * tileScale;

		int dtX = (int) Math.ceil((displayWidth) / stw);
		int dtY = (int) Math.ceil((displayHeight) / sth);

		// Log information
		Log.i("display", String.format("display size: %d, %d", displayWidth,
				displayHeight));
		Log.i("display",
				String.format("scaled tile size: %.2f, %.2f", stw, sth));
		Log.i("display", String.format("# scaled tiles: %d, %d", dtX, dtY));

		Log.i("display", String.format("window size: %d, %d",
				tileMapWindow.getWidth(), tileMapWindow.getHeight()));
		Log.i("display", String.format("tile size: %d, %d", tw, th));
		Log.i("display", String.format("# tiles on window: %d, %d", wtX, wtY));

		/* define cache size */
		int cacheSizeScreen1 = (int) Math.ceil((wtX + 1) * (wtY + 1) * 1.5);
		int cacheSizeScreen2 = (int) Math.ceil((dtX + 1) * (dtY + 1) * 1.5);
		int cacheSize = Math.max(cacheSizeScreen1, cacheSizeScreen2);

		Log.i("display", "Cache size (map window):   " + cacheSizeScreen1);
		Log.i("display", "Cache size (display size): " + cacheSizeScreen2);
		Log.i("display", "Cache size: " + cacheSize);
		return cacheSize;
	}

	/*
	 * Image Manager's LoadListener implementation
	 */

	@Override
	public void loadFailed(Tile tile)
	{
		// ignore
	}

	@Override
	public void loaded(Tile tile, ReferenceCountedBitmap image)
	{
		postInvalidate();
	}

	/*
	 * Configuration
	 */

	public void setMagnification(float magnification)
	{
		if (magnification == this.magnification) {
			return;
		}
		this.magnification = magnification;

		tileScaleFactor = 1;
		userScale = 1;

		float maxTileScale = 2;
		if (magnification <= maxTileScale) {
			tileScaleFactor = magnification;
		} else {
			tileScaleFactor = maxTileScale;
			userScale = magnification / tileScaleFactor;
		}

		int tileSize = Math.round(tileScaleFactor * Tile.SIZE);
		mapWindow.setWorldScale(tileSize);

		postInvalidate();
	}

	public float getTileScaleFactor()
	{
		return tileScaleFactor;
	}

	public float getUserScaleFactor()
	{
		return userScale;
	}

	/*
	 * Input handling
	 */

	private final EventManager<BaseMapView> eventManager = new EventManager<>(
			this, true);

	public EventManager<BaseMapView> getEventManager()
	{
		return eventManager;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		super.onTouchEvent(event);
		return eventManager.onTouchEvent(event);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event)
	{
		super.onTrackballEvent(event);
		return eventManager.onTrackballEvent(event);
	}

	@Override
	public void move(Vector2 distance)
	{
		int dx = Math.round(distance.getX());
		int dy = Math.round(distance.getY());
		mapWindow.move(dx, dy);
		postInvalidate();
	}

	@Override
	public void zoom(float zoomDistance)
	{
		double zoom = mapWindow.getZoom();
		double targetZoom = zoom + zoomDistance;
		mapWindow.zoom(targetZoom);
		postInvalidate();
	}

	@Override
	public void zoomIn()
	{
		mapWindow.zoomIn(0.5);
		postInvalidate();
	}

	@Override
	public void zoomOut()
	{
		mapWindow.zoomOut(0.5);
		postInvalidate();
	}

	@Override
	public void zoom(float x, float y, float zoomDistance)
	{
		// (lon, lat) that we want to keep fixed at the screen point (x, y)
		// double flon = mapWindow.getPositionLon(Math.round(x / density));
		// double flat = mapWindow.getPositionLat(Math.round(y / density));
		double flon = mapWindow.getPositionLon(Math.round(x));
		double flat = mapWindow.getPositionLat(Math.round(y));

		double zoom = mapWindow.getZoom();
		double targetZoom = zoom + zoomDistance;
		if (targetZoom < mapWindow.getMinZoom()) {
			targetZoom = mapWindow.getMinZoom();
		}
		if (targetZoom > mapWindow.getMaxZoom()) {
			targetZoom = mapWindow.getMaxZoom();
		}
		mapWindow.zoom(targetZoom);

		// (x, y) of the (lon, lat) after applying the zoom change
		double fx = mapWindow.getX(flon);
		double fy = mapWindow.getY(flat);
		// shift the map to keep the (lon, lat) fixed
		// mapWindow.move((int) Math.round(fx - x / density),
		// (int) Math.round(fy - y / density));
		mapWindow.move((int) Math.round(fx - x), (int) Math.round(fy - y));

		postInvalidate();
	}

	@Override
	public void zoomIn(float x, float y)
	{
		int dx = Math.round(x - mapWindow.getWidth() / 2);
		int dy = Math.round(y - mapWindow.getHeight() / 2);
		mapWindow.move(dx, dy);
		mapWindow.zoomIn(0.5);
		postInvalidate();
	}

	@Override
	public void zoomOut(float x, float y)
	{
		int dx = Math.round(x - mapWindow.getWidth() / 2);
		int dy = Math.round(y - mapWindow.getHeight() / 2);
		mapWindow.move(dx, dy);
		mapWindow.zoomOut(0.5);
		postInvalidate();
	}

	@Override
	public boolean canZoomIn()
	{
		return mapWindow.getZoom() < mapWindow.getMaxZoom();
	}

	@Override
	public boolean canZoomOut()
	{
		return mapWindow.getZoom() > mapWindow.getMinZoom();
	}

	public void setMoveSpeed(float moveSpeed)
	{
		this.moveSpeed = moveSpeed;
	}

	@Override
	public float getMoveSpeed()
	{
		return moveSpeed;
	}

	public boolean isDrawGrid()
	{
		return drawGrid;
	}

	public void setDrawGrid(boolean drawGrid)
	{
		this.drawGrid = drawGrid;
	}

	@Override
	public void longClick(float x, float y)
	{
		// do nothing at the moment
	}

}