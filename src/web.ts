import { WebPlugin } from '@capacitor/core';

import type { AddAppKeyStatus, BluetoothState, MeshNetworkObject, ModelMessageStatus, NrfMeshPlugin, Permissions, PluginCallRejection, ProvisioningCapabilities, ProvisioningStatus, ScanMeshDevices } from './definitions';

export class NrfMeshWeb extends WebPlugin implements NrfMeshPlugin {

  async isBluetoothEnabled(): Promise<BluetoothState> {
    console.log('isBluetoothEnabled');
    return { enabled: true };
  }

  async requestBluetoothEnable(): Promise<BluetoothState> {
    console.log('requestBluetoothEnabled');
    return { enabled: true };
  }

  async checkPermissions(): Promise<Permissions> {
    console.log('checkPermissions');
    return { 'LOCATION': 'granted', 'BLUETOOTH': 'granted' };
  }

  async requestPermissions(): Promise<any> {
    console.log('requestPermissions');
    return;
  }

  async scanMeshDevices(): Promise<ScanMeshDevices> {
    console.log('scanMeshDevices');
    return { unprovisioned: [], provisioned: [] };
  }

  async getProvisioningCapabilities(): Promise<ProvisioningCapabilities | void> {
    console.log('getProvisioningCapabilities');
    return;
  }

  async provisionDevice(): Promise<ProvisioningStatus> {
    console.log('provisionDevice');
    return { provisioningComplete: true, uuid: '1234' };
  }

  async unprovisionDevice(): Promise<void> {
    console.log('unprovisionDevice');
  }

  async createApplicationKey(): Promise<void> {
    console.log('createApplicationKey');
  }

  async removeApplicationKey(): Promise<void> {
    console.log('removeApplicationKey');
  }

  async addApplicationKeyToNode(): Promise<AddAppKeyStatus> {
    console.log('addApplicationKeyToNode');
    return { success: true };
  }

  async bindApplicationKeyToModel(): Promise<void> {
    console.log('bindApplicationKeyToModel');
  }

  async compositionDataGet(): Promise<void> {
    console.log('compositionDataGet');
  }

  async sendGenericOnOffSet(): Promise<ModelMessageStatus | PluginCallRejection> {
    console.log('sendGenericOnOffSet');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async sendGenericOnOffGet(): Promise<ModelMessageStatus | PluginCallRejection> {
    console.log('sendGenericOnOffSet');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async sendGenericPowerLevelSet(): Promise<ModelMessageStatus | PluginCallRejection> {
    console.log('sendGenericPowerLevelSet');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async sendGenericPowerLevelGet(): Promise<ModelMessageStatus | PluginCallRejection> {
    console.log('sendGenericPowerLevelGet');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async sendLightHslSet(): Promise<ModelMessageStatus | PluginCallRejection> {
    console.log('sendLightHslSet');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async sendLightHslGet(): Promise<ModelMessageStatus | PluginCallRejection> {
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

  async sendVendorModelMessage(options: {
    unicastAddress: number;
    appKeyIndex: number;
    modelId: number;
    companyIdentifier: number;
    opcode: number;
    payload: Uint8Array;
  }): Promise<ModelMessageStatus | PluginCallRejection> {
    console.log('sendVendorModelMessage', options);
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async sendLightCtlSet(): Promise<ModelMessageStatus | PluginCallRejection> {
    console.log('sendLightCtlSet');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async sendLightCtlGet(): Promise<ModelMessageStatus | PluginCallRejection> {
    console.log('sendLightCtlGet');
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }
}
