#!/bin/bash
# Deploy Axentra to Hangar (hangar.papermc.io)
# Usage: ./deploy-hangar.sh [version]
#
# Requires HANGAR_API_KEY environment variable to be set.
# Generate one at: https://hangar.papermc.io/auth/settings/api-keys

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_SLUG="Axentra"
HANGAR_API="https://hangar.papermc.io/api/v1"

# Get version from argument or pom.xml
if [ -n "$1" ]; then
    VERSION="$1"
else
    VERSION=$(grep -m1 '<version>' "$SCRIPT_DIR/pom.xml" | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
fi

JAR_FILE="$SCRIPT_DIR/target/axentra-${VERSION}.jar"

# Validate
if [ -z "$HANGAR_API_KEY" ]; then
    echo "ERROR: HANGAR_API_KEY environment variable is not set."
    echo "Set it with: export HANGAR_API_KEY='your-key-here'"
    exit 1
fi

if [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: JAR not found at $JAR_FILE"
    echo "Run ./mvnw package -DskipTests first."
    exit 1
fi

echo "Deploying Axentra v${VERSION} to Hangar..."

# Step 1: Authenticate
echo "Authenticating..."
AUTH_RESPONSE=$(curl -s -X POST "${HANGAR_API}/authenticate?apiKey=${HANGAR_API_KEY}")

JWT=$(echo "$AUTH_RESPONSE" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

if [ -z "$JWT" ]; then
    echo "ERROR: Authentication failed."
    echo "$AUTH_RESPONSE"
    exit 1
fi

echo "Authenticated successfully."

# Step 2: Prepare upload files in a clean temp directory (avoids spaces-in-path issues)
WORK_DIR="D:/hangar_upload_temp"
mkdir -p "$WORK_DIR"

cp "$JAR_FILE" "$WORK_DIR/axentra-${VERSION}.jar"

cat > "$WORK_DIR/version.json" <<ENDJSON
{
    "version": "${VERSION}",
    "pluginDependencies": {},
    "platformDependencies": {
        "PAPER": ["1.21","1.21.1","1.21.2","1.21.3","1.21.4","1.21.5","1.21.6","1.21.7","1.21.8","1.21.9","1.21.10","1.21.11"]
    },
    "description": "v${VERSION} - Staff Management & Modular System\n\n### Staff Management\n- /duty - Toggle on/off duty with tracked sessions\n- /staffmode - Staff tools hotbar with auto-vanish and fly\n- /report - Player reports with cooldown and chat snapshots\n- /reports - GUI to view and claim open reports\n- /staffstats - Staff performance statistics\n- /stafflist - Online staff with duty status\n\n### Improvements\n- Permission-based announcements (notify.kick, notify.ban)\n- Config auto-update on plugin update\n- 13 toggleable modules in config.yml\n- New website at axentramc.com",
    "channel": "Release",
    "files": [
        {
            "platforms": ["PAPER"]
        }
    ]
}
ENDJSON

# Step 3: Upload
echo "Uploading v${VERSION}..."
UPLOAD_RESPONSE=$(curl -s -X POST "${HANGAR_API}/projects/${PROJECT_SLUG}/upload" \
    -H "Authorization: ${JWT}" \
    -F "versionUpload=@${WORK_DIR}/version.json;type=application/json" \
    -F "files=@${WORK_DIR}/axentra-${VERSION}.jar")

# Cleanup
rm -rf "$WORK_DIR"

# Check result
if echo "$UPLOAD_RESPONSE" | grep -q '"url"'; then
    echo ""
    echo "Successfully deployed Axentra v${VERSION} to Hangar!"
    echo "View at: https://hangar.papermc.io/OlliesWorld/Axentra/versions/${VERSION}"
elif echo "$UPLOAD_RESPONSE" | grep -q 'isHangarApiException\|"title"'; then
    echo "ERROR: Upload failed!"
    echo "$UPLOAD_RESPONSE"
    exit 1
else
    echo "Response: $UPLOAD_RESPONSE"
    echo "View at: https://hangar.papermc.io/OlliesWorld/Axentra/versions/${VERSION}"
fi
