# DENOPTIM
# Copyright (C) 2026 Marco Foscato <marco.foscato@uib.no>
# Licensed under the GNU Affero General Public License v3.

"""Line-oriented JSON socket server.

Each connection carries one request and one response. Both are a single JSON
object terminated by a newline. The request names a task; the server looks
that name up and does not otherwise know what the task does.

Request:  {"task": "<name>", "parameters": { ... }}
Response: {"status": "SUCCESS", "result": { ... }}
          {"status": "ERROR", "message": "..."}
"""

import argparse
import json
import logging
import socketserver

from denoptim_rdkit import DEFAULT_HOST, DEFAULT_PORT
from denoptim_rdkit.tasks import TASKS

LOGGER = logging.getLogger("denoptim_rdkit")


def dispatch(request):
    """Run the task named by ``request`` and return the response object."""
    if not isinstance(request, dict):
        return {"status": "ERROR", "message": "Request must be a JSON object."}
    task_name = request.get("task")
    task = TASKS.get(task_name)
    if task is None:
        return {"status": "ERROR",
                "message": "Unknown task '%s'." % task_name}
    parameters = request.get("parameters", {})
    try:
        result = task(parameters)
    except Exception as error:
        LOGGER.exception("Task %s failed", task_name)
        return {"status": "ERROR", "message": str(error)}
    return {"status": "SUCCESS", "result": result}


class _RequestHandler(socketserver.StreamRequestHandler):
    def handle(self):
        raw_line = self.rfile.readline()
        if not raw_line:
            return
        try:
            request = json.loads(raw_line.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as error:
            response = {"status": "ERROR",
                        "message": "Invalid JSON request: %s" % error}
        else:
            response = dispatch(request)
        encoded = (json.dumps(response) + "\n").encode("utf-8")
        self.wfile.write(encoded)
        self.wfile.flush()


class RdkitServer(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True


def serve(host=DEFAULT_HOST, port=DEFAULT_PORT):
    """Listen until the process is stopped."""
    logging.basicConfig(level=logging.INFO,
                        format="%(levelname)s %(name)s: %(message)s")
    with RdkitServer((host, port), _RequestHandler) as server:
        LOGGER.info("Listening on %s:%s", host, port)
        server.serve_forever()


def main(argv=None):
    parser = argparse.ArgumentParser(
        description="RDKit socket server for DENOPTIM.")
    parser.add_argument("--host", default=DEFAULT_HOST,
                        help="address to bind (default: %s)" % DEFAULT_HOST)
    parser.add_argument("--port", type=int, default=DEFAULT_PORT,
                        help="port to bind (default: %s)" % DEFAULT_PORT)
    args = parser.parse_args(argv)
    serve(args.host, args.port)


if __name__ == "__main__":
    main()
