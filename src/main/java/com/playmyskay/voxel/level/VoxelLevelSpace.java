package com.playmyskay.voxel.level;

import com.badlogic.gdx.math.collision.BoundingBox;
import com.playmyskay.octree.common.OctreeNodeDescriptor;

public class VoxelLevelSpace extends VoxelLevel {
	private BoundingBox boundingBox = new BoundingBox();

	@Override
	public boolean hasBoundingBox () {
		return true;
	}

	@Override
	public BoundingBox boundingBox () {
		return boundingBox;
//		return super.boundingBox();
	}

	@Override
	public void update (VoxelLevel node, OctreeNodeDescriptor descriptor) {

	}
}
