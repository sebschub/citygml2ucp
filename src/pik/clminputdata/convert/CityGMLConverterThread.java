package pik.clminputdata.convert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.DataFormatException;

import javax.vecmath.Point3d;

import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.building.BoundarySurface;
import org.citygml4j.model.citygml.building.BoundarySurfaceProperty;
import org.citygml4j.model.citygml.building.Building;
import org.citygml4j.model.citygml.building.GroundSurface;
import org.citygml4j.model.citygml.building.RoofSurface;
import org.citygml4j.model.citygml.building.WallSurface;
import org.citygml4j.model.citygml.core.CityGMLBase;
import org.citygml4j.model.citygml.core.CityModel;
import org.citygml4j.model.citygml.core.CityObject;
import org.citygml4j.model.citygml.core.CityObjectMember;
import org.citygml4j.model.gml.Coord;
import org.citygml4j.model.gml.Coordinates;
import org.citygml4j.model.gml.Length;
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
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionPointImpl;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

class CityGMLConverterThread extends Thread {

	private static double checkradiussq = 100 * 100;
	private static double checkinway = 100;
	private static double minDist = 2;

	// where to put the height of a building: 0 at lower bottom of roof, 1 at
	// top of roof
	private final static double roofHeightFactor = 0.5;

	private static int ndistmean = 10000;

	// the id corresponding to the filename
	public final int id;
	public final String filename;

	public final UrbanCLMConfiguration uclm;
	public final Proj4 proj4;
	public final CityModel base;
	public final CityGMLConverterStats stats;
	public final ProjectionData projD;

	private int[] irlat, irlon;
	private double[] area;
	private double[] bheight;

	// number of special surfaces per building
	private int[] roofSurfaceCounter, wallSurfaceCounter;

	// function of urban class, je, ie
	private double[][][] buildingFrac;

	// function of urban class, street direction, height, je, ie
	private double[][][][][] building_height;

	// function of urban class, street direction, je, ie
	private double[][][][] buildingDistance;

	// function of urban class, street direction, je, ie
	private double[][][][] building_width;

	// function of urban class, street direction, je, ie
	private int[][][][] nStreetSurfaces;

	// function of urban class, street direction, je, ie
	private double[][][][] streetSurfaceSum;

	private RecSurface[][] buildingWalls;

	private boolean isInStreetdir[][];

	// arguments: building1, surface1, building2, surface2
	private SymmetricMatrixBoolean visible;

	private ArrayList<String> NonPlanarList = new ArrayList<String>();

	public CityGMLConverterThread(UrbanCLMConfiguration uclm, Proj4 proj4,
			CityGMLConverterStats stats, CityModel base, int id, String filename) {
		this.uclm = uclm;
		this.proj4 = proj4;
		this.base = base;
		this.id = id;
		this.filename = filename;
		this.stats = stats;

		int size = base.getCityObjectMember().size();
		projD = new ProjectionData(size);
		irlat = new int[size];
		irlon = new int[size];
		area = new double[size];
		bheight = new double[size];

		isInStreetdir = new boolean[size][uclm.getNstreedir()];

		roofSurfaceCounter = new int[size];
		wallSurfaceCounter = new int[size];

		buildingFrac = new double[uclm.getNuclasses()][uclm.getJe_tot()][uclm
				.getIe_tot()];

		buildingWalls = new RecSurface[size][];

		// only one urban class in ke_urban, CHANGE THIS IS IF NECESSARY!
		building_height = new double[uclm.getNuclasses()][uclm.getNstreedir()][uclm
				.getKe_urban(0)][uclm.getJe_tot()][uclm.getIe_tot()];

		buildingDistance = new double[uclm.getNuclasses()][uclm.getNstreedir()][uclm
				.getJe_tot()][uclm.getIe_tot()];

		building_width = new double[uclm.getNuclasses()][uclm.getNstreedir()][uclm
				.getJe_tot()][uclm.getIe_tot()];

		nStreetSurfaces = new int[uclm.getNuclasses()][uclm.getNstreedir()][uclm
				.getJe_tot()][uclm.getIe_tot()];

		streetSurfaceSum = new double[uclm.getNuclasses()][uclm.getNstreedir()][uclm
				.getJe_tot()][uclm.getIe_tot()];
	}

