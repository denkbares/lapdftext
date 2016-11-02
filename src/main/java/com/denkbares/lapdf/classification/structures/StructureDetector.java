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

import edu.isi.bmkeg.lapdf.features.ChunkFeatures;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;

/**
 * Interface for the detection of structures in {@link ChunkBlock} instances.
 * The corresponding {@link ChunkFeatures} can be exploited during the detection.
 *
 * Implementations should return a confidence value in the range [0.0, 1.0], with 1.0 representing
 * an absolutely certain match and 0.0 representing an absolutely uncertain match
 *
 * @author Sebastian Furth (denkbares GmbH)
 * @created 19.08.15
 */
public interface StructureDetector {

	/**
	 * Returns a double value that expresses the certainty that the block in focus contains a
	 * certain structure, e.g. table, lists etc.
	 *
	 * The returned confidence value is in the range [0.0, 1.0], with 1.0 representing
	 * an absolutely certain match and 0.0 representing an absolutely uncertain match
	 *
	 * @param block the block to be classified
	 * @return certainty of the block being a structure
	 */
	double classify(ChunkBlock block);

}
