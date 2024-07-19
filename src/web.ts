import { WebPlugin } from '@capacitor/core';

import type { NrfMeshPlugin, ProvisioningCapabilities, ScanDevicesResponse } from './definitions';

export class NrfMeshWeb extends WebPlugin implements NrfMeshPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }

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

  async provisionDevice(): Promise<void> {
    console.log('provisionDevice');
    return;
  }
}
