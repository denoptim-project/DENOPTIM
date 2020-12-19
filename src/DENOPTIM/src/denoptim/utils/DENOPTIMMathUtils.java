/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no> and
 *   Marco Foscato <marco.foscato@uib.no>
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

package denoptim.utils;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import denoptim.constants.DENOPTIMConstants;

/**
 * Some useful math operations
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class DENOPTIMMathUtils
{

//------------------------------------------------------------------------------

    /**
     * compute the angle between two vectors a & b of same length
     *
     * @param a 
     * @param b
     * @return angle in degrees
     */

    public static double calculateAngle (double[] a, double[] b)
    {
        double alen = length(a);
        double blen = length(b);
        double abdot = computeDotProduct(a, b);
        double angle = Math.acos(abdot/(alen*blen));
        return Math.toDegrees(angle);
    }
    
//------------------------------------------------------------------------------    
    /**
     * Normalizes a vector
     *
     * @param d A vector to be normalized.
     * @param ret The normalized vector.
     */
    public static void norm(double[] d, double[] ret) 
    {
        double length;
        length = length(d);
        ret[0] = d[0] / length;
        ret[1] = d[1] / length;
        ret[2] = d[2] / length;
    }

//------------------------------------------------------------------------------

    /**
     * Get normalized distance between two point
     *
     * @param p1 the first point
     * @param p2 the second point
     * @return the normalized vector
     */

    public static Vector3d normDist(Point3d p1, Point3d p2)
    {
	Vector3d v = new Vector3d(p1.x - p2.x,
				  p1.y - p2.y,
				  p1.z - p2.z);
	v.normalize();
	return v;
    }

//------------------------------------------------------------------------------
    /**
     * Scales a vector
     *
     * @param d A vector to be scaled
     * @param a A scaling value
     */
    public static double[] scale(double[] d, double a)
    {
	double[] scaled = new double[3];
        scaled[0] = d[0] * a;
        scaled[1] = d[1] * a;
        scaled[2] = d[2] * a;
	return scaled;
    }
    
//------------------------------------------------------------------------------    
    /**
     * Scales a vector
     *
     * @param d A vector to be scaled
     * @param a A scaling value
     * @param ret The scaled vector
     */
    public static void scalar(double[] d, double a, double[] ret) 
    {
        ret[0] = d[0] * a;
        ret[1] = d[1] * a;
        ret[2] = d[2] * a;
    }    

