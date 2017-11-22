package edu.isi.bmkeg.lapdf.parser;

import java.util.List;

import edu.isi.bmkeg.lapdf.model.WordBlock;

/**
 * @author Stefan Plehn (denkbares GmbH)
 * @created 21.11.17
 */
public class ParserStrategy11 implements ParserStrategy {
	@Override
	public List<WordBlock> addWordsToThisIteration(WordBlock word, int east, int west, int north, int south) {
		if (word.getX1() > 350) {
			return word.readNearbyWords(5, 5, -2, -2);
		}
		return word.readNearbyWords(5, 5, north, south);
	}
}
