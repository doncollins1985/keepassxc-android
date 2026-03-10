package org.keepassxc.android.autofill

import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest

class KeePassXCAutofillService : AutofillService() {
    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        // Prototype: In a full app, we would parse the ViewNode tree from the FillRequest
        // and search our unlocked database for matching credentials to return in a FillResponse.
        callback.onSuccess(null)
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // Prototype: Save flow
        callback.onSuccess()
    }
}
