package checks

import android.bluetooth.BluetoothAdapter

class AndroidPersistentUniqueIdentifierCheckSampleNoSemantics {

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
            bluetoothAdapterFunParam.address
            bluetoothAdapterFunParam.getAddress()
            maybeBluetoothAdapterFunParam?.address
            maybeBluetoothAdapterFunParam?.getAddress()

            val withParentheses = ((bluetoothAdapterFunParam).address)

            val localValBluetoothAdapter = bluetoothAdapterFunParam
            localValBluetoothAdapter.address
            var localVarBluetoothAdapter = bluetoothAdapterFunParam
            localVarBluetoothAdapter.address
            var inlineBluetoothAdapter = BluetoothAdapter().address
            valPropertyBluetoothAdapter.address
            varPropertyBluetoothAdapter.address

            with (bluetoothAdapterFunParam) { address }
        }

        fun compliantScenarios(
            bluetoothAdapterFunParam: BluetoothAdapter,
            notTheRealBluetoothAdapter: NotTheRealBluetoothAdapter,
        ) {
            bluetoothAdapterFunParam.state
            notTheRealBluetoothAdapter.address
        }
    }
}
