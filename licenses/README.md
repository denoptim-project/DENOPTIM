# Managment of licenses of third-party code

This folder collects the tools to obtain and manipulate the licenses for the libraries DENOPTIM depends on. The work is done by the `License Maven Plugin` pretty much automatically, though additional information must be provided for packages that do not properly define their license. In such case, you will have to edit both the `resolve_missing.properties` file (Maven plugin's parameter `missingFile`, which is used to list the libraries from third-parties, and the `manually_configured_licenses.xml` (parameter `licensesConfigFile`), which is used to download the license files. This is redundant, but so far it's how things work.

* This command create the list of licenses for all dependencies
	```
	mvn license:add-third-party
	```

* This command downloads the licence files. Only a unique version of each used licence is downloaded. For example, there will be only one file with the LGPL v2.1 license. 
	```
	mvn license:download-licenses
	```

