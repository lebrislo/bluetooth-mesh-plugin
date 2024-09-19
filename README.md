# nrf-bluetooth-mesh

Capacitor plugin for Bluetooth Mesh, based on nRF Mesh Libraries

## Install

```bash
npm install nrf-bluetooth-mesh
npx cap sync
```

## API

<docgen-index>

* [`scanUnprovisionedDevices(...)`](#scanunprovisioneddevices)
* [`scanProvisionedDevices(...)`](#scanprovisioneddevices)
* [`scanMeshDevices(...)`](#scanmeshdevices)
* [`getProvisioningCapabilities(...)`](#getprovisioningcapabilities)
* [`provisionDevice(...)`](#provisiondevice)
* [`unprovisionDevice(...)`](#unprovisiondevice)
* [`createApplicationKey()`](#createapplicationkey)
* [`removeApplicationKey(...)`](#removeapplicationkey)
* [`addApplicationKeyToNode(...)`](#addapplicationkeytonode)
* [`bindApplicationKeyToModel(...)`](#bindapplicationkeytomodel)
* [`compositionDataGet(...)`](#compositiondataget)
* [`sendGenericOnOffSet(...)`](#sendgenericonoffset)
* [`sendGenericPowerLevelSet(...)`](#sendgenericpowerlevelset)
* [`sendLightHslSet(...)`](#sendlighthslset)
* [`sendLightCtlSet(...)`](#sendlightctlset)
* [`sendVendorModelMessage(...)`](#sendvendormodelmessage)
* [`exportMeshNetwork()`](#exportmeshnetwork)
* [`addListener(string, ...)`](#addlistenerstring-)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

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


### scanMeshDevices(...)

```typescript
scanMeshDevices(options: { timeout: number; }) => Promise<ScanMeshDevices>
```

| Param         | Type                              |
| ------------- | --------------------------------- |
| **`options`** | <code>{ timeout: number; }</code> |

**Returns:** <code>Promise&lt;<a href="#scanmeshdevices">ScanMeshDevices</a>&gt;</code>

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
sendGenericOnOffSet(options: { unicastAddress: number; appKeyIndex: number; onOff: boolean; }) => Promise<ModelMessageStatus | PluginCallRejection>
```

| Param         | Type                                                                          |
| ------------- | ----------------------------------------------------------------------------- |
| **`options`** | <code>{ unicastAddress: number; appKeyIndex: number; onOff: boolean; }</code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a> | <a href="#plugincallrejection">PluginCallRejection</a>&gt;</code>

--------------------


### sendGenericPowerLevelSet(...)

```typescript
sendGenericPowerLevelSet(options: { unicastAddress: number; appKeyIndex: number; powerLevel: number; }) => Promise<ModelMessageStatus | PluginCallRejection>
```

| Param         | Type                                                                              |
| ------------- | --------------------------------------------------------------------------------- |
| **`options`** | <code>{ unicastAddress: number; appKeyIndex: number; powerLevel: number; }</code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a> | <a href="#plugincallrejection">PluginCallRejection</a>&gt;</code>

--------------------


### sendLightHslSet(...)

```typescript
sendLightHslSet(options: { unicastAddress: number; appKeyIndex: number; hue: number; saturation: number; lightness: number; }) => Promise<ModelMessageStatus | PluginCallRejection>
```

| Param         | Type                                                                                                              |
| ------------- | ----------------------------------------------------------------------------------------------------------------- |
| **`options`** | <code>{ unicastAddress: number; appKeyIndex: number; hue: number; saturation: number; lightness: number; }</code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a> | <a href="#plugincallrejection">PluginCallRejection</a>&gt;</code>

--------------------


### sendLightCtlSet(...)

```typescript
sendLightCtlSet(options: { unicastAddress: number; appKeyIndex: number; lightness: number; temperature: number; deltaUv: number; }) => Promise<ModelMessageStatus | PluginCallRejection>
```

| Param         | Type                                                                                                                   |
| ------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **`options`** | <code>{ unicastAddress: number; appKeyIndex: number; lightness: number; temperature: number; deltaUv: number; }</code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a> | <a href="#plugincallrejection">PluginCallRejection</a>&gt;</code>

--------------------


### sendVendorModelMessage(...)

```typescript
sendVendorModelMessage(options: { unicastAddress: number; appKeyIndex: number; modelId: number; companyIdentifier: number; opcode: number; parameters: number[]; }) => Promise<ModelMessageStatus | PluginCallRejection>
```

| Param         | Type                                                                                                                                            |
| ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| **`options`** | <code>{ unicastAddress: number; appKeyIndex: number; modelId: number; companyIdentifier: number; opcode: number; parameters: number[]; }</code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a> | <a href="#plugincallrejection">PluginCallRejection</a>&gt;</code>

--------------------


### exportMeshNetwork()

```typescript
exportMeshNetwork() => Promise<object>
```

**Returns:** <code>Promise&lt;object&gt;</code>

--------------------


### addListener(string, ...)

```typescript
addListener(eventName: string, listenerFunc: (event: ReadResult) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                                                  |
| ------------------ | --------------------------------------------------------------------- |
| **`eventName`**    | <code>string</code>                                                   |
| **`listenerFunc`** | <code>(event: <a href="#readresult">ReadResult</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

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


#### ScanMeshDevices

| Prop                | Type                         |
| ------------------- | ---------------------------- |
| **`unprovisioned`** | <code>BleMeshDevice[]</code> |
| **`provisioned`**   | <code>BleMeshDevice[]</code> |


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


#### ModelMessageStatus

| Prop                | Type                |
| ------------------- | ------------------- |
| **`src`**           | <code>number</code> |
| **`dst`**           | <code>number</code> |
| **`opcode`**        | <code>number</code> |
| **`vendorModelId`** | <code>number</code> |
| **`data`**          | <code>object</code> |


#### PluginCallRejection

| Prop          | Type                                                     |
| ------------- | -------------------------------------------------------- |
| **`message`** | <code>string</code>                                      |
| **`data`**    | <code>{ [key: string]: any; methodName: string; }</code> |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### ReadResult

| Prop        | Type                                  |
| ----------- | ------------------------------------- |
| **`value`** | <code><a href="#data">Data</a></code> |


#### DataView

| Prop             | Type                                                |
| ---------------- | --------------------------------------------------- |
| **`buffer`**     | <code><a href="#arraybuffer">ArrayBuffer</a></code> |
| **`byteLength`** | <code>number</code>                                 |
| **`byteOffset`** | <code>number</code>                                 |

| Method         | Signature                                                                           | Description                                                                                                                                                         |
| -------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **getFloat32** | (byteOffset: number, littleEndian?: boolean \| undefined) =&gt; number              | Gets the Float32 value at the specified byte offset from the start of the view. There is no alignment constraint; multi-byte values may be fetched from any offset. |
| **getFloat64** | (byteOffset: number, littleEndian?: boolean \| undefined) =&gt; number              | Gets the Float64 value at the specified byte offset from the start of the view. There is no alignment constraint; multi-byte values may be fetched from any offset. |
| **getInt8**    | (byteOffset: number) =&gt; number                                                   | Gets the Int8 value at the specified byte offset from the start of the view. There is no alignment constraint; multi-byte values may be fetched from any offset.    |
| **getInt16**   | (byteOffset: number, littleEndian?: boolean \| undefined) =&gt; number              | Gets the Int16 value at the specified byte offset from the start of the view. There is no alignment constraint; multi-byte values may be fetched from any offset.   |
| **getInt32**   | (byteOffset: number, littleEndian?: boolean \| undefined) =&gt; number              | Gets the Int32 value at the specified byte offset from the start of the view. There is no alignment constraint; multi-byte values may be fetched from any offset.   |
| **getUint8**   | (byteOffset: number) =&gt; number                                                   | Gets the Uint8 value at the specified byte offset from the start of the view. There is no alignment constraint; multi-byte values may be fetched from any offset.   |
| **getUint16**  | (byteOffset: number, littleEndian?: boolean \| undefined) =&gt; number              | Gets the Uint16 value at the specified byte offset from the start of the view. There is no alignment constraint; multi-byte values may be fetched from any offset.  |
| **getUint32**  | (byteOffset: number, littleEndian?: boolean \| undefined) =&gt; number              | Gets the Uint32 value at the specified byte offset from the start of the view. There is no alignment constraint; multi-byte values may be fetched from any offset.  |
| **setFloat32** | (byteOffset: number, value: number, littleEndian?: boolean \| undefined) =&gt; void | Stores an Float32 value at the specified byte offset from the start of the view.                                                                                    |
| **setFloat64** | (byteOffset: number, value: number, littleEndian?: boolean \| undefined) =&gt; void | Stores an Float64 value at the specified byte offset from the start of the view.                                                                                    |
| **setInt8**    | (byteOffset: number, value: number) =&gt; void                                      | Stores an Int8 value at the specified byte offset from the start of the view.                                                                                       |
| **setInt16**   | (byteOffset: number, value: number, littleEndian?: boolean \| undefined) =&gt; void | Stores an Int16 value at the specified byte offset from the start of the view.                                                                                      |
| **setInt32**   | (byteOffset: number, value: number, littleEndian?: boolean \| undefined) =&gt; void | Stores an Int32 value at the specified byte offset from the start of the view.                                                                                      |
| **setUint8**   | (byteOffset: number, value: number) =&gt; void                                      | Stores an Uint8 value at the specified byte offset from the start of the view.                                                                                      |
| **setUint16**  | (byteOffset: number, value: number, littleEndian?: boolean \| undefined) =&gt; void | Stores an Uint16 value at the specified byte offset from the start of the view.                                                                                     |
| **setUint32**  | (byteOffset: number, value: number, littleEndian?: boolean \| undefined) =&gt; void | Stores an Uint32 value at the specified byte offset from the start of the view.                                                                                     |


#### ArrayBuffer

Represents a raw buffer of binary data, which is used to store data for the
different typed arrays. ArrayBuffers cannot be read from or written to directly,
but can be passed to a typed array or <a href="#dataview">DataView</a> Object to interpret the raw
buffer as needed.

| Prop             | Type                | Description                                                                     |
| ---------------- | ------------------- | ------------------------------------------------------------------------------- |
| **`byteLength`** | <code>number</code> | Read-only. The length of the <a href="#arraybuffer">ArrayBuffer</a> (in bytes). |

| Method    | Signature                                                                               | Description                                                     |
| --------- | --------------------------------------------------------------------------------------- | --------------------------------------------------------------- |
| **slice** | (begin: number, end?: number \| undefined) =&gt; <a href="#arraybuffer">ArrayBuffer</a> | Returns a section of an <a href="#arraybuffer">ArrayBuffer</a>. |


### Type Aliases


#### Data

<code><a href="#dataview">DataView</a> | string</code>

</docgen-api>
