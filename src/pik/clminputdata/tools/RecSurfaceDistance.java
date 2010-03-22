package pik.clminputdata.tools;

import javax.vecmath.Point3d;

/**
 * Class for the distance between two polygons, which can be compared and,
 * therefore, sorted
 * 
 * @author Sebastian Schubert
 * 
 */
public class RecSurfaceDistance implements Comparable<RecSurfaceDistance> {

	/**
	 * The sending surface
	 */
	public final RecSurface sending;
	/**
	 * The receiving surface
	 */
	public final RecSurface receiving;

	/**
	 * The distance between sending and receiving surface defined by the
	 * distance between their centroids.
	 */
	public final double distance;

	public RecSurfaceDistance(RecSurface sending, RecSurface receiving) {
		this.sending = sending;
		this.receiving = receiving;

		Point3d p1 = sending.getCentroid();
		Point3d p2 = receiving.getCentroid();

		distance = p1.distance(p2);
	}

	@Override
	public int compareTo(RecSurfaceDistance arg0) {
		if (arg0 == null) {
			throw new NullPointerException();
		}

		if (arg0.distance > this.distance)
			return -1;
		if (arg0.distance < this.distance)
			return 1;

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof RecSurfaceDistance)) {
			return false;
		}

		if (((RecSurfaceDistance) obj).distance == this.distance) {
			return true;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return (int) distance;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return sending.toString() + "  " + receiving.toString() + "  "
				+ Double.toString(distance);
	}
}
