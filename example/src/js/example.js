import { BluetoothMesh } from 'bluetooth-mesh-plugin';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    BluetoothMesh.echo({ value: inputValue })
}
