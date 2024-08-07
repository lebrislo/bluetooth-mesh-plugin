import { WebPlugin } from '@capacitor/core';

import type { NrfMeshPlugin, ProvisioningCapabilities, ProvisioningStatus, ScanDevicesResponse } from './definitions';

export class NrfMeshWeb extends WebPlugin implements NrfMeshPlugin {

  async scanUnprovisionedDevices(): Promise<ScanDevicesResponse> {
    console.log('scanUnprovisionedDevices');
    return {};
  }

  async scanProvisionedDevices(): Promise<ScanDevicesResponse> {
    console.log('scanProvisionedDevices');
    return {};
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

  async sendGenericOnOffSet(): Promise<void> {
    console.log('sendGenericOnOffSet');
  }

  async sendGenericPowerLevelSet(): Promise<void> {
    console.log('sendGenericPowerLevelSet');
  }

  async sendLightHslSet(): Promise<void> {
    console.log('SendLightHslSet');
  }

  async exportMeshNetwork(): Promise<object> {
    console.log('exportMeshNetwork');
    return {};
  }

  async listenForMeshEvents(): Promise<object> {
    console.log('listenForMeshEvents');
    return {};
  }
}
