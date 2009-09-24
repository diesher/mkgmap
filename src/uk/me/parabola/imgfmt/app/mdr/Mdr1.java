/*
 * Copyright (C) 2009.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 or
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */
package uk.me.parabola.imgfmt.app.mdr;

import java.util.ArrayList;
import java.util.List;

import uk.me.parabola.imgfmt.app.ImgFileWriter;

/**
 * The section MDR 1 contains a list of maps and for each map
 * an offset to a reverse index for that map.
 *
 * The reverse index consists of a number of sections, that I call sub-sections
 * here.  The sub-sections are all lists of record numbers in other sections
 * in the MDR that contain records belonging to more than one map.
 *
 * Using the index you could extract records that belong to an individual map
 * from other MDR sections without having to go through them all and check
 * which map they belong to.
 *
 * @author Steve Ratcliffe
 */
public class Mdr1 extends MdrSection {
	private final List<Mdr1Record> maps = new ArrayList<Mdr1Record>();
	private final List<Mdr1SubSection> subSections = new ArrayList<Mdr1SubSection>();

	public Mdr1(MdrConfig config) {
		assert config != null;
		setConfig(config);
	}

	public void addMap(int mapNumber) {
		Mdr1Record rec = new Mdr1Record(mapNumber, getConfig());
		maps.add(rec);
		subSections.add(new Mdr1SubSection());
	}

	public void writeSectData(ImgFileWriter writer) {
		for (Mdr1Record rec : maps)
			rec.write(writer);
	}

	public int getItemSize() {
		System.out.println("for dev " + isForDevice());
		return isForDevice()? 4: 8;
	}

	public void writeSubSections(ImgFileWriter writer) {
		for (int i = 0; i < maps.size(); i++) {
			Mdr1Record rec = maps.get(i);
			Mdr1SubSection sub = subSections.get(i);

			rec.setIndexOffset(writer.position());

			sub.writeSubSection(writer);
		}
	}
}