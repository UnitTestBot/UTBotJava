import pkg_resources
import sys


if __name__ == "__main__":
    dependencies = sys.argv[1:]
    pkg_resources.require(dependencies)
