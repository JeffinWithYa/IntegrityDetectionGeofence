/* Copyright (c) 2011-2016 BlackBerry Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blackberry.bidlocation.bidhelper;

/**
 * Exception thrown to indicate a failure during a request to the BID
 * content provider.
 */
public final class BidRequestException extends Exception {
    /**
     * Create a new BidRequestException.
     *
     * @param message The error message.
     */
    public BidRequestException(String message) {
        super(message);
    }

    /**
     * Create a new BidRequestException wrapping an
     * existing exception.
     * <p/>
     * The specified exception will be embedded in the new
     * one, and its message will become the default message.
     *
     * @param cause The exception to be wrapped.
     */
    public BidRequestException(Throwable cause) {
        super(cause);
    }
}