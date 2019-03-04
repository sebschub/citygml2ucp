package citygml2ucp.convert;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.vecmath.Point3d;

import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.building.AbstractBoundarySurface;
import org.citygml4j.model.citygml.building.BoundarySurfaceProperty;
import org.citygml4j.model.citygml.building.Building;
import org.citygml4j.model.citygml.building.BuildingPart;
import org.citygml4j.model.citygml.building.BuildingPartProperty;
import org.citygml4j.model.citygml.building.GroundSurface;
import org.citygml4j.model.citygml.building.RoofSurface;
import org.citygml4j.model.citygml.building.WallSurface;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.citygml.core.CityModel;
import org.citygml4j.model.citygml.core.CityObjectMember;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.feature.BoundingShape;
import org.citygml4j.model.gml.geometry.complexes.CompositeSurface;
import org.citygml4j.model.gml.geometry.primitives.Solid;
import org.citygml4j.model.gml.geometry.primitives.SurfaceProperty;
import org.citygml4j.util.bbox.BoundingBoxOptions;
import org.proj4.PJ;
import org.proj4.PJException;

import citygml2ucp.configuration.UrbanCLMConfiguration;
import citygml2ucp.tools.CityGMLTools;
import citygml2ucp.tools.Polygon3d;
import citygml2ucp.tools.Polygon3dVisibility;
import citygml2ucp.tools.Polygon3dWithVisibilities;
import citygml2ucp.tools.SimpleBuilding;
import ucar.unidata.geoloc.ProjectionPoint;

/**
 * Calculation of the main properties of a city element.
 * 
 * This can be used as a single thread. All necessary locking is done in this
 * class, no external locks are required.
 * 
 * @author Sebastian Schubert
 * 
 */
class CityGMLConverterData {

	private final DecimalFormat df;

	/**
	 * Number of urban class when no urban classes are considered
	 */
	private int iuc = 0;

	/**
	 * Urban configuration which includes the global effective urban data
	 */
	public final UrbanCLMConfiguration uclm;

	/**
	 * Object for coordinate transformation
	 */
	public final PJ sourcePJ, targetPJ;

	/**
	 * Additional output information
	 */
	public final CityGMLConverterStats stats;
	/**
	 * Configuration of this run
	 */
	public final CityGMLConverterConf conf;

	final List<SimpleBuilding> buildings;


	/**
	 * Constructor.
	 * 
	 * All necessary CityModel data is saved here, the rest can be freed.
	 * 
	 * @param uclm     Global urban data
	 * @param conf     Configuration of the run
	 * @param proj4    Coordinate converter
	 * @param stats    Additional output
	 * @param base     City data
	 * @param id       ID of the city data
	 * @param filename Name of the file including the city data
	 * @throws PJException
	 */
	public CityGMLConverterData(UrbanCLMConfiguration uclm, CityGMLConverterConf conf, PJ sourcePJ, PJ targetPJ,
			CityGMLConverterStats stats, DecimalFormat df) {
		this.uclm = uclm;
		this.conf = conf;
		this.sourcePJ = sourcePJ;
		this.targetPJ = targetPJ;
		this.stats = stats;

		this.buildings = new ArrayList<SimpleBuilding>();

		this.df = df;
	}

