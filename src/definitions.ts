interface BleMeshDevice {
  name: string;
  advData: string;
  rssi: number;
  macAddress: string;
}

interface ScanDevicesResponse {
  devices?: BleMeshDevice[];
}

export interface NrfMeshPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  scanUnprovisionedDevices(options: { timeout: number }): Promise<ScanDevicesResponse>;
  scanProvisionedDevices(options: { timeout: number }): Promise<ScanDevicesResponse>;
}
