import type { PluginListenerHandle } from '@capacitor/core';

export interface BluetoothState {
  enabled: boolean;
}

export interface BluetoothConnectionStatus {
  connected: boolean;
  macAddress?: string;
}

export interface Permissions {
  [key: string]: string;
}

export interface BleMeshDevice {
  name: string;
  uuid: string;
  rssi: number;
  macAddress: string;
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

export interface UnprovisionStatus {
  status: boolean;
}

export interface ModelMessage {
  unicastAddress: number;
  appKeyIndex: number;
  acknowledgement?: boolean;
}

export interface ModelMessageStatus {
  src: number;
  dst: number;
  opcode: number;
  vendorModelId?: number;
  data: any;
}

export interface MeshNetworkObject {
  meshNetwork: string;
}

export interface ConfigHeartbeatPublicationSet {
  unicastAddress: number;
  destinationAddress: number;
  count: number;
  period: number;
  ttl: number;
  netKeyIndex: number;
}

export interface OnlineState {
  unicastAddress: number;
  isOnline: boolean;
}

export interface NodesOnlineStates {
  states: OnlineState[];
}

export enum NrfMeshPluginEvents {
  MeshModelMessageEvent = 'meshModelMessageEvent', /* Mesh model message received */
  BluetoothAdapterEvent = 'bluetoothAdapterEvent', /* Bluetooth adapter state change */
  BluetoothConnectionEvent = 'bluetoothConnectionEvent', /* Bluetooth connection state change */
  MeshDeviceScanEvent = 'meshDeviceScanEvent', /* Mesh device scan event */
}

export interface NrfMeshPlugin {
  isBluetoothEnabled(): Promise<BluetoothState>;
  requestBluetoothEnable(): Promise<BluetoothState>;
  isBluetoothConnected(): Promise<BluetoothConnectionStatus>;
  disconnectBle(): Promise<void>;
  checkPermissions(): Promise<Permissions>
  requestPermissions(): Promise<any>
  initMeshNetwork(options: { networkName: string }): Promise<MeshNetworkObject>;
  exportMeshNetwork(): Promise<MeshNetworkObject>;
  importMeshNetwork(options: { meshNetwork: string }): Promise<void>;
  scanMeshDevices(options: {
    timeout: number;
  }): Promise<ScanMeshDevices>;
  clearMeshDevicesScan(): Promise<void>;
  getNodesOnlineStates(): Promise<NodesOnlineStates>;
  getProvisioningCapabilities(options: {
    macAddress: string;
    uuid: string;
  }): Promise<ProvisioningCapabilities>;
  provisionDevice(options: { macAddress: string; uuid: string }): Promise<ProvisioningStatus>;
  unprovisionDevice(options: { unicastAddress: number }): Promise<UnprovisionStatus>;
  createApplicationKey(): Promise<ModelMessageStatus>;
  removeApplicationKey(options: { appKeyIndex: number }): Promise<ModelMessageStatus>;
  addApplicationKeyToNode(options: ModelMessage): Promise<ModelMessageStatus>;
  bindApplicationKeyToModel(options: ModelMessage & {
    modelId: number;
  }): Promise<ModelMessageStatus>;
  getCompositionData(options: { unicastAddress: number }): Promise<MeshNetworkObject>;
  sendGenericOnOffSet(options: ModelMessage & {
    onOff: boolean;
  }): Promise<ModelMessageStatus>;
  sendGenericOnOffGet(options: ModelMessage): Promise<ModelMessageStatus>;
  sendGenericPowerLevelSet(options: ModelMessage & {
    powerLevel: number;
  }): Promise<ModelMessageStatus>;
  sendGenericPowerLevelGet(options: ModelMessage): Promise<ModelMessageStatus>;
  sendLightHslSet(options: ModelMessage & {
    hue: number;
    saturation: number;
    lightness: number;
  }): Promise<ModelMessageStatus>;
  sendLightHslGet(options: ModelMessage): Promise<ModelMessageStatus>;
  sendLightCtlSet(options: ModelMessage & {
    lightness: number;
    temperature: number;
    deltaUv: number;
  }): Promise<ModelMessageStatus>;
  sendLightCtlGet(options: ModelMessage): Promise<ModelMessageStatus>;
  sendLightCtlTemperatureRangeSet(options: ModelMessage & {
    rangeMin: number;
    rangeMax: number;
  }): Promise<ModelMessageStatus>;
  sendLightCtlTemperatureRangeGet(options: ModelMessage): Promise<ModelMessageStatus>;
  sendVendorModelMessage(options: ModelMessage & {
    modelId: number;
    opcode: number;
    payload?: Uint8Array;
    opPairCode?: number
  }): Promise<ModelMessageStatus>;
  sendConfigHeartbeatPublicationSet(option: ConfigHeartbeatPublicationSet): Promise<void>;
  addListener(eventName: string, listenerFunc: (event: any) => void): Promise<PluginListenerHandle>;
  removeAllListeners(): Promise<void>;
}
