import json
import requests

JAVA_ARTIFACTS_URL="https://api.github.com/repos/UnitTestBot/UTBotJava/actions/artifacts"

request = requests.get(url = JAVA_ARTIFACTS_URL)
data = request.json()
artifacts = data['artifacts']

for artifact in artifacts:
    if "utbot-cli" in artifact['name']:
        print(artifact['archive_download_url'])
        break
        
