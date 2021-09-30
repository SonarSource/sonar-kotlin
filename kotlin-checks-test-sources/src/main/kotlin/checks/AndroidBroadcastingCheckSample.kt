package checks

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.UserHandle
import android.support.annotation.RequiresApi


class MyIntentBroadcast {
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun broadcast(
        intent: Intent?, context: Context, user: UserHandle?,
        resultReceiver: BroadcastReceiver?, scheduler: Handler?, initialCode: Int,
        initialData: String?, initialExtras: Bundle?,
        broadcastPermission: String?
    ) {
        context.sendBroadcast(intent) // Noncompliant {{Make sure that broadcasting intents is safe here.}}
        context.sendBroadcastAsUser(intent, user) // Noncompliant

        // Broadcasting intent with "null" for receiverPermission
        context.sendBroadcast(intent, null) // Noncompliant
        context.sendBroadcastAsUser(intent, user, null) // Noncompliant
        context.sendOrderedBroadcast(intent, null) // Noncompliant
        context.sendOrderedBroadcastAsUser( // Noncompliant
//              ^^^^^^^^^^^^^^^^^^^^^^^^^^
            intent,
            user,
            null,
            resultReceiver,
            scheduler,
            initialCode,
            initialData,
            initialExtras
        )
        context.sendStickyBroadcast(intent) // Noncompliant
        context.sendStickyBroadcastAsUser(intent, user) // Noncompliant
        context.sendStickyOrderedBroadcast( // Noncompliant
            intent,
            resultReceiver,
            scheduler,
            initialCode,
            initialData,
            initialExtras
        )
        context.sendStickyOrderedBroadcastAsUser( // Noncompliant
            intent,
            user,
            resultReceiver,
            scheduler,
            initialCode,
            initialData,
            initialExtras
        )


        context.sendBroadcast(intent, broadcastPermission) // Ok
        context.sendBroadcastAsUser(intent, user, broadcastPermission) // Ok
        context.sendOrderedBroadcast(intent, broadcastPermission) // Ok
        context.sendOrderedBroadcastAsUser(
            intent, user, broadcastPermission, resultReceiver,
            scheduler, initialCode, initialData, initialExtras
        ) // Ok
    }
}

class MyActivity : Activity() {
    fun doSomething() {
        val KEY = "admin"
        val SENSITIVE_DATA = ""
        val intent = Intent("android.intent.action.sticky.broadcast")
        intent.putExtra(KEY, SENSITIVE_DATA)
        this.sendStickyBroadcast(intent) // Noncompliant
    }
}

class SomeApplication : Application() {
    fun doSomething() {
        val KEY = "admin"
        val SENSITIVE_DATA = ""
        val intent = Intent("android.intent.action.sticky.broadcast")
        intent.putExtra(KEY, SENSITIVE_DATA)
        this.sendStickyBroadcast(intent) // Noncompliant
    }
}
