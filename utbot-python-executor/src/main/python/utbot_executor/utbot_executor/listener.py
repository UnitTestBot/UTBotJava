import logging
import os
import socket
import traceback

from utbot_executor.deep_serialization.memory_objects import PythonSerializer
from utbot_executor.parser import parse_request, serialize_response, ExecutionFailResponse
from utbot_executor.executor import PythonExecutor
from utbot_executor.utils import TraceMode

RECV_SIZE = 2**15


class PythonExecuteServer:
    def __init__(
            self,
            hostname: str,
            port: int,
            coverage_hostname: str,
            coverage_port: int,
            trace_mode: TraceMode,
            send_coverage: bool
            ):
        logging.info('PythonExecutor is creating...')
        self.clientsocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.clientsocket.connect((hostname, port))
        self.executor = PythonExecutor(coverage_hostname, coverage_port, trace_mode, send_coverage)

    def run(self) -> None:
        logging.info('PythonExecutor is ready...')
        try:
            self.handler()
        finally:
            self.clientsocket.close()

    def handler(self) -> None:
        logging.info('Start working...')

        while True:
            command = self.clientsocket.recv(4)

            if command == b'STOP':
                break
            if command == b'DATA':
                message_size = int(self.clientsocket.recv(16).decode())
                logging.debug('Got message size: %d bytes', message_size)
                message_body = bytearray()

                while len(message_body) < message_size:
                    message = self.clientsocket.recv(
                            min(RECV_SIZE, message_size - len(message_body))
                            )
                    message_body += message
                    logging.debug('Message: %s, size: %d', message, len(message))
                    logging.debug(
                        'Update content, current size: %d / %d bytes',
                        len(message_body),
                        message_size,
                    )

                try:
                    request = parse_request(message_body.decode())
                    logging.debug('Parsed request: %s', request)
                    response = self.executor.run_function(request)
                except Exception as ex:
                    logging.debug('Exception: %s', traceback.format_exc())
                    response = ExecutionFailResponse('fail', traceback.format_exc())

                logging.debug('Response: %s', response)

                try:
                    serialized_response = serialize_response(response)
                except Exception as ex:
                    serialized_response = serialize_response(ExecutionFailResponse('fail', ''))
                finally:
                    PythonSerializer().clear()

                logging.debug('Serialized response: %s', serialized_response)

                bytes_data = serialized_response.encode()
                logging.debug('Encoded response: %s', bytes_data)
                response_size = str(len(bytes_data))
                self.clientsocket.send((response_size + os.linesep).encode())

                sent_size = 0
                while len(bytes_data) > sent_size:
                    sent_size += self.clientsocket.send(bytes_data[sent_size:])

                logging.debug('Sent all data')
        logging.info('All done...')
