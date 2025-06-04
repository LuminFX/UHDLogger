#!/bin/bash

# Usage: ./run_uhd.sh <rate> <recv_frames> <send_frames> <gain> <duration> <freq> <uhd_path> <log_path>

RATE="$1"
RECV_FRAMES="$2"
SEND_FRAMES="$3"
GAIN="$4"
DURATION="$5"
FREQ="$6"
UHD_PATH="$7"
LOG_DIR="$8"

echo "UHD_PATH: $UHD_PATH"
echo "LOG_DIR: $LOG_DIR"

# Check for required arguments
if [ -z "$RATE" ] || [ -z "$RECV_FRAMES" ] || [ -z "$SEND_FRAMES" ] || [ -z "$GAIN" ] || \
   [ -z "$DURATION" ] || [ -z "$FREQ" ] || [ -z "$UHD_PATH" ] || [ -z "$LOG_DIR" ]; then
    echo "Usage: $0 <rate> <recv_frames> <send_frames> <gain> <duration> <freq> <uhd_path> <log_path>"
    exit 1
fi

mkdir -p "$LOG_DIR"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

EXE_PATH="$UHD_PATH/host/build/examples/rx_samples_to_file"
LOG_FILE="$LOG_DIR/uhd_log_${TIMESTAMP}_${RATE}_${RECV_FRAMES}_${SEND_FRAMES}.txt"
OUT_FILE="$LOG_DIR/data_${TIMESTAMP}_${RATE}_${RECV_FRAMES}_${SEND_FRAMES}.bin"

if [ ! -f "$EXE_PATH" ]; then
    echo "Error: UHD binary not found at $EXE_PATH"
    exit 2
fi

echo "[*] Running UHD sample capture at ${RATE}..."


"$EXE_PATH" \
    --rate "$RATE" \
    --duration "$DURATION" \
    --freq "$FREQ" \
    --gain "$GAIN" \
    --file "$OUT_FILE" \
    --args "num_recv_frames=${RECV_FRAMES},num_send_frames=${SEND_FRAMES}" \
    2>&1 | tee "$LOG_FILE"

# Validate file size
BYTES_PER_SAMPLE=4
expected_bytes=$(echo "$(printf "%.0f" "$RATE") * $DURATION * $BYTES_PER_SAMPLE" | bc)
actual_bytes=$(stat -c%s "$OUT_FILE" 2>/dev/null || stat -f%z "$OUT_FILE" 2>/dev/null)

if [ -z "$actual_bytes" ]; then
    echo "Could not read output file size for comparison."
else
    percent=$(echo "scale=2; $actual_bytes / $expected_bytes * 100" | bc)
    human_expected=$(numfmt --to=iec --suffix=B "$expected_bytes" 2>/dev/null || echo "$expected_bytes bytes")
    human_actual=$(numfmt --to=iec --suffix=B "$actual_bytes" 2>/dev/null || echo "$actual_bytes bytes")

    echo "File size analysis:"
    echo "  Expected: $human_expected"
    echo "  Actual:   $human_actual"
    echo "  Captured: $percent%"
fi

echo "File written to $OUT_FILE"