import { WebPlugin } from '@capacitor/core';

import type { NrfMeshPlugin } from './definitions';

export class NrfMeshWeb extends WebPlugin implements NrfMeshPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }

  async scanUnprovisionedDevices(): Promise<void> {
    console.log('scanUnprovisionedDevices');
  }
}
