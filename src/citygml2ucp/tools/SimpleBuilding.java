package citygml2ucp.tools;

import java.util.List;

import javax.vecmath.Point3d;

public class SimpleBuilding {

	public final String name, id;
	
	public final Point3d location;

	public final double height;

	public final double area;

	public final List<Polygon3dWithVisibilities> roofs;

	public final List<Polygon3dWithVisibilities> walls;

	public final int irlat, irlon;

	public SimpleBuilding(String name, String id, Point3d location, double height, double area, List<Polygon3dWithVisibilities> roofs,
			List<Polygon3dWithVisibilities> walls, int irlat, int irlon) {
		this.name = name;
		this.id = id;
		this.location = location;
		this.height = height;
		this.area = area;
		this.roofs = roofs;
		this.walls = walls;

		this.irlat = irlat;
		this.irlon = irlon;

	}

}
