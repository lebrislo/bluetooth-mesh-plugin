# nrf-bluetooth-mesh

Capacitor plugin for Bluetooth Mesh, based on nRF Mesh Libraries

## Install

```bash
npm install nrf-bluetooth-mesh
npx cap sync
```

## API

<docgen-index>

* [`echo(...)`](#echo)
* [`scanUnprovisionedDevices(...)`](#scanunprovisioneddevices)
* [`scanProvisionedDevices(...)`](#scanprovisioneddevices)
* [`getProvisioningCapabilities(...)`](#getprovisioningcapabilities)
* [`provisionDevice(...)`](#provisiondevice)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### echo(...)

```typescript
echo(options: { value: string; }) => Promise<{ value: string; }>
```

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ value: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### scanUnprovisionedDevices(...)

```typescript
scanUnprovisionedDevices(options: { timeout: number; }) => Promise<ScanDevicesResponse>
```

| Param         | Type                              |
| ------------- | --------------------------------- |
| **`options`** | <code>{ timeout: number; }</code> |

**Returns:** <code>Promise&lt;<a href="#scandevicesresponse">ScanDevicesResponse</a>&gt;</code>

--------------------


### scanProvisionedDevices(...)

```typescript
scanProvisionedDevices(options: { timeout: number; }) => Promise<ScanDevicesResponse>
```

| Param         | Type                              |
| ------------- | --------------------------------- |
| **`options`** | <code>{ timeout: number; }</code> |

**Returns:** <code>Promise&lt;<a href="#scandevicesresponse">ScanDevicesResponse</a>&gt;</code>

--------------------


### getProvisioningCapabilities(...)

```typescript
getProvisioningCapabilities(options: { uuid: string; }) => Promise<ProvisioningCapabilities | void>
```

| Param         | Type                           |
| ------------- | ------------------------------ |
| **`options`** | <code>{ uuid: string; }</code> |

**Returns:** <code>Promise&lt;void | <a href="#provisioningcapabilities">ProvisioningCapabilities</a>&gt;</code>

--------------------


### provisionDevice(...)

```typescript
provisionDevice(options: { uuid: string; }) => Promise<void>
```

| Param         | Type                           |
| ------------- | ------------------------------ |
| **`options`** | <code>{ uuid: string; }</code> |

--------------------


### Interfaces


#### ScanDevicesResponse

| Prop          | Type                         |
| ------------- | ---------------------------- |
| **`devices`** | <code>BleMeshDevice[]</code> |


#### BleMeshDevice

| Prop             | Type                |
| ---------------- | ------------------- |
| **`name`**       | <code>string</code> |
| **`uuid`**       | <code>string</code> |
| **`rssi`**       | <code>number</code> |
| **`macAddress`** | <code>string</code> |


#### ProvisioningCapabilities

| Prop                   | Type                  |
| ---------------------- | --------------------- |
| **`numberOfElements`** | <code>number</code>   |
| **`algorithms`**       | <code>number</code>   |
| **`publicKeyType`**    | <code>number</code>   |
| **`staticOobTypes`**   | <code>number</code>   |
| **`outputOobSize`**    | <code>number</code>   |
| **`outputOobActions`** | <code>string[]</code> |
| **`inputOobSize`**     | <code>number</code>   |
| **`inputOobActions`**  | <code>string[]</code> |

</docgen-api>
