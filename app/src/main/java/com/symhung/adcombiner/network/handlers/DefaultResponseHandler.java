package com.symhung.adcombiner.network.handlers;

/**
 * Created by HsinHung on 2016/12/29.
 */

public class DefaultResponseHandler extends ResponseHandler<String> {

    public DefaultResponseHandler(ResponseHandleQueue responseHandleQueue) {
        super(responseHandleQueue);
    }

    @Override
    public void messageReceived(String msg) throws Exception {
        if (next != null) {
            responseHandleQueue.sendReadEvent(next, msg);
        }
    }
}
