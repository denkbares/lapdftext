package edu.isi.bmkeg.lapdf.model.RTree;

import edu.isi.bmkeg.lapdf.model.*;
import edu.isi.bmkeg.lapdf.model.ordering.SpatialOrdering;
import edu.isi.bmkeg.lapdf.model.spatial.SpatialEntity;

import java.util.*;

public class RTPageBlock extends RTSpatialContainer implements PageBlock {
	
	private static final long serialVersionUID = 1L;
	
	private int pageNumber;
	private int boxHeight;
	private int boxWidth;
	
	private Map<Integer, WordBlock> indexToWordBlockMap;
	private Map<Integer, ChunkBlock> indexToChunkBlockMap;

	private LapdfDocument document;

	private double avgLineDistance;

	public RTPageBlock(int pageNumber,
			int pageBoxWidth, int pageBoxHeight,
			LapdfDocument document) {
		
		super();

		this.indexToWordBlockMap = new HashMap<Integer, WordBlock>();
		this.indexToChunkBlockMap = new HashMap<Integer, ChunkBlock>();
				
		this.pageNumber = pageNumber;
		this.boxHeight = pageBoxHeight;
		this.boxWidth = pageBoxWidth;
		this.document = document;

	}

	public double getAvgLineDistance(){
		return avgLineDistance;
	}

	private double calculateAvgLineDistance(){
		//Calculate the average distance of lines on this page
		double sumOfDistances = 0, noOfLines = 0;
		//Sum distances, then divide to get median
		ArrayList<WordBlock> wordBlocksDesc = (ArrayList<WordBlock>)getAllWordBlocks(SpatialOrdering.ORIGINAL_MODE);
		wordBlocksDesc.sort(new Comparator<WordBlock>() {
			@Override
			public int compare(WordBlock o1, WordBlock o2) {
				if(o1.getY1() < o2.getY1())
					return 1;
				else if(o1.getY1() == o2.getY1())
					return 0;
				else
					return -1;
			}
		});
		//Check downwards only, as list is ordered desc.
		ArrayList<Double> lineHeights = new ArrayList<>();
		for(WordBlock w : wordBlocksDesc) {
			double minDist = -1;

			for (int i = 0; i < wordBlocksDesc.size(); i++) {
				//look until closest y2-wb is found
				WordBlock temp = wordBlocksDesc.get(i);
				if (!w.equals(temp) && (minDist > w.getY1() - temp.getY2() || minDist == -1)) {
					if(w.getY1() > temp.getY2())
						minDist = w.getY1() - temp.getY2();
				}
			}

			//minDist is now the closest margin between w and any block (i.e. the line height)
			sumOfDistances += minDist;
			noOfLines++;
		}
		return sumOfDistances/noOfLines;
	}

	public double getAvgSpacing(){
		//Better Alternative : Space Frequency Counter from JPedalExtractor?
		//-> Would be suitable for height-dependent space checking -> More Precise?
		return getMostPopularHorizontalSpaceBetweenWordsPage();
	}
	
	public int getHeight() {
		return this.getX2()-this.getX1();
	}

	public int getWidth() {
		return this.getY2()-this.getY1();
	}

	public int getX1() {
		return (int) this.getMargin()[0];
	}

	public int getX2() {
		return (int) this.getMargin()[2];
	}

	public int getY1() {
		return (int) this.getMargin()[1];
	}

	public int getY2() {
		return (int) this.getMargin()[3];
	}

	public int getPageNumber() {
		return pageNumber;
	}
	
	@Override
	public String readLeftRightMidLine() {
		return null;
	}

	@Override
	public boolean isFlush(String condition, int value) {		
		return false;
	}

	@Override
	public Block getContainer() {
		return null;
	}

	@Override
	public void setContainer(Block block) {
	}

	@Override
	public int getPageBoxHeight() {		
		return boxHeight;
	}

	@Override
	public int getPageBoxWidth() {
		return boxWidth;
	}

	@Override
	public LapdfDocument getDocument() {
		return document;
	}

	@Override
	public PageBlock getPage() {
		return this;
	}

	@Override
	public void setPage(PageBlock page) { 
		// Do nothing
	}
	
	@Override
	public int initialize(List<WordBlock> list, int startId) {

		for(WordBlock block:list){
			block.setPage(this);
			this.add(block, startId++);
		}

		this.avgLineDistance = calculateAvgLineDistance();

		return startId;
	
	}

	@Override
	public void add(SpatialEntity entity, int id) {

		RTSpatialEntity rtSpatialEntity = (RTSpatialEntity) entity;
		rtSpatialEntity.setId(id);
		if (rtSpatialEntity instanceof ChunkBlock) {
			this.indexToChunkBlockMap.put(id, (ChunkBlock) rtSpatialEntity);
		} else {
			this.indexToWordBlockMap.put(id, (WordBlock) rtSpatialEntity);
		}
		tree.add(rtSpatialEntity, id);
						
	}	
	
	@Override
	public SpatialEntity getEntity(int id) {
		if (indexToWordBlockMap.containsKey(id))
			return indexToWordBlockMap.get(id);

		return indexToChunkBlockMap.get(id);
	}

	@Override
	public boolean delete(SpatialEntity entity, int id) {

		RTSpatialEntity rtSpatialEntity = (RTSpatialEntity) entity;

		if (indexToChunkBlockMap.containsKey(id))
			indexToChunkBlockMap.remove(id);
		else
			indexToWordBlockMap.remove(id);
		
		boolean treeDel = tree.delete(rtSpatialEntity, id);
		
		return treeDel;

	}
	
	@Override
	public List<ChunkBlock> getAllChunkBlocks(String ordering) {

		List<ChunkBlock> list = new ArrayList<ChunkBlock>(
				indexToChunkBlockMap.values());
		if (ordering != null) {
			Collections.sort(list, new SpatialOrdering(ordering));
		}

		return list;
	}
	
	@Override
	public List<WordBlock> getAllWordBlocks(String ordering) {
		
		List<WordBlock> list = new ArrayList<WordBlock>(
				indexToWordBlockMap.values());
		
		if (ordering != null) {			
			Collections.sort(list, new SpatialOrdering(ordering));
		}

		return list;
	}

}
