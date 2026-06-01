#!/bin/bash -l 
# We mean to run this as interactive shell, hence the '-l'
#
# Script meant for building the conda package locally. It also creates an
# conda environment where the local package is installed and ready for
# testing. 
# 
# WARNING: run this script from the 'conda' folder of DENOPTIM's distribution 
# folder.
#
# WARNING: now works only on osx-64. Se TODO label below.
# 

# conda-build must be in base for `conda build` (conda >=26.3 plugin model)
if ! conda build --help &>/dev/null; then
  echo "Installing build tools into base environment..."
  conda install -n base -c conda-forge conda-build anaconda-client conda-libmamba-solver -y
  if [ $? -ne 0 ]; then
    echo "ERROR: Could not install conda-build in base"
    exit -1
  fi
fi

# Get version from pom.xml
pkgVersion=$(grep -i -m 1 "<version>" ../pom.xml | sed 's/ *<.*>\(.*\)<\/.*>/\1/')
nDots=$(echo $pkgVersion | grep -o "\." | grep -c "\.")
if [ $nDots -ne 2 ]; then
  echo "ERROR: Unexpected package version format: $pkgVersion"
  exit -1
fi

# Short croot avoids extra-long paths on macOS (FileVault/encryption can worsen placeholders)
croot="${CONDA_BUILD_CROOT:-/tmp/conda-bld-denoptim}"
mkdir -p "$croot"

# Build the package
DENOPTIM_VERSION="$pkgVersion" conda build . --croot "$croot" --no-anaconda-upload
if [ $? -ne 0 ]; then 
  echo "ERROR: Non-zero exit status from conda"
  exit -1
fi

# Make a local channel.
# This is a workaround to ensure the installation of the local package
# is coupled with the installation of dependencies. See this post for
# further details:
# https://github.com/conda/conda/issues/1884#issuecomment-868488922
#
pkgArchive=$(DENOPTIM_VERSION="$pkgVersion" conda build . --croot "$croot" --output | tail -n 1)
if [ ! -f "$pkgArchive" ]; then
  echo "ERROR: Package tarball not found: $pkgArchive"
  exit 1
fi
echo "============================================================"
echo " "
echo "Package built! Tarball at $pkgArchive"
echo " "

# Make local channel
dirName=/tmp/local-channel-for-testing-conda-package
dummyEnvName=envForTestingCondaBuild
#TODO: change hard-coded OS
mkdir -p "$dirName/osx-64"
if [ $? -ne 0 ]; then
  echo "ERROR: Could not create folder for local channel."
  exit -1
fi
cp "$pkgArchive" "$dirName/osx-64"
conda index "$dirName"
conda remove --name "$dummyEnvName" --all
conda create --name "$dummyEnvName"
if [ $? -ne 0 ]; then
  echo "ERROR: Could not create dummy env."
  exit -1
fi
conda activate "$dummyEnvName"
if [ $? -ne 0 ]; then
  echo "ERROR: Could not activate dummy env."
  exit -1
fi
conda install -c file://"$dirName" denoptim

echo "=================================================================="
echo "All done: ready for you to test environment $dummyEnvName"
echo "Remember to cleanup: 'conda remove --name $dummyEnvName --all'"
exit 0
