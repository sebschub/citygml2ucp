package citygml2ucp.convert;

import static citygml2ucp.tools.Polygon2d.xyProjectedPolygon2d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
import org.citygml4j.model.gml.feature.BoundingShape;
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
class CityGMLConverterThread extends Thread {

	/**
	 * Factor for height of a building: 0 at lower bottom of roof, 1 at top of roof
	 */
	private final static double roofHeightFactor = 0.5;

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
	/**
	 * Lock for saving to global data
	 */
	private Lock lock = new ReentrantLock();

	private final List<SimpleBuilding> buildings;

	// for output
	/**
	 * Sum of area of buildings in the cell.
	 * 
	 * Arguments: urban class, lat, lon; used for calculation of building fraction
	 */
	private double[][][] buildingAreaSum;
	/**
	 * Sum of area of walls in the cell.
	 * 
	 * Arguments: urban class, street direction, height of building, lat, lon; used
	 * for calculation of height distribution
	 */
	private double[][][][][] wallAreaSum;
	/**
	 * Building distance weighted with the respective wall area.
	 * 
	 * Arguments: urban class, street direction, lat, lon; used for distance
	 * calculation (divide by {@code streetSurfaceSum})
	 */
	private double[][][][] buildingDistanceWeighted;
	/**
	 * Number of wall polygons.
	 * 
	 * Arguments: urban class, street direction, lat, lon
	 */
	private int[][][][] nStreetSurfaces;
	/**
	 * Sum of wall areas.
	 * 
	 * Arguments: urban class, street direction, lat, lon; used for distance
	 * calculation
	 */
	private double[][][][] streetSurfaceSum;

	/**
	 * List of non-planar surfaces
	 */
	private LinkedList<String> NonPlanarList = new LinkedList<String>();
	private LinkedList<String> surfWithoutDistances = new LinkedList<String>();
	private LinkedList<String> noRoof = new LinkedList<String>();
	private LinkedList<String> noWall = new LinkedList<String>();
	private LinkedList<String> noGround = new LinkedList<String>();

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
	public CityGMLConverterThread(UrbanCLMConfiguration uclm, CityGMLConverterConf conf, PJ sourcePJ, PJ targetPJ,
			CityGMLConverterStats stats) {
		this.uclm = uclm;
		this.conf = conf;
		this.sourcePJ = sourcePJ;
		this.targetPJ = targetPJ;
		this.stats = stats;

		this.buildings = new ArrayList<SimpleBuilding>();
	}

