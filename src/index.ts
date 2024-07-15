import { registerPlugin } from '@capacitor/core';

import type { NrfMeshPlugin } from './definitions';

const NrfMesh = registerPlugin<NrfMeshPlugin>('NrfMesh', {
  web: () => import('./web').then(m => new m.NrfMeshWeb()),
});

export * from './definitions';
export { NrfMesh };
