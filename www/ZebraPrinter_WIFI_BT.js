var exec = require('cordova/exec');

exports.listBondedBTDevices = function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'ZebraPrinter_WIFI_BT', 'listBondedBTDevices', []);
};

exports.printImagesBT = function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'ZebraPrinter_WIFI_BT', 'printImagesBT', [base64, MACAddress]);
};

exports.printImagesWifi = function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'ZebraPrinter_WIFI_BT', 'printImagesWifi', [base64, NETAddress]);
};

exports.connectWifi = function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'ZebraPrinter_WIFI_BT', 'connectWifi', [NETAddress]);
};

exports.disconnectWifi = function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'ZebraPrinter_WIFI_BT', 'disconnectWifi', []);
};
