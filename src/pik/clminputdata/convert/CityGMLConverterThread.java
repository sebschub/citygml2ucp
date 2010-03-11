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
import pik.clminputdata.tools.RecSurface;
import pik.clminputdata.tools.RecSurfaceDistance;
import pik.clminputdata.tools.SymmetricMatrixBoolean;
import ucar.ma2.InvalidRangeException;
import ucar.unidata.geoloc.ProjectionPoint;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

class CityGMLConverterThread extends Thread {

	private final double checkradiussq;
	private final double checkinway;
	private final double minDist;

	// where to put the height of a building: 0 at lower bottom of roof, 1 at
	// top of roof
	private final static double roofHeightFactor = 0.5;

	private static int ndistmean = 10000;

	// the id corresponding to the filename
	public final int id;
	public final String filename;

	public final UrbanCLMConfiguration uclm;
	public final Proj4 proj4;

	public final CityGMLConverterStats stats;
	public final ProjectionData bLocation;

	private int bCount;

	private int[] irlat, irlon;
	private double[] bArea;
	private double[] bHeight;

	// function of urban class, je, ie
	private double[][][] buildingFrac;
	// function of urban class, street direction, height, je, ie
	private double[][][][][] buildingHeight;
	// function of urban class, street direction, je, ie
	private double[][][][] buildingDistance;
	// function of urban class, street direction, je, ie
	private int[][][][] nStreetSurfaces;
	// function of urban class, street direction, je, ie
	private double[][][][] streetSurfaceSum;

	private RecSurface[][] buildingWalls;

	private boolean isInStreetdir[][];

	private ArrayList<String> NonPlanarList = new ArrayList<String>();

	private SymmetricMatrixBoolean visible;

	private Lock lock = new ReentrantLock();

	// no different classes in here (yet):
	private int iuc = 0;

	public CityGMLConverterThread(UrbanCLMConfiguration uclm, CityGMLConverterConf conf, Proj4 proj4,
			CityGMLConverterStats stats, CityModel base, int id, String filename) {
		this.uclm = uclm;
		this.checkinway = conf.maxcheck_radius;
		this.checkradiussq = conf.maxbuild_radius*conf.maxbuild_radius;
		this.minDist = conf.mindist;
		this.proj4 = proj4;
		this.id = id;
		this.filename = filename;
		this.stats = stats;

		// following items need to be written to when reading city data
		int size = base.getCityObjectMember().size();
		bLocation = new ProjectionData(size);
		bHeight = new double[size];
		buildingWalls = new RecSurface[size][];
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
				bLocation.x[bID] = xpos;
				bLocation.y[bID] = ypos;
				bLocation.z[bID] = lc.get(2);

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
					bHeight[bID] = calcBuildingHeight(roofs, lc.get(2));
				} else {
					System.out.println("No Roofs! " + filename);
					// maximum height = height of bounding of bounding box
					bHeight[bID] = uc.get(2) - lc.get(2);
				}

				if (grounds.size() > 0) {
					bArea[bID] = calcGroundSize(grounds);
				} else {
					System.out.println("No Grounds! " + filename);
				}

