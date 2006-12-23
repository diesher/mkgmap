/*
 * Copyright (C) 2006 Steve Ratcliffe
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 * 
 * Author: Steve Ratcliffe
 * Create date: 07-Dec-2006
 */
package uk.me.parabola.imgfmt.app;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.ArrayList;

/**
 * The map is divided into areas, depending on the zoom level.  These are
 * known as subdivisions.
 *
 * A subdivision 'belongs' to a zoom level and cannot be intepreted correctly
 * without knowing the <i>bitsPerCoord</i> of the associated zoom level.
 *
 * Subdivisions also form a tree as subdivisions are further divided at
 * lower levels.  The subdivisions need to know their child divisions
 * because this information is represented in the map.
 *
 * @author Steve Ratcliffe
 */
public class Subdivision {
	private static final Logger log = Logger.getLogger(Subdivision.class);

	private int rgnPointer;

	// The zoom level contains the number of bits per coordinate which is
	// critical for scaling quantities by.
	private final Zoom zoomLevel;

	private boolean hasPoints;
	private boolean hasIndPoints;
	private boolean hasPolylines;
	private boolean hasPolygons;

	// The location of the central point, not scaled AFAIK
	private final int longitude;
	private final int latitude;

	// The width and the height in map units scaled by the bits-per-coordinate
	// that applies at the map level.
	private final int width;
	private final int height;

	private int number;

	// Set if this is the last one.
	private boolean last;

	private final List<Subdivision> divisions = new ArrayList<Subdivision>();

	private Subdivision(Zoom z, int latitude, int longitude,
					   int width, int height)
	{
		this.zoomLevel = z;

		this.latitude = latitude;
		this.longitude = longitude;
		this.width = width;
		this.height = height;

		z.addSubdivision(this); // FIXME: use of this in object construction
	}

	/**
	 * Get the shift value, that is the number of bits to left shift by for
	 * values that need to be saved shifted in the file.
	 *
	 * @return The shift value.  It is 24 minus the number of bits per coord.
	 */
	public int getShift() {
		return 24 - zoomLevel.getBitsPerCoord();
	}

	/**
	 * Format this record to the file.
	 *
	 * @param file The file to write to.
	 */
	public void write(ImgFile file) {
		file.put3(rgnPointer);
		file.put(getType());
		file.put3(longitude);
		file.put3(latitude);
		log.debug("last is " + last);
		file.putChar((char) (width | ((last)? 0x8000: 0)));
		file.putChar((char) height);

		if (!divisions.isEmpty()) {
			file.putChar((char) getNextLevel());
		}
	}

	/**
	 * Get the number of the first subdivision at the next level.
	 * @return The first subdivision at the next level.
	 */
	private int getNextLevel() {
		return divisions.get(0).getNumber();
	}

	/**
	 * Add this subdivision as our child at the next level.  Each subdivision
	 * can be further divided into smaller divisions.  They form a tree like
	 * arrangement.
	 *
	 * @param sd One of our subdivisions.
	 */
	private void addSubdivision(Subdivision sd) {
		divisions.add(sd);
	}

	private int getNumber() {
		return number;
	}

	public void setNumber(int n) {
		number = n;
	}

	public void setLast(boolean last) {
		this.last = last;
	}

	public void setRgnPointer(int rgnPointer) {
		this.rgnPointer = rgnPointer;
	}
	public int getRgnPointer() {
		return rgnPointer;
	}

	public int getLongitude() {
		return longitude;
	}

	public int getLatitude() {
		return latitude;
	}

	public void setHasPoints(boolean hasPoints) {
		this.hasPoints = hasPoints;
	}

	public void setHasIndPoints(boolean hasIndPoints) {
		this.hasIndPoints = hasIndPoints;
	}

	public void setHasPolylines(boolean hasPolylines) {
		this.hasPolylines = hasPolylines;
	}

	public void setHasPolygons(boolean hasPolygons) {
		this.hasPolygons = hasPolygons;
	}

	/**
	 * Get a type that shows if this area has lines, points etc.
	 *
	 * @return A code showing what kinds of element are in this subdivision.
	 */
	private byte getType() {
		byte b = 0;
		if (hasPoints)
			b |= 0x10;
		if (hasIndPoints)
			b |= 0x20;
		if (hasPolylines)
			b |= 0x40;
		if (hasPolygons)
			b |= 0x80;

		return b;
	}

	/**
	 * Create a subdivision at a given zoom level.
	 *
	 * @param area The (unshifted) area that the subdivision covers.
	 * @param zoom The zoom level that this division occupies.
	 * @return A new subdivision.
	 */
	public Subdivision createSubdivision(Area area, Zoom zoom) {
		Subdivision div = createDiv(area, zoom);
		addSubdivision(div);
		return div;
	}

	/**
	 * This should be called only once per map to create the top level
	 * subdivision.  The top level subdivision covers the whole map and it
	 * must be empty.
	 *
	 * @param area The area bounded by the map.
	 * @param zoom The zoom level which must be the highest (least detailed)
	 * zoom in the map.
	 * @return The new subdivision.
	 */
	public static Subdivision topLevelSubdivision(Area area, Zoom zoom) {
		return createDiv(area, zoom);
	}

	/**
	 * Does the work of the methods that create subdivisions.
	 * @param area The area.
	 * @param zoom The zoom level.
	 * @return A new subdivision.
	 */
	private static Subdivision createDiv(Area area, Zoom zoom) {
		// Get the central point of the area.
		int lat = (area.getMinLat() + area.getMaxLat())/2;
		int lng = (area.getMinLong() + area.getMaxLong())/2;

		// Get the half width and height of the area and adjust by the
		// bits per coord.
		int width = (area.getMaxLong() - area.getMinLong())/2;
		width >>= 24 - zoom.getBitsPerCoord();
		int height = (area.getMaxLat() - area.getMinLat())/2;
		height >>= 24 - zoom.getBitsPerCoord();

		Subdivision div = new Subdivision(zoom, lat, lng, width, height);

		return div;
	}

    public boolean isHasPoints() {
        return hasPoints;
    }

    public boolean isHasIndPoints() {
        return hasIndPoints;
    }

    public boolean isHasPolylines() {
        return hasPolylines;
    }

    public boolean isHasPolygons() {
        return hasPolygons;
    }

    /**
     * The following routines answer the question 'does there need to
     * be a pointer in the rgn section to this area?'.  You need a
     * pointer for all the regions that exist except the first one.
     * There is a strict order with points first and finally polygons.
     *
     * @return Never needed as if it exists it will be first.
     */
    public boolean needsPointPtr() {
        return false;
    }

    /**
     * Needed if it exists and is not first, ie there is a points
     * section.
     * @return true if pointer needed
     */
    public boolean needsIndPointPtr() {
        return hasIndPoints && hasPoints;
    }

    /**
     * Needed if it exists and is not first, ie there is a points or
     * indexed points section.
     * @return true if pointer needed.
     */
    public boolean needsPolylinePtr() {
        return hasPolylines && (hasPoints || hasIndPoints);
    }

    /**
     * As this is last in the list it is needed if it exists and there
     * is another section.
     * @return true if pointer needed.
     */
    public boolean needsPolygonPtr() {
        return hasPolygons && (hasPoints || hasIndPoints || hasPolylines);
    }
}