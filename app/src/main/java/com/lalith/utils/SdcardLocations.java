
package com.lalith.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.lalith.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

public class SdcardLocations {
 public String title;
  final String root;
  final boolean hasApps2SD;
  final boolean rootRequired;
  final String fsType;
  
  SdcardLocations(String title, String root,
      boolean hasApps2SD, boolean rootRequired, String fsType) {
    this.title = title;
    this.root = root;
    this.hasApps2SD = hasApps2SD;
    this.rootRequired = rootRequired;
    this.fsType = fsType;
  }
  
  private static SdcardLocations defaultStorage;
  private static Map<String, SdcardLocations> sdcardLocations = new TreeMap<String, SdcardLocations>();
  private static Map<String, SdcardLocations> rootedSdcardLocations = new TreeMap<String, SdcardLocations>();
  private static boolean init = false;
  static int checksum = 0;
  

  
  public static Map<String,SdcardLocations> getSdcardLocations(Context context) {
    initSdcardLocations(context);
    return sdcardLocations;
  }

  
  public static String storageCardPath() {
    try {
      return Environment.getExternalStorageDirectory().getCanonicalPath();
    } catch (Exception e) {
      return Environment.getExternalStorageDirectory().getAbsolutePath();
    }
  }
  
  private static boolean isEmulated(String fsType) {
    return fsType.equals("sdcardfs") || fsType.equals("fuse");
  }
  
  private static void initSdcardLocations(Context context) {
    if (init) return;
    init = true;
    String storagePath = storageCardPath();
    Log.d("diskusage", "StoragePath: " + storagePath);
    
    ArrayList<SdcardLocations> mountPointsList = new ArrayList<SdcardLocations>();
    HashSet<String> excludePoints = new HashSet<String>();
    if (storagePath != null) {
      defaultStorage = new SdcardLocations(
              titleStorageCard(context), storagePath,/* null,*/ false, false, "");
      mountPointsList.add(defaultStorage);
      sdcardLocations.put(storagePath, defaultStorage);
    }
    
    try {
      checksum = 0;
      BufferedReader reader = new BufferedReader(new FileReader("/proc/mounts"));
      String line;
      while ((line = reader.readLine()) != null) {
        checksum += line.length();
        Log.d("diskusage", "line: " + line);
        String[] parts = line.split(" +");
        if (parts.length < 3) continue;
        String mountPoint = parts[1];
        Log.d("diskusage", "Mount point: " + mountPoint);
        String fsType = parts[2];
        
        StatFs stat = null;
        try {
          stat = new StatFs(mountPoint);
        } catch (Exception e) {
        }
        
        if (!(fsType.equals("vfat") || fsType.equals("tntfs") || fsType.equals("exfat")
            || fsType.equals("texfat") || isEmulated(fsType))
            || mountPoint.startsWith("/mnt/asec")
            || mountPoint.startsWith("/firmware")
            || mountPoint.startsWith("/mnt/secure")
            || mountPoint.startsWith("/data/mac")
            || stat == null
            || (mountPoint.endsWith("/legacy") && isEmulated(fsType))) {
          Log.d("diskusage", String.format("Excluded based on fsType=%s or black list", fsType));
          excludePoints.add(mountPoint);
          
          // Default storage is not vfat, removing it (real honeycomb)
          if (mountPoint.equals(storagePath)) {
            mountPointsList.remove(defaultStorage);
            sdcardLocations.remove(mountPoint);
          }
          if (/*rooted &&*/ !mountPoint.startsWith("/mnt/asec/")) {
            mountPointsList.add(new SdcardLocations(mountPoint, mountPoint,/* null,*/ false, true, fsType));
          }
        } else {
          Log.d("diskusage", "Mount point is good");
          mountPointsList.add(new SdcardLocations(mountPoint, mountPoint, /*null,*/ false, false, fsType));
        }
      }
      
      for (SdcardLocations sdcardLocations : mountPointsList) {
        String prefix = sdcardLocations.root + "/";
        boolean has_apps2sd = false;
        ArrayList<String> excludes = new ArrayList<String>();
        String mountPointName = new File(sdcardLocations.root).getName();
        
        for (SdcardLocations otherSdcardLocations : mountPointsList) {
          if (otherSdcardLocations.root.startsWith(prefix)) {
            excludes.add(mountPointName + "/" + otherSdcardLocations.root.substring(prefix.length()));
          }
        }
        for (String otherMountPoint : excludePoints) {
          if (otherMountPoint.equals(prefix + ".android_secure")) {
            has_apps2sd = true;
          }
          if (otherMountPoint.startsWith(prefix)) {
            excludes.add(mountPointName + "/" + otherMountPoint.substring(prefix.length()));
          }
        }
        SdcardLocations newSdcardLocations = new SdcardLocations(
            sdcardLocations.root, sdcardLocations.root, /*new ExcludeFilter(excludes),*/
            has_apps2sd, sdcardLocations.rootRequired, sdcardLocations.fsType);
        if (sdcardLocations.rootRequired) {
          rootedSdcardLocations.put(sdcardLocations.root, newSdcardLocations);
        } else {
          SdcardLocations.sdcardLocations.put(sdcardLocations.root, newSdcardLocations);
        }
      }
    } catch (Exception e) {
      Log.e("diskusage", "Failed to get mount points", e);
    }
    final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
    
    SdcardLocations storageCard = sdcardLocations.get(storageCardPath());
    if(sdkVersion >= Build.VERSION_CODES.HONEYCOMB
        && (storageCard == null || isEmulated(storageCard.fsType))) {
      sdcardLocations.remove(storageCardPath());
      sdcardLocations.put("/data", new SdcardLocations(
              titleStorageCard(context), "/data", /*null,*/ false, false, ""));
    }

    if (!sdcardLocations.isEmpty()) {
      defaultStorage = sdcardLocations.values().iterator().next();
      defaultStorage.title = titleStorageCard(context);
    }
  }
  
  private static String titleStorageCard(Context context) {
    return context.getString(R.string.app_name);
  }

}
