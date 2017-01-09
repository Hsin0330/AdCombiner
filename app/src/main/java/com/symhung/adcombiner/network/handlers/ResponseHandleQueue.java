package com.symhung.adcombiner.network.handlers;

import com.symhung.adcombiner.network.HttpClient;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by HsinHung on 2016/12/29.
 */

public class ResponseHandleQueue {

    private ResponseHandler head, tail;
    private final HttpClient.HttpRequest request;

    public ResponseHandleQueue(HttpClient.HttpRequest request) {
        this.request = request;
        head = new DefaultResponseHandler(this);
        tail = head;
    }

    public ResponseHandleQueue addResponseHandler(ResponseHandler handler) {
        if (handler != null) {
            handler.bind(this);

            ResponseHandler prev = tail;
            tail = handler;

            prev.next = tail;
            tail.prev = prev;
        }
        return this;
    }

    public ResponseHandler head() {
        return head;
    }

    public <T> void sendReadEvent(ResponseHandler<T> handler, T msg) {
        if (handler != null) {
            try {
                handler.messageReceived(msg);
            } catch (Exception e) {
                handler.exceptionCaught(e);
            }
        }
    }

    public <T> void sendExceptionCaught(ResponseHandler<T> handler, Exception e) {
        if (handler != null) {
            handler.exceptionCaught(e);
        }
    }


}
