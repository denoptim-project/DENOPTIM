#!/bin/bash -euo

# Assert version is set
if [ "None" == "$PKG_VERSION" ]; then
  echo "PKG_VERSION=$PKG_VERSION"
  exit 1
fi

# Build
cd "$SRC_DIR"
mkdir -p "$PREFIX/lib" "$PREFIX/bin"

mvn -version

# Skip unit tests here: conda-build uses a long PREFIX that breaks Surefire on some OSes.
# Run the full suite with mvn test (e.g. GitHub maven workflow). Recipe test: denoptim -v.
mvn clean package -DskipTests

# Install the JAR, the RDKit socket server, and the launcher.
cp "$SRC_DIR/target/denoptim-$PKG_VERSION-jar-with-dependencies.jar" "$PREFIX/lib"
cp -R "$SRC_DIR/src/main/python/denoptim_rdkit" "$PREFIX/lib/"
cp "$SRC_DIR/conda/denoptim_launcher.py" "$PREFIX/bin/denoptim-launch.py"

# The launcher starts the RDKit server when port 56100 is free, then the JAR.
# It stops only a server that this process started.
cat > "$PREFIX/bin/denoptim" <<EOF
#!/bin/bash
export DENOPTIM_JAR="$PREFIX/lib/denoptim-$PKG_VERSION-jar-with-dependencies.jar"
export DENOPTIM_PYTHONPATH="$PREFIX/lib"
exec python "$PREFIX/bin/denoptim-launch.py" "\$@"
EOF
chmod +x "$PREFIX/bin/denoptim" "$PREFIX/bin/denoptim-launch.py"