	public void addBuildings(CityModel base) throws PJException {
		// following items need to be written to when reading city data
		int numberBuildings = 0;
		for (CityObjectMember cityObjectMember : base.getCityObjectMember()) {
			if (cityObjectMember.getCityObject().getCityGMLClass() == CityGMLClass.BUILDING)
				numberBuildings++;
		}
		SimpleBuilding[] localBuildings = new SimpleBuilding[numberBuildings];
		int bID = -1;

		for (CityObjectMember cityObjectMember : base.getCityObjectMember()) {

			AbstractCityObject co = cityObjectMember.getCityObject();

			// we have a building
			if (co.getCityGMLClass() == CityGMLClass.BUILDING) {

				bID++;

				Building building = (Building) co;

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
				List<WallSurface> walls = new ArrayList<WallSurface>();
				List<RoofSurface> roofs = new ArrayList<RoofSurface>();
				List<GroundSurface> grounds = new ArrayList<GroundSurface>();

				if (building.getBoundedBySurface().size() > 0) {
					// found boundary surfaces, they should cover building the buidling just fine,
					// so building parts should not be required. Still, I haven't checked this case,
					// so throw error here if there are building part.
					if (building.getConsistsOfBuildingPart().size() > 0) {
						throw new IllegalArgumentException(
								"Building has boundary surfaces but also building parts, check building manually and modify code!");
					}
					for (BoundarySurfaceProperty boundarySurfaceProperty : building.getBoundedBySurface()) {
						AbstractBoundarySurface bs = boundarySurfaceProperty.getObject();
						if (bs instanceof WallSurface) {
							walls.add((WallSurface) bs);
						} else if (bs instanceof RoofSurface) {
							roofs.add((RoofSurface) bs);
						} else if (bs instanceof GroundSurface) {
							grounds.add((GroundSurface) bs);
						}
					}
				} else if (building.getConsistsOfBuildingPart().size() > 0) {
					for (BuildingPartProperty buildingPartProperty : building.getConsistsOfBuildingPart()) {
						BuildingPart bp = buildingPartProperty.getBuildingPart();
						for (BoundarySurfaceProperty boundarySurfaceProperty : bp.getBoundedBySurface()) {
							AbstractBoundarySurface bs = boundarySurfaceProperty.getObject();
							if (bs instanceof WallSurface) {
								walls.add((WallSurface) bs);
							} else if (bs instanceof RoofSurface) {
								roofs.add((RoofSurface) bs);
							} else if (bs instanceof GroundSurface) {
								grounds.add((GroundSurface) bs);
							}
						}
					}
				} else {
					System.out
							.println("Building " + building.getId() + " has no boundary surfaces nor building parts.");
				}

				Polygon3d[] buildingRoofs;
				double height;
				if (roofs.size() > 0) {
					// calculate weighted mean of heights of roofs and use it as
					// height information
					buildingRoofs = getAllSurfaces(roofs);
					height = calcBuildingHeight(roofs, lc.get(2));
				} else {
					noRoof.add(co.getId());
					// maximum height = height of bounding of bounding box
					height = uc.get(2) - lc.get(2);
					buildingRoofs = new Polygon3d[0];
				}

				double area = 0.;
				if (grounds.size() > 0) {
					area = calcGroundSize(grounds);
				} else {
					noGround.add(co.getId());
				}

				Polygon3dWithVisibilities[] buildingWalls;
				if (walls.size() > 0) {
					buildingWalls = getAllSurfaces(walls);
					checkCoplanarity(buildingWalls, co.getId());
				} else {
					noWall.add(co.getId());
					// ignore this building for visibility for now
					buildingWalls = new Polygon3dWithVisibilities[0];
				}

				localBuildings[bID] = new SimpleBuilding(location, height, area, buildingRoofs, buildingWalls,
						uclm.getRLatIndex(rotatedCoordinates.getY()), uclm.getRLonIndex(rotatedCoordinates.getX()));

			}

		}

		this.buildings.addAll(Arrays.asList(localBuildings));

		// only one urban class in ke_uhl, CHANGE THIS IS IF NECESSARY!
		wallAreaSum = new double[uclm.getNuclasses()][uclm.getNstreedir()][uclm.getKe_urban(0)][uclm.getJe_tot()][uclm
				.getIe_tot()];

		buildingDistanceWeighted = new double[uclm.getNuclasses()][uclm.getNstreedir()][uclm.getJe_tot()][uclm
				.getIe_tot()];

		nStreetSurfaces = new int[uclm.getNuclasses()][uclm.getNstreedir()][uclm.getJe_tot()][uclm.getIe_tot()];

		streetSurfaceSum = new double[uclm.getNuclasses()][uclm.getNstreedir()][uclm.getJe_tot()][uclm.getIe_tot()];
		buildingAreaSum = new double[uclm.getNuclasses()][uclm.getJe_tot()][uclm.getIe_tot()];
	}

	/**
	 * Calculate the height of a building.
	 * 
	 * @param roofs        Roof surfaces of the building
	 * @param groundHeight Ground height of the building (defines the height 0)
	 * @return Height of the building
	 */
	public static double calcBuildingHeight(List<RoofSurface> roofs, double groundHeight) {
		double sumRoofArea = 0.;
		double roofHeight = 0.;

		for (RoofSurface roof : roofs) {
			double roofArea;
			List<SurfaceProperty> surface = roof.getLod2MultiSurface().getMultiSurface().getSurfaceMember();
			for (int i = 0; i < surface.size(); i++) {
				SurfaceProperty surfaceProperty = surface.get(i);

				roofArea = xyProjectedPolygon2d(surfaceProperty).getArea();
				sumRoofArea += roofArea;

				// min and max height of surfaceProperty
				double[] minmax = getMinMaxHeight(surfaceProperty);

				// a+f(b-a) with a=minmax[0]-groundHeight,
				// b=minmax[1]-groundHeight (ground height cancels in
				// brakets)
				roofHeight += roofArea * (minmax[0] - groundHeight + roofHeightFactor * (minmax[1] - minmax[0]));
			}
		}

		// normalize
		roofHeight /= sumRoofArea;

		return roofHeight;
	}

	/**
	 * Calculate the ground size of a building.
	 * 
	 * @param grounds Ground surfaces of the building
	 * @return Ground area of the building
	 */
	public static double calcGroundSize(List<GroundSurface> grounds) {
		double area = 0.;
		for (GroundSurface ground : grounds) {
			List<SurfaceProperty> surface = ground.getLod2MultiSurface().getMultiSurface().getSurfaceMember();
			for (SurfaceProperty surfaceProperty : surface) {
				// area in km2
				area += xyProjectedPolygon2d(surfaceProperty).getArea() / 1000000.;
			}
		}
		return area;
	}

