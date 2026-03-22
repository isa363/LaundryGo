import numpy as np
import pandas as pd

# ---------------------------------------------------------
# 1. Real-time IIR low-pass filter (same as C++)
# ---------------------------------------------------------
def lowpass_filter(data, alpha=0.15):
    y = np.zeros_like(data)
    y[0] = data[0]
    for i in range(1, len(data)):
        y[i] = alpha * data[i] + (1 - alpha) * y[i-1]
    return y

# ---------------------------------------------------------
# 2. Load CSV and compute gravity-removed magnitude
# ---------------------------------------------------------
def load_clean(csv):
    df = pd.read_csv(csv)
    ax, ay, az = df["ax_mps2"], df["ay_mps2"], df["az_mps2"]

    a_tot = np.sqrt(ax**2 + ay**2 + az**2)
    gravity_mean = np.mean(a_tot)

    a_clean = a_tot - gravity_mean
    return a_clean, gravity_mean

# ---------------------------------------------------------
# 3. RMS computation
# ---------------------------------------------------------
def compute_rms(a, window=100):
    rms = []
    for i in range(len(a)-window):
        rms.append(np.sqrt(np.mean(a[i:i+window]**2)))
    return np.array(rms)

# ---------------------------------------------------------
# 4. Compute thresholds from idle data
# ---------------------------------------------------------
def compute_thresholds(idle_csv):
    a_clean, gravity_mean = load_clean(idle_csv)

    # Apply SAME smoothing as C++
    a_smooth = lowpass_filter(a_clean, alpha=0.15)

    rms_idle = compute_rms(a_smooth)

    idle_mean = np.mean(rms_idle)
    idle_std  = np.std(rms_idle)

    # Hysteresis thresholds
    threshold_off = idle_mean + 2 * idle_std
    threshold_on  = idle_mean + 3 * idle_std

    return gravity_mean, threshold_on, threshold_off

# ---------------------------------------------------------
# MAIN
# ---------------------------------------------------------
gravity_mean, th_on, th_off = compute_thresholds("data/idle_50Hz.csv")
print("\n=== CALIBRATION RESULTS idle_50Hz.csv ===")
print(f"Gravity Mean      : {gravity_mean:.6f}")
print(f"Threshold ON      : {th_on:.6f}")
print(f"Threshold OFF     : {th_off:.6f}")
print("===========================\n")

gravity_mean, th_on, th_off = compute_thresholds("data/fill_wash.csv")
print("\n=== CALIBRATION RESULTS fill_wash.csv ===")
print(f"Gravity Mean      : {gravity_mean:.6f}")
print(f"Threshold ON      : {th_on:.6f}")
print(f"Threshold OFF     : {th_off:.6f}")
print("===========================\n")

gravity_mean, th_on, th_off = compute_thresholds("data/running_empty_50Hz.csv")
print("\n=== CALIBRATION RESULTS running_empty_50Hz.csv ===")
print(f"Gravity Mean      : {gravity_mean:.6f}")
print(f"Threshold ON      : {th_on:.6f}")
print(f"Threshold OFF     : {th_off:.6f}")
print("===========================\n")

gravity_mean, th_on, th_off = compute_thresholds("data/transition_draintorinse.csv")
print("\n=== CALIBRATION RESULTS transition_draintorinse.csv ===")
print(f"Gravity Mean      : {gravity_mean:.6f}")
print(f"Threshold ON      : {th_on:.6f}")
print(f"Threshold OFF     : {th_off:.6f}")
print("===========================\n")

gravity_mean, th_on, th_off = compute_thresholds("data/idlerinse_lastspin.csv")
print("\n=== CALIBRATION RESULTS idlerinse_lastspin.csv ===")
print(f"Gravity Mean      : {gravity_mean:.6f}")
print(f"Threshold ON      : {th_on:.6f}")
print(f"Threshold OFF     : {th_off:.6f}")
print("===========================\n")