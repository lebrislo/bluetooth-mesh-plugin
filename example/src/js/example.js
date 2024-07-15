import { NrfMesh } from 'nrf-bluetooth-mesh';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    NrfMesh.echo({ value: inputValue })
}
