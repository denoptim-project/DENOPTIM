package denoptim.fitness;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.openscience.cdk.IImplementationSpecification;
import org.openscience.cdk.qsar.DescriptorEngine;
import org.openscience.cdk.qsar.IDescriptor;

import denoptim.exception.DENOPTIMException;

public class DescriptorUtils
{	
	private static final String FS = System.getProperty("file.separator");
	private static final String NL = System.getProperty("line.separator");
	
	/**
	 * List of descriptor names that are excluded from default import because
	 * they have been shown to contain bugs.
	 */
	private static List<String> rejectedDescriptors = Arrays.asList(
	        new String[]{"JPLogP"});
	
//------------------------------------------------------------------------------
    
	/**
	 * Search for packages in the class path and in the appended archives.
	 * @param packageName package name that uses '.' as separator.
	 * @param jarFileWildQuery a query to identify the jar file that should
	 * contain the wanter package.
	 * @return the list of class names reported in Java format (e.g.,
	 * 'org.openscience.cdk.qsar.descriptors....'.
	 */
    public static List<String> getAllClassNamesInPackage(String packageName, 
            String jarFileWildQuery)
    {
        String pkgPath = packageName.replaceAll("\\.", FS);
        String jarPathName  = "";
        String classPath = System.getProperty("java.class.path");
        String[] jarsAndDirs = classPath.split(File.pathSeparator);
        for (int i = 0; i < jarsAndDirs.length; i++)
        {
            String pathName = jarsAndDirs[i];
            if (!pathName.endsWith(".jar"))
            {
                continue;
            }
            
            JarFile jarFile;
            try {
                jarFile = new JarFile(pathName);
                Enumeration<JarEntry> enumeration = jarFile.entries();
                while (enumeration.hasMoreElements()) {
                    JarEntry jarEntry = enumeration.nextElement();
                    if (jarEntry.toString().contains(pkgPath))
                    {
                        jarPathName = pathName;
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            if (jarPathName.equals("") && jarFileWildQuery!=null 
                    && !jarFileWildQuery.equals(""))
            {
                File parentFolder = new File(System.getProperty("user.dir"));
                if (pathName.endsWith("DenoptimGA.jar") 
                        || pathName.endsWith("FragSpaceExplorer.jar")
                        || pathName.endsWith("GUI.jar"))
                {
                    File myClassPath = new File(pathName);
                    if (pathName.contains(FS))
                    {
                        parentFolder = new File(myClassPath.getParent());
                    }
                }
                
                FileFilter fileFilter = new WildcardFileFilter(jarFileWildQuery);
                File[] cands = parentFolder.listFiles(fileFilter);
                if (cands.length == 1)
                {
                    jarPathName = cands[0].getAbsolutePath();
                }
                
                File libFolder = new File(parentFolder.getAbsolutePath() 
                        + FS + "lib");
                cands = libFolder.listFiles(fileFilter);
                if (cands.length == 1)
                {
                    jarPathName = cands[0].getAbsolutePath();
                }
            }
        }
        
        List<String> classNames = new ArrayList<String>();
        if (!jarPathName.equals(""))
        {
            classNames = DescriptorEngine.getDescriptorClassNameByPackage(
                    packageName, new String[]{jarPathName});
        }
        
        ClassLoader cl = new ClassLoader() {};
        Enumeration<URL> roots = null;
        try
        {
            roots = cl.getResources("");
            while (roots.hasMoreElements())
            {
                URL url = roots.nextElement();
                File root = new File(url.getPath());
                Iterator<File> it = FileUtils.iterateFiles(root, 
                        new String[] {"class"}, true);
                while(it.hasNext())
                {
                    File f = it.next();
                    if (f.getPath().contains(
                            pkgPath.replaceAll(FS,File.separator)))
                    {
                        String tmp = f.getPath()
                                .substring(f.toString().indexOf(
                                        pkgPath
                                        .replaceAll(FS,File.separator)))
                                .replaceAll(File.separator, ".")
                                .replaceAll("\\.class", "");
                        if (tmp.indexOf('$') != -1) continue;
                        if (tmp.indexOf("Test") != -1) continue;
                        if (!classNames.contains(tmp)) classNames.add(tmp);
                    }
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        return classNames;
	}

//------------------------------------------------------------------------------
    
    public static List<String> getClassNamesToDenoptimDescriptors()
    {
        return getAllClassNamesInPackage("denoptim.fitness.descriptors",
                "DENOPTIM*.jar");
    }
	
//------------------------------------------------------------------------------
	
	public static List<String> getClassNamesToCDKDescriptors()
	{
	    return getAllClassNamesInPackage("org.openscience.cdk.qsar.descriptors",
	            "cdk*.jar");
	}
	
//------------------------------------------------------------------------------

	//TODO only CDK? or all?
	public static DescriptorEngine getCDKDescriptorEngine()
	{
		return new DescriptorEngine(getClassNamesToCDKDescriptors(), null);
	}
	
//------------------------------------------------------------------------------

    /**
     * Searches for descriptor implementations in the CDK packages.
     * This method excludes descriptors
     * that have been listed in {@link #rejectedDescriptors}.
     * @param requiredDescriptors list of descriptor short names that we want
     * to obtain. All the rest will be ignored. This parameter can be null, in 
     * which case we'll return all the descriptors.
     * @return the list of descriptor information bundles.
     * @throws DENOPTIMException
     */
    public static List<DescriptorForFitness> findAllCDKDescriptors(
            Set<String> requiredDescriptors) throws DENOPTIMException
    {
        List<String> classNames = new ArrayList<String>();
        classNames.addAll(getClassNamesToCDKDescriptors());
        return findDescriptorImplementations(requiredDescriptors, classNames);
    }
	
//------------------------------------------------------------------------------

    /**
     * Searches for descriptor implementations in the DENOPTIM packages.
     * This method excludes descriptors
     * that have been listed in {@link #rejectedDescriptors}.
     * @param requiredDescriptors list of descriptor short names that we want
     * to obtain. All the rest will be ignored. This parameter can be null, in 
     * which case we'll return all the descriptors.
     * @return the list of descriptor information bundles.
     * @throws DENOPTIMException
     */
    public static List<DescriptorForFitness> findAllDENOPTIMDescriptors(
            Set<String> requiredDescriptors) throws DENOPTIMException
    {
        List<String> classNames = new ArrayList<String>();
        classNames.addAll(getClassNamesToDenoptimDescriptors());
        return findDescriptorImplementations(requiredDescriptors, classNames);
    }
//------------------------------------------------------------------------------

	/**
	 * Searches for descriptor implementations. Searches in both CDK and
	 * DENOPTIM packages.
	 * This method excludes descriptors
	 * that have been listed in {@link #rejectedDescriptors}.
	 * @param requiredDescriptors list of descriptor short names that we want
	 * to obtain. All the rest will be ignored. This parameter can be null, in 
	 * which case we'll return all the descriptors.
	 * @return the list of descriptor information bundles.
	 * @throws DENOPTIMException
	 */
	public static List<DescriptorForFitness> findAllDescriptorImplementations(
			Set<String> requiredDescriptors) throws DENOPTIMException
	{
		List<String> classNames = new ArrayList<String>();
		classNames.addAll(getClassNamesToCDKDescriptors());
        classNames.addAll(getClassNamesToDenoptimDescriptors());
        return findDescriptorImplementations(requiredDescriptors, classNames);
	}

//------------------------------------------------------------------------------

    /**
     * Searches for descriptor implementations. This method excludes descriptors
     * that have been listed in {@link #rejectedDescriptors}.
     * @param requiredDescriptors list of descriptor short names that we want
     * to obtain. All the rest will be ignored. This parameter can be null, in 
     * which case we'll return all the descriptors.
     * @param classNames the classnames of the implementations.
     * @return the list of descriptor information bundles.
     * @throws DENOPTIMException
     */
    public static List<DescriptorForFitness> findDescriptorImplementations(
            Set<String> requiredDescriptors, List<String> classNames) 
                    throws DENOPTIMException
    {	
		//We use the engine to get the instances of descriptors calculators
		DescriptorEngine engine = new DescriptorEngine(classNames, null);
		List<IDescriptor> iDescs =  engine.instantiateDescriptors(classNames);
        
		List<DescriptorForFitness> chosenOnes = 
				new ArrayList<DescriptorForFitness>();
		Set<String> chosenOnesShortNames = new TreeSet<String>();
		// NB: Descriptors names are supposed to be unique in CDK, but we check
		// for duplicates. This, in case of additional descriptors that do not
		// belong to CDK and are added to the list.
        Map<String,String> unq = new HashMap<String,String>();
		for (int i=0; i<classNames.size(); i++)
		{
			String className = classNames.get(i);
			String[] descrNames = iDescs.get(i).getDescriptorNames();
			String simpleName = iDescs.get(i).getClass().getSimpleName();
			
			if (descrNames != null)
			{
				for (int j=0; j<descrNames.length;j++)
				{   
					String descName = descrNames[j];
					
					if (unq.containsKey(descName))
					{
						String msg = "Descriptor '" + descName + "' in part of " 
								+ simpleName
								+ " but its name was already used in "
								+ unq.get(descName);
						throw new DENOPTIMException(msg);
					}
					unq.put(descName,simpleName);
					boolean isChosen = false;
					if (requiredDescriptors == null)
					{
						isChosen = true;
					} else {
						for (String requiredDescName : requiredDescriptors)
						{
							if (descName.equals(requiredDescName))
							{
								isChosen = true;
								break;
							}
						}
					}
					
					// We reject selected descriptor implementations that might
					// have shown to be unreliable
					for (String rejectedDescName : rejectedDescriptors)
					{
					    if (descName.equals(rejectedDescName))
                        {
                            isChosen = false;
                            break;
                        }
					}
					
					if (isChosen)
					{
						chosenOnesShortNames.add(simpleName);
						IDescriptor impl = iDescs.get(i);
						IImplementationSpecification implSpec = 
						        impl.getSpecification();
						DescriptorForFitness d = null;
						if (impl instanceof IDenoptimDescriptor)
						{
						    IDenoptimDescriptor dnpDescImpl = 
						            (IDenoptimDescriptor) impl;
						    d = new DescriptorForFitness(
	                                descName, className,impl, j,
	                                //engine.getDictionaryType(implSpec),
	                                null,
	                                dnpDescImpl.getDictionaryClass(),
	                                dnpDescImpl.getDictionaryDefinition(),
	                                dnpDescImpl.getDictionaryTitle());
						} else {
						    d = new DescriptorForFitness(
								descName, className,impl, j,
								//engine.getDictionaryType(implSpec),
								null,
								engine.getDictionaryClass(implSpec),
                                engine.getDictionaryDefinition(
                                        implSpec.getSpecificationReference()),
                                engine.getDictionaryTitle(
                                        implSpec.getSpecificationReference()));
						}
						chosenOnes.add(d);
					}
				}
			}
		}
		return chosenOnes;
	}
	
//------------------------------------------------------------------------------

}
