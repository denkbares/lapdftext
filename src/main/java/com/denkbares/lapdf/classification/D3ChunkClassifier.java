/*
 * Copyright (C) 2013 denkbares GmbH
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
package com.denkbares.lapdf.classification;

import java.util.List;

import edu.isi.bmkeg.lapdf.classification.Classifier;
import edu.isi.bmkeg.lapdf.extraction.exceptions.ClassificationException;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;

/**
 * @author Sebastian Furth (denkbares GmbH)
 * @created 11.05.15
 */
public class D3ChunkClassifier implements Classifier<ChunkBlock> {

	@Override
	public void classify(List<ChunkBlock> blockList) throws ClassificationException {

		// TODO: load d3web knowledge base

		// TODO: for each ChunkBlock instance start a d3web session and derive classification

		// TODO: find most probable sequence of chunk classifications (e.g. using a viterbi style algorithm)
			// TODO: * consider ambiguous classification
			// TODO: * consider document model

	}
}
