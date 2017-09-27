/*
 * Copyright (C) 2015 denkbares GmbH, Germany
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
package com.denkbares.lapdf.classification.structures;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import edu.isi.bmkeg.lapdf.model.WordBlock;

/**
 * @author Sebastian Furth (denkbares GmbH)
 * @created 19.08.15
 */
public class NaiveOrderedListDetector implements StructureDetector {

	private static final Pattern NUM_PATTERN = Pattern.compile("\\d+");

	@Override
	public double classify(ChunkBlock block) {
		List<Long> numbers = new ArrayList<>();

		try {

			// extract all numbers
			List<WordBlock> wordBlocks = block.getWordBlocks();
			for (WordBlock wordBlock : wordBlocks) {
				Matcher matcher = NUM_PATTERN.matcher(wordBlock.getWord());
				while (matcher.find()) {
					String number = matcher.group(0);
					numbers.add(Long.parseLong(number));
				}
			}

			// compute sequence length
			int sequenceLength = 0;
			for (int i = 1; i <= wordBlocks.size(); i++) {
				if (!numbers.contains(i)) {
					break;
				}
				sequenceLength++;
			}

			return (double) sequenceLength / (double) numbers.size();

		} catch (Exception e) {
			return 0.0;
		}

	}
}