package citygml2ucp.tools;

import javax.vecmath.Point3d;

public class SimpleBuilding {

	public final Point3d location;

	public final double height;

	public final double area;

	public final Polygon3d[] roofs;

	public final Polygon3dWithVisibilities[] walls;

	public final int irlat, irlon;

	public SimpleBuilding(Point3d location, double height, double area, Polygon3d[] roofs,
			Polygon3dWithVisibilities[] walls, int irlat, int irlon) {
		this.location = location;
		this.height = height;
		this.area = area;
		this.roofs = roofs;
		this.walls = walls;

		this.irlat = irlat;
		this.irlon = irlon;

	}

}
