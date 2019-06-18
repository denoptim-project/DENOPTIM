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

package utils;

import java.util.ArrayList;
import java.math.BigDecimal;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import constants.DENOPTIMConstants;

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
     * @param a A scaler value
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
        double len = Math.sqrt (v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        return len;
    }

//------------------------------------------------------------------------------

    /**
     * Compute the dot product (a scalar) between two vectors.
     *
     * @param v0   Vectors to compute dot product between.
     * @param v1
     * @return        Dot product of given vectors.
     */
    public static double computeDotProduct (double[] v0, double[] v1)
    {
        double d = v0[0] * v1[0] + v0[1] * v1[1] + v0[2] * v1[2];
        return d;
    }

//------------------------------------------------------------------------------

    /**
     * Compute the cross product of two vectors. Result is a vector
     *
     * @param v0        Vectors to compute cross product between.
     * @param v1        Vectors to compute cross product between.
     * @return crossProduct  Cross product of vectors [x,y,z].
     */
    public static double[] computeCrossProduct (double[] v0, double[] v1)
    {
        double crossProduct[] = new double[3];
        crossProduct[0] = v0[1] * v1[2] - v0[2] * v1[1];
        crossProduct[1] = v0[2] * v1[0] - v0[0] * v1[2];
        crossProduct[2] = v0[0] * v1[1] - v0[1] * v1[0];
        return crossProduct;
    }

//------------------------------------------------------------------------------

    public static double[] subtract (double[] v0, double[] v1)
    {
        double res[] = new double[3];
        res[0] = v0[0] - v1[0];
        res[1] = v0[1] - v1[1];
        res[2] = v0[2] - v1[2];
        return res;
    }

//------------------------------------------------------------------------------

    public static double[] add (double[] v0, double[] v1)
    {
        double res[] = new double[3];
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

    // 1 kcal mol-1  =	4.184 J mol-1
    // 1 Electron volt = 23.06035 kcal mol-1
    public static double convertEVToKCalPerMol(double energy)
    {
        return (energy * 23.06035);
    }

//------------------------------------------------------------------------------

    // 1AU to nm-1
    public static double convertEVToReciprocalNM(double energy)
    {
        return (energy * 1239.8424121); // in nm-1
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
    
    public static void diff(double[] a, double[] b, double[] ab)
    {
        for (int i=0; i<a.length; i++)
        {
            ab[i] = a[i] - b[i];
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Call the three points A = (x1,y1,z1), B = (x2,y2,z2), and 
     * C = (x3,y3,z3). Assuming you want angle ABC, the angle between vector 
     * BA and vector BC i.e. centered at B
     * @param A 
     * @param B
     * @param C
     * @return the angle
     */
    
    public static double getAngle(double[] A, double[] B, double[] C)
    {
        double[] vBA = {A[0]-B[0], A[1]-B[1], A[2]-B[2]}; // Vector from B to A
        double[] vBC = {C[0]-B[0], C[1]-B[1], C[2]-B[2]}; // Vector from B to C

	return calculateAngle(vBA, vBC);
    }

    
//------------------------------------------------------------------------------

    public static double distance(Point3d a, Point3d b)
    {
        double dx = a.x - b.x, dy = a.y - b.y, dz = a.z - b.z;
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

//------------------------------------------------------------------------------

    /** 
     * Static distance squared method.
     * @param a
     * @param b
     * @return  squared distance
     */
    public static double distanceSquared(Point3d a, Point3d b)
    {
        double dx = a.x - b.x, dy = a.y - b.y, dz = a.z - b.z;
        return dx*dx + dy*dy + dz*dz;
    }

//------------------------------------------------------------------------------

    private static double[] tp1 = new double[3];
    private static double[] tp2 = new double[3];
    private static double[] tp3 = new double[3];
    private static double[] tp4 = new double[3];

    /** 
     * Calculate the torsion angle between the 4 points.
     * @param p1
     * @param p2
     * @param p3
     * @param p4
     * @return  torsion
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
    
    public static double[][] makeMatrix(double[] X, double[] Y)  
    {  
        double [][] M = new double[3][3];
        for (int i=0; i<3; i++)
            M[i] = new double[3];
        double[] nX = new double[3];
        double[] nY = new double[3];
        double[] nZ = new double[3];

        norm(X, nX); //M.X = normalise( X );
        norm(computeCrossProduct(X, Y), nZ); // M.Z = normalise( cross_product(X,Y) );
        norm(computeCrossProduct(nZ, X), nY); // M.Y = //normalise( cross_product(M.Z,X) );  

        M[0][0] = nX[0];
        M[0][1] = nX[1];
        M[0][2] = nX[2];

        M[1][0] = nY[0];
        M[1][1] = nY[1];
        M[1][2] = nY[2];

        M[2][0] = nZ[0];
        M[2][1] = nZ[1];
        M[2][2] = nZ[2]; 
    
    
        return M;
    }
    
//------------------------------------------------------------------------------    
    
    /**
     * Return sum of all values in array.
     * @param a
     * @return array sum
     */
    public static double sum(double[] a) 
    {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) 
        {
            sum += a[i];
        }
        return sum;
    }

//------------------------------------------------------------------------------        
    
    /**
     * Return average value in array, NaN if no such value.
     * @param a
     * @return 
     */
    public static double mean(double[] a) 
    {
        if (a.length == 0) 
            return Double.NaN;
        double sum = sum(a);
        return sum / a.length;
    }
    
//------------------------------------------------------------------------------    
    
    /**
     * Return sample standard deviation of array, NaN if no such value.
     * @param a
     * @return 
     */
    public static double stddev(double[] a) 
    {
        return Math.sqrt(var(a));
    }
    
//------------------------------------------------------------------------------        
    
    /**
     * Return population standard deviation of array, NaN if no such value.
     * @param a
     * @return 
     */
    public static double stddevp(double[] a) 
    {
        return Math.sqrt(varp(a));
    }    
    
//------------------------------------------------------------------------------        
    
    /**
     * Return population variance of array, NaN if no such value.
     * @param a
     * @return 
     */
    public static double varp(double[] a) 
    {
        if (a.length == 0) 
            return Double.NaN;
        double avg = mean(a);
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) 
        {
            sum += (a[i] - avg) * (a[i] - avg);
        }
        return sum / a.length;
    }        
    
//------------------------------------------------------------------------------
    
    /**
     * Return sample variance of array, NaN if no such value.
     * @param a
     * @return 
     */
    public static double var(double[] a) 
    {
        if (a.length == 0) return Double.NaN;
        double avg = mean(a);
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) 
        {
            sum += (a[i] - avg) * (a[i] - avg);
        }
        return sum / (a.length - 1);
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

        for (int i=0; i<candidates.size(); i++)
        {
            double res = vecA.dot(candidates.get(i));
	    if (Math.abs(res) > 0.1 && Math.abs(res) < 0.9)
	    {
		normalDir.cross(vecA,candidates.get(i));
		break;
	    }
	}
	normalDir.normalize();

        return normalDir;
    }

//------------------------------------------------------------------------------
}
