package checks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.support.annotation.RequiresApi

class MyIntentReceiver {
    @RequiresApi(api = Build.VERSION_CODES.O)
    fun register(
        context: Context, receiver: BroadcastReceiver?,
        filter: IntentFilter?,
        scheduler: Handler?,
        flags: Int,
        broadcastPermission: String,
    ) {
        context.registerReceiver(receiver, filter) // Noncompliant
        context.registerReceiver(receiver, filter, flags) // Noncompliant

        // Broadcasting intent with "null" for broadcastPermission
        context.registerReceiver(receiver, filter, null, scheduler) // Noncompliant
        context.registerReceiver(receiver, filter, null, scheduler, flags) // Noncompliant {{Make sure that intents are received safely here.}}
//              ^^^^^^^^^^^^^^^^

        context.registerReceiver(receiver, filter, broadcastPermission, scheduler)
        context.registerReceiver(receiver, filter, broadcastPermission, scheduler, flags)

    }
}
