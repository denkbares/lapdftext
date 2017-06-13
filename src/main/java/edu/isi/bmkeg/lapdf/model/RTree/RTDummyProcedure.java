package edu.isi.bmkeg.lapdf.model.RTree;

import edu.isi.bmkeg.lapdf.model.spatial.SpatialContainer;
import edu.isi.bmkeg.lapdf.model.spatial.SpatialEntity;
import gnu.trove.procedure.TIntProcedure;

public class RTDummyProcedure implements TIntProcedure {
	
	SpatialContainer tree;

	public RTDummyProcedure(SpatialContainer tree) {
		this.tree = tree;
	}

	@Override
	public boolean execute(int arg0) {
		SpatialEntity entity = this.tree.getEntity(arg0);
		System.out.println(arg0);
		return true;
	}

}