				if (walls.size() > 0) {
					buildingWalls[bID] = getWallSurfaces(walls, co.getId());
					globalWallSurfaceCounter += buildingWalls[bID].length;
				} else {
					System.out.println("No Walls! " + filename);
				}
			}
		}

		visible = new SymmetricMatrixBoolean(globalWallSurfaceCounter, false);
		bCount = bID + 1;

		irlat = new int[bCount];
		irlon = new int[bCount];

		isInStreetdir = new boolean[bCount][uclm.getNstreedir()];

		// only one urban class in ke_urban, CHANGE THIS IS IF NECESSARY!
		buildingHeight = new double[uclm.getNuclasses()][uclm.getNstreedir()][uclm
				.getKe_urban(0)][uclm.getJe_tot()][uclm.getIe_tot()];

		buildingDistance = new double[uclm.getNuclasses()][uclm.getNstreedir()][uclm
				.getJe_tot()][uclm.getIe_tot()];

		nStreetSurfaces = new int[uclm.getNuclasses()][uclm.getNstreedir()][uclm
				.getJe_tot()][uclm.getIe_tot()];

		streetSurfaceSum = new double[uclm.getNuclasses()][uclm.getNstreedir()][uclm
				.getJe_tot()][uclm.getIe_tot()];
		buildingFrac = new double[uclm.getNuclasses()][uclm.getJe_tot()][uclm
				.getIe_tot()];
	}

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

				roofArea = calcArea(surfaceProperty);
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

	public static double calcGroundSize(List<GroundSurface> grounds) {
		double area = 0.;
		for (GroundSurface ground : grounds) {
			List<SurfaceProperty> surface = ground.getLod2MultiSurface()
					.getMultiSurface().getSurfaceMember();
			for (SurfaceProperty surfaceProperty : surface) {
				area += calcArea(surfaceProperty);
			}
		}
		return area;
	}

	private RecSurface[] getWallSurfaces(List<WallSurface> walls, String coID) {
		int wallCounter = 0;

		// just get the number of wall surfaces
		for (WallSurface wall : walls) {
			List<SurfaceProperty> surface = wall.getLod2MultiSurface()
					.getMultiSurface().getSurfaceMember();
			wallCounter += surface.size();
		}

		// for visibility surfaces take only walls into
		// account: new array to include all these surfaces
		RecSurface[] wallSurfaces = new RecSurface[wallCounter];

		for (WallSurface wall : walls) {
			List<SurfaceProperty> surface = wall.getLod2MultiSurface()
					.getMultiSurface().getSurfaceMember();
			for (int i = 0; i < surface.size(); i++) {
				wallSurfaces[i] = new RecSurface(surface.get(i));
				if (!wallSurfaces[i].checkCoplanarity()) {
					NonPlanarList.add(coID);
				}
			}
		}
		return wallSurfaces;
	}

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
				} else {
					throw new IllegalArgumentException(
							"Linear ring is no PosList, handle this case!");
				}
			} else {
				throw new IllegalArgumentException(
						"Polygon is no linear ring, handle this case!");
			}
		} else {
			throw new IllegalArgumentException(
					"Surface is no Polygon, handle this case!");
		}
	}

	public static double calcArea(SurfaceProperty surfaceProperty) {
		if (surfaceProperty.getSurface() instanceof Polygon) {
			Polygon polygon = (Polygon) surfaceProperty.getSurface();
			if (polygon.getExterior().getRing() instanceof LinearRing) {
				LinearRing lRing = (LinearRing) polygon.getExterior().getRing();
				if (lRing.isSetPosList()) {
					List<Double> coord = lRing.getPosList().getValue();

					double lArea = 0;
					int pos = 0;
					int ppos = 3;

					// http://en.wikipedia.org/wiki/Polygon#Area_and_centroid
					for (int i = 0; i < coord.size() / 3 - 1; i++) {
						lArea += (coord.get(pos) * coord.get(ppos + 1))
								- (coord.get(ppos) * coord.get(pos + 1));
						// System.out.println(lArea);
						pos = ppos;
						// System.out.println(pos);
						ppos += 3;
					}

					// in pos list, last and first element are equal
					// lArea += (coord.get(pos) * coord.get(1))
					// - (coord.get(0) * coord.get(pos + 1));
					// km^2
					return 0.0000005 * Math.abs(lArea);
				} else {
					throw new IllegalArgumentException(
							"Linear ring is no PosList, handle this case!");
				}
			} else {
				throw new IllegalArgumentException(
						"Polygon is no linear ring, handle this case!");
			}
		} else {
			throw new IllegalArgumentException(
					"Surface is no Polygon, handle this case!");
		}
	}

	/**
	 * Calculate the visibility for walls. The bLocation data needs to be still
	 * in metre!
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
					double dist = pow(bLocation.x[i] - bLocation.x[k], 2)
							+ pow(bLocation.y[i] - bLocation.y[k], 2);
					if (dist > checkradiussq) {
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
							visible.set(wcount, wrcount, false);
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
							dist = sqrt(pow(bLocation.x[i] - bLocation.x[m], 2)
									+ pow(bLocation.y[i] - bLocation.y[m], 2))
									+ sqrt(pow(bLocation.x[k] - bLocation.x[m],
											2)
											+ pow(bLocation.y[k]
													- bLocation.y[m], 2))
									- sqrt(pow(bLocation.x[i] - bLocation.x[k],
											2)
											+ pow(bLocation.y[i]
													- bLocation.y[k], 2));
							if (dist > checkinway) {
								continue;
							}

							for (int n = 0; n < buildingWalls[m].length; n++) {
								if (i == m && j == n)
									continue;
								if (k == m && l == n)
									continue;

								if (buildingWalls[m][n].contains(
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
		// transform the coordinates, has to be after visibility determination
		// because old system is used there
		proj4.transform(bLocation, bCount, 1);
		// now x is lon, y is lat

		for (int i = 0; i < bCount; i++) {
			// apply rotated pole
			ProjectionPoint pp = uclm.rotpol.latLonToProj(bLocation.y[i],
					bLocation.x[i]);
			// and write back to projD
			bLocation.x[i] = pp.getX();
			bLocation.y[i] = pp.getY();

			try {
				irlat[i] = uclm.getRLatIndex(bLocation.y[i]);
			} catch (InvalidRangeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				irlon[i] = uclm.getRLonIndex(bLocation.x[i]);
			} catch (InvalidRangeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void calcStreetProperties() {
		int wcount = -1;

		for (int i = 0; i < bCount; i++) {
			for (int j = 0; j < buildingWalls[i].length; j++) {
				wcount++;

				if (buildingWalls[i][j].isHorizontal()) {
					continue;
				}

				List<RecSurfaceDistance> dist = new LinkedList<RecSurfaceDistance>();
				int wrcount = -1;
				
				for (int k = 0; k < bCount; k++) {
					if (i == k) {
						wrcount += buildingWalls[k].length;
						continue;
					}

					for (int l = 0; l < buildingWalls[k].length; l++) {

						wrcount++;

						if (visible.get(wcount, wrcount)) {

							dist.add(new RecSurfaceDistance(
									buildingWalls[i][j], buildingWalls[k][l]));

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
						&& dist.get(ind).distance < minDist) {
					ind++;
				}

				do {
					if (Double.isNaN(dist.get(ind).distance)) {
						System.out.println("distance is nan");
					}
					distance += dist.get(ind).distance
							* dist.get(ind).receiving.getArea();
					sumArea += dist.get(ind).receiving.getArea();
					ind += 1;
				} while (sumArea < maxArea && ind < dist.size());
				distance /= sumArea;

				int indexAngle = 0;
				try {
					indexAngle = uclm.getStreetdirIndex(buildingWalls[i][j]
							.getAngle());
				} catch (InvalidRangeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// Weight distance with surface size
				buildingDistance[iuc][indexAngle][irlat[i]][irlon[i]] += distance
						* buildingWalls[i][j].getArea();

				streetSurfaceSum[iuc][indexAngle][irlat[i]][irlon[i]] += buildingWalls[i][j]
						.getArea();
				try {
					buildingHeight[iuc][indexAngle][uclm
							.getHeightIndex(bHeight[i])][irlat[i]][irlon[i]] += buildingWalls[i][j]
							.getArea();
				} catch (InvalidRangeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				nStreetSurfaces[iuc][indexAngle][irlat[i]][irlon[i]]++;

				isInStreetdir[i][indexAngle] = true;
			}
		}
	}

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
					if (buildingFrac[c][j][i] > 1.e-13) {
						try {
							// add urban fraction
							uclm
									.incBuildingFrac(c, j, i,
											buildingFrac[c][j][i]);
						} catch (InvalidRangeException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						for (int dir = 0; dir < uclm.getNstreedir(); dir++) {
							// inc street width and its counter to norm later
							uclm.incStreetWidth(c, dir, j, i,
									buildingDistance[c][dir][j][i]);
							uclm.incStreetSurfaceSum(c, dir, j, i,
									streetSurfaceSum[c][dir][j][i]);
							for (int height = 0; height < uclm.getKe_urban(c); height++) {
								try {
									uclm
											.incBuildProb(
													c,
													dir,
													height,
													j,
													i,
													buildingHeight[c][dir][height][j][i]);
								} catch (InvalidRangeException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}
				}
			}
		}

		// save where not planar
		stats.addNonPlanar(id, NonPlanarList);
		for (int i = 0; i < bCount; i++) {
			stats.buildingHeights.add(bHeight[i]);
			stats.buildingGrounds.add(bArea[i]);
		}
		lock.unlock();
	}

	private void runChecks() {
		for (int c = 0; c < uclm.getNuclasses(); c++) {
			for (int j = 0; j < uclm.getJe_tot(); j++) {
				for (int i = 0; i < uclm.getIe_tot(); i++) {
					// if urban there
					if (buildingFrac[c][j][i] > 1.e-13) {
						int sumNStreetSurfaces = 0;
						for (int dir = 0; dir < uclm.getNstreedir(); dir++) {
							sumNStreetSurfaces += nStreetSurfaces[c][dir][j][i];
						}
						if (sumNStreetSurfaces == 0) {
							System.out.println(buildingFrac[c][j][i]);
							System.out
									.println("no surface but urban fraction>0 "
											+ filename);
							System.out.println(j);
							System.out.println(i);
						}
					}
				}
			}
		}
	}
	
	@Override
	public void run() {

		calcVisibility();
		System.out.println("Finished visibility determination for " + filename);

		// calculate irlat and irlon
		calcLatLonIndices();

		calcStreetProperties();
		System.out.println("Distance stuff finished for " + filename);

		// sum of areas of buildings
		for (int i = 0; i < bCount; i++) {
			buildingFrac[iuc][irlat[i]][irlon[i]] += bArea[i];
		}
		
		runChecks();
		saveToGlobal();

	}
}
