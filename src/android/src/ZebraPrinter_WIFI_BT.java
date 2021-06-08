package it.zenitlab.cordova.plugins.zbtprinter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.graphics.internal.ZebraImageAndroid;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.SGD;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.ZebraPrinterLinkOs;
import com.zebra.sdk.printer.discovery.BluetoothDiscoverer;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Set;

public class ZebraPrinter_WIFI_BT extends CordovaPlugin implements DiscoveryHandler {

    private static final String LOG_TAG = "ZebraPrinter_WIFI_BT";
    private CallbackContext callbackContext;
    private boolean printerFound;
    private Connection thePrinterConn;
    private PrinterStatus printerStatus;
    private ZebraPrinter printer;
    private final int MAX_PRINT_RETRIES = 1;
    private Connection printerConnection;

    public ZebraPrinter_WIFI_BT() {}

    // @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        if (action.equals("printImagesBT")) {
            try {
                JSONArray images = args.getJSONArray(0);
                String MACAddress = args.getString(1);
                sendImagesBT(images, MACAddress);
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        } else if (action.equals("discoverPrinters")) {
            discoverPrinters();
            return true;
        } else if (action.equals("listBondedBTDevices")) {
            listBondedBTDevices(callbackContext);
            return true;
        }else if (action.equals("getPrinterName")) {
            String MACAddress = args.getString(0);
            getPrinterName(MACAddress);
            return true;
        } else if (action.equals("connectWifi")) {
            try {
                String address = args.getString(0);
                connectWifi(address);
            } catch (Exception e){
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        }
        else if (action.equals("printImagesWifi")) {
            try {
                JSONArray images = args.getJSONArray(0);
                String NETAddress = args.getString(1);
                sendImagesWIFI(images, NETAddress);
            } catch (IOException e){
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        }
        else if (action.equals("disconnectWifi")) {
            try {
                disconnectWifi();
            } catch (Exception e){
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        }

        return false;
    }

    private void sendImagesBT(final JSONArray labels, final String MACAddress) throws IOException 
    {
        printImagesBT(labels, MACAddress);
    }

    private void sendImagesWIFI(final JSONArray labels, final String NETAddress) throws IOException 
    {
        printImagesWifi(labels, NETAddress);                   
    }

    private void printImagesBT(JSONArray labels, String MACAddress) {
        try 
        {
            boolean isConnected = openBluetoothConnection(MACAddress);
            if (isConnected)
            {
                initializePrinter();

                boolean isPrinterReady = getPrinterStatus(0);

                if (isPrinterReady) 
                {
                    printImages(labels);
                    callbackContext.success();
                } 
                else 
                {
                    callbackContext.error("La impresora no está lista");
                }
            }
            else
            {
                callbackContext.error("No se pudo conectar");
            }
        } 
        catch (ConnectionException e) 
        {
            Log.e(LOG_TAG, "Connection exception: " + e.getMessage());
            if (e.getMessage().toLowerCase().contains("broken pipe")) {
                callbackContext.error("Povezava med napravo in tiskalnikom je bila prekinjena. Poskusite znova.");
            } else if (e.getMessage().toLowerCase().contains("socket might closed")) {
                int SEARCH_NEW_PRINTERS = -1;
                callbackContext.error(SEARCH_NEW_PRINTERS);
            } else {
                callbackContext.error("Prišlo je do neznane napake tiskalnika. Znova zaženite tiskalnik in poskusite znova.");
            }
        } 
        catch (ZebraPrinterLanguageUnknownException e) 
        {
            Log.e(LOG_TAG, "ZebraPrinterLanguageUnknown exception: " + e.getMessage());
            callbackContext.error("Prišlo je do neznane napake tiskalnika. Znova zaženite tiskalnik in poskusite znova.");
        } 
        catch (Exception e) 
        {
            Log.e(LOG_TAG, "Exception: " + e.getMessage());
            callbackContext.error(e.getMessage());
        }
        finally
        {
            try
            {
                thePrinterConn.close();
            }
            catch(Exception e) 
            {}           
        }
    }

    private void printImagesWifi(JSONArray labels, String NETAddress) {
        try 
        {
            connectWifi(NETAddress);
            printImages(labels);
            callbackContext.success();
        } 
        catch (ConnectionException e) 
        {
            Log.e(LOG_TAG, "Connection exception: " + e.getMessage());
            if (e.getMessage().toLowerCase().contains("broken pipe")) {
                callbackContext.error("Povezava med napravo in tiskalnikom je bila prekinjena. Poskusite znova.");
            } else if (e.getMessage().toLowerCase().contains("socket might closed")) {
                int SEARCH_NEW_PRINTERS = -1;
                callbackContext.error(SEARCH_NEW_PRINTERS);
            } else {
                callbackContext.error("Prišlo je do neznane napake tiskalnika. Znova zaženite tiskalnik in poskusite znova.");
            }
        } catch (ZebraPrinterLanguageUnknownException e) {
            Log.e(LOG_TAG, "ZebraPrinterLanguageUnknown exception: " + e.getMessage());
            callbackContext.error("Prišlo je do neznane napake tiskalnika. Znova zaženite tiskalnik in poskusite znova.");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception: " + e.getMessage());
            callbackContext.error(e.getMessage());
        }
        finally
        {
            try
            {
                disconnectWifi();
            }
            catch(Exception e) 
            {}           
        }
    }

    private void initializePrinter() throws ConnectionException, ZebraPrinterLanguageUnknownException {
        Log.d(LOG_TAG, "Initializing printer...");
        printer = ZebraPrinterFactory.getInstance(thePrinterConn);
        String printerLanguage = SGD.GET("device.languages", thePrinterConn);

        if (!printerLanguage.contains("zpl")) {
            SGD.SET("device.languages", "hybrid_xml_zpl", thePrinterConn);
            Log.d(LOG_TAG, "printer language set...");
        }
    }

    private boolean openBluetoothConnection(String MACAddress) throws ConnectionException {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        try
        {
            thePrinterConn.close();
        }
        catch(Exception e) 
        {}

        if (bluetoothAdapter.isEnabled()) {
            Log.d(LOG_TAG, "Creating a bluetooth-connection for mac-address " + MACAddress);

            thePrinterConn = new BluetoothConnection(MACAddress);

            Log.d(LOG_TAG, "Opening connection...");
            thePrinterConn.open();
            Log.d(LOG_TAG, "connection successfully opened...");

            return true;
        } else {
            Log.d(LOG_TAG, "Bluetooth is disabled...");
            callbackContext.error("Bluetooth ni vklopljen.");
        }
        return false;
    }

    private void printImages(JSONArray labels) throws Exception {
        ZebraPrinterLinkOs zebraPrinterLinkOs = ZebraPrinterFactory.createLinkOsPrinter(printer);

        for (int i = labels.length() - 1; i >= 0; i--) 
        {
            String base64Image = labels.get(i).toString();
            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);

            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            ZebraImageAndroid zebraimage = new ZebraImageAndroid(decodedByte);

            if (zebraPrinterLinkOs != null) 
            {
                printer.printImage(zebraimage, 0, 0, zebraimage.getWidth(), zebraimage.getHeight(), false);
            }
            else
            {
                Log.d(LOG_TAG, "Storing label on printer...");
                printer.storeImage("wgkimage.pcx", zebraimage, -1, -1);
                printImageTheOldWay(zebraimage);
                SGD.SET("device.languages", "line_print", thePrinterConn);
            }
            //Thread.sleep(3000);
        }
    }

    private void printImageTheOldWay(ZebraImageAndroid zebraimage) throws Exception {

        Log.d(LOG_TAG, "Printing image...");

        String cpcl = "! 0 200 200 ";
        cpcl += zebraimage.getHeight();
        cpcl += " 1\r\n";
		// print diff
        cpcl += "PW 750\r\nTONE 0\r\nSPEED 6\r\nSETFF 203 5\r\nON - FEED FEED\r\nAUTO - PACE\r\nJOURNAL\r\n";
		//cpcl += "TONE 0\r\nJOURNAL\r\n";
        cpcl += "PCX 150 0 !<wgkimage.pcx\r\n";
        cpcl += "FORM\r\n";
        cpcl += "PRINT\r\n";
        thePrinterConn.write(cpcl.getBytes());
    }

    private void listBondedBTDevices(CallbackContext callbackContext) throws JSONException {
        JSONArray deviceList = new JSONArray();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

        for (BluetoothDevice device : bondedDevices) {
            deviceList.put(deviceToJSON(device));
        }
        callbackContext.success(deviceList);
    }

    private JSONObject deviceToJSON(BluetoothDevice device) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", device.getName());
        json.put("address", device.getAddress());
        json.put("id", device.getAddress());
        if (device.getBluetoothClass() != null) {
            json.put("class", device.getBluetoothClass().getDeviceClass());
        }
        return json;
    }

