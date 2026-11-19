# denoptim_rdkit

Socket server that runs RDKit tasks for DENOPTIM. The `denoptim` launcher starts this process only when nothing is already listening on `127.0.0.1:56100`, and it stops only the server it started. Start the server yourself when you want it to stay up across JAR runs.

Use a Python that can `import rdkit`. The `dnp_devel` environment from `environment.yml` is one such environment.

## Start from the source tree

From the DENOPTIM repository root:

```bash
conda activate dnp_devel
PYTHONPATH=src/main/python python -m denoptim_rdkit
```

The process listens on `127.0.0.1:56100` until you stop it with Ctrl-C. Those are the address and port the Java client uses by default, so a JAR started afterwards connects to this process and the launcher leaves it running.

## Start from a conda install

The package is not on the default module path. Point `PYTHONPATH` at the directory that contains `denoptim_rdkit`:

```bash
PYTHONPATH="$CONDA_PREFIX/lib" python -m denoptim_rdkit
```

On Windows the directory is `%CONDA_PREFIX%\Library\lib`.

## Options

```bash
python -m denoptim_rdkit --host 127.0.0.1 --port 56100
```

`--host` defaults to `127.0.0.1` and `--port` defaults to `56100`. Keep those values when the server is meant for the JAR. The client opens `127.0.0.1`, not the name `localhost`.

## Request

Each connection carries one JSON line in and one JSON line out. A `smiles_to_3d` request for ethanol, with embedding seed 17:

```json
{"task": "smiles_to_3d", "parameters": {"smiles": "CCO", "randomSeed": 17}}
```

`smiles` is a non-empty string. `randomSeed` is an integer. The same seed and SMILES produce the same conformation.

Send it to a server that is already listening:

```bash
python -c 'import json,socket; request={"task":"smiles_to_3d","parameters":{"smiles":"CCO","randomSeed":17}}; connection=socket.create_connection(("127.0.0.1",56100)); connection.sendall((json.dumps(request)+"\n").encode()); connection.shutdown(socket.SHUT_WR); print(connection.makefile().readline())'
```

A successful response has `"status": "SUCCESS"` and `result.results`, a V2000 molblock with explicit hydrogens and 3D coordinates. A failure has `"status": "ERROR"` and `message`.
