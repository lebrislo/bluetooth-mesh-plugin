interface BleMeshDevice {
  name: string;
  uuid: string;
  rssi: number;
  macAddress: string;
}

export interface ScanDevicesResponse {
  devices?: BleMeshDevice[];
}

export interface ProvisioningCapabilities {
  numberOfElements: number;
  algorithms: number;
  publicKeyType: number;
  staticOobTypes: number;
  outputOobSize: number;
  outputOobActions: string[];
  inputOobSize: number;
  inputOobActions: string[];
}

export interface NrfMeshPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  scanUnprovisionedDevices(options: { timeout: number }): Promise<ScanDevicesResponse>;
  scanProvisionedDevices(options: { timeout: number }): Promise<ScanDevicesResponse>;
  getProvisioningCapabilities(options: { uuid: string }): Promise<ProvisioningCapabilities | void>;
  provisionDevice(options: { uuid: string }): Promise<void>;
}
