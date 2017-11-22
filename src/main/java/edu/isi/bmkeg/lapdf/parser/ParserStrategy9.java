/*
 * Copyright (C) 2017 denkbares GmbH, Germany
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package edu.isi.bmkeg.lapdf.parser;

import java.util.List;

import edu.isi.bmkeg.lapdf.model.WordBlock;

/**
 * @author Jonas Müller
 * @created 27.09.17
 */
public class ParserStrategy9 implements ParserStrategy {

	@Override
	public List<WordBlock> addWordsToThisIteration(WordBlock word, int east, int west, int north, int south) {
		if (word.getY1() > 750) {
			return word.readNearbyWords(37, 37, 1, 1);
		}
		else {
			return word.readNearbyWords(west, east, north, south);
		}
	}
}
