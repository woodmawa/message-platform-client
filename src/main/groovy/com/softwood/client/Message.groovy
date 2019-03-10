package com.softwood.client

interface Message {

    void setHeaders (Map headers)
    void setMessage (String)
    String getMessage ()
}