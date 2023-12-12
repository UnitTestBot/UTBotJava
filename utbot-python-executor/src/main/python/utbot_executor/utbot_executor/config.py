import dataclasses

from utbot_executor.utils import TraceMode


@dataclasses.dataclass
class HostConfig:
    hostname: str
    port: int


@dataclasses.dataclass
class CoverageConfig:
    server: HostConfig
    trace_mode: TraceMode
    send_coverage: bool


@dataclasses.dataclass
class LoggingConfig:
    logfile: str | None
    loglevel: int


@dataclasses.dataclass
class Config:
    server: HostConfig
    coverage: CoverageConfig
    logging: LoggingConfig
    state_assertions: bool