	public void addBuildings(CityModel base) throws PJException {

		for (CityObjectMember cityObjectMember : base.getCityObjectMember()) {

			AbstractCityObject co = cityObjectMember.getCityObject();

			// we have a building
			if (co.getCityGMLClass() == CityGMLClass.BUILDING) {

				Building building = (Building) co;

				String buildingName = "";
				for (Code nameElement : building.getName()) {
					buildingName = buildingName.concat(nameElement.getValue());
				}
				String buildingId = building.getId();
				if (conf.debugOutput) {
					if (buildingName == "") {
						System.out.println("  Found building with ID " + buildingId);
					} else {
						System.out.println("  Found building with ID " + buildingId + " and name " + buildingName);
					}
				}

				// get bounding box to get the centre of building
				BoundingShape boundingShape;
				if (!building.isSetBoundedBy()) {
					boundingShape = building.calcBoundedBy(BoundingBoxOptions.defaults());
				} else {
					boundingShape = building.getBoundedBy();
				}
				List<Double> lc = boundingShape.getEnvelope().getLowerCorner().getValue();
				List<Double> uc = boundingShape.getEnvelope().getUpperCorner().getValue();
				double xpos = 0.5 * (lc.get(0) + uc.get(0));
				double ypos = 0.5 * (lc.get(1) + uc.get(1));

				ProjectionPoint rotatedCoordinates;
				// keep centre and lower position for transformation
				Point3d location = new Point3d(xpos, ypos, lc.get(2));
				rotatedCoordinates = calcLatLonIndices(location);

				// analyse semantic elements of building: get walls, roofs and
				// ground surfaces
				List<Polygon3dWithVisibilities> buildingWalls = new ArrayList<>();
				List<Polygon3dWithVisibilities> buildingRoofs = new ArrayList<>();
				List<Polygon3d> buildingGrounds = new ArrayList<>();
				if (building.isSetBoundedBySurface()) {
					// found boundary surfaces, they should cover building the building just fine,
					// so building parts should not be required. Still, I haven't checked this case,
					// so take a note for this.
					if (building.isSetConsistsOfBuildingPart()) {
						stats.addIgnoredBuildingPart(buildingId);
					}
					for (BoundarySurfaceProperty boundarySurfaceProperty : building.getBoundedBySurface()) {
						AbstractBoundarySurface bs = boundarySurfaceProperty.getObject();
						List<Polygon3dWithVisibilities> polygons = getAllSurfaces(bs, buildingId);
						if (bs instanceof WallSurface) {
							buildingWalls.addAll(polygons);
						} else if (bs instanceof RoofSurface) {
							buildingRoofs.addAll(polygons);
						} else if (bs instanceof GroundSurface) {
							buildingGrounds.addAll(polygons);
						}
					}
				} else if (building.isSetLod1Solid()) {
					// found lod1dsolid, they should cover main part of the building building.
					// Ignore the rest for now but take a note.
					if (building.isSetConsistsOfBuildingPart()) {
						stats.addIgnoredBuildingPart(buildingId);
					}
					
					List<Polygon3dWithVisibilities> horizontalSurfaces = new ArrayList<>();
					
					CompositeSurface surfaceCS = (CompositeSurface)((Solid)building.getLod1Solid().getSolid()).getExterior().getSurface();
					List<SurfaceProperty> surfacesSP = surfaceCS.getSurfaceMember();
					
					int minHeightIndex = -1;
					double minHeight = Double.MAX_VALUE;
					
					for (int surfaceSPIndex = 0; surfaceSPIndex < surfacesSP.size(); surfaceSPIndex++) {
						try {
							Polygon3dWithVisibilities polygon = new Polygon3dWithVisibilities(buildingId, surfacesSP.get(surfaceSPIndex));
							if (polygon.isHorizontal()) {
								horizontalSurfaces.add(polygon);
								if (polygon.getHeight() < minHeight) {
									minHeightIndex = surfaceSPIndex;
								}
							} else {
								buildingWalls.add(polygon);
							}
						} catch (IllegalArgumentException e) {
							stats.addInvalid(buildingId, surfaceCS.getId());
						}
					}
					// assume only lowest horizontal surface is ground
					buildingGrounds.add(horizontalSurfaces.get(minHeightIndex));
					buildingRoofs = horizontalSurfaces;
					buildingRoofs.remove(minHeightIndex);
				} else if (building.isSetConsistsOfBuildingPart()) {
					for (BuildingPartProperty buildingPartProperty : building.getConsistsOfBuildingPart()) {
						BuildingPart bp = buildingPartProperty.getBuildingPart();
						for (BoundarySurfaceProperty boundarySurfaceProperty : bp.getBoundedBySurface()) {
							AbstractBoundarySurface bs = boundarySurfaceProperty.getObject();
							List<Polygon3dWithVisibilities> polygons = getAllSurfaces(bs, buildingId);
							if (bs instanceof WallSurface) {
								buildingWalls.addAll(polygons);
							} else if (bs instanceof RoofSurface) {
								buildingRoofs.addAll(polygons);
							} else if (bs instanceof GroundSurface) {
								buildingGrounds.addAll(polygons);
							}
						}
					}
				} else {
					System.out
							.println("Building " + building.getId() + " has no boundary surfaces nor building parts nor Lod1Solid.");
				}

				double height;
				if (buildingRoofs.size() > 0) {
					// calculate weighted mean of heights of roofs and use it as
					// height information
					double sumRoofArea = 0.;
					height = 0.;

					for (Polygon3d roof : buildingRoofs) {
						double roofArea = roof.getXYProjectedArea();
						sumRoofArea += roofArea;
						// height of roof - ground height
						height += roofArea * (roof.getHeight() - lc.get(2));
					}
					// normalize
					height /= sumRoofArea;
				} else {
					stats.addNoRoof(buildingId);
					// maximum height = height of bounding of bounding box
					height = uc.get(2) - lc.get(2);
				}

				double area = 0.;
				if (buildingGrounds.size() > 0) {
					for (Polygon3d ground : buildingGrounds) {
						area += ground.getXYProjectedArea();
					}
					// area in km2
					area /= 1000000.;
				} else {
					stats.addNoGround(buildingId);
				}

				if (buildingWalls.size() > 0) {
					// check coplanarity
					for (Polygon3dWithVisibilities wall : buildingWalls) {
						if (!wall.checkCoplanarity()) {
							stats.addNonPlanar(buildingId, wall.id);
						}
					}
				} else {
					stats.addNoWall(buildingId);
					// ignore this building for visibility for now
				}

				this.buildings.add(new SimpleBuilding(buildingName, buildingId, location, height, area,
						buildingRoofs, buildingWalls, uclm.getRLatIndex(rotatedCoordinates.getY()),
						uclm.getRLonIndex(rotatedCoordinates.getX())));
				// add some statistics
				stats.addBuildingHeight(height);
				stats.addBuildingGround(area);
			}
		}
	}


