package citygml2ucp.tools;

import java.awt.geom.Point2D;

import org.proj4.Proj4;
import org.proj4.ProjectionData;

import citygml2ucp.configuration.*;

import static java.lang.System.out;

public class Test {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		// CLMConfiguration conf = new
		// CLMConfiguration(40.f,-170.f,1f,1f,0.0f,0.0f,6,6,50);
		// CLMConfiguration conf = new
		// CLMConfiguration(40.f,-170.f,0.0625f,0.0625f,-20.f,-18.f,665,657,40);
		// CLMConfiguration conf = new
		// CLMConfiguration(39.25,-162.,0.16500,0.16500,-22.1925,-25.0475,257,271,32);
		// CLMConfiguration conf = new
		// CLMConfiguration(39.25,-162.,0.16500,0.16700,-22.1925,-25.0475,257,271,32);
		// UrbanCLMConfiguration conf = new UrbanCLMConfiguration();
		// List<Dimension> l = conf.ke_urban.dimlist;
		// System.out.println(conf.getRLatIndex(0.6));
		// System.out.println(conf.getStreetdirIndex(70));
		// System.out.println(l);
		// conf.toNetCDFfile("/home/schubert/temp/test2.nc");

		UrbanCLMConfiguration uclm = new UrbanCLMConfiguration(90., -180.,
				0.0069048, 0.011667, 52. + 15. / 60., 12. + 50. / 60, 100, 70,
				1, new double[] { -45, 0, 45, 90 }, new int[] { 22 },
				new double[] { 0.f, 5.f, 10.f, 15.f, 20.f, 25.f, 30.f, 35.f,
						40.f, 45.f, 50.f, 55.f, 60.f, 65.f, 70.f, 75.f, 80.f,
						90.f, 110.f, 140.f, 200.f, 300.f });

		System.out.println(uclm.getStreetDir(0));
		System.out.println(uclm.getStreetLength(0, 0));
		System.out.println(uclm.getStreetDir(1));
		System.out.println(uclm.getStreetLength(1, 0));
		System.out.println(uclm.getStreetDir(2));
		System.out.println(uclm.getStreetLength(2, 0));
		System.out.println(uclm.getStreetDir(3));
		System.out.println(uclm.getStreetLength(3, 0));

		// Proj4 testProjection = new Proj4("+init=epsg:3068",
		// "+init=epsg:4326 +latlong");
		// // testProjection.printSrcProjInfo();
		// // testProjection.printDestProjInfo();
		//		
		// ProjectionData dataTP = new ProjectionData(new double[][] {{31032.985
		// , 30874.249}}, new double[] {54.640});
		//
		// testProjection.transform(dataTP, 1, 1);
		//		
		// System.out.println((dataTP.y[0]));
		// System.out.println((dataTP.x[0]));

		// Projection projection = ProjectionFactory
		// .fromPROJ4Specification(new String[] { "+init:init=epsg:3068",
		// "+to", "+init=epsg:4326" });
		//		
		// Point2D.Double dest = new Point2D.Double();
		//		
		// projection.inverseTransform(new Point2D.Double(20281.021, 19760.743),
		// dest);
		// System.out.println(dest);

		// Soldner bsold = new Soldner();
		// LatLonPoint llp = bsold.projToLatLon(20281.021, 19760.743);

		// System.out.println(llp.getLatitude());
		// System.out.println(llp.getLongitude());

	}

}
