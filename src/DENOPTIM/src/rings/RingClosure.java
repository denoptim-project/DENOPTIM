/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
 * 
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rings;

import java.util.ArrayList;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import utils.GenUtils;

/**
 * RingClosure represents the arrangement of atoms and 
 * PseudoAtoms identifying the head and tail of a chain of atoms.
 * The chain may have a proper conformation that allows to close a ring by
 * forming a new bond. The four points contained in RingClosure
 * represents (in this order) atom H1, PseudoAtom H2, atom T1,
 * and PseudoAtom T2. keep in mind this scheme:<br>
 * H2--H1.....rest of chain.....T1--T2<br>
 *
 * @author Marco Foscato 
 */

public class RingClosure
{
    /**
     * The firs point defining the head vector
     */
    private Point3d h1;

    /**
     * The second point defining the head vector
     */
    private Point3d h2;

    /**
     * The firs point defining the tail vector
     */
    private Point3d t1;

    /**
     * The second point defining the tail vector
     */
    private Point3d t2;

    /**
     * vector head of chain
     */
    private Vector3d h;

    /**
     * vector tail of chain
     */
    private Vector3d t; 

    /**
     * Distance H1 T2
     */
    private double distH1T2;

    /**
     * Distance H2 T1
     */
    private double distH2T1;

    /**
     * Distance H2 T2
     */
    private double distH2T2;

    /**
     * Dot product H.T
     */
    private double dotProd;

    /**
     * Quality score of the head and tail vectors alignement
     */
    private double qscore = Double.NaN;

//-----------------------------------------------------------------------------

    /**
     *  Constructs an empty RingClosure
     */

    public RingClosure()
    {
    }

//-----------------------------------------------------------------------------

    /**
     *  Constructs a RingClosure from the involved points
     */

    public RingClosure(Point3d h1, Point3d h2, Point3d t1, Point3d t2)
    {
        this.h1 = h1;
        this.h2 = h2;
        this.t1 = t1;
        this.t2 = t2;
    }

//-----------------------------------------------------------------------------

    /**
     * Calculate desctiprots (distances and dot product). This method is not 
     * executed during creation of the object to allow faster handling of
     * the RingClosure object.
     */

    public void calculateDescriptors()
    {
        this.distH1T2 = h1.distance(t2);
        this.distH2T1 = h2.distance(t1);
        this.distH2T2 = h2.distance(t2);
        this.h = new Vector3d(h2.x-h1.x, h2.y-h1.y, h2.z-h1.z);
        this.t = new Vector3d(t2.x-t1.x, t2.y-t1.y, t2.z-t1.z);
        this.h.normalize();
        this.t.normalize();
        this.dotProd = h.dot(t);
    }

//-----------------------------------------------------------------------------

    /**
     * Evaluate closability by comparing the distances and the dot product
     * with the given critera.
     * @param clsablConds the closability conditions
     * @return <code>true</code> is the arrangement of points respect the
     * closability condition
     */

    public boolean isClosable(ArrayList<Double> clsablConds, boolean debug)
    {
        return isClosable(clsablConds.get(0), 
                          clsablConds.get(1),
                          clsablConds.get(2),
                          clsablConds.get(3),
                          clsablConds.get(4),
                          clsablConds.get(5),
                          clsablConds.get(6),
                          debug);
    }

//-----------------------------------------------------------------------------

    /**
     * Evaluate closability by comparing the distances and the dot product 
     * with the given critera (i.e., the closability conditions).
     *
     * @param minDH1T2 minimum threshold value for distance h1-t2
     * @param maxDH1T2 maximum threshold value for distance h1-t2 
     * @param minDH2T1 minimum threshold value for distance h2-t1
     * @param maxDH2T1 maximum threshold value for distance h2-t1
     * @param minDH2T2 minimum threshold value for distance h2-t2
     * @param maxDH2T2 maximum threshold value for distance h2-t2
     * @param maxDotHT maximum value for the dot product
     * @param debug this flag makes me printing a lot of info
     * @return <code>true</code> is the arrangement of points respect the
     * closability condition
     */