	/**
	 * Extract all polygons from surface with potentially sub-polygons.
	 * 
	 * @param              <T> either RoofSurface, WallSurface or GroundSurface
	 * @param surface Surface
	 * @return Array of polygons
	 */
	public <T extends AbstractBoundarySurface> List<Polygon3dWithVisibilities> getAllSurfaces(T surface,
			String buildingId) {

		// new array to include all these surfaces
		List<Polygon3dWithVisibilities> polygons = new ArrayList<Polygon3dWithVisibilities>();

		List<SurfaceProperty> surf = surface.getLod2MultiSurface().getMultiSurface().getSurfaceMember();
		for (int i = 0; i < surf.size(); i++) {
			try {
				polygons.add(new Polygon3dWithVisibilities(surface.getId(), surf.get(i)));
			} catch (IllegalArgumentException e) {
				stats.addInvalid(buildingId, surface.getId());
			}
		}
		return polygons;
	}

	/**
	 * Get the minimum and maximum height of surface
	 * 
	 * @param surfaceProperty Surfaces
	 * @return Array with { minimum, maximum } height
	 */
	public static double[] getMinMaxHeight(SurfaceProperty surfaceProperty) {
		List<Double> coord = CityGMLTools.coordinatesFromSurfaceProperty(surfaceProperty);

		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;

		// height information in every 3rd element
		for (int i = 2; i < coord.size(); i += 3) {
			if (coord.get(i) > max) {
				max = coord.get(i);
			}
			if (coord.get(i) < min) {
				min = coord.get(i);
			}
		}
		return new double[] { min, max };
	}

