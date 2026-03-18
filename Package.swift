// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "BluetoothMeshPlugin",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "BluetoothMeshPlugin",
            targets: ["BluetoothMeshPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", branch: "main"),
        .package(name: "NordicMesh", path: "./IOS-nRF-Mesh-Library")
    ],
    targets: [
        .target(
            name: "BluetoothMeshPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm"),
                .product(name: "NordicMesh", package: "NordicMesh")
            ],
            path: "ios/Sources/BluetoothMeshPlugin"),
        .testTarget(
            name: "BluetoothMeshPluginTests",
            dependencies: ["BluetoothMeshPlugin"],
            path: "ios/Tests/BluetoothMeshPluginTests")
    ]
)