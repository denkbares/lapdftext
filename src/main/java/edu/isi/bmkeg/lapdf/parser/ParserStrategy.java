package edu.isi.bmkeg.lapdf.parser;

import java.util.List;

import edu.isi.bmkeg.lapdf.model.WordBlock;

/**
 * This interface makes it possible to use custom strategies for building word blocks = chunks
 *
 * @author Stefan Plehn (denkbares GmbH)
 * @created 25.10.17
 */
@FunctionalInterface
public interface ParserStrategy {

	List<WordBlock> addWordsToThisIteration(WordBlock word, int east, int west, int north, int south);
}
