var exec = require('cordova/exec');

exports.listBondedBTDevices = function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'ZebraPrinter_WIFI_BT', 'listBondedBTDevices', []);
};

exports.printImagesBT = function(base64, MACAddress, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'ZebraPrinter_WIFI_BT', 'printImagesBT', [base64, MACAddress]);
};

exports.printImagesWifi = function(base64, NETAddress, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'ZebraPrinter_WIFI_BT', 'printImagesWifi', [base64, NETAddress]);
};

exports.connectWifi = function(NETAddress, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'ZebraPrinter_WIFI_BT', 'connectWifi', [NETAddress]);
};

exports.disconnectWifi = function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'ZebraPrinter_WIFI_BT', 'disconnectWifi', []);
};
