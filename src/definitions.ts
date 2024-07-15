export interface NrfMeshPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
