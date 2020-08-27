.PHONY: clean all lib

JC = javac
JFLAGS = -encoding utf-8 -d .
JAR = jar cvfm


lib: lib/DENOPTIM.jar

all: DenoptimGA.jar DenoptimRND.jar DenoptimCG.jar



ALLJARS := $(filter-out lib/DENOPTIM.jar,$(wildcard lib/*.jar))
manifest.mf: $(ALLJARS)
	echo "Manifest-Version: 1.0" > $@
	echo "Class-Path: $^" >> $@

## extract the classpath jars from the manifest
get_cp = $(shell grep 'Class-Path:' $(1) | cut -d' ' -f2- | tr '[ ]' '[:]')
## find all the source files
get_src = $(shell find ../src/$(basename $(1))/src -name *.java)
## construct the lowercase directory name from the jar name
get_dirname = $(shell echo $(basename $(1)) | tr 'A-Z' 'a-z')

## Build rule for the main DENOPTIM library
DENOPTIM_SRC := $(call get_src,DENOPTIM.jar)
lib/DENOPTIM.jar: manifest.mf $(DENOPTIM_SRC)
	$(JC) $(JFLAGS) -cp 'lib/*' $(DENOPTIM_SRC)
	$(JAR) $@ $< denoptim > /dev/null
	rm -r denoptim
	
## pattern build rule for all Denoptim*.jar
.SECONDEXPANSION: ## needed to use the $@ variable in prereqs
Denoptim%.jar: manifest%.mf lib/DENOPTIM.jar $$(call get_src,$$@)
	$(JC) $(JFLAGS) -cp $(call get_cp,$<) $(call get_src,$@)
	$(JAR) $@ $< $(call get_dirname,$@) > /dev/null
	rm -r $(call get_dirname,$@)
	


	
clean:
	rm -f manifest.mf
	rm -f lib/DENOPTIM.jar
	rm -f *.jar
