#!/bin/bash -euo

# Assert version is set
if [ "None" == "$PKG_VERSION" ]; then
  echo "PKG_VERSION=$PKG_VERSION"
  exit 1
fi

cd "$SRC_DIR"
mkdir -p "$PREFIX/lib" "$PREFIX/bin"

mvn clean package 
cp "$SRC_DIR/target/denoptim-$PKG_VERSION-jar-with-dependencies.jar" "$PREFIX/lib"

cd "$SRC_DIR/src/main/python"
$PYTHON -m pip install --no-deps .

cd "$SRC_DIR"
echo '#!/bin/bash' > "$PREFIX/bin/denoptim"
echo '"'$JAVA_HOME'/bin/java" -jar "'$PREFIX'/lib/denoptim-'$PKG_VERSION'-jar-with-dependencies.jar" "$@"' >> "$PREFIX/bin/denoptim"

chmod +x "${PREFIX}/bin/denoptim"
