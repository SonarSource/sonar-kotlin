package checks

import android.bluetooth.BluetoothAdapter
import android.content.ContentResolver
import android.content.Context
import android.net.wifi.WifiInfo
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.ads.identifier.internal.HoldingConnectionClient

typealias ComGoogleAdvertisingIdClient = com.google.android.gms.ads.identifier.AdvertisingIdClient
typealias AndroidxAdsAdvertisingIdClient = androidx.ads.identifier.AdvertisingIdClient
typealias ComHuaweiAdvertisingIdClient = com.huawei.hms.ads.identifier.AdvertisingIdClient
typealias SettingsSecureAlias = android.provider.Settings.Secure

class AndroidPersistentUniqueIdentifierCheckSample {

    class BluetoothAdapterTests {
        class NotTheRealBluetoothAdapter {
            val address = "00:00:00:00:00:00"
        }

        val valPropertyBluetoothAdapter = BluetoothAdapter()
        var varPropertyBluetoothAdapter = BluetoothAdapter()

        fun nonCompliantScenarios(
            bluetoothAdapterFunParam: BluetoothAdapter,
            maybeBluetoothAdapterFunParam: BluetoothAdapter?,
        ) {
            bluetoothAdapterFunParam.address // Noncompliant {{Using a hardware identifier puts user privacy at risk. Make sure it is safe here.}}
            //                       ^^^^^^^
            bluetoothAdapterFunParam.getAddress() // Noncompliant
            //                       ^^^^^^^^^^
            maybeBluetoothAdapterFunParam?.address // Noncompliant
            //                             ^^^^^^^
            maybeBluetoothAdapterFunParam?.getAddress() // Noncompliant
            //                             ^^^^^^^^^^

            // Noncompliant@+2
            // Noncompliant@+1
            var multiple = bluetoothAdapterFunParam.address + bluetoothAdapterFunParam.address

            val withParentheses = ((bluetoothAdapterFunParam).address) // Noncompliant
            //                                                ^^^^^^^

            val localValBluetoothAdapter = bluetoothAdapterFunParam
            localValBluetoothAdapter.address // Noncompliant
            var localVarBluetoothAdapter = bluetoothAdapterFunParam
            localVarBluetoothAdapter.address // Noncompliant
            var inlineBluetoothAdapter = BluetoothAdapter().address // Noncompliant
            valPropertyBluetoothAdapter.address // Noncompliant
            varPropertyBluetoothAdapter.address // Noncompliant

            with (bluetoothAdapterFunParam) { address } // Noncompliant
            bluetoothAdapterFunParam.let { it.address } // Noncompliant
            bluetoothAdapterFunParam.let { with(it) { address } } // Noncompliant
            bluetoothAdapterFunParam?.let { it.address } // Noncompliant
            bluetoothAdapterFunParam.run { address } // Noncompliant
            bluetoothAdapterFunParam.apply { this.address } // Noncompliant

            val localValGetAddressMethodRef = bluetoothAdapterFunParam::getAddress
            localValGetAddressMethodRef() // FN
            var localVarGetAddressMethodRef = bluetoothAdapterFunParam::getAddress
            localVarGetAddressMethodRef() // FN
        }

        fun compliantScenarios(
            bluetoothAdapterFunParam: BluetoothAdapter,
            notTheRealBluetoothAdapter: NotTheRealBluetoothAdapter,
        ) {
            bluetoothAdapterFunParam.state
            bluetoothAdapterFunParam != null
            bluetoothAdapterFunParam.toString()
            bluetoothAdapterFunParam.hashCode()
            bluetoothAdapterFunParam.equals(bluetoothAdapterFunParam)

            notTheRealBluetoothAdapter.address
        }
    }

    class WifiInfoTest {
        fun nonCompliantScenarios(wifiInfo: WifiInfo) {
            wifiInfo.macAddress // Noncompliant {{Using a hardware identifier puts user privacy at risk. Make sure it is safe here.}}
            //       ^^^^^^^^^^
            wifiInfo.getMacAddress() // Noncompliant
        }

        fun compliantScenarios(wifiInfo: WifiInfo) {
            wifiInfo.lostTxPacketsPerSecond
            wifiInfo.lostTxPacketsPerSecond = 0.0
            wifiInfo.getLostTxPacketsPerSecond()
            wifiInfo.setLostTxPacketsPerSecond(0.0)
        }
    }

    class TelephonyManagerTest {
        fun nonCompliantScenarios(telephonyManager: TelephonyManager) {
            val slotIndex = 42
            telephonyManager.simSerialNumber // Noncompliant {{Using a hardware identifier puts user privacy at risk. Make sure it is safe here.}}
            telephonyManager.getSimSerialNumber() // Noncompliant
            telephonyManager.deviceId // Noncompliant {{Using a hardware identifier puts user privacy at risk. Make sure it is safe here.}}
            telephonyManager.getDeviceId() // Noncompliant
            telephonyManager.getDeviceId(slotIndex) // Noncompliant
            telephonyManager.imei // Noncompliant {{Using a hardware identifier puts user privacy at risk. Make sure it is safe here.}}
            telephonyManager.getImei() // Noncompliant
            telephonyManager.getImei(slotIndex) // Noncompliant
            telephonyManager.meid // Noncompliant {{Using a hardware identifier puts user privacy at risk. Make sure it is safe here.}}
            telephonyManager.getMeid() // Noncompliant
            telephonyManager.getMeid(slotIndex) // Noncompliant
            telephonyManager.line1Number // Noncompliant {{Using a phone number puts user privacy at risk. Make sure it is safe here.}}
            telephonyManager.getLine1Number() // Noncompliant
            telephonyManager.getLine1Number(slotIndex) // Noncompliant
        }

