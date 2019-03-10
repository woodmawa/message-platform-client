package com.softwood.client

interface AbstractMessagePlatformFactory {
    MessageSystemClient getMessagePlatformInstance (String platformType)
}