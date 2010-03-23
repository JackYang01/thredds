/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft;

import junit.framework.TestCase;
import ucar.nc2.TestAll;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridCoordSys;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.fmrc.Fmrc;
import ucar.nc2.ft.fmrc.FmrcInv;

import java.util.Formatter;
import java.util.Iterator;
import java.util.List;

/**
 * Test the new fmrc aggregation
 *
 * @author caron
 * @since Feb 25, 2010
 */
public class TestFmrc extends TestCase {

  public TestFmrc(String name) {
    super(name);
  }

  private static String datadir = TestAll.cdmUnitTestDir + "fmrc/";
  private static boolean showCount = true;

  public void testNcML() throws Exception {
    doOne(datadir + "bom/BoM_test.ncml", 1, 1, 8, 0, "eta_t", 2, 7);
    doOne(datadir + "ncom/ncom_fmrc.ncml", 1, 1, 5, 1, "surf_el", 3, 25);
    doOne(datadir + "rtofs/rtofs.ncml", 7, 4, 10, 1, "Three-D_Temperature", 2, 3);
  }

  static void doOne(String pathname, int ngrids, int ncoordSys, int ncoordAxes, int nVertCooordAxes,
                    String gridName, int nruns, int ntimes) throws Exception {
    System.out.println("test read Fmrc = " + pathname);
    Formatter errlog = new Formatter();
    Formatter debug = new Formatter();
    Fmrc fmrc = Fmrc.open(pathname, errlog, debug);
    if (fmrc == null) {
      System.out.printf("Fmrc failed to open %s%n", pathname);
      System.out.printf("errlog= %s%n", errlog.toString());
      System.out.printf("debug=  %s%n", debug.toString());
      return;
    }

    ucar.nc2.dt.GridDataset gridDs = fmrc.getDataset2D(true);
    NetcdfDataset ncd = (NetcdfDataset) gridDs.getNetcdfFile();

    int countGrids = gridDs.getGrids().size();
    int countCoordAxes = ncd.getCoordinateAxes().size();
    int countCoordSys = ncd.getCoordinateSystems().size();

    // count vertical axes
    int countVertCooordAxes = 0;
    List axes = ncd.getCoordinateAxes();
    for (int i = 0; i < axes.size(); i++) {
      CoordinateAxis axis =  (CoordinateAxis) axes.get(i);
      AxisType t = axis.getAxisType();
      if ((t == AxisType.GeoZ) || (t == AxisType.Height) || (t == AxisType.Pressure) )
        countVertCooordAxes++;
    }

    Iterator iter = gridDs.getGridsets().iterator();
    while (iter.hasNext()) {
      GridDataset.Gridset gridset = (GridDataset.Gridset) iter.next();
      gridset.getGeoCoordSystem();
    }

    GridDatatype grid = gridDs.findGridDatatype(gridName);
    assert (grid != null) : "Cant find grid "+gridName;

    GridCoordSystem gcs = grid.getCoordinateSystem();
    CoordinateAxis1DTime runtime = gcs.getRunTimeAxis();
    assert (runtime != null) : "Cant find runtime for "+gridName;
    assert runtime.getSize() == nruns : runtime.getSize()+" != "+ nruns;

    CoordinateAxis time = gcs.getTimeAxis();
    assert (time != null) : "Cant find time for "+gridName;
    assert (time.getRank() == 2) : "Time should be 2D "+gridName;
    assert time.getDimension(0).getLength() == nruns : " nruns should ne "+ nruns;
    assert time.getDimension(1).getLength() == ntimes : " ntimes should ne "+ ntimes;

    if (showCount) {
      System.out.println(" grids=" + countGrids + ((ngrids < 0) ? " *" : ""));
      System.out.println(" coordSys=" + countCoordSys + ((ncoordSys < 0) ? " *" : ""));
      System.out.println(" coordAxes=" + countCoordAxes + ((ncoordAxes < 0) ? " *" : ""));
      System.out.println(" vertAxes=" + countVertCooordAxes + ((nVertCooordAxes < 0) ? " *" : ""));
    }

    if (ngrids >= 0)
      assert ngrids == countGrids : "Grids " + ngrids + " != " + countGrids;
    if (ncoordSys >= 0)
      assert ncoordSys == countCoordSys : "CoordSys " + ncoordSys + " != " + countCoordSys;
    if (ncoordAxes >= 0)
      assert ncoordAxes == countCoordAxes : "CoordAxes " + ncoordAxes + " != " + countCoordAxes;
    if (nVertCooordAxes >= 0)
      assert nVertCooordAxes == countVertCooordAxes : "VertAxes" + nVertCooordAxes + " != " + countVertCooordAxes;

    gridDs.close();
  }

}
