package edu.isi.bmkeg.lapdf.model.RTree;

import edu.isi.bmkeg.lapdf.model.spatial.SpatialEntity;
import gnu.trove.procedure.TIntProcedure;

public class RTSimpleProcedure implements TIntProcedure {

	private final RTSpatialContainer spatialContainer;
	private SpatialEntity foundEntity;

	public RTSimpleProcedure(RTSpatialContainer tree) {
		this.spatialContainer = tree;
	}

	@Override
	public boolean execute(int id) {
		
		SpatialEntity entity = this.spatialContainer.getEntity(id);

		if (entity!= null) {
			
			this.foundEntity = entity;
			return true;

		} else {
		
			return false;
		
		}

	}

	public SpatialEntity getFoundEntity() {
		return foundEntity;
	}

	public void setFoundEntity(SpatialEntity foundEntity) {
		this.foundEntity = foundEntity;
	}

}