	/**
	 * Extract all polygons from a list of surfaces.
	 * 
	 * @param              <T> either RoofSurface, WallSurface or GroundSurface
	 * @param listSurfaces List of surfaces
	 * @return Array of polygons
	 */
	public <T extends AbstractBoundarySurface> Polygon3dWithVisibilities[] getAllSurfaces(List<T> listSurfaces) {

		int counter = 0;

		// just get the number of surfaces
		for (T surface : listSurfaces) {
			List<SurfaceProperty> surf = surface.getLod2MultiSurface().getMultiSurface().getSurfaceMember();
			counter += surf.size();
		}

		// new array to include all these surfaces
		Polygon3dWithVisibilities[] surfaces = new Polygon3dWithVisibilities[counter];

		// reset counter again
		counter = 0;

		for (T surface : listSurfaces) {
			List<SurfaceProperty> surf = surface.getLod2MultiSurface().getMultiSurface().getSurfaceMember();
			for (int i = 0; i < surf.size(); i++) {
				surfaces[counter++] = new Polygon3dWithVisibilities(surf.get(i));
			}
		}
		return surfaces;
	}

	/**
	 * Check polygons independently for planarity.
	 * 
	 * @param surfaces Array of Polygons
	 * @param coID     ID of the building which includes the polygon
	 */
	private void checkCoplanarity(Polygon3d[] surfaces, String coID) {
		for (int i = 0; i < surfaces.length; i++) {
			if (!surfaces[i].checkCoplanarity()) {
				NonPlanarList.add(coID);
			}
		}
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

	private void calcVisibility() {
		if (!conf.separateFiles)
			System.out.println("Visibility calculation");
		int buildingSizeLength = (int) (Math.log10(buildings.size()) + 1);
		for (int iBuildingSending = 0; iBuildingSending < buildings.size(); iBuildingSending++) {
			if (!conf.separateFiles)
				System.out.println(" Building " + String.format("%" + buildingSizeLength + "d", iBuildingSending + 1)
						+ "/" + buildings.size());
			SimpleBuilding buildingSending = buildings.get(iBuildingSending);

			for (int iWallSending = 0; iWallSending < buildingSending.walls.length; iWallSending++) {
				Polygon3dWithVisibilities wallSending = buildingSending.walls[iWallSending];

				for (int iBuildingReceiving = iBuildingSending; iBuildingReceiving < buildings
						.size(); iBuildingReceiving++) {
					SimpleBuilding buildingReceiving = buildings.get(iBuildingReceiving);

					// if buildings are too far away, skip:
					double distanceSendiungReceiving = buildingSending.location.distance(buildingReceiving.location);
					if (distanceSendiungReceiving > conf.maxbuild_radius) {
						continue;
					}

					// distance is ok, so check every other surface
					for (int iWallReceiving = 0; iWallReceiving < buildingReceiving.walls.length; iWallReceiving++) {
						Polygon3dWithVisibilities wallReceiving = buildingReceiving.walls[iWallReceiving];

						if (iBuildingSending == iBuildingReceiving && iWallSending >= iWallReceiving)
							continue;

						boolean vis = true;

						// which to check
						for (int iBuildingChecking = 0; iBuildingChecking < buildings.size(); iBuildingChecking++) {
							SimpleBuilding buildingChecking = buildings.get(iBuildingChecking);

							// in principle, building to check should be on
							// the
							// connection between the starting and end
							// building,
							// so sum of distances - distance of buildings
							// approx. 0, because of buildings larger radius
							double distenceDifference = buildingChecking.location.distance(buildingSending.location)
									+ buildingChecking.location.distance(buildingReceiving.location)
									- distanceSendiungReceiving;
							if (distenceDifference > conf.maxcheck_radius) {
								continue;
							}

							// check wall surfaces
							for (Polygon3dWithVisibilities wallChecking : buildingChecking.walls) {
								if (wallChecking.isHitBy(wallSending.getCentroid(), wallReceiving.getCentroid())) {
									vis = false;
									break;
								}
							}
							if (!vis) {
								break;
							}

							// check roof surfaces
							for (Polygon3d roofChecking : buildingChecking.roofs) {
								if (roofChecking.isHitBy(wallSending.getCentroid(), wallReceiving.getCentroid())) {
									vis = false;
									break;
								}
							}
							if (!vis) {
								break;
							}
						}
						
						wallSending.visibilities.add(  new Polygon3dVisibility(wallSending, wallReceiving, conf.effDist));
						wallReceiving.visibilities.add(new Polygon3dVisibility(wallReceiving, wallSending, conf.effDist));

					}
				}
			}
		}

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
	private void calcStreetProperties() {
		if (!conf.separateFiles)
			System.out.println("Averaging of surface properties to grid cells");
		for (SimpleBuilding building : buildings) {
			for (Polygon3dWithVisibilities sendingWall : building.walls) {

				if (sendingWall.visibilities.size() == 0 || sendingWall.isHorizontal()) {
					continue;
				}

				Collections.sort(sendingWall.visibilities);

				// mean until area of sending surface is reached
				double maxArea = sendingWall.getArea();
				double sumArea = 0;
				double distance = 0;
				int ind = 0;

				while (ind < sendingWall.visibilities.size() - 1 && sendingWall.visibilities.get(ind).distance < conf.mindist) {
					ind++;
				}

				do {
					if (Double.isNaN(sendingWall.visibilities.get(ind).distance)) {
						System.out.println("distance is nan");
					}

					double weight = sendingWall.visibilities.get(ind).receiving.getArea();
					if (conf.effDist) {
						weight *= Math.abs(sendingWall.visibilities.get(ind).getCosAngle());
					}
					distance += sendingWall.visibilities.get(ind).distance * weight;
					sumArea += weight;

					ind += 1;
				} while (sumArea < maxArea && ind < sendingWall.visibilities.size());
				if (sumArea < 1.e-5)
					continue;
				distance /= sumArea;

				int indexAngle = 0;
				indexAngle = uclm.getStreetdirIndex(sendingWall.getAngle());

				// Weight distance with surface size
				buildingDistanceWeighted[iuc][indexAngle][building.irlat][building.irlon] += distance
						* sendingWall.getArea();

				streetSurfaceSum[iuc][indexAngle][building.irlat][building.irlon] += sendingWall.getArea();
				wallAreaSum[iuc][indexAngle][uclm
						.getHeightIndex(building.height)][building.irlat][building.irlon] += sendingWall.getArea();

				nStreetSurfaces[iuc][indexAngle][building.irlat][building.irlon]++;

			}
		}
	}

	/**
	 * Save results to global data.
	 * 
	 * This is the only routine which uses locks for saving the data to the global
	 * classes.
	 */
	private void saveToGlobal() {
		lock.lock();

		// min and max height of complete city
		for (SimpleBuilding building : buildings) {
			if (building.height >= uclm.maxHeight) {
				uclm.maxHeight = building.height;
			}
			if (building.height <= uclm.minHeight) {
				uclm.minHeight = building.height;
			}
		}

		for (int c = 0; c < uclm.getNuclasses(); c++) {
			for (int j = 0; j < uclm.getJe_tot(); j++) {
				for (int i = 0; i < uclm.getIe_tot(); i++) {
					// if urban there
					if (buildingAreaSum[c][j][i] > 1.e-13) {
						// add urban fraction
						uclm.incBuildingFrac(c, j, i, buildingAreaSum[c][j][i]);
						for (int dir = 0; dir < uclm.getNstreedir(); dir++) {
							// increase street width and its counter to norm
							// later
							uclm.incStreetWidth(c, dir, j, i, buildingDistanceWeighted[c][dir][j][i]);
							uclm.incStreetSurfaceSum(c, dir, j, i, streetSurfaceSum[c][dir][j][i]);
							for (int height = 0; height < uclm.getKe_urban(c); height++) {
								uclm.incBuildProb(c, dir, height, j, i, wallAreaSum[c][dir][height][j][i]);
							}
						}
					}
				}
			}
		}

//		// save where not planar
//		stats.addNonPlanar(id, NonPlanarList);
//		// save when ground surface but no distance taken into account
//		stats.addNoSurfButBuildFrac(id, surfWithoutDistances);
//		stats.addNoGround(id, noGround);
//		stats.addNoRoof(id, noRoof);
//		stats.addNoWall(id, noWall);
		for (SimpleBuilding building : buildings) {
			stats.buildingHeights.add(building.height);
			stats.buildingGrounds.add(building.area);
		}
		lock.unlock();
	}

	/**
	 * Check for sane results.
	 */
	private void runChecks() {
		for (int c = 0; c < uclm.getNuclasses(); c++) {
			for (int j = 0; j < uclm.getJe_tot(); j++) {
				for (int i = 0; i < uclm.getIe_tot(); i++) {
					// if urban there
					if (buildingAreaSum[c][j][i] > 1.e-13) {
						int sumNStreetSurfaces = 0;
						for (int dir = 0; dir < uclm.getNstreedir(); dir++) {
							sumNStreetSurfaces += nStreetSurfaces[c][dir][j][i];
						}
						if (sumNStreetSurfaces == 0) {
							surfWithoutDistances.add(Double.toString(buildingAreaSum[c][j][i]));
						}
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc) Start the analysis.
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {

		calcVisibility();

		calcStreetProperties();
		// System.out.println("Distance stuff finished for " + filename);

		for (SimpleBuilding building : buildings) {
			buildingAreaSum[iuc][building.irlat][building.irlon] += building.area;
		}

		runChecks();
		saveToGlobal();

	}
}
