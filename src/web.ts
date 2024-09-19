import { WebPlugin } from '@capacitor/core';

import type { ModelMessageStatus, NrfMeshPlugin, PluginCallRejection, ProvisioningCapabilities, ProvisioningStatus, ScanDevicesResponse, ScanMeshDevices } from './definitions';

export class NrfMeshWeb extends WebPlugin implements NrfMeshPlugin {

  async scanUnprovisionedDevices(): Promise<ScanDevicesResponse> {
    console.log('scanUnprovisionedDevices');
    return {};
  }

  async scanProvisionedDevices(): Promise<ScanDevicesResponse> {
    console.log('scanProvisionedDevices');
    return {};
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

  async addApplicationKeyToNode(): Promise<void> {
    console.log('addApplicationKeyToNode');
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

  async sendGenericPowerLevelSet(): Promise<ModelMessageStatus | PluginCallRejection> {
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async sendLightHslSet(): Promise<ModelMessageStatus | PluginCallRejection> {
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  async exportMeshNetwork(): Promise<object> {
    console.log('exportMeshNetwork');
    return {};
  }

  async listenForMeshEvents(): Promise<object> {
    console.log('listenForMeshEvents');
    return {};
  }

  async sendVendorModelMessage(): Promise<ModelMessageStatus | PluginCallRejection> {
    return { src: 1, dst: 2, opcode: 3, data: {} };
  }

  sendLightCtlSet(): Promise<ModelMessageStatus | PluginCallRejection> {
    throw new Error('Method not implemented.');
  }
}
