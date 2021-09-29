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

package denoptim.rings;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.io.DenoptimIO;
import denoptim.utils.DENOPTIMMathUtils;
import denoptim.utils.GenUtils;

/**
 * Tool to explore the conformational space of chains of atoms and 
 * identify ring closing conformations.
 *
 * @author Marco Foscato
 */

public class RingClosureFinder
{

    /**
     * verbosity level
     */
    private static int verbosity = RingClosureParameters.getVerbosity();

    /**
     * Flag to exit conformational search as soon as possible
     */
    private static boolean stopAtFirstMatch = 
				!RingClosureParameters.doExhaustiveConfSrch();

    // hard coded flag for debug: ***VERY*** time consuming
    private final static boolean writeAllConfs = false; 

    // Recursion counter for debug purposes
    private static int rec = 0;

//----------------------------------------------------------------------------

    /**
     * Giving a list of points in 3D space (the path) this method evaluates
     * whether it exists at least one conformation of the chain that allows to
     * form ring by connecting the head and the tail of the chain.
     *
     * @param path the chain of atoms
     * @param rotatability list of flags defining which bond is rotatable
     * @return <code>true</code> is a closable conformation is found
     */

    public static boolean evaluateClosability(List<IAtom> path, 
                                   ArrayList<Boolean> rotatability,
                                   ArrayList<ArrayList<Point3d>> dihRefs,
                                   ArrayList<ArrayList<Double>> closableConfs)
    {
        boolean res = false;

        // Check input
        int sz = path.size();
        if (rotatability.size() != sz-1)
        {
            System.out.println("ERROR! Cannot evaluate closability of path: "
                                + "path and list of bonds are not compatible!"
                                + "(" + sz + ", " + rotatability.size() + ")"
                                + " Please, Report this bug to the author.");
            System.exit(1);
        }
        //NB: the '+2' comes from the 'bonds' to RCAs: 2 per fundamental ring
        if (sz > RingClosureParameters.getMaxNumberRotatableBonds()+2)
        {
            if (verbosity > 0)
            {
                System.out.println("Too many rotatable bonds for systematic "
                        + "search. We assume the path is closable.");
            }
            return true;
        }
        

        // Create the chain of points to work with
        List<Point3d> ptsChain = new ArrayList<Point3d>();
        for (int i=0; i<path.size(); i++)
        {
            ptsChain.add(new Point3d(path.get(i).getPoint3d()));
        }

        // Get closability condition
        //  Nomenclature:
        //   head vector          tail vector
        //    h2-----h1|..........|t1-----t2
        int h1 = 1;
        int h2 = 0;
        int t1 = sz - 2;
        int t2 = sz - 1;
        RingClosure rc = new RingClosure(path.get(h1).getPoint3d(),
                                         path.get(h2).getPoint3d(),
                                         path.get(t1).getPoint3d(),
                                         path.get(t2).getPoint3d());
        ArrayList<Double> clsablConds = rc.getClosabilityConditions(
                           RingClosureParameters.getConfPathExtraTolerance());
        if (verbosity > 1)
        {
            System.out.println("RingClosability conditions vector:");
            System.out.println(clsablConds);
        }

        // Make work vector of dihedrals (angles around rotatable bonds)
        // avoiding linearities
        ArrayList<Double> dihedrals = new ArrayList<Double>();
        ArrayList<Double> dihIncement = new ArrayList<Double>();
        int nn = 0;
        for (int i=2; i<ptsChain.size(); i++)
        {
            double a = DENOPTIMMathUtils.angle(ptsChain.get(i-2),
                                               ptsChain.get(i-1),
                                               ptsChain.get(i));
            if (a >= RingClosureParameters.getLinearityLimit())
            {
                if (verbosity > 0)
                    System.out.println("Skipping linearity in point "+i);
                rotatability.set(i-1,false);
            }
            else
            {
                nn++;
            }
            if (i==2)
            {
                // add FIRST even if it is always not rotatable
                dihedrals.add(0.0);
                dihIncement.add(0.0);
            }
            else
            {
                ArrayList<Point3d> refPoints = dihRefs.get(i-3);
                dihedrals.add(DENOPTIMMathUtils.computeDihedralAngle(
                                                        refPoints.get(0), 
                                                        refPoints.get(1), 
                                                        refPoints.get(2),
                                                        refPoints.get(3)));
                dihIncement.add(0.0);
            }
        }
        // add LAST even if it is always not rotatable
        dihedrals.add(0.0);
        dihIncement.add(0.0);

        if (verbosity > 0)
        {
            System.out.println("Exploring torsional space... (dim:"
                               + nn + " - complete:" + !stopAtFirstMatch
			       + ")");
        }

        long startTime = System.nanoTime();
        boolean innerRes = hasClosableRotamer(ptsChain,
                                rotatability,
                                dihedrals,
                                dihIncement,
                                0,
                                RingClosureParameters.getPathConfSearchStep(),
                                h1,h2,t1,t2,
                                clsablConds,
                                closableConfs);
        long endTime = System.nanoTime();
        long time = (endTime - startTime) / (long) 1000.0;

        if (verbosity > 0)
        {
	    String s = "TIME (microsec) for exploration of torsional space: ";
            System.out.println(s + time);
        }

        if (closableConfs.size() > 0)
            res = true;

        return res;
    }

//----------------------------------------------------------------------------