    public boolean isClosable  (double minDH1T2, double maxDH1T2,
                                double minDH2T1, double maxDH2T1,
                                double minDH2T2, double maxDH2T2,
                                double maxDotHT, boolean debug)
    {
        boolean res = false;
        this.distH1T2 = h1.distance(t2);
        this.distH2T1 = h2.distance(t1);
        this.distH2T2 = h2.distance(t2);
        if (debug)
        {

            System.out.println("Values for evaluation of closability:");
            System.out.printf("  distH1T2: %8.4f min: %8.4f max: %8.4f%n",
						distH1T2, minDH1T2, maxDH1T2);
            System.out.printf("  distH2T1: %8.4f min: %8.4f max: %8.4f%n",
                                                distH2T1, minDH2T1, maxDH2T1);
            System.out.printf("  distH2T2: %8.4f min: %8.4f max: %8.4f%n",
                                                distH2T2, minDH2T2, maxDH2T2);
            h = new Vector3d(h2.x-h1.x, h2.y-h1.y, h2.z-h1.z);
            t = new Vector3d(t2.x-t1.x, t2.y-t1.y, t2.z-t1.z);
            h.normalize();
            t.normalize();
            System.out.printf("  dot:      %8.4f max: %8.4f%n",
                                                h.dot(t), maxDotHT);
        }

        if (distH1T2 < maxDH1T2 && distH1T2 > minDH1T2 &&
            distH2T1 < maxDH2T1 && distH2T1 > minDH2T1 &&
            distH2T2 < maxDH2T2 && distH2T2 > minDH2T2)
        {
            h = new Vector3d(h2.x-h1.x, h2.y-h1.y, h2.z-h1.z);
            t = new Vector3d(t2.x-t1.x, t2.y-t1.y, t2.z-t1.z);
            h.normalize();
            t.normalize();

            if (h.dot(t) <= maxDotHT)
            {
                if (debug)
                    System.out.println("  CLOSABLE!");
                res = true;
            }
        }
        return res;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the score evaluating the overal closure quality of this ring
     * closure
     */

    public double getRingClosureQuality()
    {
        if (Double.isNaN(qscore))
        {
            double lenH = h1.distance(h2);
            double lenT = t1.distance(t2);
            double optDistH1T2 = 0.0;
            double optDistH2T1 = 0.0;
            double optDistH1T1 = 0.0;
            double optDistH2T2 = 0.0;
            if (RingClosureParameters.getRCStrategy().equals("BONDOVERLAP"))
            {
                optDistH1T1 = (lenH + lenT) / 2.0;
                optDistH2T2 = optDistH1T1;
            }
            else if (RingClosureParameters.getRCStrategy().
					     equals("BONDCOMPLEMENTARITY"))
            {
                optDistH1T2 = lenH;
                optDistH2T1 = lenT;
                optDistH1T1 = lenH + lenT;
            }
    
            qscore = Math.abs(h1.distance(t1) - optDistH1T1) +
                     Math.abs(h2.distance(t2) - optDistH2T2) +
                     Math.abs(h1.distance(t2) - optDistH1T2) +
                     Math.abs(h2.distance(t1) - optDistH2T1);        
        }
        return qscore;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the point requested
     * @param i index of the point to return (0,3)
     */

    public Point3d getPoint(int i)
    {
        if (i == 0)
            return h1;
        else if (i == 1)
            return h2;
        else if (i == 2)
            return t1;
        else if (i == 3)
            return t2;
        return null;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the list of min/max values defining the closability conditions.
     * This method is made available to garantee that only one set of 
     * closability conditions is used by whichever tool needs to evaluate 
     * ring closability.
     * @param etrxTol additional factor that multiplies dist. tolerance
     * @return the vector of conditions in this exact order: 
     * <ol>
     * <li>
     * minDistH1T2
     * </li>
     * <li>
     * maxDistH1T2
     * </li>
     * <li>
     * minDistH2T1
     * </li>
     * <li>
     * maxDistH2T1
     * </li>
     * <li>
     * minDistH2T2
     * </li>
     * <li>
     * maxDistH2T2
     * </li>
     * <li>
     * maxDotProdHT
     * </li>
     * </ol>
     */

    public ArrayList<Double> getClosabilityConditions(double etrxTol)
    {
        ArrayList<Double> clsablConds = new ArrayList<Double>();

        //Define conditions based on SrcAtom-to-RCA bonds and RC-strategy
        double lenH = h1.distance(h2);
        double lenT = t1.distance(t2);
        double distTolerance = (lenH + lenT) / 2.0;
        distTolerance = distTolerance 
                        * etrxTol * RingClosureParameters.getRCDistTolerance();
        double minDistH1T2 = -1.0;
        double minDistH2T1 = -1.0;
        double minDistH2T2 = -1.0;
        double maxDistH1T2 = 0.0;
        double maxDistH2T1 = 0.0;
        double maxDistH2T2 = 0.0;
        double maxDotProdHT = RingClosureParameters.getRCDotPrTolerance();
        if (RingClosureParameters.getRCStrategy().equals("BONDOVERLAP"))
        {
            maxDistH1T2 = distTolerance;
            maxDistH2T1 = distTolerance;
            maxDistH2T2 = lenH + lenT;
        }
        else if (RingClosureParameters.getRCStrategy().
						equals("BONDCOMPLEMENTARITY"))
        {
            distTolerance = distTolerance / 2.0;
            minDistH1T2 = lenH - distTolerance;
            minDistH2T1 = lenT - distTolerance;
            maxDistH1T2 = lenH + distTolerance;
            maxDistH2T1 = lenT + distTolerance;
            maxDistH2T2 = distTolerance 
			   * RingClosureParameters.getRCDistTolerance();
        }

        //Collect conditions as a vector
        clsablConds.add(minDistH1T2);
        clsablConds.add(maxDistH1T2);
        clsablConds.add(minDistH2T1);
        clsablConds.add(maxDistH2T1);
        clsablConds.add(minDistH2T2);
        clsablConds.add(maxDistH2T2);
        clsablConds.add(maxDotProdHT);

        return clsablConds;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns a deep copy of this RingClosure object
     */

    public RingClosure deepCopy()
    {
        Point3d nH1 = new Point3d(h1.x, h1.y, h1.z);
        Point3d nH2 = new Point3d(h2.x, h2.y, h2.z);
        Point3d nT1 = new Point3d(t1.x, t1.y, t1.z);
        Point3d nT2 = new Point3d(t2.x, t2.y, t2.z);
        RingClosure nRc = new RingClosure(nH1, nH2, nT1, nT2);
        return nRc;
    }

//-----------------------------------------------------------------------------

    /**
     * @return the string representation of this RingClosure
     */

    public String toString()
    {
        String s = "RingClosure [" + h1 + " " + h2 + " " + t1 + " " + t2 + "]";
        return s;
    }

//-----------------------------------------------------------------------------
}

