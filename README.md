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
* [`unprovisionDevice(...)`](#unprovisiondevice)
* [`createApplicationKey()`](#createapplicationkey)
* [`removeApplicationKey(...)`](#removeapplicationkey)
* [`addApplicationKeyToNode(...)`](#addapplicationkeytonode)
* [`bindApplicationKeyToModel(...)`](#bindapplicationkeytomodel)
* [`compositionDataGet(...)`](#compositiondataget)
* [`sendGenericOnOffSet(...)`](#sendgenericonoffset)
* [`exportMeshNetwork()`](#exportmeshnetwork)
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
provisionDevice(options: { uuid: string; }) => Promise<ProvisioningStatus>
```

| Param         | Type                           |
| ------------- | ------------------------------ |
| **`options`** | <code>{ uuid: string; }</code> |

**Returns:** <code>Promise&lt;<a href="#provisioningstatus">ProvisioningStatus</a>&gt;</code>

--------------------


### unprovisionDevice(...)

```typescript
unprovisionDevice(options: { unicastAddress: number; }) => Promise<void>
```

| Param         | Type                                     |
| ------------- | ---------------------------------------- |
| **`options`** | <code>{ unicastAddress: number; }</code> |

--------------------


### createApplicationKey()

```typescript
createApplicationKey() => Promise<void>
```

--------------------


### removeApplicationKey(...)

```typescript
removeApplicationKey(options: { appKeyIndex: number; }) => Promise<void>
```

| Param         | Type                                  |
| ------------- | ------------------------------------- |
| **`options`** | <code>{ appKeyIndex: number; }</code> |

--------------------


### addApplicationKeyToNode(...)

```typescript
addApplicationKeyToNode(options: { unicastAddress: number; appKeyIndex: number; }) => Promise<void>
```

| Param         | Type                                                          |
| ------------- | ------------------------------------------------------------- |
| **`options`** | <code>{ unicastAddress: number; appKeyIndex: number; }</code> |

--------------------


### bindApplicationKeyToModel(...)

```typescript
bindApplicationKeyToModel(options: { elementAddress: number; appKeyIndex: number; modelId: number; }) => Promise<void>
```

| Param         | Type                                                                           |
| ------------- | ------------------------------------------------------------------------------ |
| **`options`** | <code>{ elementAddress: number; appKeyIndex: number; modelId: number; }</code> |

--------------------


### compositionDataGet(...)

```typescript
compositionDataGet(options: { unicastAddress: number; }) => Promise<void>
```

| Param         | Type                                     |
| ------------- | ---------------------------------------- |
| **`options`** | <code>{ unicastAddress: number; }</code> |

--------------------


### sendGenericOnOffSet(...)

```typescript
sendGenericOnOffSet(options: { unicastAddress: number; appKeyIndex: number; onOff: boolean; }) => Promise<void>
```

| Param         | Type                                                                          |
| ------------- | ----------------------------------------------------------------------------- |
| **`options`** | <code>{ unicastAddress: number; appKeyIndex: number; onOff: boolean; }</code> |

--------------------


### exportMeshNetwork()

```typescript
exportMeshNetwork() => Promise<object>
```

**Returns:** <code>Promise&lt;object&gt;</code>

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

| Prop                    | Type                  |
| ----------------------- | --------------------- |
| **`numberOfElements`**  | <code>number</code>   |
| **`availableOOBTypes`** | <code>string[]</code> |
| **`algorithms`**        | <code>number</code>   |
| **`publicKeyType`**     | <code>number</code>   |
| **`staticOobTypes`**    | <code>number</code>   |
| **`outputOobSize`**     | <code>number</code>   |
| **`outputOobActions`**  | <code>number</code>   |
| **`inputOobSize`**      | <code>number</code>   |
| **`inputOobActions`**   | <code>number</code>   |


#### ProvisioningStatus

| Prop                       | Type                 |
| -------------------------- | -------------------- |
| **`provisioningComplete`** | <code>boolean</code> |
| **`uuid`**                 | <code>string</code>  |
| **`unicastAddress`**       | <code>number</code>  |

</docgen-api>