	private ProjectionPoint calcLatLonIndices(Point3d location) throws PJException {

		// put data to transform in one array with x1, y1, z1, x2, y2, y2, ...
		double[] xyz = new double[3];
		location.get(xyz);

		// transform the coordinates, has to be after visibility determination
		// because old system is used there
		sourcePJ.transform(targetPJ, 3, xyz, 0, 1);

		// apply rotated pole
		return (uclm.rotpol.latLonToProj(xyz[1], xyz[0]));
	}

	/**
	 * Calculate urban parameters after visibility is know.
	 */
	public void calcStreetProperties() {
		System.out.println("Averaging of surface properties to grid cells");
		for (SimpleBuilding building : buildings) {
			if (conf.debugOutput) {
				if (building.name == "") {
					System.out.println(" Building with ID " + building.id);
				} else {
					System.out.println(" Building with ID " + building.id + " and name " + building.name);
				}
			}

			for (Polygon3dWithVisibilities sendingWall : building.walls) {

				if (sendingWall.isHorizontal())
					continue;

				if (sendingWall.visibilities.size() == 0) {
					// add information for later
					stats.addSurfaceWithoutDistance(building.id, sendingWall.id);
					continue;
				}

				if (conf.debugOutput) {
					System.out.println(
							"  Wall with area " + df.format(sendingWall.getArea()) + " and ID " + sendingWall.id);
				}

				List<Polygon3dVisibility> visibilityList = sendingWall.generateVisibilityList(conf.effDist);
				Collections.sort(visibilityList);

				// mean until area of sending surface is reached
				double maxArea = sendingWall.getArea();
				double sumArea = 0;
				double distance = 0;
				int ind = 0;

				while (ind < visibilityList.size() - 1
						&& visibilityList.get(ind).distance < conf.mindist) {
					ind++;
				}

				do {
					if (Double.isNaN(visibilityList.get(ind).distance)) {
						System.out.println("distance is nan");
					}

					if (conf.debugOutput) {
						System.out.println("   Considering target with area "
								+ df.format(visibilityList.get(ind).receiving.getArea()) + ", distance "
								+ df.format(visibilityList.get(ind).distance) + ", and ID "
								+ visibilityList.get(ind).receiving.id);
					}

					double weight = visibilityList.get(ind).receiving.getArea();
					if (conf.effDist) {
						weight *= Math.abs(visibilityList.get(ind).getCosAngle());
					}
					distance += visibilityList.get(ind).distance * weight;
					sumArea += weight;

					ind += 1;
				} while (sumArea < maxArea && ind < visibilityList.size());
				if (sumArea < 1.e-5)
					continue;
				distance /= sumArea;

				int indexAngle = 0;
				indexAngle = uclm.getStreetdirIndex(sendingWall.getAngle());

				// Weight distance with surface size
				uclm.incStreetWidth(iuc, indexAngle, building.irlat, building.irlon, distance * sendingWall.getArea());

				uclm.incStreetSurfaceSum(iuc, indexAngle, building.irlat, building.irlon, sendingWall.getArea());
				
				int indexHeight;
				try {
					indexHeight = uclm.getHeightIndex(building.height);
				} catch (IllegalArgumentException e) {
					// assume that building is too high for specified hhl_uhl so use highest possibility
					indexHeight = uclm.getKe_urban(iuc) - 1;
					uclm.incBuildProbAdjusted(iuc, indexAngle, building.irlat, building.irlon, sendingWall.getArea());
				}
				uclm.incBuildProb(iuc, indexAngle, indexHeight, building.irlat, building.irlon, sendingWall.getArea());

			}
			uclm.incBuildingFrac(iuc, building.irlat, building.irlon, building.area);
		}
	}

}