    /**
     * Scan rotatable space looking for conformations that satisfy closability
     * condition.
     *
     * @param chain the chain of atoms as a list of points in 3D space
     * @param rotatability flags defining which bond is rotatable
     * @param dihedrals the current value of the dihedral angles
     * @param activeRot index of the currently active (rotating) bond
     * @param step the step taken by each sequential rotation of the bond
     * @param h1 the index of the first point defining the head vector
     * @param h2 the index of the second point defining the head vector
     * @param t1 the index of the first point defining the tail vector
     * @param t2 the index of the second point defining the tail vector
     * @param clsablConds the closability condition vector meant to feed
     * <code>RingClosure</code> 
     * @return <code>true</code> when the first closable conformation is found
     */

    public static boolean hasClosableRotamer(List<Point3d> chain, 
                                ArrayList<Boolean> rotatability,
                                ArrayList<Double> dihedrals,
                                ArrayList<Double> dihIncement,
                                int activeRot,
                                double step,
                                int h1, int h2, int t1, int t2,
                                ArrayList<Double> clsablConds,
                                ArrayList<ArrayList<Double>> closableConfs)
    {

        boolean res = false;
        if (verbosity > 2)
            System.out.println(rec+"-Rec: "+activeRot);
        int totStp = (int) (360.0 / step);
        if (!rotatability.get(activeRot))
        {
            if (verbosity > 2)
                System.out.println(rec+"-Rec: not active");
            totStp = 1;
        }
        for (int i=0; i<totStp; i++)
        {
            if (verbosity > 2)
                System.out.println(rec+"-RecLop: "+activeRot+" I:"+i);
            if (i != 0)
            {
                dihIncement.set(activeRot,dihIncement.get(activeRot) + step);

                // Move the whole branch of points that lie after the rotbond
                Point3d srcRotBnd = chain.get(activeRot);
                Point3d endRotBnd = chain.get(activeRot+1);
                Vector3d rotAxis = new Vector3d(endRotBnd.x - srcRotBnd.x, 
                                                endRotBnd.y - srcRotBnd.y,
                                                endRotBnd.z - srcRotBnd.z);
                rotAxis.normalize();
                Matrix3d rotMat = new Matrix3d();

                rotMat.set(new AxisAngle4d(rotAxis,Math.toRadians(step)));

                if (verbosity > 2)
                {
                    System.out.println(" srcRotBnd: " + srcRotBnd);
                    System.out.println(" endRotBnd: " + endRotBnd);
                    System.out.println(" rotAxis:   " + rotAxis);
                    System.out.println(" rotMat:    " + rotMat);
                }

                for (int ip = activeRot+2; ip<chain.size(); ip++)
                {
                    Point3d pt = chain.get(ip);
                    // Translate to origin of rot. axis while making vector
                    Vector3d newVec = new Vector3d(pt.x - srcRotBnd.x,
                                                   pt.y - srcRotBnd.y,
                                                   pt.z - srcRotBnd.z);
                    // Rotate
                    rotMat.transform(newVec);

                    // Translate back to original space
                    pt.x = newVec.x + srcRotBnd.x;
                    pt.y = newVec.y + srcRotBnd.y;
                    pt.z = newVec.z + srcRotBnd.z;
                }
            }

            if (activeRot+1 < dihedrals.size())
            {
                // Lauch exploration of next level
                rec++;
                int nextRot = activeRot+1;
                res = hasClosableRotamer(chain,
                                rotatability,
                                dihedrals,
                                dihIncement,
                                nextRot,
                                step,
                                h1,h2,t1,t2,
                                clsablConds,
                                closableConfs);
                rec--;
            }
            else
            {
                // Evaluate current conformation
                Point3d pH1 = chain.get(h1);
                Point3d pH2 = chain.get(h2);
                Point3d pT1 = chain.get(t1);
                Point3d pT2 = chain.get(t2);
                RingClosure rc = new RingClosure(pH1,pH2,pT1,pT2);
                res = rc.isClosable(clsablConds,false);
                if (res)
                {
                    // Store vector of dihedrals
                    ArrayList<Double> conf = new ArrayList<Double>();
                    for (int ib=0; ib<dihedrals.size(); ib++)
                    {
                        double tot = dihedrals.get(ib) + dihIncement.get(ib);
                        if (tot > 180.0)
                        {
                            tot = tot - 360.0;
                        }
                        conf.add(tot);
                    }
                    closableConfs.add(conf);

                    if (verbosity > 0)
                    {
                        System.out.println("Found closable path conformation!");
                        if (verbosity > 1 && writeAllConfs)
                        {
                            reportForDebug("closable.sdf",chain);
                            System.out.println(" Dihedrals:  " + dihedrals);
                            System.out.println(" Increments: " + dihIncement);
                            System.out.println(" Conf.:      " + conf);
                            System.out.println(" See 'closable.sdf'");
//			    GenUtils.pause();
                        }
                    }
                }
                else
                {
                    if (verbosity > 1 && writeAllConfs)
                    {
                        reportForDebug("not_closable.sdf",chain);
                        System.out.println("Conformation of path is NOT "
                                        +"closable! See 'not_closable.sdf'");
                        System.out.println(" Dihedrals:  " + dihedrals);
                        System.out.println(" Increments: " + dihIncement);
                        System.out.println(" Chain:      ");
                        for (int ii=0; ii<chain.size(); ii++)
                            System.out.println("   " + chain.get(ii));
//                        GenUtils.pause();
                    }
                }
            }
    	    if (stopAtFirstMatch)
    	    {
                if (res)
                {
            	    if (verbosity > 1)
            	    {
            		System.out.println("Stop recursive conf. search."
            				   + " (rec.: " + rec + ")");
            	    }
                    break;
                }
            }
        }

        // reset
        if (rotatability.get(activeRot))
        {
            dihIncement.set(activeRot,dihIncement.get(activeRot) 
                                                          - step*(totStp-1));

            // Move back the whole branch of points that lie after the rotbond
            Point3d srcRotBnd = chain.get(activeRot);
            Point3d endRotBnd = chain.get(activeRot+1);
            Vector3d rotAxis = new Vector3d(endRotBnd.x - srcRotBnd.x,
                                            endRotBnd.y - srcRotBnd.y,
                                            endRotBnd.z - srcRotBnd.z);
            rotAxis.normalize();
            Matrix3d rotMat = new Matrix3d();
            rotMat.set(new AxisAngle4d(rotAxis,
                                       Math.toRadians(-step * (totStp - 1))));

            for (int ip = activeRot+2; ip<chain.size(); ip++)
            {
                Point3d pt = chain.get(ip);
                // Translate to origin of rot. axis while making vector
                Vector3d newVec = new Vector3d(pt.x - srcRotBnd.x,
                                               pt.y - srcRotBnd.y,
                                               pt.z - srcRotBnd.z);
                // Rotate
                rotMat.transform(newVec);

                // Translate back to original space
                pt.x = newVec.x + srcRotBnd.x;
                pt.y = newVec.y + srcRotBnd.y;
                pt.z = newVec.z + srcRotBnd.z;
            }
        }

        return res;
    }

//----------------------------------------------------------------------------

    /**
     * Method for reporting a path of atoms (list of points) as SDF file
     */

    private static void reportForDebug(String filename, List<Point3d> chain)
    {
        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        IAtomContainer mol = builder.newAtomContainer();
        for (int ia=0 ; ia<chain.size(); ia++)
        {
            Atom atm = new Atom("He",chain.get(ia));
            mol.addAtom(atm);
            if (ia > 0)
               mol.addBond(ia-1,ia,IBond.Order.valueOf("SINGLE"));
        }
        try
        {
            DenoptimIO.writeMolecule(filename, mol, true);
        } catch (Throwable thr)
        {
            System.out.println("Unable to write SDF: "+thr);
            GenUtils.pause();
        }
    }

//----------------------------------------------------------------------------

}
