package edu.isi.bmkeg.lapdf.extraction;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import edu.isi.bmkeg.lapdf.model.WordBlock;
import edu.isi.bmkeg.utils.FrequencyCounter;
import edu.isi.bmkeg.utils.IntegerFrequencyCounter;

public interface Extractor extends Iterator<List<WordBlock>> {

	int getCurrentPageBoxHeight();

	int getCurrentPageBoxWidth();

	IntegerFrequencyCounter getAvgHeightFrequencyCounter();

	FrequencyCounter getFontFrequencyCounter();

	IntegerFrequencyCounter getSpaceFrequencyCounter(int height);

	void init(File file) throws Exception;

}
