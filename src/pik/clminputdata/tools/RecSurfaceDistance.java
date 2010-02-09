package pik.clminputdata.tools;

import javax.vecmath.Point3d;


public class RecSurfaceDistance implements Comparable<RecSurfaceDistance> {

	public final RecSurface sending, receiving;

	public final double distance;

	public RecSurfaceDistance(RecSurface sending, RecSurface receiving) {
		this.sending = sending;
		this.receiving = receiving;

		Point3d p1 = sending.getCentroid();
		Point3d p2 = receiving.getCentroid();
				
		distance = p1.distance(p2);
//		if (Double.isNaN(distance)) {
//			System.out.println(p1);
//			System.out.println(p2);
//			System.out.println("++++++++++++++++++++++");
//		}
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