	public double[] getMinMaxHeight(SurfaceProperty surfaceProperty) {
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

	public double calcArea(SurfaceProperty surfaceProperty) {
		if (surfaceProperty.getSurface() instanceof Polygon) {
			Polygon polygon = (Polygon) surfaceProperty.getSurface();
			if (polygon.getExterior().getRing() instanceof LinearRing) {
				LinearRing lRing = (LinearRing) polygon.getExterior().getRing();
				if (lRing.isSetPosList()) {
					List<Double> coord = lRing.getPosList().getValue();

					// end and beginning points are the same (this is the test:)
					// System.out.println((coord.get(0) - coord
					// .get(coord.size() - 3))
					// + "   "
					// + (coord.get(1) - coord.get(coord.size() - 2)
					// + "   " + (coord.get(2) - coord.get(coord
					// .size() - 1))));

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

	@Override
	public void run() {

		Lock lock = new ReentrantLock();

		// no different classes in here (yet):
		int iuc = 0;

		// number of buildings - 1, so usable with array index
		int bCount = -1;

		int globalWallSurfaceCounter = 0;
		int globalRoofSurfaceCounter = 0;

		for (CityObjectMember cityObjectMember : base.getCityObjectMember()) {

			CityObject co = cityObjectMember.getCityObject();

			// System.out
			// .println("Found a " + co.getCityGMLClass() + " instance.");
			// System.out.println(co.getId());
			// double maxHeight =-Double.MAX_VALUE;
			// double minHeight = Double.MAX_VALUE;

			// we have a building
			if (co.getCityGMLClass() == CityGMLClass.BUILDING) {

				Building building = (Building) co;
				bCount++;

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

				// System.out.println("max. surface: " + (uc.get(0) - lc.get(0))
				// * (uc.get(1) - lc.get(1)) * 0.000001);

				// keep centre and lower position for transformation
				projD.x[bCount] = xpos;
				projD.y[bCount] = ypos;
				projD.z[bCount] = lc.get(2);

				// if (building.isSetMeasuredHeight()) {
				// // retrieve the measured height of the building
				// height = building.getMeasuredHeight().getValue();
				// System.out.println((uc.get(2) - lc.get(2))/height);
				//					
				// } else {

				// maximum height = height of bounding of bounding box
				bheight[bCount] = uc.get(2) - lc.get(2);

				// }

				// // uclm.incBuildProb(0,height, bsold.projToLatLon(xpos,
				// ypos));

				// System.out.println(building.getLocation());
				// System.out.println(building.getFunction());
				// System.out.println(building.getRoofType());
				// System.out.println(building.isSetRoofType());

				// System.out.println(building.getBoundedBySurfaces());

				// analyze semantic elements of building: get walls, roofs and
				// ground surfaces
				List<WallSurface> walls = new ArrayList<WallSurface>();
				List<RoofSurface> roofs = new ArrayList<RoofSurface>();
				List<GroundSurface> grounds = new ArrayList<GroundSurface>();
				List<BoundarySurfaceProperty> lbbp = building
						.getBoundedBySurfaces();
				for (BoundarySurfaceProperty boundarySurfaceProperty : lbbp) {
					BoundarySurface bs = boundarySurfaceProperty.getObject();
					// System.out.println(bbp.getObject());
					if (bs instanceof WallSurface) {
						walls.add((WallSurface) bs);
						// System.out.println(" * Wall element found");
					} else if (bs instanceof RoofSurface) {
						roofs.add((RoofSurface) bs);
						// System.out.println(" * Roof element found");
					} else if (bs instanceof GroundSurface) {
						grounds.add((GroundSurface) bs);
						// System.out.println(" * Ground element found");
					}
				}

				if (walls.size() == 0) {
					System.out.println("No Walls! " + filename);
				}
				if (roofs.size() == 0) {
					System.out.println("No Roofs! " + filename);
				}
				if (grounds.size() == 0) {
					System.out.println("No Grounds! " + filename);
				}

				// calculate weighted mean of heights of roofs and use it as
				// height information
				if (!roofs.isEmpty()) {

					double sumRoofArea = 0.;
					double roofHeight = 0.;

					for (RoofSurface roof : roofs) {

						double roofArea;
						List<SurfaceProperty> surface = roof
								.getLod2MultiSurface().getMultiSurface()
								.getSurfaceMember();
						for (int i = 0; i < surface.size(); i++) {
							roofSurfaceCounter[bCount]++;

							SurfaceProperty surfaceProperty = surface.get(i);

							roofArea = calcArea(surfaceProperty);
							sumRoofArea += roofArea;

							// min and max height of surfaceProperty
							double[] minmax = getMinMaxHeight(surfaceProperty);

							// a+f(b-a) with a=minmax[0]-lc.get(2),
							// b=minmax[1]-lc.get(2) (ground height cancels in
							// brakets)
							roofHeight += roofArea
									* (minmax[0] - lc.get(2) + roofHeightFactor
											* (minmax[1] - minmax[0]));
						}

						// if (!roof.isSetBoundedBy()) {
						// roof.calcBoundedBy();
						// }
						// List<Double> lcRoof =
						// roof.getBoundedBy().getEnvelope()
						// .getLowerCorner().getValue();
						// List<Double> ucRoof =
						// roof.getBoundedBy().getEnvelope()
						// .getUpperCorner().getValue();

					}

					// normalize
					roofHeight /= sumRoofArea;

					globalRoofSurfaceCounter += roofSurfaceCounter[bCount];

					// if (roofHeight / height ==0.) {
					// System.out.println(co.getId());
					// System.out.println(lc);
					// System.out.println(lcRoof);
					// System.out.println(ucRoof);
					// }

					// if (roofHeight / height < 0.3) {
					// System.out.println(roofHeight / height);
					// System.out.println(co.getId());
					// System.out.println(bCount);
					// }

					// height is roofHeight!
					bheight[bCount] = roofHeight;
				}

				// calculate ground size
				if (!grounds.isEmpty()) {
					for (GroundSurface ground : grounds) {
						List<SurfaceProperty> surface = ground
								.getLod2MultiSurface().getMultiSurface()
								.getSurfaceMember();

						for (SurfaceProperty surfaceProperty : surface) {
							area[bCount] += calcArea(surfaceProperty);
						}

						// System.out.println("Area: " + area[bCount]);
					}
				}

				// just get numbers of walls
				if (!walls.isEmpty()) {
					for (WallSurface wall : walls) {
						List<SurfaceProperty> surface = wall
								.getLod2MultiSurface().getMultiSurface()
								.getSurfaceMember();
						wallSurfaceCounter[bCount] += surface.size();
					}
				}
				globalWallSurfaceCounter += wallSurfaceCounter[bCount];

				// for visibility surfaces take only walls into
				// account: new array to include all these surfaces
				buildingWalls[bCount] = new RecSurface[wallSurfaceCounter[bCount]];

				if (!walls.isEmpty()) {

					// need a counter if there are several wall surfaces
					int wCount = -1;

					for (WallSurface wall : walls) {
						List<SurfaceProperty> surface = wall
								.getLod2MultiSurface().getMultiSurface()
								.getSurfaceMember();
						for (int i = 0; i < surface.size(); i++) {
							wCount++;
							buildingWalls[bCount][wCount] = new RecSurface(
									surface.get(i));
							if (!buildingWalls[bCount][wCount]
									.checkCoplanarity()) {
								NonPlanarList.add(co.getId());
							}
							// System.out.println(buildingSurfaces[bCount][i].getCentroid());
						}
					}
				}

				// min and max height of complete city
				lock.lock();
				if (bheight[bCount] >= uclm.maxHeight) {
					uclm.maxHeight = bheight[bCount];
					// System.out.println(co.getId());
				}
				if (bheight[bCount] <= uclm.minHeight) {
					uclm.minHeight = bheight[bCount];
				}
				lock.unlock();
			}
		}

		visible = new SymmetricMatrixBoolean(globalWallSurfaceCounter, false);

		int wcount = -1;

		// all walls and roofs, one side of visibility
		for (int i = 0; i <= bCount; i++) {
			for (int j = 0; j < buildingWalls[i].length; j++) {

				wcount++;

				int wrcount = -1;

				// walls and roofs, other side of visibility
				// the already analysed, visiblility is symmetric:
				for (int k = 0; k < i; k++) {
					wrcount += buildingWalls[k].length;
				}

				// the new ones
				for (int k = i; k <= bCount; k++) {

					// if buildings are too far away, skip:
					double dist = pow(projD.x[i] - projD.x[k], 2)
							+ pow(projD.y[i] - projD.y[k], 2);
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
						for (int m = 0; m <= bCount; m++) {

							// in principle, building to check should be on
							// the
							// connection between the starting and end
							// building,
							// so sum of distances - distance of buildings
							// approx. 0, because of buildings larger radius
							dist = sqrt(pow(projD.x[i] - projD.x[m], 2)
									+ pow(projD.y[i] - projD.y[m], 2))
									+ sqrt(pow(projD.x[k] - projD.x[m], 2)
											+ pow(projD.y[k] - projD.y[m], 2))
									- sqrt(pow(projD.x[i] - projD.x[k], 2)
											+ pow(projD.y[i] - projD.y[k], 2));
							if (dist > checkinway) {
								// visible[wcount][wrcount] = false;
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
								} else {
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

		System.out.println("finished visibility determination");

		// transform the coordinates, has to be after visibility determination
		// because old system is used there
		proj4.transform(projD, bCount + 1, 1);
		// now x is lon, y is lat

		for (int i = 0; i <= bCount; i++) {
			// apply rotated pole
			ProjectionPoint pp = uclm.rotpol.latLonToProj(projD.y[i],
					projD.x[i]);
			// and write back to projD
			projD.x[i] = pp.getX();
			projD.y[i] = pp.getY();

			try {
				irlat[i] = uclm.getRLatIndex(projD.y[i]);
			} catch (InvalidRangeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				irlon[i] = uclm.getRLonIndex(projD.x[i]);
			} catch (InvalidRangeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		// sum of areas of buildings
		for (int i = 0; i <= bCount; i++) {
			buildingFrac[iuc][irlat[i]][irlon[i]] += area[i];
		}

		wcount = -1;
		
		for (int i = 0; i <= bCount; i++) {
			for (int j = 0; j < buildingWalls[i].length; j++) {
				wcount++;

				if (buildingWalls[i][j].isHorizontal()) {
					continue;
				}

				List<RecSurfaceDistance> dist = new LinkedList<RecSurfaceDistance>();
				int wrcount = -1;
				// walls and roofs
				for (int k = 0; k <= bCount; k++) {
					if (i == k) {
						wrcount += buildingWalls[k].length;
						continue;
					}

					for (int l = 0; l < buildingWalls[k].length; l++) {

						wrcount++;

						if (visible.get(wcount, wrcount)) {

							dist.add(new RecSurfaceDistance(
									buildingWalls[i][j],
									buildingWalls[k][l]));
						
							if (dist.size() > ndistmean) {
								Collections.sort(dist);
								dist.subList(ndistmean, dist.size()).clear();
							}

						} else {
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
				
				// if (distance > 100) {
				// System.out.println("Distance: " + distance + "  "
				// + filename);
				// }

				int indexAngle = 0;
				try {
					indexAngle = uclm.getStreetdirIndex(buildingWalls[i][j]
							.getAngle());
				} catch (InvalidRangeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// wheight distance with surface size
				buildingDistance[iuc][indexAngle][irlat[i]][irlon[i]] += distance
						* buildingWalls[i][j].getArea();

				streetSurfaceSum[iuc][indexAngle][irlat[i]][irlon[i]] += buildingWalls[i][j]
						.getArea();
				try {
					building_height[iuc][indexAngle][uclm
							.getHeightIndex(bheight[i])][irlat[i]][irlon[i]] += buildingWalls[i][j]
							.getArea();
				} catch (InvalidRangeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				nStreetSurfaces[iuc][indexAngle][irlat[i]][irlon[i]]++;

				isInStreetdir[i][indexAngle] = true;
			}
		}

		System.out.println("distance stuff finished");

		for (int c = 0; c < uclm.getNuclasses(); c++) {
			for (int j = 0; j < uclm.getJe_tot(); j++) {
				for (int i = 0; i < uclm.getIe_tot(); i++) {
					// if urban there
					if (buildingFrac[c][j][i] > 1.e-13) {
						int sumNStreetSurfaces = 0;
						for (int dir = 0; dir < uclm.getNstreedir(); dir++) {
							// if (nStreetSurfaces[c][dir][j][i] > 0) {
							// buildingDistance[c][dir][j][i] /=
							// streetSurfaceSum[c][dir][j][i];
							// }
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

		lock.lock();
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
													building_height[c][dir][height][j][i]);
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
		// for (int b = 0; b <= bCount; b++) {
		// for (int s = 0; s < uclm.getNstreedir(); s++) {
		// if (isInStreetdir[b][s]) {
		// try {
		// uclm
		// .incBuildProb(iuc, s, uclm
		// .getHeightIndex(bheight[b]), irlat[b],
		// irlon[b]);
		// } catch (InvalidRangeException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		// }
		// }

		// save where not planar
		stats.addNonPlanar(id, NonPlanarList);
		for (int i = 0; i <= bCount; i++) {
			stats.buildingHeights.add(bheight[i]);
			stats.buildingGrounds.add(area[i]);
		}
		lock.unlock();

		// System.out.println("zu ENDE");

	}
}
