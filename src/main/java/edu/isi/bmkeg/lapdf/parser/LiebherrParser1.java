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
import edu.isi.bmkeg.lapdf.model.factory.AbstractModelFactory;

/**
 * @author Jonas Müller
 * @created 27.09.17
 */
public class LiebherrParser1 extends SpiralBlockParser {
	public LiebherrParser1(AbstractModelFactory modelFactory) throws Exception {
		super(modelFactory);
	}

	@Override
	protected List<WordBlock> addWordsToThisIteration(WordBlock word, int eastWest, int northSouth) {
		List<WordBlock> wordsToAddThisIteration;
		if(word.getX1() > 350) {
			wordsToAddThisIteration = word.readNearbyWords(
					eastWest, eastWest, -3, -3);
		} else {
			wordsToAddThisIteration = super.addWordsToThisIteration(word, eastWest, northSouth);
		}
		return wordsToAddThisIteration;
	}
}
