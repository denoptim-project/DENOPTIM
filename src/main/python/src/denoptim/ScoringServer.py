"""ScoringServer

This module provides a shortcut to create socket servers able to provide
scores according to the conventions of DENOPTIM
(https://github.com/denoptim-project/DENOPTIM). Namely,
    * use a JSON formatted string (UTF-8) for both request and response,
    * use conventional JSON member keys (See ``JSON_KEY_*`` attributes).

To use this module, first import it:
    ``from denoptim import ScoringServer``
then you can start a server that runs ``some_function`` to calculate the score
for any JSON-formatted request sent to ``hostname:port``. The JSON
request is passed to ``some_function`` so the definition of such function
controls what information is used to calculate the score:
    ``ScoringServer.start(some_function, hostname, port)``
Once, you are done using the server, you must shut it down like this:
    ``ScoringServer.stop(hostname, port)``

"""
import socket
import sys
import json
import socketserver
from threading import Thread

MY_NAME = "ScoringServer"

# NB: the strings defined here are part of a convention.
JSON_KEY_SMILES = 'SMILES'
JSON_KEY_SCORE = 'SCORE'
JSON_KEY_ERROR = 'ERROR'


class ScoreError(Exception):
    """Formats and exception as a JSON object adhering to DENOPTIM's convention.

    The JSON format is used to communicate any result to the client waiting for
    an answer, i.e., waiting for a score. If an exception occurs, we must
    communicate that the score cannot be produced and why. This method creates
    the JSON response that conveys this information to DENOPTIM. Such response
    is accessible as ``self.json_errmsg``"""
    def __init__(self, message):
        super().__init__(message)
        self.json_errmsg = {JSON_KEY_ERROR: f"#{MY_NAME}: {message}"}


def start(scoring_function, host: str = "localhost", port: int = 0xf17):
    """Starts a separate thread that creates and runs the server.

    Parameters
    ----------
    scoring_function :
        The function the server should use to calculate the score for a given
        request.
    host : str
        Either a hostname in internet domain notation like ``host.name.org`` or
        an IPv4 address like ``100.50.200.5``.
    port : int
        the port number."""
    serverThread = Thread(target=__run_server, args=[scoring_function,
                                                     host, port])
    serverThread.start()


def __run_server(scoring_function, host: str = "localhost", port: int = 0xf17):
    """Start the server and keeps it running forever.

    Parameters
    ----------
    scoring_function :
        The function the server should use to calculate the score for a given
        request.
    host : str
        Either a hostname in internet domain notation like ``host.name.org`` or
        an IPv4 address like ``100.50.200.5``.
    port : int
        the port number.
    """
    CustomHandler = __make_score_request_handler(scoring_function)
    with socketserver.ThreadingTCPServer(
            (host, port),
            CustomHandler
    ) as server:
        server.serve_forever()


def __make_score_request_handler(scoring_function):
    """Factory creating a customized request handler from the given function.

    Parameters
    ----------
    scoring_function :
        The function the handler should use to calculate the score for a given
        request."""
    class ScoreRequestHandler(socketserver.StreamRequestHandler):
        def handle(self):
            message = self.rfile.read().decode('utf8')
            if 'shutdown' in message:
                self.server.shutdown()
                return
            try:
                json_msg = json.loads(message)
            except json.decoder.JSONDecodeError as e:
                raise ScoreError(f"Invalid JSON: {e}")
            try:
                score = scoring_function(json_msg)
                answer = json.dumps({
                    JSON_KEY_SCORE: score,
                })
            except ScoreError as e:
                print('Error:', e, file=sys.stderr)
                answer = json.dumps(e.json_errmsg)
            finally:
                answer += '\n'
                self.wfile.write(answer.encode('utf8'))
    return ScoreRequestHandler


def stop(host: str = "localhost", port: int = 0xf17):
    """Sends a shutdown request to the server to close it for good.

    Parameters
    ----------
    host : str
        Either a hostname in internet domain notation like ``host.name.org`` or
        an IPv4 address like ``100.50.200.5``.
    port : int
        the port number.
    """
    try:
        socket_connection = socket.create_connection((host, port))
        socket_connection.send('shutdown'.encode('utf8'))
    except Exception as e:
        raise Exception('Could not communicate with socket server.', e)
