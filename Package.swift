// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "BluetoothMeshPlugin",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "BluetoothMeshPlugin",
            targets: ["BluetoothMeshPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", branch: "main")
    ],
    targets: [
        .target(
            name: "BluetoothMeshPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/BluetoothMeshPlugin"),
        .testTarget(
            name: "BluetoothMeshPluginTests",
            dependencies: ["BluetoothMeshPlugin"],
            path: "ios/Tests/BluetoothMeshPluginTests")
    ]
)