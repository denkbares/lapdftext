package edu.isi.bmkeg.lapdf.extraction;

import java.io.File;
import java.util.HashMap;

import edu.isi.bmkeg.lapdf.model.RTree.RTModelFactory;
import edu.isi.bmkeg.lapdf.model.factory.AbstractModelFactory;
import edu.isi.bmkeg.utils.IntegerFrequencyCounter;

/**
 * @author Stefan Plehn (denkbares GmbH)
 * @created 25.10.17
 */
public abstract class AbstractExtractor implements Extractor {

	protected final AbstractModelFactory modelFactory;
	protected final int startPage;
	protected final int endPage;
	protected int currentPage;
	protected final IntegerFrequencyCounter avgHeightFrequencyCounter;
	protected final HashMap<Integer, IntegerFrequencyCounter> spaceFrequencyCounterMap;
	protected File pdfFile;

	public AbstractExtractor() {
		this(1, -1);
	}

	public AbstractExtractor(int startPage, int endPage) {
		this.startPage = startPage;
		this.endPage = endPage;
		this.currentPage = startPage;
		this.modelFactory = new RTModelFactory();
		this.avgHeightFrequencyCounter = new IntegerFrequencyCounter(1);
		this.spaceFrequencyCounterMap = new HashMap<>();
	}
}
