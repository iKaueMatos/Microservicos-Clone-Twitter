package br.com.souza.twitterclone.notifications.handler.exceptions;

import br.com.souza.twitterclone.notifications.enums.InternalTypeErrorCodesEnum;

public class UserNotFoundException extends ErrorCodeException{

    public UserNotFoundException(){
        super(InternalTypeErrorCodesEnum.E410000);
    }

    public UserNotFoundException(String message){
        super(InternalTypeErrorCodesEnum.E410000, message);
    }
}
