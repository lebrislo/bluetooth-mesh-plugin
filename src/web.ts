import { WebPlugin } from '@capacitor/core';

import type { BluetoothConnectionStatus, BluetoothState, MeshNetworkObject, ModelMessageStatus, NodesOnlineStates, BluetoothMeshPlugin, Permissions, ProvisioningCapabilities, ProvisioningStatus, ScanMeshDevices, UnprovisionStatus } from './definitions';

export class BluetoothMeshWeb extends WebPlugin implements BluetoothMeshPlugin {

  async isBluetoothEnabled(): Promise<BluetoothState> {
    console.log('isBluetoothEnabled');
    return { enabled: true };
  }

  async requestBluetoothEnable(): Promise<BluetoothState> {
    console.log('requestBluetoothEnabled');
    return { enabled: true };
  }

  async isBluetoothConnected(): Promise<BluetoothConnectionStatus> {
    console.log('isBluetoothConnected');
    return { connected: true };
  }

  async disconnectBle(): Promise<void> {
    console.log('disconnectBle');
  }

  async checkPermissions(): Promise<Permissions> {
    console.log('checkPermissions');
    return { 'LOCATION': 'granted', 'BLUETOOTH': 'granted' };
  }

  async requestPermissions(): Promise<any> {
    console.log('requestPermissions');
    return;
  }

  async fetchMeshDevices(): Promise<ScanMeshDevices> {
    console.log('scanMeshDevices');
    return { unprovisioned: [], provisioned: [] };
  }

  async reloadScanMeshDevices(): Promise<void> {
    console.log('reloadScanMeshDevices');
  }

  async getNodesOnlineStates(): Promise<NodesOnlineStates> {
    console.log('getNodesOnlineStates');
    return { states: [] };
  }

  async getProvisioningCapabilities(): Promise<ProvisioningCapabilities> {
    console.log('getProvisioningCapabilities');
    return {
      numberOfElements: 1,
      availableOOBTypes: ['availableOOBTypes'],
      algorithms: 1,
      publicKeyType: 1,
      staticOobTypes: 1,
      outputOobSize: 1,
      outputOobActions: 1,
      inputOobSize: 1,
      inputOobActions: 1
    };
  }

  async provisionDevice(): Promise<ProvisioningStatus> {
    console.log('provisionDevice');
    return { provisioningComplete: true, uuid: '1234' };
  }

  async unprovisionDevice(): Promise<UnprovisionStatus> {
    console.log('unprovisionDevice');
    return { status: true };
  }

  async createApplicationKey(): Promise<ModelMessageStatus> {
    console.log('createApplicationKey');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async removeApplicationKey(): Promise<ModelMessageStatus> {
    console.log('removeApplicationKey');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async addApplicationKeyToNode(): Promise<ModelMessageStatus> {
    console.log('addApplicationKeyToNode');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async bindApplicationKeyToModel(): Promise<ModelMessageStatus> {
    console.log('bindApplicationKeyToModel');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async getCompositionData(): Promise<MeshNetworkObject> {
    console.log('compositionDataGet');
    return { meshNetwork: 'meshNetwork' };
  }

  async sendGenericOnOffSet(): Promise<ModelMessageStatus> {
    console.log('sendGenericOnOffSet');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async sendGenericOnOffGet(): Promise<ModelMessageStatus> {
    console.log('sendGenericOnOffSet');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async sendGenericPowerLevelSet(): Promise<ModelMessageStatus> {
    console.log('sendGenericPowerLevelSet');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async sendGenericPowerLevelGet(): Promise<ModelMessageStatus> {
    console.log('sendGenericPowerLevelGet');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async sendLightHslSet(): Promise<ModelMessageStatus> {
    console.log('sendLightHslSet');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async sendLightHslGet(): Promise<ModelMessageStatus> {
    console.log('sendLightHslGet');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  initMeshNetwork(): Promise<MeshNetworkObject> {
    return Promise.resolve({ meshNetwork: 'meshNetwork' });
  }

  async exportMeshNetwork(): Promise<MeshNetworkObject> {
    console.log('exportMeshNetwork');
    return { meshNetwork: 'meshNetwork' };
  }

  async importMeshNetwork(): Promise<void> {
    console.log('importMeshNetwork');
  }

  async sendVendorModelMessage(): Promise<ModelMessageStatus> {
    console.log('sendVendorModelMessage');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async sendLightCtlSet(): Promise<ModelMessageStatus> {
    console.log('sendLightCtlSet');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async sendLightCtlGet(): Promise<ModelMessageStatus> {
    console.log('sendLightCtlGet');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async sendLightCtlTemperatureRangeSet(): Promise<ModelMessageStatus> {
    console.log('sendLightCtlTemperatureRangeSet');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async sendLightCtlTemperatureRangeGet(): Promise<ModelMessageStatus> {
    console.log('sendLightCtlTemperatureRangeGet');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async sendConfigHeartbeatPublicationSet(): Promise<void> {
    console.log('sendConfigHeartbeatPublicationSet');
  }

  async sendHealthFaultGet(): Promise<ModelMessageStatus> {
    console.log('sendHealthFaultGet');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }
}