    private boolean getPrinterStatus(int retryAttempt) throws Exception {
        try {
            printerStatus = printer.getCurrentStatus();

            if (printerStatus.isReadyToPrint) {
                Log.d(LOG_TAG, "Printer is ready to print...");
                return true;
            } else {
                if (printerStatus.isPaused) {
                    throw new Exception("Printer is gepauzeerd. Gelieve deze eerst te activeren.");
                } else if (printerStatus.isHeadOpen) {
                    throw new Exception("Printer staat open. Gelieve deze eerst te sluiten.");
                } else if (printerStatus.isPaperOut) {
                    throw new Exception("Gelieve eerst de etiketten aan te vullen.");
                } else {
                    throw new Exception("Kon de printerstatus niet ophalen. Gelieve opnieuw te proberen. " +
                        "Herstart de printer indien dit probleem zich blijft voordoen");
                }
            }
        } catch (ConnectionException e) {
            if (retryAttempt < MAX_PRINT_RETRIES) {
                Thread.sleep(5000);
                return getPrinterStatus(++retryAttempt);
            } else {
                throw new Exception("Kon de printerstatus niet ophalen. Gelieve opnieuw te proberen. " +
                    "Herstart de printer indien dit probleem zich blijft voordoen.");
            }
        }

    }

    /**
     * Gebruik de Zebra Android SDK om de lengte te bepalen indien de printer LINK-OS ondersteunt
     *
     * @param zebraimage
     * @throws Exception
     */
    private void setLabelLength(ZebraImageAndroid zebraimage) throws Exception {
        ZebraPrinterLinkOs zebraPrinterLinkOs = ZebraPrinterFactory.createLinkOsPrinter(printer);

        if (zebraPrinterLinkOs != null) {
            String currentLabelLength = zebraPrinterLinkOs.getSettingValue("zpl.label_length");
			Log.d(LOG_TAG, "mitja " + currentLabelLength);
            if (!currentLabelLength.equals(String.valueOf(zebraimage.getHeight()))) {
				// printer_diff
				Log.d(LOG_TAG, "mitja me " + zebraimage.getHeight());
                zebraPrinterLinkOs.setSetting("zpl.label_length", zebraimage.getHeight() + "");
            }
        }
    }

