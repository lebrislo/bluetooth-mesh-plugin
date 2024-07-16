export interface NrfMeshPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  scanUnprovisionedDevices(): Promise<void>;
}
