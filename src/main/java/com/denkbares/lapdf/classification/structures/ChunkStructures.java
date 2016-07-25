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

import java.util.Objects;

import edu.isi.bmkeg.lapdf.features.ChunkFeatures;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;

/**
 * This class serves as gateway for detecting basic structures like tables or lists in
 * {@link ChunkBlock} instances.
 *
 * An instance can be created using the static {@link ChunkStructures.Builder} class that
 * allows the configuration of the underlying {@link StructureDetector} instances.
 *
 * @author Sebastian Furth (denkbares GmbH)
 * @created 19.08.15
 */
public class ChunkStructures {

	/**
	 * Builder for the creation of {@link ChunkStructures} instances
	 */
	public static class Builder {

		// required
		private final ChunkBlock block;
		private final ChunkFeatures features;

		// optional
		private StructureDetector tableDetector = new LineBasedTableDetector();
		private StructureDetector orderedListDetector = new NaiveOrderedListDetector();
		private StructureDetector unorderedListDetector = new NaiveUnorderedListDetector();

		public Builder(ChunkBlock block, ChunkFeatures features) {
			Objects.requireNonNull(block);
			Objects.requireNonNull(features);
			this.block = block;
			this.features = features;
		}

		/**
		 * Sets the specified {@link StructureDetector} as table detector.
		 *
		 * @param tableDetector a detector for tables
		 * @return a {@link ChunkStructures.Builder} instances configured with the table detector.
		 */
		public Builder setTableDetector(StructureDetector tableDetector) {
			this.tableDetector = tableDetector;
			return this;
		}

		/**
		 * Sets the specified {@link StructureDetector} as detector for ordered lists.
		 *
		 * @param orderedListDetector a detector for ordered lists
		 * @return a {@link ChunkStructures.Builder} instances configured with the ordered list detector.
		 */
		public Builder setOrderedListDetector(StructureDetector orderedListDetector) {
			this.orderedListDetector = orderedListDetector;
			return this;
		}

		/**
		 * Sets the specified {@link StructureDetector} as detector for unordered lists.
		 *
		 * @param unorderedListDetector a detector for unordered lists
		 * @return a {@link ChunkStructures.Builder} instances configured with the unordered list detector.
		 */
		public Builder setUnorderedListDetector(StructureDetector unorderedListDetector) {
			this.unorderedListDetector = unorderedListDetector;
			return this;
		}

		/**
		 * Returns a {@link ChunkStructures} instances configured according to this builder.
		 * @return a configured {@link ChunkStructures} instance.
		 */
		public ChunkStructures build() {
			return new ChunkStructures(this);
		}
	}

	private final ChunkBlock block;
	private final ChunkFeatures features;

	private final StructureDetector tableDetector;
	private final StructureDetector orderedListDetector;
	private final StructureDetector unorderedListDetector;

	private ChunkStructures(Builder builder) {
		this.block = builder.block;
		this.features = builder.features;
		this.tableDetector = builder.tableDetector;
		this.unorderedListDetector = builder.unorderedListDetector;
		this.orderedListDetector = builder.orderedListDetector;
	}

	/**
	 * Returns a double value that expresses the confidence the block being a table.
	 * @return confidence for block being a table.
	 */
	public double isTable() {
		return 0;
//		return tableDetector.classify(block, features);
	}

	/**
	 * Returns a double value that expresses the confidence the block being a ordered list.
	 * @return confidence for block being a ordered list.
	 */
	public double isOrderedList() {
		return orderedListDetector.classify(block, features);
	}

	/**
	 * Returns a double value that expresses the confidence the block being a unordered list.
	 * @return confidence for block being a unordered list.
	 */
	public double isUnorderedList() {
		return unorderedListDetector.classify(block, features);
	}

}
