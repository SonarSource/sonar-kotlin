package checks

import android.app.Activity
import android.app.Application
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

class MyActivity2 : Activity() {
    fun bad(br: BroadcastReceiver?, filter: IntentFilter?) {
        activity.registerReceiver(br, filter) // Noncompliant
    }

    val activity: Activity
        get() = this
}

class SomeApplication2 : Application() {
    fun bad(br: BroadcastReceiver?, filter: IntentFilter?) {
        application.registerReceiver(br, filter) // Noncompliant
    }

    val application: Application
        get() = this
}
