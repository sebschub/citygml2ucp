package citygml2ucp.tools;

/**
 * An abstract closed surface with calculation of its area and a centroid.
 * 
 * @author Sebastian Schubert
 * 
 */
public abstract class ClosedSurface<T> {

	/**
	 * Centroid already calculated?
	 */
	private boolean isSetCentroid = false;
	/**
	 * Centroid of the polygon
	 */
	private T centroid;

	/**
	 * Area of the polygon
	 */
	private double signedArea;
	/**
	 * Area already calculated?
	 */
	private boolean isSetArea = false;

	public double getArea() {
		if (!this.isSetArea) {
			this.signedArea = calcSignedArea();
			this.isSetArea = true;
		}
		return Math.abs(this.signedArea);
	}

	public double getSignedArea() {
		if (!this.isSetArea) {
			this.signedArea = calcSignedArea();
			this.isSetArea = true;
		}
		return this.signedArea;
	}
	
	protected abstract double calcSignedArea();

	public T getCentroid() {
		if (!this.isSetCentroid) {
			this.centroid = calcCentroid();
			this.isSetCentroid = true;
		}
		return centroid;
	}

	protected abstract T calcCentroid();

	@Override
	public String toString() {
		return getCentroid().toString();
	}
}