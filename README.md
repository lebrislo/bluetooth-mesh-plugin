# bluetooth-mesh-plugin

Capacitor plugin for Bluetooth Mesh, based on nRF Mesh Libraries

## Install

```bash
npm install bluetooth-mesh-plugin
npx cap sync
```

## API

<docgen-index>

* [`isBluetoothEnabled()`](#isbluetoothenabled)
* [`requestBluetoothEnable()`](#requestbluetoothenable)
* [`isBluetoothConnected()`](#isbluetoothconnected)
* [`disconnectBle(...)`](#disconnectble)
* [`checkPermissions()`](#checkpermissions)
* [`requestPermissions()`](#requestpermissions)
* [`initMeshNetwork(...)`](#initmeshnetwork)
* [`exportMeshNetwork()`](#exportmeshnetwork)
* [`importMeshNetwork(...)`](#importmeshnetwork)
* [`fetchMeshDevices()`](#fetchmeshdevices)
* [`reloadScanMeshDevices()`](#reloadscanmeshdevices)
* [`getNodesOnlineStates()`](#getnodesonlinestates)
* [`getProvisioningCapabilities(...)`](#getprovisioningcapabilities)
* [`provisionDevice(...)`](#provisiondevice)
* [`unprovisionDevice(...)`](#unprovisiondevice)
* [`createApplicationKey()`](#createapplicationkey)
* [`removeApplicationKey(...)`](#removeapplicationkey)
* [`addApplicationKeyToNode(...)`](#addapplicationkeytonode)
* [`bindApplicationKeyToModel(...)`](#bindapplicationkeytomodel)
* [`getCompositionData(...)`](#getcompositiondata)
* [`sendGenericOnOffSet(...)`](#sendgenericonoffset)
* [`sendGenericOnOffGet(...)`](#sendgenericonoffget)
* [`sendGenericPowerLevelSet(...)`](#sendgenericpowerlevelset)
* [`sendGenericPowerLevelGet(...)`](#sendgenericpowerlevelget)
* [`sendLightHslSet(...)`](#sendlighthslset)
* [`sendLightHslGet(...)`](#sendlighthslget)
* [`sendLightCtlSet(...)`](#sendlightctlset)
* [`sendLightCtlGet(...)`](#sendlightctlget)
* [`sendLightCtlTemperatureRangeSet(...)`](#sendlightctltemperaturerangeset)
* [`sendLightCtlTemperatureRangeGet(...)`](#sendlightctltemperaturerangeget)
* [`sendVendorModelMessage(...)`](#sendvendormodelmessage)
* [`sendConfigHeartbeatPublicationSet(...)`](#sendconfigheartbeatpublicationset)
* [`sendHealthFaultGet(...)`](#sendhealthfaultget)
* [`addListener(string, ...)`](#addlistenerstring-)
* [`removeAllListeners()`](#removealllisteners)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### isBluetoothEnabled()

```typescript
isBluetoothEnabled() => Promise<BluetoothState>
```

**Returns:** <code>Promise&lt;<a href="#bluetoothstate">BluetoothState</a>&gt;</code>

--------------------


### requestBluetoothEnable()

```typescript
requestBluetoothEnable() => Promise<BluetoothState>
```

**Returns:** <code>Promise&lt;<a href="#bluetoothstate">BluetoothState</a>&gt;</code>

--------------------


### isBluetoothConnected()

```typescript
isBluetoothConnected() => Promise<BluetoothConnectionStatus>
```

**Returns:** <code>Promise&lt;<a href="#bluetoothconnectionstatus">BluetoothConnectionStatus</a>&gt;</code>

--------------------


### disconnectBle(...)

```typescript
disconnectBle(options: { autoReconnect?: boolean; }) => Promise<void>
```

| Param         | Type                                      |
| ------------- | ----------------------------------------- |
| **`options`** | <code>{ autoReconnect?: boolean; }</code> |

--------------------


### checkPermissions()

```typescript
checkPermissions() => Promise<Permissions>
```

**Returns:** <code>Promise&lt;<a href="#permissions">Permissions</a>&gt;</code>

--------------------


### requestPermissions()

```typescript
requestPermissions() => Promise<any>
```

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### initMeshNetwork(...)

```typescript
initMeshNetwork(options: { networkName: string; }) => Promise<MeshNetworkObject>
```

| Param         | Type                                  |
| ------------- | ------------------------------------- |
| **`options`** | <code>{ networkName: string; }</code> |

**Returns:** <code>Promise&lt;<a href="#meshnetworkobject">MeshNetworkObject</a>&gt;</code>

--------------------


### exportMeshNetwork()

```typescript
exportMeshNetwork() => Promise<MeshNetworkObject>
```

**Returns:** <code>Promise&lt;<a href="#meshnetworkobject">MeshNetworkObject</a>&gt;</code>

--------------------


### importMeshNetwork(...)

```typescript
importMeshNetwork(options: { meshNetwork: string; }) => Promise<void>
```

| Param         | Type                                  |
| ------------- | ------------------------------------- |
| **`options`** | <code>{ meshNetwork: string; }</code> |

--------------------


### fetchMeshDevices()

```typescript
fetchMeshDevices() => Promise<ScanMeshDevices>
```

**Returns:** <code>Promise&lt;<a href="#scanmeshdevices">ScanMeshDevices</a>&gt;</code>

--------------------


### reloadScanMeshDevices()

```typescript
reloadScanMeshDevices() => Promise<void>
```

--------------------


### getNodesOnlineStates()

```typescript
getNodesOnlineStates() => Promise<NodesOnlineStates>
```

**Returns:** <code>Promise&lt;<a href="#nodesonlinestates">NodesOnlineStates</a>&gt;</code>

--------------------


### getProvisioningCapabilities(...)

```typescript
getProvisioningCapabilities(options: { macAddress: string; uuid: string; }) => Promise<ProvisioningCapabilities>
```

| Param         | Type                                               |
| ------------- | -------------------------------------------------- |
| **`options`** | <code>{ macAddress: string; uuid: string; }</code> |

**Returns:** <code>Promise&lt;<a href="#provisioningcapabilities">ProvisioningCapabilities</a>&gt;</code>

--------------------


### provisionDevice(...)

```typescript
provisionDevice(options: { macAddress: string; uuid: string; }) => Promise<ProvisioningStatus>
```

| Param         | Type                                               |
| ------------- | -------------------------------------------------- |
| **`options`** | <code>{ macAddress: string; uuid: string; }</code> |

**Returns:** <code>Promise&lt;<a href="#provisioningstatus">ProvisioningStatus</a>&gt;</code>

--------------------


### unprovisionDevice(...)

```typescript
unprovisionDevice(options: { unicastAddress: number; }) => Promise<UnprovisionStatus>
```

| Param         | Type                                     |
| ------------- | ---------------------------------------- |
| **`options`** | <code>{ unicastAddress: number; }</code> |

**Returns:** <code>Promise&lt;<a href="#unprovisionstatus">UnprovisionStatus</a>&gt;</code>

--------------------


### createApplicationKey()

```typescript
createApplicationKey() => Promise<ModelMessageStatus>
```

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a>&gt;</code>

--------------------


### removeApplicationKey(...)

```typescript
removeApplicationKey(options: { appKeyIndex: number; }) => Promise<ModelMessageStatus>
```

| Param         | Type                                  |
| ------------- | ------------------------------------- |
| **`options`** | <code>{ appKeyIndex: number; }</code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a>&gt;</code>

--------------------


### addApplicationKeyToNode(...)

```typescript
addApplicationKeyToNode(options: ModelMessage) => Promise<ModelMessageStatus>
```

| Param         | Type                                                  |
| ------------- | ----------------------------------------------------- |
| **`options`** | <code><a href="#modelmessage">ModelMessage</a></code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a>&gt;</code>

--------------------


### bindApplicationKeyToModel(...)

```typescript
bindApplicationKeyToModel(options: ModelMessage & { modelId: number; }) => Promise<ModelMessageStatus>
```

| Param         | Type                                                                         |
| ------------- | ---------------------------------------------------------------------------- |
| **`options`** | <code><a href="#modelmessage">ModelMessage</a> & { modelId: number; }</code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a>&gt;</code>

--------------------


### getCompositionData(...)

```typescript
getCompositionData(options: { unicastAddress: number; }) => Promise<MeshNetworkObject>
```

| Param         | Type                                     |
| ------------- | ---------------------------------------- |
| **`options`** | <code>{ unicastAddress: number; }</code> |

**Returns:** <code>Promise&lt;<a href="#meshnetworkobject">MeshNetworkObject</a>&gt;</code>

--------------------


### sendGenericOnOffSet(...)

```typescript
sendGenericOnOffSet(options: ModelMessage & { onOff: boolean; }) => Promise<ModelMessageStatus>
```

| Param         | Type                                                                        |
| ------------- | --------------------------------------------------------------------------- |
| **`options`** | <code><a href="#modelmessage">ModelMessage</a> & { onOff: boolean; }</code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a>&gt;</code>

--------------------


### sendGenericOnOffGet(...)

```typescript
sendGenericOnOffGet(options: ModelMessage) => Promise<ModelMessageStatus>
```

| Param         | Type                                                  |
| ------------- | ----------------------------------------------------- |
| **`options`** | <code><a href="#modelmessage">ModelMessage</a></code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a>&gt;</code>

--------------------


### sendGenericPowerLevelSet(...)

```typescript
sendGenericPowerLevelSet(options: ModelMessage & { powerLevel: number; }) => Promise<ModelMessageStatus>
```

| Param         | Type                                                                            |
| ------------- | ------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#modelmessage">ModelMessage</a> & { powerLevel: number; }</code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a>&gt;</code>

--------------------


### sendGenericPowerLevelGet(...)

```typescript
sendGenericPowerLevelGet(options: ModelMessage) => Promise<ModelMessageStatus>
```

| Param         | Type                                                  |
| ------------- | ----------------------------------------------------- |
| **`options`** | <code><a href="#modelmessage">ModelMessage</a></code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a>&gt;</code>

--------------------


### sendLightHslSet(...)

```typescript
sendLightHslSet(options: ModelMessage & { hue: number; saturation: number; lightness: number; }) => Promise<ModelMessageStatus>
```

| Param         | Type                                                                                                            |
| ------------- | --------------------------------------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#modelmessage">ModelMessage</a> & { hue: number; saturation: number; lightness: number; }</code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a>&gt;</code>

--------------------


### sendLightHslGet(...)

```typescript
sendLightHslGet(options: ModelMessage) => Promise<ModelMessageStatus>
```

| Param         | Type                                                  |
| ------------- | ----------------------------------------------------- |
| **`options`** | <code><a href="#modelmessage">ModelMessage</a></code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a>&gt;</code>

--------------------


### sendLightCtlSet(...)

```typescript
sendLightCtlSet(options: ModelMessage & { lightness: number; temperature: number; deltaUv: number; }) => Promise<ModelMessageStatus>
```

| Param         | Type                                                                                                                 |
| ------------- | -------------------------------------------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#modelmessage">ModelMessage</a> & { lightness: number; temperature: number; deltaUv: number; }</code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a>&gt;</code>

--------------------


### sendLightCtlGet(...)

```typescript
sendLightCtlGet(options: ModelMessage) => Promise<ModelMessageStatus>
```

| Param         | Type                                                  |
| ------------- | ----------------------------------------------------- |
| **`options`** | <code><a href="#modelmessage">ModelMessage</a></code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a>&gt;</code>

--------------------


### sendLightCtlTemperatureRangeSet(...)

```typescript
sendLightCtlTemperatureRangeSet(options: ModelMessage & { rangeMin: number; rangeMax: number; }) => Promise<ModelMessageStatus>
```

| Param         | Type                                                                                            |
| ------------- | ----------------------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#modelmessage">ModelMessage</a> & { rangeMin: number; rangeMax: number; }</code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a>&gt;</code>

--------------------


### sendLightCtlTemperatureRangeGet(...)

```typescript
sendLightCtlTemperatureRangeGet(options: ModelMessage) => Promise<ModelMessageStatus>
```

| Param         | Type                                                  |
| ------------- | ----------------------------------------------------- |
| **`options`** | <code><a href="#modelmessage">ModelMessage</a></code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a>&gt;</code>

--------------------


### sendVendorModelMessage(...)

```typescript
sendVendorModelMessage(options: ModelMessage & { modelId: number; opcode: number; payload?: Uint8Array; opPairCode?: number; }) => Promise<ModelMessageStatus>
```

| Param         | Type                                                                                                                                                              |
| ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#modelmessage">ModelMessage</a> & { modelId: number; opcode: number; payload?: <a href="#uint8array">Uint8Array</a>; opPairCode?: number; }</code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a>&gt;</code>

--------------------


### sendConfigHeartbeatPublicationSet(...)

```typescript
sendConfigHeartbeatPublicationSet(options: ConfigHeartbeatPublicationSet) => Promise<void>
```

| Param         | Type                                                                                    |
| ------------- | --------------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#configheartbeatpublicationset">ConfigHeartbeatPublicationSet</a></code> |

--------------------


### sendHealthFaultGet(...)

```typescript
sendHealthFaultGet(options: ModelMessage & { companyId: number; }) => Promise<ModelMessageStatus>
```

| Param         | Type                                                                           |
| ------------- | ------------------------------------------------------------------------------ |
| **`options`** | <code><a href="#modelmessage">ModelMessage</a> & { companyId: number; }</code> |

**Returns:** <code>Promise&lt;<a href="#modelmessagestatus">ModelMessageStatus</a>&gt;</code>

--------------------


### addListener(string, ...)

```typescript
addListener(eventName: string, listenerFunc: (event: any) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                 |
| ------------------ | ------------------------------------ |
| **`eventName`**    | <code>string</code>                  |
| **`listenerFunc`** | <code>(event: any) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

--------------------


### Interfaces


#### BluetoothState

| Prop          | Type                 |
| ------------- | -------------------- |
| **`enabled`** | <code>boolean</code> |


#### BluetoothConnectionStatus

| Prop             | Type                 |
| ---------------- | -------------------- |
| **`connected`**  | <code>boolean</code> |
| **`macAddress`** | <code>string</code>  |


#### Permissions


#### MeshNetworkObject

| Prop              | Type                |
| ----------------- | ------------------- |
| **`meshNetwork`** | <code>string</code> |


#### ScanMeshDevices

| Prop                | Type                         |
| ------------------- | ---------------------------- |
| **`unprovisioned`** | <code>BleMeshDevice[]</code> |
| **`provisioned`**   | <code>BleMeshDevice[]</code> |


#### BleMeshDevice

| Prop             | Type                |
| ---------------- | ------------------- |
| **`name`**       | <code>string</code> |
| **`uuid`**       | <code>string</code> |
| **`rssi`**       | <code>number</code> |
| **`macAddress`** | <code>string</code> |


#### NodesOnlineStates

| Prop         | Type                       |
| ------------ | -------------------------- |
| **`states`** | <code>OnlineState[]</code> |


#### OnlineState

| Prop                 | Type                 |
| -------------------- | -------------------- |
| **`unicastAddress`** | <code>number</code>  |
| **`isOnline`**       | <code>boolean</code> |


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


#### UnprovisionStatus

| Prop         | Type                 |
| ------------ | -------------------- |
| **`status`** | <code>boolean</code> |


#### ModelMessageStatus

| Prop                | Type                |
| ------------------- | ------------------- |
| **`src`**           | <code>number</code> |
| **`dst`**           | <code>number</code> |
| **`opcode`**        | <code>number</code> |
| **`vendorModelId`** | <code>number</code> |
| **`data`**          | <code>any</code>    |


#### ModelMessage

| Prop                  | Type                 |
| --------------------- | -------------------- |
| **`unicastAddress`**  | <code>number</code>  |
| **`appKeyIndex`**     | <code>number</code>  |
| **`acknowledgement`** | <code>boolean</code> |


#### Uint8Array

A typed array of 8-bit unsigned integer values. The contents are initialized to 0. If the
requested number of bytes could not be allocated an exception is raised.

| Prop                    | Type                                                        | Description                                                                  |
| ----------------------- | ----------------------------------------------------------- | ---------------------------------------------------------------------------- |
| **`BYTES_PER_ELEMENT`** | <code>number</code>                                         | The size in bytes of each element in the array.                              |
| **`buffer`**            | <code><a href="#arraybufferlike">ArrayBufferLike</a></code> | The <a href="#arraybuffer">ArrayBuffer</a> instance referenced by the array. |
| **`byteLength`**        | <code>number</code>                                         | The length in bytes of the array.                                            |
| **`byteOffset`**        | <code>number</code>                                         | The offset in bytes of the array.                                            |
| **`length`**            | <code>number</code>                                         | The length of the array.                                                     |

| Method             | Signature                                                                                                                                                                      | Description                                                                                                                                                                                                                                 |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **copyWithin**     | (target: number, start: number, end?: number \| undefined) =&gt; this                                                                                                          | Returns the this object after copying a section of the array identified by start and end to the same array starting at position target                                                                                                      |
| **every**          | (predicate: (value: number, index: number, array: <a href="#uint8array">Uint8Array</a>) =&gt; unknown, thisArg?: any) =&gt; boolean                                            | Determines whether all the members of an array satisfy the specified test.                                                                                                                                                                  |
| **fill**           | (value: number, start?: number \| undefined, end?: number \| undefined) =&gt; this                                                                                             | Returns the this object after filling the section identified by start and end with value                                                                                                                                                    |
| **filter**         | (predicate: (value: number, index: number, array: <a href="#uint8array">Uint8Array</a>) =&gt; any, thisArg?: any) =&gt; <a href="#uint8array">Uint8Array</a>                   | Returns the elements of an array that meet the condition specified in a callback function.                                                                                                                                                  |
| **find**           | (predicate: (value: number, index: number, obj: <a href="#uint8array">Uint8Array</a>) =&gt; boolean, thisArg?: any) =&gt; number \| undefined                                  | Returns the value of the first element in the array where predicate is true, and undefined otherwise.                                                                                                                                       |
| **findIndex**      | (predicate: (value: number, index: number, obj: <a href="#uint8array">Uint8Array</a>) =&gt; boolean, thisArg?: any) =&gt; number                                               | Returns the index of the first element in the array where predicate is true, and -1 otherwise.                                                                                                                                              |
| **forEach**        | (callbackfn: (value: number, index: number, array: <a href="#uint8array">Uint8Array</a>) =&gt; void, thisArg?: any) =&gt; void                                                 | Performs the specified action for each element in an array.                                                                                                                                                                                 |
| **indexOf**        | (searchElement: number, fromIndex?: number \| undefined) =&gt; number                                                                                                          | Returns the index of the first occurrence of a value in an array.                                                                                                                                                                           |
| **join**           | (separator?: string \| undefined) =&gt; string                                                                                                                                 | Adds all the elements of an array separated by the specified separator string.                                                                                                                                                              |
| **lastIndexOf**    | (searchElement: number, fromIndex?: number \| undefined) =&gt; number                                                                                                          | Returns the index of the last occurrence of a value in an array.                                                                                                                                                                            |
| **map**            | (callbackfn: (value: number, index: number, array: <a href="#uint8array">Uint8Array</a>) =&gt; number, thisArg?: any) =&gt; <a href="#uint8array">Uint8Array</a>               | Calls a defined callback function on each element of an array, and returns an array that contains the results.                                                                                                                              |
| **reduce**         | (callbackfn: (previousValue: number, currentValue: number, currentIndex: number, array: <a href="#uint8array">Uint8Array</a>) =&gt; number) =&gt; number                       | Calls the specified callback function for all the elements in an array. The return value of the callback function is the accumulated result, and is provided as an argument in the next call to the callback function.                      |
| **reduce**         | (callbackfn: (previousValue: number, currentValue: number, currentIndex: number, array: <a href="#uint8array">Uint8Array</a>) =&gt; number, initialValue: number) =&gt; number |                                                                                                                                                                                                                                             |
| **reduce**         | &lt;U&gt;(callbackfn: (previousValue: U, currentValue: number, currentIndex: number, array: <a href="#uint8array">Uint8Array</a>) =&gt; U, initialValue: U) =&gt; U            | Calls the specified callback function for all the elements in an array. The return value of the callback function is the accumulated result, and is provided as an argument in the next call to the callback function.                      |
| **reduceRight**    | (callbackfn: (previousValue: number, currentValue: number, currentIndex: number, array: <a href="#uint8array">Uint8Array</a>) =&gt; number) =&gt; number                       | Calls the specified callback function for all the elements in an array, in descending order. The return value of the callback function is the accumulated result, and is provided as an argument in the next call to the callback function. |
| **reduceRight**    | (callbackfn: (previousValue: number, currentValue: number, currentIndex: number, array: <a href="#uint8array">Uint8Array</a>) =&gt; number, initialValue: number) =&gt; number |                                                                                                                                                                                                                                             |
| **reduceRight**    | &lt;U&gt;(callbackfn: (previousValue: U, currentValue: number, currentIndex: number, array: <a href="#uint8array">Uint8Array</a>) =&gt; U, initialValue: U) =&gt; U            | Calls the specified callback function for all the elements in an array, in descending order. The return value of the callback function is the accumulated result, and is provided as an argument in the next call to the callback function. |
| **reverse**        | () =&gt; <a href="#uint8array">Uint8Array</a>                                                                                                                                  | Reverses the elements in an Array.                                                                                                                                                                                                          |
| **set**            | (array: <a href="#arraylike">ArrayLike</a>&lt;number&gt;, offset?: number \| undefined) =&gt; void                                                                             | Sets a value or an array of values.                                                                                                                                                                                                         |
| **slice**          | (start?: number \| undefined, end?: number \| undefined) =&gt; <a href="#uint8array">Uint8Array</a>                                                                            | Returns a section of an array.                                                                                                                                                                                                              |
| **some**           | (predicate: (value: number, index: number, array: <a href="#uint8array">Uint8Array</a>) =&gt; unknown, thisArg?: any) =&gt; boolean                                            | Determines whether the specified callback function returns true for any element of an array.                                                                                                                                                |
| **sort**           | (compareFn?: ((a: number, b: number) =&gt; number) \| undefined) =&gt; this                                                                                                    | Sorts an array.                                                                                                                                                                                                                             |
| **subarray**       | (begin?: number \| undefined, end?: number \| undefined) =&gt; <a href="#uint8array">Uint8Array</a>                                                                            | Gets a new <a href="#uint8array">Uint8Array</a> view of the <a href="#arraybuffer">ArrayBuffer</a> store for this array, referencing the elements at begin, inclusive, up to end, exclusive.                                                |
| **toLocaleString** | () =&gt; string                                                                                                                                                                | Converts a number to a string by using the current locale.                                                                                                                                                                                  |
| **toString**       | () =&gt; string                                                                                                                                                                | Returns a string representation of an array.                                                                                                                                                                                                |
| **valueOf**        | () =&gt; <a href="#uint8array">Uint8Array</a>                                                                                                                                  | Returns the primitive value of the specified object.                                                                                                                                                                                        |


#### ArrayLike

| Prop         | Type                |
| ------------ | ------------------- |
| **`length`** | <code>number</code> |


#### ArrayBufferTypes

Allowed <a href="#arraybuffer">ArrayBuffer</a> types for the buffer of an ArrayBufferView and related Typed Arrays.

| Prop              | Type                                                |
| ----------------- | --------------------------------------------------- |
| **`ArrayBuffer`** | <code><a href="#arraybuffer">ArrayBuffer</a></code> |


#### ArrayBuffer

Represents a raw buffer of binary data, which is used to store data for the
different typed arrays. ArrayBuffers cannot be read from or written to directly,
but can be passed to a typed array or DataView Object to interpret the raw
buffer as needed.

| Prop             | Type                | Description                                                                     |
| ---------------- | ------------------- | ------------------------------------------------------------------------------- |
| **`byteLength`** | <code>number</code> | Read-only. The length of the <a href="#arraybuffer">ArrayBuffer</a> (in bytes). |

| Method    | Signature                                                                               | Description                                                     |
| --------- | --------------------------------------------------------------------------------------- | --------------------------------------------------------------- |
| **slice** | (begin: number, end?: number \| undefined) =&gt; <a href="#arraybuffer">ArrayBuffer</a> | Returns a section of an <a href="#arraybuffer">ArrayBuffer</a>. |


#### ConfigHeartbeatPublicationSet

| Prop                     | Type                |
| ------------------------ | ------------------- |
| **`unicastAddress`**     | <code>number</code> |
| **`destinationAddress`** | <code>number</code> |
| **`count`**              | <code>number</code> |
| **`period`**             | <code>number</code> |
| **`ttl`**                | <code>number</code> |
| **`netKeyIndex`**        | <code>number</code> |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


### Type Aliases


#### ArrayBufferLike

<code>ArrayBufferTypes[keyof ArrayBufferTypes]</code>

</docgen-api>