        fun compliantScenarios(telephonyManager: TelephonyManager) {
            telephonyManager.phoneCount
            telephonyManager.getPhoneCount()
            telephonyManager.activeModemCount
            telephonyManager.getActiveModemCount()
        }
    }

    class SubscriptionManagerTest {
        fun nonCompliantScenarios(subscriptionManager: SubscriptionManager, source: Int) {
            subscriptionManager.getPhoneNumber(1) // Noncompliant {{Using a phone number puts user privacy at risk. Make sure it is safe here.}}
            subscriptionManager.getPhoneNumber(1, source) // Noncompliant
        }

        fun compliantScenarios(subscriptionManager: SubscriptionManager) {
            subscriptionManager.getActiveSubscriptionInfoCount()
        }
    }

    class AdvertisingIdClientTest {
        fun nonCompliantScenarios(
            context: Context,
            comGoogleAdvertisingIdClientInfo: com.google.android.gms.ads.identifier.AdvertisingIdClient.Info,
            androidxAdsAdvertisingIdInfo: androidx.ads.identifier.AdvertisingIdInfo,
            holdingConnectionClient: HoldingConnectionClient,
            comHuaweiAdvertisingIdClientInfo: com.huawei.hms.ads.identifier.AdvertisingIdClient.Info,
        ) {
            comGoogleAdvertisingIdClientInfo.id // Noncompliant {{Using Advertising ID puts user privacy at risk. Make sure it is safe here.}}
            comGoogleAdvertisingIdClientInfo.getId() // Noncompliant
            ComGoogleAdvertisingIdClient.getAdvertisingIdInfo(context).id // Noncompliant

            androidxAdsAdvertisingIdInfo.id // Noncompliant {{Using Advertising ID puts user privacy at risk. Make sure it is safe here.}}
            androidxAdsAdvertisingIdInfo.getId() // Noncompliant
            var advertisingIdInfo = AndroidxAdsAdvertisingIdClient.getIdInfo(holdingConnectionClient)
            var androidxAdsAdvertisingId = advertisingIdInfo.id // Noncompliant

            comHuaweiAdvertisingIdClientInfo.id // Noncompliant {{Using Advertising ID puts user privacy at risk. Make sure it is safe here.}}
            comHuaweiAdvertisingIdClientInfo.getId() // Noncompliant
            ComHuaweiAdvertisingIdClient.getAdvertisingIdInfo(context).id // Noncompliant
        }

        fun compliantScenarios(
            comGoogleAdvertisingIdClientInfo: com.google.android.gms.ads.identifier.AdvertisingIdClient.Info,
            androidxAdsAdvertisingIdInfo: androidx.ads.identifier.AdvertisingIdInfo,
            comHuaweiAdvertisingIdClientInfo: com.huawei.hms.ads.identifier.AdvertisingIdClient.Info,
        ) {
            comGoogleAdvertisingIdClientInfo.isLimitAdTrackingEnabled
            androidxAdsAdvertisingIdInfo.providerPackageName
            comHuaweiAdvertisingIdClientInfo.isLimitAdTrackingEnabled
        }
    }

    class SettingsSecureTest {
        class CustomSettingsSecure {
            companion object {
                const val ANDROID_ID = "android_id"

                fun getString(contentResolver: ContentResolver, name: String): String? {
                    throw Exception("not the real Settings.Secure.getString")
                }
            }
        }

        fun nonCompliantScenarios(contentResolver: ContentResolver) {
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) // Noncompliant {{Using a non-resettable persistent identifier puts user privacy at risk. Make sure it is safe here.}}
            //              ^^^^^^^^^
            Settings.Secure.getString(contentResolver, (Settings.Secure.ANDROID_ID)) // Noncompliant
            Settings.Secure.getString(contentResolver, "android_id") // Noncompliant
            SettingsSecureAlias.getString(contentResolver, SettingsSecureAlias.ANDROID_ID) // Noncompliant
            val valSettingName = Settings.Secure.ANDROID_ID
            Settings.Secure.getString(contentResolver, valSettingName) // Noncompliant

            var varSettingName = Settings.Secure.ANDROID_ID
            Settings.Secure.getString(contentResolver, varSettingName) // FN
            Settings.Secure.getString(contentResolver, Settings.ANDROID_ID) // FN
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID.toString()) // FN
            val getString = Settings.Secure::getString
            getString(contentResolver, Settings.Secure.ANDROID_ID) // FN
            (Settings.Secure::getString).invoke(contentResolver, Settings.Secure.ANDROID_ID) // FN
        }

        fun compliantScenarios(contentResolver: ContentResolver) {
            Settings.Secure.getString(contentResolver, Settings.WIFI_WATCHDOG_PING_COUNT)
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID + "suffix")
            CustomSettingsSecure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            Settings.Secure.putString(contentResolver, Settings.Secure.ANDROID_ID, "new value")
        }
    }
}
