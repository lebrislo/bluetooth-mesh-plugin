import { WebPlugin } from '@capacitor/core';

import type { NrfMeshPlugin } from './definitions';

export class NrfMeshWeb extends WebPlugin implements NrfMeshPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }

  async scanUnprovisionedDevices(): Promise<object> {
    console.log('scanUnprovisionedDevices');
    return {};
  }

  async scanProvisionedDevices(): Promise<object> {
    console.log('scanProvisionedDevices');
    return {};
  }

  async provisionDevice(): Promise<void> {
    console.log('provisionDevice');
    return;
  }
}
