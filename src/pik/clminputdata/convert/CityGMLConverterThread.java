package pik.clminputdata.convert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.building.BoundarySurface;
import org.citygml4j.model.citygml.building.BoundarySurfaceProperty;
import org.citygml4j.model.citygml.building.Building;
import org.citygml4j.model.citygml.building.GroundSurface;
import org.citygml4j.model.citygml.building.RoofSurface;
import org.citygml4j.model.citygml.building.WallSurface;
import org.citygml4j.model.citygml.core.CityModel;
import org.citygml4j.model.citygml.core.CityObject;
import org.citygml4j.model.citygml.core.CityObjectMember;
import org.citygml4j.model.gml.LinearRing;
import org.citygml4j.model.gml.Polygon;
import org.citygml4j.model.gml.SurfaceProperty;
import org.proj4.Proj4;
import org.proj4.ProjectionData;

import pik.clminputdata.configuration.UrbanCLMConfiguration;
import pik.clminputdata.tools.Polygon3d;
import pik.clminputdata.tools.Polygon3dDistance;
import pik.clminputdata.tools.SymmetricMatrixBoolean;
import ucar.unidata.geoloc.ProjectionPoint;

import javax.vecmath.Point3d;

import static pik.clminputdata.tools.Polygon2d.xyProjectedPolygon2d;

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
	 * Factor for height of a building: 0 at lower bottom of roof, 1 at top of
	 * roof
	 */
	private final static double roofHeightFactor = 0.5;

	/**
	 * Number of visible polygons that are taken into account for distance
	 * calculation
	 */
	private static int ndistmean = 10000;

	/**
	 * Number of urban class when no urban classes are considered
	 */
	private int iuc = 0;

	/**
	 * ID of the CityGML file
	 */
	public final int id;
	/**
	 * File name of the CityGML file
	 */
	public final String filename;

	/**
	 * Urban configuration which includes the global effective urban data
	 */
	public final UrbanCLMConfiguration uclm;
	/**
	 * Object for coordinate transformation
	 */
	public final Proj4 proj4;

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

	/**
	 * Number of buildings
	 */
	private int bCount;
	/**
	 * Coordinates of the centre of the building's bounding box at the ground in
	 * original coordinates
	 */
	public final Point3d[] bLocation;
	/**
	 * Index of the building's coordinates in rotated system
	 */
	private int[] irlat, irlon;
	/**
	 * Ground area of the buildings
	 */
	private double[] bArea;
	/**
	 * Height of the buildings
	 */
	private double[] bHeight;
	/**
	 * Visibility between wall surfaces
	 */
	private SymmetricMatrixBoolean visible;

	/**
	 * List of all wall surfaces
	 */
	private Polygon3d[][] buildingWalls;
	/**
	 * List of all roof surfaces
	 */
	private Polygon3d[][] buildingRoofs;

	// for output
	/**
	 * Sum of area of buildings in the cell.
	 * 
	 * Arguments: urban class, lat, lon; used for calculation of building
	 * fraction
	 */
	private double[][][] buildingAreaSum;
	/**
	 * Sum of area of walls in the cell.
	 * 
	 * Arguments: urban class, street direction, height of building, lat, lon;
	 * used for calculation of height distribution
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
	 * @param uclm
	 *            Global urban data
	 * @param conf
	 *            Configuration of the run
	 * @param proj4
	 *            Coordinate converter
	 * @param stats
	 *            Additional output
	 * @param base
	 *            City data
	 * @param id
	 *            ID of the city data
	 * @param filename
	 *            Name of the file including the city data
	 */
	public CityGMLConverterThread(UrbanCLMConfiguration uclm,
			CityGMLConverterConf conf, Proj4 proj4,
			CityGMLConverterStats stats, CityModel base, int id, String filename) {
		this.uclm = uclm;
		this.conf = conf;
		this.proj4 = proj4;
		this.id = id;
		this.filename = filename;
		this.stats = stats;

		// following items need to be written to when reading city data
		int size = base.getCityObjectMember().size();
		bLocation = new Point3d[size];
		bHeight = new double[size];
		buildingWalls = new Polygon3d[size][];
		buildingRoofs = new Polygon3d[size][];
		bArea = new double[size];

		// ID of building
		int bID = -1;

		int globalWallSurfaceCounter = 0;

		for (CityObjectMember cityObjectMember : base.getCityObjectMember()) {

			CityObject co = cityObjectMember.getCityObject();

			// we have a building
			if (co.getCityGMLClass() == CityGMLClass.BUILDING) {

				Building building = (Building) co;
				bID++;

				// get bounding box to get the centre of building
				if (!building.isSetBoundedBy()) {
					building.calcBoundedBy();
				}
				List<Double> lc = building.getBoundedBy().getEnvelope()
						.getLowerCorner().getValue();
				List<Double> uc = building.getBoundedBy().getEnvelope()
						.getUpperCorner().getValue();
				double xpos = 0.5 * (lc.get(0) + uc.get(0));
				double ypos = 0.5 * (lc.get(1) + uc.get(1));

				// keep centre and lower position for transformation
				bLocation[bID] = new Point3d(xpos, ypos, lc.get(2));

				// analyse semantic elements of building: get walls, roofs and
				// ground surfaces
				List<WallSurface> walls = new ArrayList<WallSurface>();
				List<RoofSurface> roofs = new ArrayList<RoofSurface>();
				List<GroundSurface> grounds = new ArrayList<GroundSurface>();
				List<BoundarySurfaceProperty> lbbp = building
						.getBoundedBySurfaces();
				for (BoundarySurfaceProperty boundarySurfaceProperty : lbbp) {
					BoundarySurface bs = boundarySurfaceProperty.getObject();
					if (bs instanceof WallSurface) {
						walls.add((WallSurface) bs);
					} else if (bs instanceof RoofSurface) {
						roofs.add((RoofSurface) bs);
					} else if (bs instanceof GroundSurface) {
						grounds.add((GroundSurface) bs);
					}
				}

				if (roofs.size() > 0) {
					// calculate weighted mean of heights of roofs and use it as
					// height information
					buildingRoofs[bID] = getAllSurfaces(roofs);
					bHeight[bID] = calcBuildingHeight(roofs, lc.get(2));
				} else {
					noRoof.add(co.getId());
					// maximum height = height of bounding of bounding box
					bHeight[bID] = uc.get(2) - lc.get(2);
					buildingRoofs[bID] = new Polygon3d[0];
				}

				if (grounds.size() > 0) {
					bArea[bID] = calcGroundSize(grounds);
				} else {
					noGround.add(co.getId());
				}

				if (walls.size() > 0) {
					buildingWalls[bID] = getAllSurfaces(walls);
					checkCoplanarity(buildingWalls[bID], co.getId());
					globalWallSurfaceCounter += buildingWalls[bID].length;
				} else {
					noWall.add(co.getId());
					// ignore this building for visibility for now
					buildingWalls[bID] = new Polygon3d[0];
				}
			}
		}

		visible = new SymmetricMatrixBoolean(globalWallSurfaceCounter, false);
		bCount = bID + 1;

		irlat = new int[bCount];
		irlon = new int[bCount];

		// only one urban class in ke_urban, CHANGE THIS IS IF NECESSARY!
		wallAreaSum = new double[uclm.getNuclasses()][uclm.getNstreedir()][uclm
				.getKe_urban(0)][uclm.getJe_tot()][uclm.getIe_tot()];

		buildingDistanceWeighted = new double[uclm.getNuclasses()][uclm
				.getNstreedir()][uclm.getJe_tot()][uclm.getIe_tot()];

		nStreetSurfaces = new int[uclm.getNuclasses()][uclm.getNstreedir()][uclm
				.getJe_tot()][uclm.getIe_tot()];

		streetSurfaceSum = new double[uclm.getNuclasses()][uclm.getNstreedir()][uclm
				.getJe_tot()][uclm.getIe_tot()];
		buildingAreaSum = new double[uclm.getNuclasses()][uclm.getJe_tot()][uclm
				.getIe_tot()];
	}

	/**
	 * Calculate the height of a building.
	 * 
	 * @param roofs
	 *            Roof surfaces of the building
	 * @param groundHeight
	 *            Ground height of the building (defines the height 0)
	 * @return Height of the building
	 */
	public static double calcBuildingHeight(List<RoofSurface> roofs,
			double groundHeight) {
		double sumRoofArea = 0.;
		double roofHeight = 0.;

		for (RoofSurface roof : roofs) {
			double roofArea;
			List<SurfaceProperty> surface = roof.getLod2MultiSurface()
					.getMultiSurface().getSurfaceMember();
			for (int i = 0; i < surface.size(); i++) {
				SurfaceProperty surfaceProperty = surface.get(i);

				roofArea = xyProjectedPolygon2d(surfaceProperty).getArea();
				sumRoofArea += roofArea;

				// min and max height of surfaceProperty
				double[] minmax = getMinMaxHeight(surfaceProperty);

				// a+f(b-a) with a=minmax[0]-groundHeight,
				// b=minmax[1]-groundHeight (ground height cancels in
				// brakets)
				roofHeight += roofArea
						* (minmax[0] - groundHeight + roofHeightFactor
								* (minmax[1] - minmax[0]));
			}
		}

		// normalize
		roofHeight /= sumRoofArea;

		return roofHeight;
	}

	/**
	 * Calculate the ground size of a building.
	 * 
	 * @param grounds
	 *            Ground surfaces of the building
	 * @return Ground area of the building
	 */
	public static double calcGroundSize(List<GroundSurface> grounds) {
		double area = 0.;
		for (GroundSurface ground : grounds) {
			List<SurfaceProperty> surface = ground.getLod2MultiSurface()
					.getMultiSurface().getSurfaceMember();
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
	 * @param <T>
	 *            either RoofSurface, WallSurface or GroundSurface
	 * @param listSurfaces
	 *            List of surfaces
	 * @return Array of polygons
	 */
	public <T extends BoundarySurface> Polygon3d[] getAllSurfaces(
			List<T> listSurfaces) {

		int counter = 0;

		// just get the number of surfaces
		for (T surface : listSurfaces) {
			List<SurfaceProperty> surf = surface.getLod2MultiSurface()
					.getMultiSurface().getSurfaceMember();
			counter += surf.size();
		}

		// new array to include all these surfaces
		Polygon3d[] surfaces = new Polygon3d[counter];

		// reset counter again
		counter = 0;

		for (T surface : listSurfaces) {
			List<SurfaceProperty> surf = surface.getLod2MultiSurface()
					.getMultiSurface().getSurfaceMember();
			for (int i = 0; i < surf.size(); i++) {
				surfaces[counter++] = new Polygon3d(surf.get(i));
			}
		}
		return surfaces;
	}

	/**
	 * Check polygons independently for planarity.
	 * 
	 * @param surfaces
	 *            Array of Polygons
	 * @param coID
	 *            ID of the building which includes the polygon
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
	 * @param surfaceProperty
	 *            Surfaces
	 * @return Array with { minimum, maximum } height
	 */
	public static double[] getMinMaxHeight(SurfaceProperty surfaceProperty) {
		if (surfaceProperty.getSurface() instanceof Polygon) {
			Polygon polygon = (Polygon) surfaceProperty.getSurface();
			if (polygon.getExterior().getRing() instanceof LinearRing) {
				LinearRing lRing = (LinearRing) polygon.getExterior().getRing();
				if (lRing.isSetPosList()) {
					List<Double> coord = lRing.getPosList().getValue();

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
				throw new IllegalArgumentException(
						"Linear ring is no PosList, handle this case!");
			}
			throw new IllegalArgumentException(
					"Polygon is no linear ring, handle this case!");
		}
		throw new IllegalArgumentException(
				"Surface is no Polygon, handle this case!");
	}

	/**
	 * Calculate the visibility between wall surfaces.
	 * 
	 * Every pair of wall surfaces are checked for visibility. Since this
	 * relationship is symmetric only half of them are analysed. To increase the
	 * calculation speed, only buildings which are less then a defined distance
	 * apart are considered.
	 */
	private void calcVisibility() {

		int wcount = -1;

		// all walls and roofs, one side of visibility
		for (int i = 0; i < bCount; i++) {
			for (int j = 0; j < buildingWalls[i].length; j++) {

				wcount++;

				int wrcount = -1;

				// walls and roofs, other side of visibility
				// the already analysed, visibility is symmetric:
				for (int k = 0; k < i; k++) {
					wrcount += buildingWalls[k].length;
				}

				// the new ones
				for (int k = i; k < bCount; k++) {

					// if buildings are too far away, skip:
					double dist = bLocation[i].distanceSquared(bLocation[k]);
					if (dist > conf.maxbuild_radius_sq) {
						for (int l = 0; l < buildingWalls[k].length; l++) {
							wrcount++;
							visible.set(wcount, wrcount, false);
						}
						continue;
					}

					// distance is ok, so check every other surface
					for (int l = 0; l < buildingWalls[k].length; l++) {

						wrcount++;

						if (i == k && j == l) {
							// already done at initialization of matrix
							// visible.set(wcount, wrcount, false);
							continue;
						}

						boolean vis = true;

						// which to check
						for (int m = 0; m < bCount; m++) {

							// in principle, building to check should be on
							// the
							// connection between the starting and end
							// building,
							// so sum of distances - distance of buildings
							// approx. 0, because of buildings larger radius
							dist = bLocation[m].distance(bLocation[i])
									+ bLocation[m].distance(bLocation[k])
									- bLocation[i].distance(bLocation[k]);
							if (dist > conf.maxcheck_radius) {
								continue;
							}

							// check wall surfaces
							for (int n = 0; n < buildingWalls[m].length; n++) {
								if (i == m && j == n)
									continue;
								if (k == m && l == n)
									continue;

								if (buildingWalls[m][n].isHitBy(
										buildingWalls[i][j].getCentroid(),
										buildingWalls[k][l].getCentroid())) {
									vis = false;
									break;
								}
							}
							if (!vis) {
								break;
							}

							// check roof surfaces
							for (int n = 0; n < buildingRoofs[m].length; n++) {
								if (buildingRoofs[m][n].isHitBy(
										buildingWalls[i][j].getCentroid(),
										buildingWalls[k][l].getCentroid())) {
									vis = false;
									break;
								}
							}
							if (!vis) {
								break;
							}
						}

						visible.set(wcount, wrcount, vis);

					}
				}

			}
		}

	}

	/**
	 * Convert building coordinates to rotated pole and calculate lattice
	 * indices.
	 */
	private void calcLatLonIndices() {

		// create ProjectionData object for transformation
		ProjectionData loc;

		double[][] xy = new double[bCount][2];
		double[] z = new double[bCount];
		for (int i = 0; i < bCount; i++) {
			xy[i][0] = bLocation[i].x;
			xy[i][1] = bLocation[i].y;
			z[i] = bLocation[i].z;
		}
		loc = new ProjectionData(xy, z);

		// transform the coordinates, has to be after visibility determination
		// because old system is used there
		proj4.transform(loc, bCount, 1);
		// now x is lon, y is lat

		for (int i = 0; i < bCount; i++) {
			// apply rotated pole
			ProjectionPoint pp = uclm.rotpol.latLonToProj(loc.y[i], loc.x[i]);
			// and write back to projD
			loc.x[i] = pp.getX();
			loc.y[i] = pp.getY();

			irlat[i] = uclm.getRLatIndex(loc.y[i]);

			irlon[i] = uclm.getRLonIndex(loc.x[i]);

		}
	}

	/**
	 * Calculate urban parameters after visibility is know.
	 */
	private void calcStreetProperties() {
		int wcount = -1;

		for (int i = 0; i < bCount; i++) {
			for (int j = 0; j < buildingWalls[i].length; j++) {
				wcount++;

				if (buildingWalls[i][j].isHorizontal()) {
					continue;
				}

				List<Polygon3dDistance> dist = new LinkedList<Polygon3dDistance>();
				int wrcount = -1;

				for (int k = 0; k < bCount; k++) {
					if (i == k) {
						wrcount += buildingWalls[k].length;
						continue;
					}

					for (int l = 0; l < buildingWalls[k].length; l++) {

						wrcount++;

						if (visible.get(wcount, wrcount)) {

							dist.add(new Polygon3dDistance(buildingWalls[i][j],
									buildingWalls[k][l], conf.effDist));

							if (dist.size() > ndistmean) {
								Collections.sort(dist);
								dist.subList(ndistmean, dist.size()).clear();
							}

						}

					}
				}

				if (dist.size() == 0) {
					// System.out.println("nothing near it");
					continue;
				}

				Collections.sort(dist);

				// mean until area of sending surface is reached
				double maxArea = buildingWalls[i][j].getArea();
				double sumArea = 0;
				double distance = 0;
				int ind = 0;

				while (ind < dist.size() - 1
						&& dist.get(ind).distance < conf.mindist) {
					ind++;
				}

				do {
					if (Double.isNaN(dist.get(ind).distance)) {
						System.out.println("distance is nan");
					}

					double weight = dist.get(ind).receiving.getArea();
					if (conf.effDist) {
						weight *= Math.abs(dist.get(ind).getCosAngle());
					}
					distance += dist.get(ind).distance * weight;
					sumArea += weight;

					ind += 1;
				} while (sumArea < maxArea && ind < dist.size());
				if (sumArea < 1.e-5) continue;
				distance /= sumArea;

				int indexAngle = 0;
				indexAngle = uclm.getStreetdirIndex(buildingWalls[i][j]
						.getAngle());

				// Weight distance with surface size
				buildingDistanceWeighted[iuc][indexAngle][irlat[i]][irlon[i]] += distance
						* buildingWalls[i][j].getArea();

				streetSurfaceSum[iuc][indexAngle][irlat[i]][irlon[i]] += buildingWalls[i][j]
						.getArea();
				wallAreaSum[iuc][indexAngle][uclm.getHeightIndex(bHeight[i])][irlat[i]][irlon[i]] += buildingWalls[i][j]
						.getArea();

				nStreetSurfaces[iuc][indexAngle][irlat[i]][irlon[i]]++;

			}
		}
	}

	/**
	 * Save results to global data.
	 * 
	 * This is the only routine which uses locks for saving the data to the
	 * global classes.
	 */
	private void saveToGlobal() {
		lock.lock();

		// min and max height of complete city
		for (int i = 0; i < bCount; i++) {
			if (bHeight[i] >= uclm.maxHeight) {
				uclm.maxHeight = bHeight[i];
			}
			if (bHeight[i] <= uclm.minHeight) {
				uclm.minHeight = bHeight[i];
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
							uclm.incStreetWidth(c, dir, j, i,
									buildingDistanceWeighted[c][dir][j][i]);
							uclm.incStreetSurfaceSum(c, dir, j, i,
									streetSurfaceSum[c][dir][j][i]);
							for (int height = 0; height < uclm.getKe_urban(c); height++) {
								uclm.incBuildProb(c, dir, height, j, i,
										wallAreaSum[c][dir][height][j][i]);
							}
						}
					}
				}
			}
		}

		// save where not planar
		stats.addNonPlanar(id, NonPlanarList);
		// save when ground surface but no distance taken into account
		stats.addNoSurfButBuildFrac(id, surfWithoutDistances);
		stats.addNoGround(id, noGround);
		stats.addNoRoof(id, noRoof);
		stats.addNoWall(id, noWall);
		for (int i = 0; i < bCount; i++) {
			stats.buildingHeights.add(bHeight[i]);
			stats.buildingGrounds.add(bArea[i]);
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
							surfWithoutDistances.add(Double
									.toString(buildingAreaSum[c][j][i]));
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
		// System.out.println("Finished visibility determination for " +
		// filename);

		// calculate irlat and irlon
		calcLatLonIndices();

		calcStreetProperties();
		// System.out.println("Distance stuff finished for " + filename);

		// sum of areas of buildings
		for (int i = 0; i < bCount; i++) {
			buildingAreaSum[iuc][irlat[i]][irlon[i]] += bArea[i];
		}

		runChecks();
		saveToGlobal();

	}
}
