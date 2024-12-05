import { registerPlugin } from '@capacitor/core';

import type { BluetoothMeshPlugin } from './definitions';

const BluetoothMesh = registerPlugin<BluetoothMeshPlugin>('BluetoothMesh', {
  web: () => import('./web').then(m => new m.BluetoothMeshWeb()),
});

export * from './definitions';
export { BluetoothMesh };
