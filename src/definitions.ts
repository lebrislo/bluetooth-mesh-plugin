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
  meshUuid: string;
  rssi: number;

  /**
   * ID of the device, which will be needed for further calls.
   * On **Android** this is the BLE MAC address.
   * On **iOS** and **web** it is an identifier.
   */
  deviceId: string;
  macAddress?: string;
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
  deviceId: string;
  unicastAddress?: number;
  uuid?: string;
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
  meshNetwork: object;
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

export enum BluetoothMeshPluginEvents {
  MeshModelMessageEvent = 'meshModelMessageEvent', /* Mesh model message received */
  BluetoothAdapterEvent = 'bluetoothAdapterEvent', /* Bluetooth adapter state change */
  BluetoothConnectionEvent = 'bluetoothConnectionEvent', /* Bluetooth connection state change */
  MeshDeviceScanEvent = 'meshDeviceScanEvent', /* Mesh device scan event */
}

export interface BluetoothMeshPlugin {
  isBluetoothEnabled(): Promise<BluetoothState>;
  requestBluetoothEnable(): Promise<BluetoothState>;
  isBluetoothConnected(): Promise<BluetoothConnectionStatus>;
  disconnectBle(options: { autoReconnect?: boolean }): Promise<void>;
  checkPermissions(): Promise<Permissions>
  requestPermissions(): Promise<any>
  initMeshNetwork(options: { networkName: string }): Promise<MeshNetworkObject>;
  exportMeshNetwork(): Promise<MeshNetworkObject>;
  importMeshNetwork(options: { meshNetwork: string }): Promise<void>;
  fetchMeshDevices(): Promise<ScanMeshDevices>;
  reloadScanMeshDevices(): Promise<void>;
  getNodesOnlineStates(): Promise<NodesOnlineStates>;
  getProvisioningCapabilities(options: {
  deviceId: string;
  meshUuid: string;
  }): Promise<ProvisioningCapabilities>;
  provisionDevice(options: { deviceId: string; meshUuid: string }): Promise<ProvisioningStatus>;
  unprovisionDevice(options: { unicastAddress: number }): Promise<UnprovisionStatus>;
  createApplicationKey(): Promise<ModelMessageStatus>;
  removeApplicationKey(options: { appKeyIndex: number }): Promise<ModelMessageStatus>;
  addApplicationKeyToNode(options: ModelMessage): Promise<ModelMessageStatus>;
  sendAppKeyGet(options: { unicastAddress: number, netKeyIndex: number }): Promise<ModelMessageStatus>;
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
    timeout?: number;
  }): Promise<ModelMessageStatus>;
  sendConfigHeartbeatPublicationSet(options: ConfigHeartbeatPublicationSet): Promise<void>;
  sendHealthFaultGet(options: ModelMessage & { companyId: number }): Promise<ModelMessageStatus>;
  sendHealthFaultClear(options: ModelMessage & { companyId: number }): Promise<ModelMessageStatus>;
  addListener(eventName: string, listenerFunc: (event: any) => void): Promise<PluginListenerHandle>;
  removeAllListeners(): Promise<void>;
}