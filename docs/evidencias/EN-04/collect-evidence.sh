#!/usr/bin/env bash
# collect-evidence.sh - Collects all evidence for EN-04
# Usage: ./collect-evidence.sh
# Generates: EN-04-evidencias.zip with POSIX paths and SHA256SUMS

set -euo pipefail

# Configuration
EVIDENCE_ROOT="docs/evidencias/EN-04"
OUTPUT_ZIP="EN-04-evidencias.zip"
SHA256SUMS="SHA256SUMS"
TMP_DIR=$(mktemp -d)
MASK_PATTERNS=(
    "AIza[0-9A-Za-z_-]{35}"
    "AQ\.[0-9A-Za-z_-]+"
    "GOOGLE_AI_API_KEY[[:space:]]*=[[:space:]]*[^[:space:]]+"
)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() { echo -e "${GREEN}[INFO]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# Secret masking function
mask_secrets() {
    local file="$1"
    local output="$2"
    cp "$file" "$output"
    for pattern in "${MASK_PATTERNS[@]}"; do
        # Use sed with extended regex
        sed -i -E "s/$pattern/***MASKED***/g" "$output" 2>/dev/null || true
    done
}

# Check git status - ensure no uncommitted changes
check_git_status() {
    log_info "Checking git status..."
    if ! git diff --quiet || ! git diff --cached --quiet; then
        log_error "Working tree has uncommitted changes. Commit or stash before collecting evidence."
        git status --short
        return 1
    fi
    log_info "Git working tree is clean."
}

# Run gradle clean check once
run_gradle_clean_check() {
    log_info "Running ./gradlew clean check --no-daemon..."
    if ! ./gradlew clean check --no-daemon; then
        log_error "Gradle clean check failed"
        return 1
    fi
    log_info "Gradle clean check completed successfully."
}

# Clean up Docker containers and volumes
cleanup_docker() {
    log_info "Cleaning up Docker containers..."
    docker container prune -f >/dev/null 2>&1 || true
    docker volume prune -f >/dev/null 2>&1 || true
    log_info "Docker cleanup completed."
}

# Verify CI run
verify_ci_run() {
    log_info "Verifying latest CI run on GitHub..."
    local run_id
    run_id=$(gh run list --branch sprint-1 --limit 1 --json databaseId --jq '.[0].databaseId' 2>/dev/null)
    if [[ -z "$run_id" || "$run_id" == "null" ]]; then
        log_warn "Could not find CI run for sprint-1 branch"
        return 0
    fi

    local result
    result=$(gh run view "$run_id" --json headSha,conclusion,jobs --jq '{headSha, conclusion, jobs: [.jobs[] | {name, conclusion}]}' 2>/dev/null)
    echo "$result" > "$EVIDENCE_ROOT/01-entorno/gh-run-view-final.json"

    local conclusion
    # Use jq if available, otherwise parse with grep/sed (extract only top-level conclusion)
    if command -v jq >/dev/null 2>&1; then
        conclusion=$(echo "$result" | jq -r '.conclusion')
    else
        # Fallback: extract top-level conclusion from JSON without jq
        # Match the first "conclusion" at the start of the object (before "jobs")
        conclusion=$(echo "$result" | sed -n 's/.*"conclusion":"\([^"]*\)".*/\1/p' | head -1)
    fi
    if [[ "$conclusion" != "success" ]]; then
        log_error "Latest CI run ($run_id) concluded with: $conclusion"
        return 1
    fi
    log_info "CI run $run_id passed successfully."
    echo "$result"
}

# Collect all evidence files (Windows-compatible: uses cp instead of rsync)
collect_files() {
    log_info "Collecting evidence files..."

    # Copy entire evidence directory to temp location (Windows-compatible)
    mkdir -p "$TMP_DIR/evidence"
    # Copy all files except the zip, SHA256SUMS, and this script
    find "$EVIDENCE_ROOT" -type f ! -name "$OUTPUT_ZIP" ! -name "SHA256SUMS" ! -name "collect-evidence.sh" | while read -r file; do
        rel_path="${file#$EVIDENCE_ROOT/}"
        mkdir -p "$TMP_DIR/evidence/$(dirname "$rel_path")"
        cp "$file" "$TMP_DIR/evidence/$rel_path"
    done

    # Mask secrets in all text files
    log_info "Masking secrets in evidence files..."
    find "$TMP_DIR/evidence" -type f \( -name "*.txt" -o -name "*.log" -o -name "*.md" -o -name "*.json" -o -name "*.xml" \) | while read -r file; do
        mask_secrets "$file" "$file.masked"
        mv "$file.masked" "$file"
    done

    # Also mask in test-results XML files
    find "$TMP_DIR/evidence" -type f -name "*.xml" | while read -r file; do
        mask_secrets "$file" "$file.masked"
        mv "$file.masked" "$file"
    done

    log_info "Secrets masked in all evidence files."
}

# Generate SHA256SUMS
generate_sha256sums() {
    log_info "Generating SHA256SUMS..."
    local original_pwd=$(pwd)
    cd "$TMP_DIR/evidence"
    # Use find -print0 and xargs -0 to handle filenames with spaces
    find . -type f ! -name "SHA256SUMS" -print0 | sort -z | xargs -0 sha256sum > "$original_pwd/$EVIDENCE_ROOT/$SHA256SUMS"
    cd - >/dev/null
    log_info "SHA256SUMS generated at $original_pwd/$EVIDENCE_ROOT/$SHA256SUMS"
}

# Create final zip with POSIX paths (Windows-compatible: uses PowerShell Compress-Archive)
create_zip() {
    log_info "Creating $OUTPUT_ZIP with POSIX paths..."

    # Use PowerShell Compress-Archive on Windows, zip on Linux
    if command -v zip >/dev/null 2>&1; then
        cd "$TMP_DIR"
        zip -r -X "$OLDPWD/$OUTPUT_ZIP" evidence/
        cd - >/dev/null
    else
        # Windows: use PowerShell
        local ps_script="
            \$source = '$TMP_DIR/evidence'
            \$dest = '$OLDPWD/$OUTPUT_ZIP'
            if (Test-Path \$dest) { Remove-Item \$dest -Force }
            Compress-Archive -Path \$source\* -DestinationPath \$dest -Force
        "
        powershell.exe -NoProfile -Command "$ps_script"
    fi

    log_info "Created $OUTPUT_ZIP"

    # Verify zip contents
    log_info "Verifying zip contents..."
    unzip -l "$OUTPUT_ZIP" | head -30
}

# Final secret scan on the zip
secret_scan_zip() {
    log_info "Scanning zip for secrets..."
    # Extract to temp and scan
    local scan_dir=$(mktemp -d)
    unzip -q "$OUTPUT_ZIP" -d "$scan_dir"

    local found=0
    for pattern in "${MASK_PATTERNS[@]}"; do
        if grep -rE "$pattern" "$scan_dir" 2>/dev/null | grep -v "MASKED" | head -5; then
            found=1
        fi
    done

    rm -rf "$scan_dir"

    if [[ $found -eq 1 ]]; then
        log_error "SECRETS FOUND IN ZIP! Aborting."
        return 1
    fi
    log_info "No secrets found in zip."
}

# Main execution
main() {
    log_info "=== EN-04 Evidence Collection Started ==="
    log_info "Evidence root: $EVIDENCE_ROOT"
    log_info "Output zip: $OUTPUT_ZIP"
    local original_pwd=$(pwd)

    # Check git status
    check_git_status || exit 1

    # Run gradle clean check
    run_gradle_clean_check || exit 1

    # Cleanup docker
    cleanup_docker

    # Verify CI run
    verify_ci_run || exit 1

    # Collect files
    collect_files

    # Generate SHA256SUMS
    generate_sha256sums

    # Copy SHA256SUMS to temp for zip
    cp "$original_pwd/$EVIDENCE_ROOT/$SHA256SUMS" "$TMP_DIR/evidence/$SHA256SUMS"

    # Create zip
    create_zip

    # Secret scan
    secret_scan_zip || exit 1

    # Cleanup
    rm -rf "$TMP_DIR"

    log_info "=== EN-04 Evidence Collection Completed Successfully ==="
    log_info "Output: $OUTPUT_ZIP"
    log_info "SHA256SUMS: $EVIDENCE_ROOT/$SHA256SUMS"
}

# Run main
main "$@"