    private void discoverPrinters() {
        printerFound = false;

        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                try {

                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (bluetoothAdapter.isEnabled()) {
                        Log.d(LOG_TAG, "Searching for printers...");
                        BluetoothDiscoverer.findPrinters(cordova.getActivity().getApplicationContext(), ZebraPrinter_WIFI_BT.this);
                    } else {
                        Log.d(LOG_TAG, "Bluetooth is disabled...");
                        callbackContext.error("Bluetooth ni vklopljen.");
                    }

                } catch (ConnectionException e) {
                    Log.e(LOG_TAG, "Connection exception: " + e.getMessage());
                    callbackContext.error(e.getMessage());
                } finally {
                    Looper.myLooper().quit();
                }
            }
        }).start();
    }

    private void getPrinterName(final String macAddress) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String printerName = searchPrinterNameForMacAddress(macAddress);

                if (printerName != null) {
                    Log.d(LOG_TAG, "Successfully found connected printer with name " + printerName);
                    callbackContext.success(printerName);
                } else {
                    callbackContext.error("Ni najdenega tiskalnika. Če težave ne odpravite, ponovno zaženite tiskalnik.");
                }
            }
        }).start();
    }

    private String searchPrinterNameForMacAddress(String macAddress) {
        Log.d(LOG_TAG, "Connecting with printer " + macAddress + " over bluetooth...");

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                Log.d(LOG_TAG, "Paired device found: " + device.getName());
                if (device.getAddress().equalsIgnoreCase(macAddress)) {
                    return device.getName();
                }
            }
        }
        return null;
    }

    @Override
    public void foundPrinter(DiscoveredPrinter discoveredPrinter) {
        Log.d(LOG_TAG, "Printer found: " + discoveredPrinter.address);
        if (!printerFound) {
            printerFound = true;
            callbackContext.success(discoveredPrinter.address);
        }
    }

    @Override
    public void discoveryFinished() {
        Log.d(LOG_TAG, "Finished searching for printers...");
        if (!printerFound) {
            callbackContext.error("Ni najdenega tiskalnika. Če težave ne odpravite, ponovno zaženite tiskalnik.");
        }
    }

    @Override
    public void discoveryError(String s) {
        Log.e(LOG_TAG, "An error occurred while searching for printers. Message: " + s);
        callbackContext.error(s);
    }

    // WIFI
    public boolean connectWifi(String address) {
        
        disconnectWifi();

        try 
        {
            printerConnection = new TcpConnection(address, 9100);
            printerConnection.open();
        } 
        catch (Exception e) 
        {
            callbackContext.error(e.getMessage());
            return false;
        }

        if (printerConnection.isConnected()) 
        {
            try 
            {
                printer = ZebraPrinterFactory.getInstance(printerConnection);
            } 
            catch (ConnectionException e) 
            {
                printer = null;
                callbackContext.error(e.getMessage());
                return false;
            } 
            catch (ZebraPrinterLanguageUnknownException e) 
            {
                printer = null;
                callbackContext.error(e.getMessage());
                return false;
            }
        }
        else
        {
            callbackContext.error("Not connnected");
            return false;
        }
        callbackContext.success();
        return true;
    }

    public void disconnectWifi() {
        try 
        {
            if (printerConnection != null) 
            {
                printerConnection.close();
            }
            callbackContext.success();
        }
        catch (ConnectionException e)
        {
            callbackContext.error(e.getMessage());
        } 
    }

}

