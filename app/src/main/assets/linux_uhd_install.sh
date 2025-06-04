INSTALL_PATH=$7

if [ -z "$INSTALL_PATH" ]; then
    echo -e "\033[0;31mNo install path provided. Usage: $0 <install_path>\033[0m"
    exit 1
fi

# check for needed requirements
echo -e "\033[0;32m[*] Checking Superuser privelages...\033[0m"
if su -c "echo hello from root" >/dev/null 2>&1; then
    echo -e "\033[0;32mSuperuser access granted.\033[0m"
else
    echo -e "\033[0;31mFailed to gain superuser access. Is the device properly rooted?\033[0m"
    exit 1
fi

echo -e "\033[0;32m[*] Checking for BusyBox...\033[0m"
if command -v busybox >/dev/null 2>&1; then
    echo -e "\033[0;32mBusyBox installation verified.\033[0m"
else
    echo -e "\033[0;31mBusyBox is NOT installed.\033[0m"
    exit 1
fi

echo -e "\033[0;32m[*] Checking internet connection...\033[0m"
if busybox ping -c 1 8.8.8.8 >/dev/null 2>&1; then
    echo -e "\033[0;32mInternet connection verified.\033[0m"
else
    echo -e "\033[0;31mNo internet connection. Please check your network and try again.\033[0m"
    exit 1
fi

echo -e "\033[0;32m[*] Attempting to install Ubuntu at ${INSTALL_PATH}...\033[0m"

INSTALL_DIR="${INSTALL_PATH%/}"
mkdir -p "$INSTALL_DIR" || {
    echo -e "\033[0;31mFailed to create install directory: $INSTALL_DIR\033[0m"
    exit 1
}

cd "$INSTALL_DIR" || {
    echo -e "\033[0;31mFailed to enter install directory.\033[0m"
    exit 1
}

UBUNTU_TARBALL="ubuntu-base-24.04.2-base-arm64.tar.gz"

echo -e "\033[0;32m[*] Downloading Ubuntu base image...\033[0m"
if busybox wget "https://cdimage.ubuntu.com/ubuntu-base/releases/noble/release/$UBUNTU_TARBALL"; then
    echo -e "\033[0;32mUbuntu base downloaded successfully.\033[0m"
else
    echo -e "\033[0;31mFailed to download Ubuntu base. Check your internet connection.\033[0m"
    exit 1
fi

echo -e "\033[0;32m[*] Extracting Ubuntu base image...\033[0m"
if tar xpvf "$UBUNTU_TARBALL" --numeric-owner; then
    echo -e "\033[0;32mExtraction completed.\033[0m"
else
    echo -e "\033[0;31mExtraction failed.\033[0m"
    exit 1
fi

# INSTALLING UHD

echo -e "\033[0;32m[*] Linux install complete. Moving on to UHD...\033[0m"

mkdir -p "$INSTALL_DIR/dev/pts" "$INSTALL_DIR/sdcard"

busybox mount -o remount,dev,suid /data
busybox mount --bind /dev "$INSTALL_DIR/dev"
busybox mount --bind /sys "$INSTALL_DIR/sys"
busybox mount --bind /proc "$INSTALL_DIR/proc"
busybox mount -t devpts devpts "$INSTALL_DIR/dev/pts"
busybox mount --bind /sdcard "$INSTALL_DIR/sdcard"

echo -e "\033[0;32m[*] This step may take some time... please be patient\033[0m"

chroot "$INSTALL_DIR" /bin/su - root -c "

echo -e '\033[0;32m[*] Hello from inside chroot Ubuntu!!!\033[0m'

echo 'nameserver 8.8.8.8' > /etc/resolv.conf
echo '127.0.0.1 localhost' > /etc/hosts

groupadd -g 3003 aid_inet
groupadd -g 3004 aid_net_raw
groupadd -g 1003 aid_graphics

usermod -g 3003 -G 3003,3004 -a _apt
usermod -G 3003 -a root

echo -e '\033[0;32m[*] Installing & updating necessary packages...\033[0m'

export DEBIAN_FRONTEND=noninteractive
export TZ=America/Denver

apt update
apt upgrade -y

apt install -y git build-essential cmake libusb-1.0-0-dev libboost-all-dev pkg-config python3 python3-pip python3-requests libuhd-dev sudo bc

echo -e '\033[0;32m[*] Cloning UHD repository...\033[0m'

cd /root
git clone https://github.com/EttusResearch/uhd.git
cd uhd/host
mkdir build
cd build

echo -e '\033[0;32m[*] Compiling UHD...\033[0m'

rm -rf CMakeCache.txt CMakeFiles  # Clean previous config if re-running

cmake ..
make -j$(nproc)
make install
ldconfig

echo -e '\033[0;32m[*] Downloading SDR drivers...\033[0m'

uhd_images_downloader
"

echo -e "\033[0;32m[*] Cleaning up...\033[0m"
busybox umount "$INSTALL_DIR/dev/pts"
busybox umount "$INSTALL_DIR/dev"
busybox umount "$INSTALL_DIR/proc"
busybox umount "$INSTALL_DIR/sys"
busybox umount "$INSTALL_DIR/sdcard"

echo -e "\033[0;32m[*] Install complete\033[0m"