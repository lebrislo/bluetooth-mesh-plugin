import type { PluginListenerHandle } from '@capacitor/core';

export interface BleMeshDevice {
  name: string;
  uuid: string;
  rssi: number;
  macAddress: string;
}

export interface ScanDevicesResponse {
  devices?: BleMeshDevice[];
}

export interface ScanMeshDevices {
  unprovisioned: BleMeshDevice[];
  provisioned: BleMeshDevice[];
}

export interface ProvisioningCapabilities {
  numberOfElements: number;
  availableOOBTypes: string[];
  algorithms: number;
  publicKeyType: number;
  staticOobTypes: number;
  outputOobSize: number;
  outputOobActions: number;
  inputOobSize: number;
  inputOobActions: number;
}

export interface ProvisioningStatus {
  provisioningComplete: boolean;
  uuid: string;
  unicastAddress?: number;
}

export interface PluginCallRejection {
  message: string;
  data: {
    methodName: string;
    [key: string]: any;
  };
}

export type Data = DataView | string;

export interface ReadResult {
  value?: Data;
}

export interface ModelMessageStatus {
  src: number;
  dst: number;
  opcode: number;
  vendorModelId?: number;
  data: any;
}

export interface AddAppKeyStatus {
  success: boolean;
}

export interface MeshNetworkObject {
  meshNetwork: string;
}

export interface NrfMeshPlugin {
  scanUnprovisionedDevices(options: {
    timeout: number;
  }): Promise<ScanDevicesResponse>;
  scanProvisionedDevices(options: {
    timeout: number;
  }): Promise<ScanDevicesResponse>;
  scanMeshDevices(options: {
    timeout: number;
  }): Promise<ScanMeshDevices>;
  getProvisioningCapabilities(options: {
    macAddress: string;
    uuid: string;
  }): Promise<ProvisioningCapabilities | void>;
  provisionDevice(options: { macAddress: string; uuid: string }): Promise<ProvisioningStatus>;
  unprovisionDevice(options: { unicastAddress: number }): Promise<void>;
  createApplicationKey(): Promise<void>;
  removeApplicationKey(options: { appKeyIndex: number }): Promise<void>;
  addApplicationKeyToNode(options: {
    unicastAddress: number;
    appKeyIndex: number;
  }): Promise<AddAppKeyStatus>;
  bindApplicationKeyToModel(options: {
    elementAddress: number;
    appKeyIndex: number;
    modelId: number;
  }): Promise<void>;
  compositionDataGet(options: { unicastAddress: number }): Promise<void>;
  sendGenericOnOffSet(options: {
    unicastAddress: number;
    appKeyIndex: number;
    onOff: boolean;
  }): Promise<ModelMessageStatus | PluginCallRejection>;
  sendGenericOnOffGet(options: {
    unicastAddress: number;
    appKeyIndex: number;
  }): Promise<ModelMessageStatus | PluginCallRejection>;
  sendGenericPowerLevelSet(options: {
    unicastAddress: number;
    appKeyIndex: number;
    powerLevel: number;
  }): Promise<ModelMessageStatus | PluginCallRejection>;
  sendGenericPowerLevelGet(options: {
    unicastAddress: number;
    appKeyIndex: number;
  }): Promise<ModelMessageStatus | PluginCallRejection>;
  sendLightHslSet(options: {
    unicastAddress: number;
    appKeyIndex: number;
    hue: number;
    saturation: number;
    lightness: number;
  }): Promise<ModelMessageStatus | PluginCallRejection>;
  sendLightHslGet(options: {
    unicastAddress: number;
    appKeyIndex: number;
  }): Promise<ModelMessageStatus | PluginCallRejection>;
  sendLightCtlSet(options: {
    unicastAddress: number;
    appKeyIndex: number;
    lightness: number;
    temperature: number;
    deltaUv: number;
  }): Promise<ModelMessageStatus | PluginCallRejection>;
  sendVendorModelMessage(options: {
    unicastAddress: number;
    appKeyIndex: number;
    modelId: number;
    opcode: number;
    payload: Uint8Array;
    acknowledgement?: boolean;
    opPairCode?: number
  }): Promise<ModelMessageStatus | PluginCallRejection>;
  initMeshNetwork(options: { networkName: string }): Promise<MeshNetworkObject>;
  exportMeshNetwork(): Promise<MeshNetworkObject>;
  importMeshNetwork(options: { meshNetwork: string }): Promise<void>;
  addListener(eventName: string, listenerFunc: (event: ModelMessageStatus) => void): Promise<PluginListenerHandle>;
  removeAllListeners(): Promise<void>;
}
