package com.sonrlabs.test.sonr;


/**
 * Process a user action initiated from the remote or the dock buttons,
 */
interface IUserActionHandler {
   /**
    * Process the action.
    * @param receivedByte byte encoding of user action.
    */
   public void processAction(int receivedByte);
}