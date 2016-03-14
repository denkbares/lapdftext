package edu.isi.bmkeg.lapdf.model;

import edu.isi.bmkeg.lapdf.model.spatial.SpatialContainer;

import java.util.List;

public interface PageBlock extends Block, SpatialContainer {

	public int getPageNumber();

	public int initialize(List<WordBlock> list, int startId);

	public int getPageBoxHeight();

	public int getPageBoxWidth();

	public LapdfDocument getDocument();

	public double getAvgLineDistance();

	public double getAvgSpacing();
}
