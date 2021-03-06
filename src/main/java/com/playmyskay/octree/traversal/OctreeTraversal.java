package com.playmyskay.octree.traversal;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.playmyskay.octree.common.Octree;
import com.playmyskay.octree.common.OctreeCalc;
import com.playmyskay.octree.common.OctreeNode;

public class OctreeTraversal {

	public static <N extends OctreeNode<N>> N next (N node, Vector3 v, OctreeCalc calc) {
		calc = calc.child();
		if (!node.boundingBox(calc).contains(v)) {
			return null;
		}

		for (int i = 0; i < 8; i++) {
			if (node.child(i) == null) continue;
			if (node.child(i).contains(v, calc)) {
				return node.child(i);
			}
			calc.reset();
		}

		return null;
	}

	public static <N extends OctreeNode<N>> N getFromNode (N node, int levelFrom, Vector3 v, OctreeCalc calc) {
		for (int level = levelFrom; level > 0; --level) {
			node = next(node, v, calc);
			if (node == null) {
				return null;
			}
		}

		return node;
	}

	public static <N extends OctreeNode<N>> N getFromRoot (Octree<N, ?> octree, Vector3 v, OctreeCalc calc) {
		N node = octree.rootNode;
		for (int level = octree.curLevel; level > 0; --level) {
			node = next(node, v, calc);
			if (node == null) {
				return null;
			}
		}

		return node;
	}

	public static <N extends OctreeNode<N>> void intersects (N node, Ray ray, int level, IntersectionRecorder<N> ir,
			OctreeCalc calc) {
		if (node == null) return;
		if (ir.settings.maxLevel == level) return;
		if (node.leaf()) return;
		calc.reset();
		for (int i = 0; i < 8; i++) {
			if (node.childs() == null) continue;
			if (node.child(i) == null) continue;
			if (Intersector.intersectRayBoundsFast(ray, node.child(i).boundingBox(calc))) {
				if (ir.settings().recordLevelSet.contains(level - 1)) {
					Vector3 point = new Vector3();
					if (Intersector.intersectRayBounds(ray, node.child(i).boundingBox(calc), point)) {
						if (!ir.settings().filter(node.child(i))) {
							IntersectionData<N> id = new IntersectionData<>();
							id.node = node.child(i);
							id.point = point;
							ir.intersections.add(id);
						}
					}
				} else {
					intersects(node.child(i), ray, level - 1, ir, calc);
				}
			}
			calc.reset();
		}
	}

	public static <N extends OctreeNode<N>> void intersects (N node, BoundingBox boundingBox, int level,
			IntersectionRecorder<N> ir) {
		if (node == null) return;

		if (ir.settings().recordLevelSet.contains(level)) {
			IntersectionData<N> entry = new IntersectionData<>();
			entry.node = node;
			ir.intersections.add(entry);
		}

		if (ir.settings.maxLevel > level) return;
		for (int i = 0; i < 8; i++) {
			if (node.childs() == null) continue;
			if (node.child(i) == null) continue;
			if (boundingBox.contains(node.child(i).boundingBox())
					|| boundingBox.intersects(node.child(i).boundingBox())) {
				intersects(node.child(i), boundingBox, level - 1, ir);
			}
		}
	}

	public static <N extends OctreeNode<N>> IntersectionRecorder<N> getIntersections (Octree<N, ?> octree, Ray ray,
			OctreeTraversalSettings settings, OctreeCalc calc) {
		if (octree == null || octree.rootNode == null) return null;
		if (!Intersector.intersectRayBoundsFast(ray, octree.rootNode.boundingBox(calc))) {
			return null;
		}

		IntersectionRecorder<N> ir = new IntersectionRecorder<N>();
		ir.settings(settings);
		intersects(octree.rootNode, ray, octree.curLevel, ir, calc);

		return ir;
	}

	public static <N extends OctreeNode<N>> IntersectionRecorder<N> getIntersections (Octree<N, ?> octree, Ray ray,
			OctreeCalc calc) {
		OctreeTraversalSettings settings = new OctreeTraversalSettings();
		settings.recordLevelSet.add(0);
		return getIntersections(octree, ray, settings, calc);
	}

	public static <N extends OctreeNode<N>> IntersectionRecorder<N> getIntersections (Octree<N, ?> octree,
			BoundingBox boundingBox, OctreeTraversalSettings settings) {
		if (octree == null || octree.rootNode == null) return null;
		if (!boundingBox.intersects(octree.rootNode.boundingBox())
				&& !boundingBox.contains(octree.rootNode.boundingBox())) {
			return null;
		}

		IntersectionRecorder<N> ir = new IntersectionRecorder<N>();
		ir.settings(settings);
		intersects(octree.rootNode, boundingBox, octree.curLevel, ir);

		return ir;
	}

	public static <N extends OctreeNode<N>> IntersectionRecorder<N> getIntersections (Octree<N, ?> octree,
			BoundingBox boundingBox) {
		OctreeTraversalSettings settings = new OctreeTraversalSettings();
		settings.recordLevelSet.add(0);
		return getIntersections(octree, boundingBox, settings);
	}

	public static <N extends OctreeNode<N>> IntersectionData<N> getClosestIntersection (Octree<N, ?> octree, Ray ray,
			OctreeTraversalSettings settings, OctreeCalc calc) {
		IntersectionRecorder<N> ir = getIntersections(octree, ray, settings, calc);
		if (ir != null && !ir.intersections.isEmpty()) {
			if (ir.intersections.size() == 1) {
				return ir.intersections.get(0);
			}

			IntersectionData<N> nearestEntry = null;
			float distance = 0f;
			float nearestDistance = Float.POSITIVE_INFINITY;
			for (IntersectionData<N> entry : ir.intersections) {
				distance = entry.point.dst2(ray.origin);
				if (distance < nearestDistance) {
					nearestEntry = entry;
					nearestDistance = distance;
				}
			}

			return nearestEntry;
		}
		return null;
	}

	public static <N extends OctreeNode<N>> IntersectionData<N> getIntersectedNormal (Octree<N, ?> octree, Ray ray,
			OctreeTraversalSettings settings, OctreeCalc calc) {
		IntersectionData<N> entry = OctreeTraversal.getClosestIntersection(octree, ray, settings, calc);
		if (entry == null) return null;
		return entry;
	}

}