//------------------------------------------------------------------------------

    /**
     * Return the norm of a vector.
     *
     * @param v  Vector to compute length of.
     * @return   Length of vector.
     */
    public static double length (double[] v)
    {
        return Math.sqrt (v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
    }

//------------------------------------------------------------------------------

    /**
     * Compute the dot product (a scalar) between two vectors.
     *
     * @param v0 First vector
     * @param v1 Second vector
     * @return Dot product of given vectors.
     */
    public static double computeDotProduct (double[] v0, double[] v1)
    {
        return v0[0] * v1[0] + v0[1] * v1[1] + v0[2] * v1[2];
    }

//------------------------------------------------------------------------------

    /**
     * Compute the cross product of two vectors. Result is a vector
     *
     * @param v0 First vector
     * @param v1 Second vector
     * @return crossProduct  Cross product of vectors [x,y,z].
     */
    public static double[] computeCrossProduct (double[] v0, double[] v1)
    {
        double[] crossProduct = new double[3];
        crossProduct[0] = v0[1] * v1[2] - v0[2] * v1[1];
        crossProduct[1] = v0[2] * v1[0] - v0[0] * v1[2];
        crossProduct[2] = v0[0] * v1[1] - v0[1] * v1[0];
        return crossProduct;
    }

//------------------------------------------------------------------------------

    /**
     * Perform vector subtraction.
     *
     * @param v0 Vector to subtract from
     * @param v1 Vector to subtract
     * @return v0 - v1
     */
    public static double[] subtract (double[] v0, double[] v1)
    {
        double[] res = new double[3];
        res[0] = v0[0] - v1[0];
        res[1] = v0[1] - v1[1];
        res[2] = v0[2] - v1[2];
        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Perform vector addition
     *
     * @param v0 First vector
     * @param v1 Second vector
     * @return v0 + v1
     */
    public static double[] add (double[] v0, double[] v1)
    {
        double[] res = new double[3];
        res[0] = v0[0] + v1[0];
        res[1] = v0[1] + v1[1];
        res[2] = v0[2] + v1[2];
        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Compute the dihedral angle
     * @param p0 point in 3D
     * @param p1 point in 3D
     * @param p2 point in 3D
     * @param p3 point in 3D
     * @return angle
     */

    public static double computeDihedralAngle(Point3d p0, Point3d p1, 
						Point3d p2, Point3d p3)
    {
        double[] v0 = new double[] {p0.x, p0.y, p0.z};
        double[] v1 = new double[] {p1.x, p1.y, p1.z};
        double[] v2 = new double[] {p2.x, p2.y, p2.z};
        double[] v3 = new double[] {p3.x, p3.y, p3.z};

	return computeDihedralAngle(v0, v1, v2, v3);
    }

//------------------------------------------------------------------------------

    /**
     * Compute the dihedral angle
     *
     * @param v0 3D coordinates
     * @param v1 3D coordinates
     * @param v2 3D coordinates
     * @param v3 3D coordinates
     * @return angle
     */

    public static double computeDihedralAngle (double[] v0, double[] v1,
                                                double[] v2, double[] v3)
    {
        double[] r1 = subtract(v1, v0);
        double[] r2 = subtract(v2, v1);
        double[] r3 = subtract(v3, v2);

        double[] n1 = computeCrossProduct(r1, r2);
        double[] n2 = computeCrossProduct(r2, r3);

        double psin = computeDotProduct(n1, r3) *
                            Math.sqrt(computeDotProduct(r2, r2));
        double pcos = computeDotProduct(n1, n2);

        return Math.toDegrees(Math.atan2(psin, pcos));
    }

//------------------------------------------------------------------------------

    public static double log2(double x)
    {
        return Math.log(x)/Math.log(2);
    }


//------------------------------------------------------------------------------

    public static double roundValue(double val, int decimalPlaces)
    {
        BigDecimal bd = new BigDecimal(val);
        bd = bd.setScale(decimalPlaces, BigDecimal.ROUND_UP);
        return bd.doubleValue();
    }

//------------------------------------------------------------------------------

    /** 
     * Calculate the angle between the 3 points.
     * @param a
     * @param b
     * @param c
     * @return angle calculated 
     */
    public static double angle(Point3d a, Point3d b, Point3d c)
    {
        double xba = a.x - b.x;
        double xbc = c.x - b.x;
        double yba = a.y - b.y;
        double ybc = c.y - b.y;
        double zba = a.z - b.z;
        double zbc = c.z - b.z;

        double ba = Math.sqrt(xba*xba + yba*yba + zba*zba);
        double bc = Math.sqrt(xbc*xbc + ybc*ybc + zbc*zbc);

        double dot = xba*xbc + yba*ybc + zba*zbc;
        dot /= (ba * bc);

        double angle;
	if (dot < 0.0 && 
       Math.abs(Math.abs(dot)-1.0) < DENOPTIMConstants.FLOATCOMPARISONTOLERANCE)
	{
	    angle = 180.0;
	}
	else
	{
	    angle = Math.toDegrees(Math.acos(dot));
	}

        return angle;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Calculates angle (in degrees) between vectors BA and BC
     *
     * @param A Point
     * @param B Point
     * @param C Point
     * @return Angle (degrees)
     */
    
    public static double getAngle(double[] A, double[] B, double[] C)
    {
        double[] vBA = {A[0]-B[0], A[1]-B[1], A[2]-B[2]}; // Vector from B to A
        double[] vBC = {C[0]-B[0], C[1]-B[1], C[2]-B[2]}; // Vector from B to C

	return calculateAngle(vBA, vBC);
    }

    
//------------------------------------------------------------------------------

    /**
     * Calculates distance between point a and point b
     *
     * @param a Point
     * @param b Point
     * @return Distance between a and b
     */
    public static double distance(Point3d a, Point3d b)
    {
        double dx = a.x - b.x, dy = a.y - b.y, dz = a.z - b.z;
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

//------------------------------------------------------------------------------

    private static final double[] tp1 = new double[3];
    private static final double[] tp2 = new double[3];
    private static final double[] tp3 = new double[3];
    private static final double[] tp4 = new double[3];

    /** 
     * Calculate the torsion angle between the 4 points.
     * @param p1 Point
     * @param p2 Point
     * @param p3 Point
     * @param p4 Point
     * @return Torsion between arguments
     */
    public static double torsion(Point3d p1, Point3d p2, Point3d p3, Point3d p4)
    {
        p1.get(tp1);
        p2.get(tp2);
        p3.get(tp3);
        p4.get(tp4);

	return computeDihedralAngle(tp1, tp2, tp3, tp4);
    }
    
//------------------------------------------------------------------------------    
    
    /**
     * Return sum of all values in array.
     * @param a Array to sum elements of.
     * @return array sum.
     */
    public static double sum(double[] a)
    {
        double sum = 0.0;
        for (double v : a) {
            sum += v;
        }
        return sum;
    }

//------------------------------------------------------------------------------        
    
    /**
     * Calculate mean value.
     * @param a Array to calculate mean from.
     * @return mean value of array, NaN if empty array.
     */
    public static double mean(double[] a) 
    {
        if (a.length == 0) 
            return Double.NaN;
        double sum = sum(a);
        return sum / a.length;
    }
    
//------------------------------------------------------------------------------
    
	public static double mean(List<Double> vals) 
	{
		double[] arr = new double[vals.size()];
        for (int i=0; i<vals.size(); i++)
        {
            arr[i] = vals.get(i);
        }
		
		return mean(arr);
	}
    
//------------------------------------------------------------------------------    

    /**
     * Generate a vector that is perpendicular to the given one.
     * No control over which perpendicular direction will be chosen.
     * @param vecA input vector
     * @return a normal vector
     */

    public static Vector3d getNormalDirection(Vector3d vecA)
    {
        Vector3d normalDir = new Vector3d();
        ArrayList<Vector3d> candidates = new ArrayList<>();
        candidates.add(new Vector3d(1.0, 0.0, 0.0));
        candidates.add(new Vector3d(0.0, 1.0, 0.0));
        candidates.add(new Vector3d(0.0, 0.0, 1.0));
        candidates.add(new Vector3d(1.0/Math.sqrt(2.0), 
				    1.0/Math.sqrt(2.0),
				    0.0));
        candidates.add(new Vector3d(1.0/Math.sqrt(2.0), 
				    0.0,
				    1.0/Math.sqrt(2.0)));
        candidates.add(new Vector3d(0.0, 
				    1.0/Math.sqrt(2.0), 
				    1.0/Math.sqrt(2.0)));

        for (Vector3d candidate : candidates) {
            double res = vecA.dot(candidate);
            if (Math.abs(res) > 0.1 && Math.abs(res) < 0.9) {
                normalDir.cross(vecA, candidate);
                break;
            }
        }
	normalDir.normalize();

        return normalDir;
    }

//------------------------------------------------------------------------------
}
