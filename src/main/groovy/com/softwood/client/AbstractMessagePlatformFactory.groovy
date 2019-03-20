package com.softwood.client

interface AbstractMessagePlatformFactory {
    MessageSystemClient getMessagePlatformInstance (String platformType)
    MessageSystemClient getMessagePlatformInstance (String platformType, String altHostname)

}