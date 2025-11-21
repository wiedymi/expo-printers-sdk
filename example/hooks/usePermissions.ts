import { useState } from "react";
import { Platform, PermissionsAndroid, Alert } from "react-native";

export const usePermissions = () => {
  const [isRequestingPermissions, setIsRequestingPermissions] = useState(false);

  const requestPermissions = async (): Promise<boolean> => {
    setIsRequestingPermissions(true);
    try {
      if (Platform.OS === "android") {
        const permissions = [
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
        ].filter(Boolean);

        if (permissions.length === 0) {
          return true;
        }

        const results = await PermissionsAndroid.requestMultiple(permissions);

        const allGranted = Object.values(results).every(
          (result) => result === PermissionsAndroid.RESULTS.GRANTED
        );

        if (!allGranted) {
          Alert.alert(
            "Permission Required",
            "Location and Bluetooth permissions are required to search for printers."
          );
          return false;
        }

        return true;
      }
      return true;
    } catch (error) {
      console.error("Error requesting permissions:", error);
      return false;
    } finally {
      setIsRequestingPermissions(false);
    }
  };

  return { isRequestingPermissions, requestPermissions };
};
