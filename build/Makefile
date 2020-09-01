.PHONY: clean all lib
SHELL := /bin/bash ## ubuntu uses dash by default

JC := javac
JFLAGS := -encoding utf-8 -d .
JAR := jar cvfm


lib: lib/DENOPTIM.jar


MANIFESTS := $(filter-out manifest.mf,$(wildcard *.mf))
ALL := $(subst mf,jar,$(MANIFESTS))
all: $(ALL)

dist: $(ALL) $(wildcard lib/*.jar)
	mkdir -p $@
	cp $? $@
	touch $@
	
images: ../src/GUI/images
	cp -r $< $@
	touch $@
	
clean:
	rm -f manifest.mf
	rm -f lib/DENOPTIM.jar
	rm -f *.jar
	rm -rf dist images

## additional dependencies
SetupBRICS.jar: lib/cdk-1.4.19.jar
CheckpointReader.jar: FragSpaceExplorer.jar
UpdateUID.jar: lib/cdk-1.4.19.jar lib/commons-cli-1.3.1.jar
GUI.jar: DenoptimGA.jar
##

ALLDEPS := $(filter-out lib/DENOPTIM.jar,$(wildcard lib/*.jar))
manifest.mf: $(ALLDEPS)
	echo "Manifest-Version: 1.0" > $@
	echo "Class-Path: $^" >> $@

## extract the classpath jars from the manifest
get_cp = $(shell grep 'Class-Path:' $(1) | cut -d' ' -f2- | tr ' ' ':')
## find all matching source files in ../src or ../src/misc
get_src = $(shell find ../src/{,misc/}$(basename $(1))/src -name '*.java' 2> /dev/null)
## construct the lowercase directory name from the jar name
get_builddir = $(shell echo $(basename $(1)) | tr '[:upper:]' '[:lower:]')

## Build rule for the main DENOPTIM library
DENOPTIM_SRC := $(call get_src,DENOPTIM.jar)
lib/DENOPTIM.jar: manifest.mf $(DENOPTIM_SRC)
	$(JC) $(JFLAGS) -cp 'lib/*' $(DENOPTIM_SRC)
	$(JAR) $@ $< denoptim > /dev/null
	rm -r denoptim
	
## pattern build rule for all other *.jar from their .mf manifest
.SECONDEXPANSION: ## needed to use the $@ variable in prereqs
%.jar: %.mf lib/DENOPTIM.jar $$(call get_src,$$@)
	$(JC) $(JFLAGS) -cp $(call get_cp,$<) $(call get_src,$@) > /dev/null
	$(JAR) $@ $< $(call get_builddir,$@) > /dev/null
	rm -r $(call get_builddir,$@)
	
GUI.jar: GUI.mf lib/DENOPTIM.jar $(call get_src,GUI.jar) images
	$(JC) $(JFLAGS) -cp $(call get_cp,$<) $(call get_src,$@) > /dev/null
	$(JAR) $@ $< $(call get_builddir,$@) images > /dev/null
	rm -r $(call get_builddir,$@)
	
