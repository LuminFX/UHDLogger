#!/bin/sh

GAIN=$1
DURATION=$2
RECV_FRAMES=$3
SEND_FRAMES=$4
FREQUENCY=$5
RATE=$6
CHROOT_PATH=$7
UHD_PATH=$8
LOG_PATH=$9

if [ -z "$GAIN" ] || [ -z "$DURATION" ] || [ -z "$RECV_FRAMES" ] || [ -z "$SEND_FRAMES" ] || [ -z "$FREQUENCY" ] || [ -z "$RATE" ] || [ -z "$CHROOT_PATH" ] || [ -z "$UHD_PATH" ] || [ -z "$LOG_PATH" ]; then
    echo "Usage: $0 <gain> <duration> <recv_frames> <send_frames> <frequency> <rate> <chroot_path> <uhd_path> <log_path>"
    exit 1
fi

echo "[*] Mounting..."

mkdir -p "$CHROOT_PATH/dev/shm" "$CHROOT_PATH/dev/pts" "$CHROOT_PATH/sdcard"

busybox mount -o remount,dev,suid /data
busybox mount --bind /dev "$CHROOT_PATH/dev"
busybox mount --bind /sys "$CHROOT_PATH/sys"
busybox mount --bind /proc "$CHROOT_PATH/proc"
busybox mount -t devpts devpts "$CHROOT_PATH/dev/pts"
busybox mount --bind /sdcard "$CHROOT_PATH/sdcard"

cp /data/user/0/com.example.uhdlogger/files/run_uhd.sh "$CHROOT_PATH/root/"
chmod 755 "$CHROOT_PATH/root/run_uhd.sh"

echo "[*] Running UHD test inside chroot... be patient..."

echo "/root/run_uhd.sh $RATE $RECV_FRAMES $SEND_FRAMES $GAIN $DURATION $FREQUENCY $UHD_PATH $LOG_PATH"

chroot "$CHROOT_PATH" /bin/su - root -c "/root/run_uhd.sh $RATE $RECV_FRAMES $SEND_FRAMES $GAIN $DURATION $FREQUENCY $UHD_PATH $LOG_PATH"

echo "[*] Cleaning up..."
busybox umount "$CHROOT_PATH/dev/pts"
busybox umount "$CHROOT_PATH/dev"
busybox umount "$CHROOT_PATH/proc"
busybox umount "$CHROOT_PATH/sys"
busybox umount "$CHROOT_PATH/sdcard"
