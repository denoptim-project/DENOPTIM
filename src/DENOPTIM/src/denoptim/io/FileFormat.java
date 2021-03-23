package denoptim.io;


public enum FileFormat {
    
    JSON, SDF;
        
    private String extension = "";
    
    static {
        SDF.extension = "sdf";
        JSON.extension = "json";
    }
    
    
    public static FileFormat fromString(String extension) {
        FileFormat ff = SDF;
        switch (extension.toUpperCase())
        {
            case "SDF":
                ff = SDF;
                break;
            case "JSON":
                ff = JSON;
                break;
        }
        return ff;
    }
    
    /**
     * @return 0:scaffold, 1:fragment, 2:capping group
     */
    public String getExtension()
    {
        return extension;
    }
}
