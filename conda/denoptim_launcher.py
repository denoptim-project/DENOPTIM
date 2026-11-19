#!/usr/bin/env python
# DENOPTIM
# Copyright (C) 2026 Marco Foscato <marco.foscato@uib.no>
# Licensed under the GNU Affero General Public License v3.

"""Start the RDKit socket server when it is not already running, then the JAR.

The process that starts the server stops it when DENOPTIM exits. A server that
was already listening is left running.
"""

import os
import socket
import subprocess
import sys
import time

HOST = "127.0.0.1"
PORT = 56100
STARTUP_TIMEOUT_S = 30.0


def port_open(host, port):
    try:
        with socket.create_connection((host, port), 0.5):
            return True
    except OSError:
        return False


def start_server(host, port, pythonpath):
    """Start ``python -m denoptim_rdkit`` and wait until it accepts connections.

    Returns the process when this launcher owns it, or ``None`` when another
    process took the port first. Raises ``SystemExit`` when the server does not
    come up.
    """
    env = os.environ.copy()
    previous = env.get("PYTHONPATH", "")
    env["PYTHONPATH"] = (
        pythonpath if previous == "" else pythonpath + os.pathsep + previous)
    server = subprocess.Popen(
        [sys.executable, "-m", "denoptim_rdkit",
         "--host", host, "--port", str(port)],
        env=env)
    deadline = time.time() + STARTUP_TIMEOUT_S
    while time.time() < deadline:
        if port_open(host, port):
            return server
        if server.poll() is not None:
            break
        time.sleep(0.1)
    if port_open(host, port):
        return None
    if server.poll() is None:
        server.terminate()
    sys.stderr.write(
        "RDKit server did not open %s:%s.\n" % (host, port))
    raise SystemExit(1)


def stop_server(server):
    if server is None or server.poll() is not None:
        return
    server.terminate()
    try:
        server.wait(timeout=5)
    except subprocess.TimeoutExpired:
        server.kill()


def main():
    jar = os.environ.get("DENOPTIM_JAR")
    pythonpath = os.environ.get("DENOPTIM_PYTHONPATH")
    if not jar or not pythonpath:
        sys.stderr.write(
            "DENOPTIM_JAR and DENOPTIM_PYTHONPATH must be set.\n")
        return 1
    host = os.environ.get("DENOPTIM_RDKIT_HOST", HOST)
    port = int(os.environ.get("DENOPTIM_RDKIT_PORT", str(PORT)))

    server = None
    if not port_open(host, port):
        server = start_server(host, port, pythonpath)
    try:
        completed = subprocess.run(["java", "-jar", jar] + sys.argv[1:])
        return completed.returncode
    finally:
        stop_server(server)


if __name__ == "__main__":
    sys.exit(main())
