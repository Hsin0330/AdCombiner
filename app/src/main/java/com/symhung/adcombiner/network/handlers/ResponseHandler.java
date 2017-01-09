package com.symhung.adcombiner.network.handlers;

/**
 * Created by HsinHung on 2016/12/29.
 */

public abstract class ResponseHandler<S> {

    ResponseHandler prev, next;
    ResponseHandleQueue responseHandleQueue;

    public ResponseHandler() {

    }

    ResponseHandler(ResponseHandleQueue responseHandleQueue) {
        this.responseHandleQueue = responseHandleQueue;
    }

    void bind(ResponseHandleQueue responseHandleQueue) {
        this.responseHandleQueue = responseHandleQueue;
    }

    public <T> void writeNext(T msg) {
        responseHandleQueue.sendReadEvent(next, msg);
    }

    public void throwException(Exception e) {
        responseHandleQueue.sendExceptionCaught(next, e);
    }

    public void exceptionCaught(Exception e) {
        throwException(e);
    }

    public abstract void messageReceived(S msg) throws Exception;
}
